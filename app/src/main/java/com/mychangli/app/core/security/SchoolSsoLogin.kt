package com.mychangli.app.core.security

import com.dcelysia.csust_spider.core.AESUtils
import com.dcelysia.csust_spider.core.Resource
import com.dcelysia.csust_spider.core.RetrofitUtils
import com.dcelysia.csust_spider.mooc.data.remote.api.SSOAuthApi
import com.dcelysia.csust_spider.mooc.data.remote.api.SSOEhallApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.flow
import org.jsoup.Jsoup
import java.net.URI
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/** School binding depends on SSO and education; MOOC has its own optional session. */
object SchoolSsoLogin {
    private val authenticator by lazy {
        SchoolSsoAuthenticator(RetrofitSchoolSsoTransport(), AESUtils::encryptPassword)
    }

    fun login(studentId: String, password: String) = flow {
        emit(Resource.Loading())
        emit(authenticator.authenticate(studentId, password))
    }
}

internal data class SchoolSsoPage(val code: Int, val url: String, val html: String)

internal interface SchoolSsoTransport {
    suspend fun loginPage(): SchoolSsoPage
    suspend fun captchaRequired(studentId: String): Boolean
    suspend fun submit(studentId: String, encryptedPassword: String, execution: String): SchoolSsoPage
    suspend fun currentAccount(): String?
}

internal class SchoolSsoAuthenticator(
    private val transport: SchoolSsoTransport,
    private val encrypt: (String, String?) -> String
) {
    suspend fun authenticate(studentId: String, password: String): Resource<Boolean> {
        var stage = "登录页面"
        try {
            val page = transport.loginPage()
            if (page.code !in 200..299) return httpError(stage, page.code)
            if (!isEhall(page.url)) {
                if (!isAuthserver(page.url)) return Resource.Error("学校认证跳转异常，请稍后重试。")
                val document = Jsoup.parse(page.html)
                val salt = document.selectFirst("input#pwdEncryptSalt")?.attr("value").orEmpty()
                val execution = document.selectFirst("input[name=execution]")?.attr("value").orEmpty()
                if (salt.isBlank() || execution.isBlank()) {
                    return Resource.Error("学校登录页面发生变化，请尝试网页登录。")
                }
                stage = "验证码检查"
                if (transport.captchaRequired(studentId)) {
                    return Resource.Error("请在手机网页登录一次，完成学校验证码验证。")
                }
                stage = "身份验证"
                val submitted = transport.submit(studentId, encrypt(password, salt), execution)
                if (submitted.code !in 200..299) return httpError(stage, submitted.code)
                if (!isEhall(submitted.url)) {
                    return Resource.Error("学校统一认证未通过，请确认统一认证密码；需要验证码时请选择网页登录。")
                }
            }
            stage = "学校账号核对"
            if (transport.currentAccount() != studentId) {
                return Resource.Error("学校账号信息未核对成功，请稍后重试或使用网页登录。")
            }
            return Resource.Success(true)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: UnknownHostException) {
            return Resource.Error("学校${stage}域名暂时无法解析，请稍后重试。")
        } catch (_: SocketTimeoutException) {
            return Resource.Error("学校${stage}连接超时，请稍后重试。")
        } catch (_: SSLException) {
            return Resource.Error("学校${stage}安全连接失败，请稍后重试。")
        } catch (_: Exception) {
            // Never expose response bodies, credentials, ticket URLs or exception messages.
            return Resource.Error("学校${stage}暂时不可用，请稍后重试。")
        }
    }

    private fun httpError(stage: String, code: Int): Resource<Boolean> =
        Resource.Error("学校${stage}暂时不可用（HTTP $code），请稍后重试。")

    private fun trustedHttps(url: String, host: String): Boolean = runCatching {
        val uri = URI(url)
        uri.scheme == "https" && uri.host == host && uri.userInfo == null &&
            (uri.port == -1 || uri.port == 443)
    }.getOrDefault(false)

    private fun isEhall(url: String) = trustedHttps(url, "ehall.csust.edu.cn")
    private fun isAuthserver(url: String) = trustedHttps(url, "authserver.csust.edu.cn")
}

private class RetrofitSchoolSsoTransport : SchoolSsoTransport {
    private val auth by lazy { RetrofitUtils.instanceSSOAuth.create(SSOAuthApi::class.java) }
    private val ehall by lazy { RetrofitUtils.instanceSSOEhall.create(SSOEhallApi::class.java) }

    override suspend fun loginPage() = auth.getLoginForm().let {
        SchoolSsoPage(it.code(), it.raw().request.url.toString(), it.body().orEmpty())
    }

    override suspend fun captchaRequired(studentId: String): Boolean {
        val response = auth.checkNeedCaptcha(studentId, System.currentTimeMillis())
        check(response.isSuccessful) { "Captcha endpoint unavailable" }
        return checkNotNull(response.body()).isNeed
    }

    override suspend fun submit(studentId: String, encryptedPassword: String, execution: String) =
        auth.login(username = studentId, password = encryptedPassword, execution = execution).let {
            SchoolSsoPage(it.code(), it.raw().request.url.toString(), it.body().orEmpty())
        }

    override suspend fun currentAccount(): String? {
        val response = ehall.getLoginUser()
        check(response.isSuccessful) { "School identity endpoint unavailable" }
        return response.body()?.data?.userAccount
    }
}
