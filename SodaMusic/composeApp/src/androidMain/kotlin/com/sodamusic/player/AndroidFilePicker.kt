package com.sodamusic.player

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference

object AndroidFilePicker {
    private var activityRef: WeakReference<Activity>? = null
    private var deferred: CompletableDeferred<String?>? = null
    private var launcher: ActivityResultLauncher<Array<String>>? = null

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
    }

    fun detach() {
        launcher?.unregister()
        launcher = null
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
}
