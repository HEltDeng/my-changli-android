package com.mychangli.app.core.security

import com.mychangli.app.feature.mooc.data.expireMoocCookies
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.*
import org.junit.Test

class MoocCookieScopeTest {
    @Test fun moocExpiryPreservesEducationAndSsoSessions() {
        val hosts = listOf("pt.csust.edu.cn", "xk.csust.edu.cn", "authserver.csust.edu.cn", "ehall.csust.edu.cn")
        val data = hosts.associateWith { host -> listOf(Cookie.Builder().name("session")
            .value("synthetic").hostOnlyDomain(host).path("/meol").build()) }.toMutableMap()
        val original = data.toMap()
        val writtenHosts = mutableListOf<String>()
        val jar = object : CookieJar {
            override fun loadForRequest(url: HttpUrl) = data[url.host].orEmpty()
            override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
                writtenHosts += url.host
                cookies.forEach { cookie ->
                    assertEquals(original[url.host]!!.first().name, cookie.name)
                    assertEquals(original[url.host]!!.first().domain, cookie.domain)
                    assertEquals(original[url.host]!!.first().path, cookie.path)
                    assertEquals(1L, cookie.expiresAt)
                }
                data[url.host] = cookies.filter { it.expiresAt > System.currentTimeMillis() }
            }
        }
        expireMoocCookies(jar)
        assertEquals(listOf("pt.csust.edu.cn"), writtenHosts)
        assertTrue(data["pt.csust.edu.cn"]!!.isEmpty())
        hosts.drop(1).forEach { assertEquals(original[it], data[it]) }
    }
}
