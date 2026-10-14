package com.mychangli.app.core.security

import com.dcelysia.csust_spider.core.Resource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.net.SocketTimeoutException

class SchoolSsoAuthenticatorTest {
    private val account = "synthetic-student"
    private val password = "synthetic-password"

    private inner class Transport : SchoolSsoTransport {
        var page = SchoolSsoPage(200, "https://authserver.csust.edu.cn/authserver/login",
            "<input id='pwdEncryptSalt' value='synthetic-salt'><input name='execution' value='form-id'>")
        var result = SchoolSsoPage(200, "https://ehall.csust.edu.cn/index.html", "")
        var identity: String? = account
        var captcha = false
        var submitted = false
        var failure: Exception? = null
        override suspend fun loginPage(): SchoolSsoPage { failure?.let { throw it }; return page }
        override suspend fun captchaRequired(studentId: String) = captcha
        override suspend fun submit(studentId: String, encryptedPassword: String, execution: String): SchoolSsoPage {
            assertEquals(account, studentId)
            assertEquals("encrypted", encryptedPassword)
            assertEquals("form-id", execution)
            submitted = true
            return result
        }
        override suspend fun currentAccount() = identity
    }

    private fun auth(t: Transport) = SchoolSsoAuthenticator(t) { value, salt ->
        assertEquals(password, value)
        assertEquals("synthetic-salt", salt)
        "encrypted"
    }

    @Test fun schoolAuthenticationCompletesWithoutAnyMoocEndpoint() = runBlocking {
        val t = Transport()
        assertTrue(auth(t).authenticate(account, password) is Resource.Success)
        assertTrue(t.submitted)
    }

    @Test fun existingSessionStillRequiresExactAccountMatch() = runBlocking {
        val t = Transport().apply { page = result; identity = "another-student" }
        assertTrue(auth(t).authenticate(account, password) is Resource.Error)
        assertFalse(t.submitted)
    }

    @Test fun missingIdentityCannotBecomeSuccessfulBinding() = runBlocking {
        val t = Transport().apply { identity = null }
        assertTrue(auth(t).authenticate(account, password) is Resource.Error)
    }

    @Test fun captchaStopsPasswordSubmission() = runBlocking {
        val t = Transport().apply { captcha = true }
        val result = auth(t).authenticate(account, password) as Resource.Error
        assertTrue(result.msg.contains("请在手机网页登录一次"))
        assertFalse(t.submitted)
    }

    @Test fun errorPageCannotBeMistakenForLoginForm() = runBlocking {
        val t = Transport().apply { page = page.copy(html = "maintenance") }
        assertTrue(auth(t).authenticate(account, password) is Resource.Error)
        assertFalse(t.submitted)
    }

    @Test fun lookalikeEhallHostnameCannotAuthenticate() = runBlocking {
        val t = Transport().apply { result = result.copy(url = "https://ehall.csust.edu.cn.attacker.invalid/index.html") }
        assertTrue(auth(t).authenticate(account, password) is Resource.Error)
    }

    @Test fun rejectedPasswordCannotAuthenticate() = runBlocking {
        val t = Transport().apply { result = page }
        assertTrue(auth(t).authenticate(account, password) is Resource.Error)
    }

    @Test fun unavailableSchoolResponseIdentifiesStageWithoutSecrets() = runBlocking {
        val t = Transport().apply { result = result.copy(code = 503, html = password) }
        val result = auth(t).authenticate(account, password) as Resource.Error
        assertTrue(result.msg.contains("身份验证"))
        assertTrue(result.msg.contains("503"))
        assertFalse(result.msg.contains(password))
    }

    @Test fun timeoutDoesNotExposeExceptionContents() = runBlocking {
        val t = Transport().apply { failure = SocketTimeoutException(password) }
        val result = auth(t).authenticate(account, password) as Resource.Error
        assertTrue(result.msg.contains("超时"))
        assertFalse(result.msg.contains(password))
    }

    @Test(expected = CancellationException::class)
    fun cancellationIsNotConvertedToLoginFailure() = runBlocking {
        val t = Transport().apply { failure = CancellationException("cancel") }
        auth(t).authenticate(account, password)
        Unit
    }
}
