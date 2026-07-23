package com.sodamusic.player.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sodamusic.player.model.Track

@Composable
fun TrackPicker(
    onPickLocal: (String) -> Unit,
    onPickOnline: (String) -> Unit,
    demoTracks: List<Track>,
    onSelectTrack: (Track) -> Unit,
    onDismiss: () -> Unit
) {
    var mode by remember { mutableStateOf<Tab>(Tab.Demos) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择音乐源", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TabButton("示例", Icons.Default.MusicNote, selected = mode == Tab.Demos) { mode = Tab.Demos }
                    TabButton("本地文件", Icons.Default.Folder, selected = mode == Tab.Local) { mode = Tab.Local }
                    TabButton("在线", Icons.Default.Language, selected = mode == Tab.Online) { mode = Tab.Online }
                }
                Spacer(Modifier.height(12.dp))
                when (mode) {
                    Tab.Demos -> {
                        LazyColumn(modifier = Modifier.height(280.dp)) {
                            items(demoTracks) { track ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onSelectTrack(track) }
                                        .padding(vertical = 10.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp))
                                            .background(Color(track.coverColor)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        // Cover tiles are saturated brand colors; white icon always reads.
                                        Icon(Icons.Default.MusicNote, null, tint = Color.White)
                                    }
                                    Spacer(Modifier.size(10.dp))
                                    Column {
                                        Text(
                                            track.title,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            track.artist,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Tab.Local -> {
                        var path by remember { mutableStateOf("") }
                        OutlinedTextField(
                            value = path,
                            onValueChange = { path = it },
                            label = { Text("本地文件路径 (如 /path/to/song.mp3)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { if (path.isNotBlank()) onPickLocal(path.trim()) }) {
                            Text("加载并播放")
                        }
                    }
                    Tab.Online -> {
                        var url by remember { mutableStateOf("") }
                        OutlinedTextField(
                            value = url,
                            onValueChange = { url = it },
                            label = { Text("在线音频 URL (http/https)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { if (url.isNotBlank()) onPickOnline(url.trim()) }) {
                            Text("连接并播放")
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } }
    )
}

@Composable
private fun TabButton(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
    }
    val iconTint = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val textColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = iconTint, modifier = Modifier.size(16.dp))
            Spacer(Modifier.size(4.dp))
            Text(label, fontSize = 13.sp, color = textColor)
        }
    }
}

private enum class Tab { Demos, Local, Online }
