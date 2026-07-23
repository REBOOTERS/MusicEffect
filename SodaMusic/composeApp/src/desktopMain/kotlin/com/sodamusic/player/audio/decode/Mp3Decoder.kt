package com.sodamusic.player.audio.decode

import javazoom.jl.decoder.Bitstream
import javazoom.jl.decoder.Header
import javazoom.jl.decoder.Obuffer
import java.io.BufferedInputStream
import java.io.InputStream
import java.io.PushbackInputStream
import java.nio.file.Path
import kotlin.io.path.inputStream
import kotlin.math.min

/**
 * Decodes an MP3 file to interleaved-stereo 16-bit PCM ShortArray using JLayer (pure Java).
 *
 * We install a custom [Obuffer] via [javazoom.jl.decoder.Decoder.setOutputBuffer] and stream
 * [Obuffer.append] calls into a growing ShortArray. JLayer calls append(channel, short) for
 * each sample after float→short conversion; L is always delivered before R within a frame.
 *
 * For mono sources, we duplicate L into R so downstream code can assume stereo output.
 */
object Mp3Decoder {
    data class Result(
        val samples: ShortArray,
        val sampleRate: Int,
        val channels: Int,
        val durationMs: Long
    )

    fun decode(path: Path): Result? {
        var sampleRate = 0
        var sourceChannels = 0
        var framesDecoded = 0
        var maxAbs = 0
        val collected = ArrayList<Short>(1 shl 20)
        // Interleave buffer: we collect L samples then back-fill R slots.
        var leftCursor = 0

        try {
            val raw = skipId3v2(path.inputStream().buffered())
            val guarded = object : InputStream() {
                override fun read() = raw.read()
                override fun read(b: ByteArray, off: Int, len: Int) = raw.read(b, off, len)
                override fun available() = raw.available()
                override fun close() { /* guard */ }
                fun reallyClose() = raw.close()
            }

            val bitstream = Bitstream(guarded)
            val decoder = javazoom.jl.decoder.Decoder()
            val ob = object : Obuffer() {
                override fun append(channel: Int, value: Short) {
                    val v = value.toInt()
                    val av = if (v < 0) -v else v
                    if (av > maxAbs) maxAbs = av
                    if (channel == 0) {
                        collected.add(value)
                        collected.add(0) // placeholder for right
                    } else {
                        if (leftCursor < collected.size) collected[leftCursor + 1] = value
                        leftCursor += 2
                    }
                }
                override fun appendSamples(channel: Int, f: FloatArray) {
                    for (s in f) append(channel, (s * 32767f).toInt().coerceIn(-32768, 32767).toShort())
                }
                override fun clear_buffer() {
                    // After a frame is fully delivered, reset the cursor to point to the
                    // next (still-empty) L slot. This is necessary because JLayer calls
                    // clear_buffer() between frames; without this, leftCursor drifts.
                    leftCursor = collected.size
                }
                override fun close() {}
                override fun write_buffer(v: Int) {}
                override fun set_stop_flag() {}
            }
            decoder.setOutputBuffer(ob)

            while (true) {
                val header: Header = bitstream.readFrame() ?: break
                if (sampleRate == 0) {
                    sampleRate = header.frequency()
                    sourceChannels = if (header.mode() == Header.SINGLE_CHANNEL) 1 else 2
                }
                decoder.decodeFrame(header, bitstream)
                framesDecoded++
                bitstream.closeFrame()
            }
            guarded.reallyClose()

            if (sampleRate <= 0 || collected.isEmpty()) {
                return null
            }

            val samples = collected.toShortArray()
            if (sourceChannels == 1) {
                var i = 1
                while (i < samples.size) { samples[i] = samples[i - 1]; i += 2 }
            }
            val totalPerChannel = samples.size / 2
            val durationMs = totalPerChannel * 1000L / sampleRate
            return Result(samples, sampleRate, 2, durationMs)
        } catch (t: Throwable) {
            return null
        }
    }

    private fun skipId3v2(inputStream: BufferedInputStream): BufferedInputStream {
        val pbis = PushbackInputStream(inputStream, 10)
        val head = ByteArray(10)
        val readN = pbis.read(head)
        if (readN < 10) {
            if (readN > 0) pbis.unread(head, 0, readN)
            return BufferedInputStream(pbis)
        }
        if (head[0] == 'I'.code.toByte() && head[1] == 'D'.code.toByte() && head[2] == '3'.code.toByte()) {
            val size = ((head[6].toInt() and 0x7F) shl 21) or
                    ((head[7].toInt() and 0x7F) shl 14) or
                    ((head[8].toInt() and 0x7F) shl 7) or
                    (head[9].toInt() and 0x7F)
            var remaining = size.toLong()
            val skipBuf = ByteArray(8192)
            while (remaining > 0) {
                val skipped = pbis.read(skipBuf, 0, minOf(skipBuf.size.toLong(), remaining).toInt())
                if (skipped < 0) break
                remaining -= skipped
            }
            return BufferedInputStream(pbis)
        }
        pbis.unread(head)
        return BufferedInputStream(pbis)
    }
}
