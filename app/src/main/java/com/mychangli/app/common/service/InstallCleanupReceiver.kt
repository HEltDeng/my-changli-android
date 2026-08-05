package com.mychangli.app.common.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.io.File

class InstallCleanupReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        runCatching {
            listOfNotNull(context.externalCacheDir, context.cacheDir)
                .distinct()
                .forEach { cacheRoot ->
                    val apkFile = File(File(cacheRoot, "updates"), DownloadService.APK_FILE_NAME)
                    if (apkFile.exists()) apkFile.delete()
                }
        }
    }
}
