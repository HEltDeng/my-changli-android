package com.mychangli.app.core.web

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent

object PublicWebsiteNavigator {
    const val HOME_URL = "https://mychangliapp.cn"
    const val DOWNLOAD_URL = "https://mychangliapp.cn/download.html"
    const val PRIVACY_URL = "https://mychangliapp.cn/privacy.html"
    const val TERMS_URL = "https://mychangliapp.cn/terms.html"

    /** Opens the public home page in the app's restricted website container. */
    fun openHomeInApp(context: Context): Boolean = runCatching {
        context.startActivity(
            OfficialWebsiteActivity.createIntent(context, HOME_URL).apply {
                if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
        )
        true
    }.getOrDefault(false)

    /** Keeps policy and download pages in the existing browser-backed flow. */
    fun open(context: Context, url: String): Boolean {
        val uri = Uri.parse(url)
        if (uri.scheme != "https" || uri.host != "mychangliapp.cn") return false
        return runCatching {
            CustomTabsIntent.Builder()
                .setShowTitle(true)
                .build()
                .launchUrl(context, uri)
            true
        }.getOrElse {
            runCatching {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, uri).addCategory(Intent.CATEGORY_BROWSABLE)
                )
                true
            }.getOrDefault(false)
        }
    }
}
