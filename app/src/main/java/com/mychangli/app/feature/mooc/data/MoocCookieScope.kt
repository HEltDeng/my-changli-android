package com.mychangli.app.feature.mooc.data

import okhttp3.CookieJar
import okhttp3.HttpUrl.Companion.toHttpUrl

/** PersistentCookieJar stores all cookies under the exact response host. */
internal fun expireMoocCookies(cookieJar: CookieJar) {
    val url = "http://pt.csust.edu.cn/".toHttpUrl()
    val expired = cookieJar.loadForRequest(url).map { it.newBuilder().expiresAt(1L).build() }
    cookieJar.saveFromResponse(url, expired)
}
