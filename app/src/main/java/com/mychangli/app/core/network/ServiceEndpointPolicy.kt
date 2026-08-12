package com.mychangli.app.core.network

import com.mychangli.app.BuildConfig
import java.io.IOException
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

class ServiceUnavailableException(
    override val message: String = "服务暂未开放",
) : IOException(message)

object ServiceEndpointPolicy {
    fun isConfigured(url: String): Boolean {
        val host = url.toHttpUrlOrNull()?.host ?: return false
        return host.isNotBlank() && !host.endsWith(".invalid", ignoreCase = true)
    }

    fun isCampusToolsConfigured(): Boolean =
        isConfigured(BuildConfig.CAMPUS_TOOLS_API_BASE_URL)

}

/** Prevents placeholder hosts from ever reaching DNS or the network stack. */
class UnavailableEndpointInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        if (chain.request().url.host.endsWith(".invalid", ignoreCase = true)) {
            throw ServiceUnavailableException()
        }
        return chain.proceed(chain.request())
    }
}
