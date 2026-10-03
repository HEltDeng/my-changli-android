package com.mychangli.app.core.privacy

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.mychangli.app.BuildConfig
import com.mychangli.app.core.MainActivity
import com.mychangli.app.core.network.campustools.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import java.io.File

/** Isolated transport test. School success callback is synthetic, not an official school login. */
class AutomaticSyncAcceptanceTest {
    @get:Rule val compose=createEmptyComposeRule()
    private val context get()=InstrumentationRegistry.getInstrumentation().targetContext
    private fun fault(on:Boolean) { java.net.URL("http://127.0.0.1:8787/_qa/school-failure/"+(if(on) "on" else "off")).openConnection().apply { connectTimeout=5000;readTimeout=5000 }.getInputStream().use { it.readBytes() } }
    private fun until(check:()->Boolean) { val end=System.currentTimeMillis()+25000;while(!check() && System.currentTimeMillis()<end) android.os.SystemClock.sleep(100);assertTrue("Sync timeout: "+SchoolBindingSync.lastFailure,check()) }
    @Test fun welcomeAutomaticallyEnrollsAndSchoolRetryKeepsEvent() = runBlocking {
        Assume.assumeTrue(BuildConfig.DEBUG && BuildConfig.CAMPUS_TOOLS_API_BASE_URL.startsWith("http://127.0.0.1:"))
        CampusToolsSession.logout()
        context.getSharedPreferences("privacy_consent",0).edit().clear().commit()
        context.getSharedPreferences("optional_device_statistics_v1",0).edit().putInt("choice",0).commit()
        context.getSharedPreferences("school_sync_v1",0).edit().remove("allowed").commit()
        val identity=InstallationIdentity.id
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.onNodeWithText("同意并继续").performClick()
            until { context.getSharedPreferences("optional_device_statistics_v1",0).getBoolean("activated",false) }
            assertTrue(SchoolBindingSync.allowed())
            val pending=File(context.noBackupFilesDir,"pending-school-binding.json")
            try {
                fault(true)
                SchoolBindingSync.verified("0010773901",null)
                until { pending.exists() }
                val json=com.google.gson.JsonParser.parseString(pending.readText()).asJsonObject
                val event=json["eventId"].asString
                assertEquals(setOf("eventId","action","schoolId","studentId","source","consentVersion","verifiedClientAt","owner"),json.keySet())
                repeat(3) { SchoolBindingSync.foreground();android.os.SystemClock.sleep(150) }
                assertEquals(event,com.google.gson.JsonParser.parseString(pending.readText()).asJsonObject["eventId"].asString)
                fault(false);SchoolBindingSync.foreground()
                until { !pending.exists() && SchoolBindingSync.status()=="已同步" }
                CampusToolsSession.login("19000000001","Synthetic-User-20261003!")
                SchoolBindingSync.verified("0010773901",CampusToolsSession.currentUser?.id)
                until { SchoolBindingSync.status()=="已同步" }
                repeat(10) { InstallationTelemetry.foreground() }
                assertEquals(identity,InstallationIdentity.id)
                File(context.getExternalFilesDir(null),"automatic-sync-result.json").writeText("{\"installationId\":\"$identity\",\"student\":\"0010773901\",\"schoolLogin\":\"SYNTHETIC_CALLBACK\",\"scope\":\"LOOPBACK_ONLY\"}")
            } finally { fault(false) }
        }
    }
}
