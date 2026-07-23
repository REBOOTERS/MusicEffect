package com.sodamusic.player

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference

object AndroidFilePicker {
    private var activityRef: WeakReference<Activity>? = null
    private var deferred: CompletableDeferred<String?>? = null
    private var launcher: ActivityResultLauncher<Array<String>>? = null

    private var permDeferred: CompletableDeferred<Boolean>? = null
    private var permLauncher: ActivityResultLauncher<String>? = null

    fun attach(activity: ComponentActivity) {
        activityRef = WeakReference(activity)
        launcher = activity.activityResultRegistry.register(
            "soda-music-audio-picker",
            activity,
            ActivityResultContracts.OpenDocument()
        ) { uri: Uri? ->
            val result = uri?.toString()
            val d = deferred
            deferred = null
            if (result != null) {
                try {
                    activity.contentResolver.takePersistableUriPermission(
                        uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (ignore: Exception) {}
            }
            d?.complete(result)
        }
        permLauncher = activity.activityResultRegistry.register(
            "soda-music-audio-permission",
            activity,
            ActivityResultContracts.RequestPermission()
        ) { granted: Boolean ->
            val d = permDeferred
            permDeferred = null
            d?.complete(granted)
        }
    }

    fun detach() {
        launcher?.unregister()
        permLauncher?.unregister()
        launcher = null
        permLauncher = null
        activityRef?.clear()
        activityRef = null
    }

    suspend fun pick(): String? = withContext(Dispatchers.Main) {
        val completable = CompletableDeferred<String?>()
        deferred = completable
        val l = launcher
        if (l == null) {
            deferred = null
            completable.complete(null)
        } else {
            l.launch(arrayOf("audio/*"))
        }
        completable.await()
    }

    /**
     * Ensures we can read the device music library. Requests READ_MEDIA_AUDIO on API 33+
     * and READ_EXTERNAL_STORAGE below. Returns true if access is already granted or the
     * user approves; false on denial (caller should fall back to the SAF file picker).
     */
    suspend fun ensureAudioPermission(): Boolean {
        val activity = activityRef?.get() ?: return false
        val perm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        if (ContextCompat.checkSelfPermission(activity, perm) == PackageManager.PERMISSION_GRANTED) {
            return true
        }
        return withContext(Dispatchers.Main) {
            val completable = CompletableDeferred<Boolean>()
            permDeferred = completable
            val l = permLauncher
            if (l == null) {
                permDeferred = null
                completable.complete(false)
            } else {
                l.launch(perm)
            }
            completable.await()
        }
    }
}
