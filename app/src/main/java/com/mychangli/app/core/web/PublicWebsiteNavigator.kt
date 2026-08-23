package com.mychangli.app.core.web

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent

object PublicWebsiteNavigator {
    const val HOME_URL = "https://mychangliapp.cn"
    const val DOWNLOAD_URL = "https://mychangliapp.cn/download.html"
    const val PRIVACY_URL = "https://mychangliapp.cn/privacy.html"
    const val TERMS_URL = "https://mychangliapp.cn/terms.html"

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
