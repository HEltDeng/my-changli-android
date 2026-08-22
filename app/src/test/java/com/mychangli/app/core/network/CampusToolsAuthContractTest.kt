package com.mychangli.app.core.network

import com.mychangli.app.core.network.campustools.ApiEnvelope
import com.mychangli.app.core.network.campustools.AuthData
import com.mychangli.app.core.network.campustools.DeviceAuthRequest
import com.mychangli.app.core.network.campustools.LoginRequest
import com.mychangli.app.core.network.campustools.RegisterRequest
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CampusToolsAuthContractTest {
    private val gson = Gson()

    @Test
    fun deviceIdentityUsesOnlyTheOpaqueInstallationIdentifier() {
        val request = gson.toJsonTree(DeviceAuthRequest("installation-id")).asJsonObject
        assertEquals(setOf("installationId"), request.keySet())
        assertFalse(request.has("phone"))
        assertFalse(request.has("password"))
    }

    @Test
    fun registrationAndLoginRequestsUseBackendContractWithoutCredentialLoggingFields() {
        val register = gson.toJsonTree(
            RegisterRequest("19100000000", "local-test-password", "长理同学", "installation-id")
        ).asJsonObject
        assertEquals(setOf("phone", "password", "nickname", "installationId"), register.keySet())

        val login = gson.toJsonTree(
            LoginRequest("19100000000", "local-test-password", "installation-id")
        ).asJsonObject
        assertEquals(setOf("phone", "password", "installationId"), login.keySet())
        assertFalse(login.has("studentId"))
        assertFalse(login.has("schoolPassword"))
        assertFalse(login.has("cookie"))
    }

    @Test
    fun successfulAuthEnvelopeRestoresPhoneAccountAndOpaqueToken() {
        val envelope = gson.fromJson(
            """
            {"ok":true,"data":{"token":"opaque-local-token","user":{
              "id":"internal-user-id","role":"USER","accountType":"PHONE",
              "nickname":"长理同学","phoneMasked":"191****0000","accountStatus":"ACTIVE"
            }}}
            """.trimIndent(),
            ApiEnvelope::class.java,
        )
        assertTrue(envelope.ok)

        val typed = gson.fromJson(
            gson.toJson(envelope.data),
            AuthData::class.java,
        )
        assertEquals("internal-user-id", typed.user.id)
        assertEquals("PHONE", typed.user.accountType)
        assertEquals("opaque-local-token", typed.token)
    }
}
