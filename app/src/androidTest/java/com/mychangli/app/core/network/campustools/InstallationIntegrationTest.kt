package com.mychangli.app.core.network.campustools

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.mychangli.app.BuildConfig
import com.mychangli.app.core.privacy.PrivacyConsentManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.Executors
import java.util.concurrent.Callable

/** Run only against the isolated loopback QA server, never a production endpoint. */
@RunWith(AndroidJUnit4::class)
class InstallationIntegrationTest {
    @Test fun stableIdentityConsentAndWithdrawal() = runBlocking {
        assumeTrue(BuildConfig.DEBUG && BuildConfig.CAMPUS_TOOLS_API_BASE_URL.startsWith("http://127.0.0.1:"))
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val pool = Executors.newFixedThreadPool(8)
        val ids = try { pool.invokeAll((1..80).map { Callable { InstallationIdentity.id } }).map { it.get() }.toSet() }
            finally { pool.shutdown() }
        assertEquals(1, ids.size)
        val original = ids.single()
        PrivacyConsentManager.acceptAppConsent(context)
        InstallationTelemetry.decide(false)
        assertFalse(InstallationTelemetry.enabled())
        assertNull(DeviceIdentity.deviceScopedId(context))
        CampusToolsSession.restore()
        assertEquals(original, CampusToolsSession.installationId)
        val enrollment = CampusToolsApiClient.unauthenticated.deviceAuth(DeviceAuthRequest(
            installationId = InstallationIdentity.id, installationSecret = InstallationIdentity.secret,
            versionName = BuildConfig.VERSION_NAME, versionCode = BuildConfig.VERSION_CODE))
        assertTrue("Isolated enrollment must succeed", enrollment.ok)
        InstallationTelemetry.decide(true)
        val preferences = context.getSharedPreferences("optional_device_statistics_v1", 0)
        val deadline = System.currentTimeMillis() + 20000
        while (!preferences.getBoolean("activated", false) && System.currentTimeMillis() < deadline) {
            kotlinx.coroutines.delay(100)
        }
        assertTrue("Activation must reach the isolated backend", preferences.getBoolean("activated", false))
        repeat(15) { InstallationTelemetry.foreground() }
        assertEquals(original, InstallationIdentity.id)
        CampusToolsSession.logout()
        assertEquals(original, InstallationIdentity.id)
        InstallationTelemetry.decide(false)
        kotlinx.coroutines.delay(500)
        val withdrawalDeadline = System.currentTimeMillis() + 20000
        while (preferences.getBoolean("withdraw_pending", false) && System.currentTimeMillis() < withdrawalDeadline) {
            kotlinx.coroutines.delay(100)
        }
        assertFalse(preferences.getBoolean("withdraw_pending", false))
        assertFalse(InstallationTelemetry.enabled())
        assertNull(DeviceIdentity.deviceScopedId(context))
        assertEquals(original, InstallationIdentity.id)
    }
}
