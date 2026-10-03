package com.mychangli.app.core.privacy

import android.graphics.Bitmap
import android.view.autofill.AutofillValue
import android.widget.EditText
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.mychangli.app.R
import com.mychangli.app.BuildConfig
import com.mychangli.app.settings.ui.BindingUserActivity
import com.mychangli.app.core.network.campustools.*
import kotlinx.coroutines.*
import org.junit.Test
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import java.io.File

class CandidatePrivacyIntegrationTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun isolated() { assumeTrue(BuildConfig.DEBUG && BuildConfig.CAMPUS_TOOLS_API_BASE_URL.startsWith("http://127.0.0.1:")) }
    @Test fun basePolicyDoesNotChangeOptionalDecisions() {
        isolated()
        val p=context.getSharedPreferences("optional_device_statistics_v1",0)
        val original=p.getInt("choice",0)
        val school=SchoolBindingSync.allowed()
        val id=InstallationIdentity.id
        try {
            for (choice in listOf(0,1,2,3)) {
                p.edit().putInt("choice",choice).commit()
                PrivacyConsentManager.acceptAppConsent(context)
                assertEquals(choice,InstallationTelemetry.choice(context))
                assertEquals(school,SchoolBindingSync.allowed())
                assertEquals(id,InstallationIdentity.id)
            }
        } finally { p.edit().putInt("choice",original).commit() }
    }
    @Test fun optionalSchoolSyncAndAccountSwitch() = runBlocking {
        isolated()
        PrivacyConsentManager.acceptAppConsent(context)
        CampusToolsSession.login("19000000000","Synthetic-User-20261003!")
        val owner=CampusToolsSession.currentUser!!.id
        val original=InstallationIdentity.id
        val p=context.getSharedPreferences("school_sync_v1",0)
        SchoolBindingSync.decide(true)
        SchoolBindingSync.verified("009900001",owner)
        val deadline=System.currentTimeMillis()+20000
        while (!p.getBoolean("synced",false) && System.currentTimeMillis()<deadline) { delay(150); SchoolBindingSync.foreground() }
        assertTrue("School identity must reach isolated backend: "+SchoolBindingSync.lastFailure,p.getBoolean("synced",false))
        CampusToolsSession.logout()
        CampusToolsSession.login("19000000001","Synthetic-User-20261003!")
        SchoolBindingSync.verified("009900001",owner)
        delay(400)
        assertEquals("当前账号尚未验证学校绑定",SchoolBindingSync.status())
        SchoolBindingSync.decide(false)
        val end=System.currentTimeMillis()+20000
        while (p.getBoolean("withdraw_pending",false) && System.currentTimeMillis()<end) { delay(150);SchoolBindingSync.foreground() }
        assertFalse(p.getBoolean("withdraw_pending",false))
        assertFalse(SchoolBindingSync.allowed())
        assertEquals(original,InstallationIdentity.id)
        CampusToolsSession.logout()
    }
    @Test fun nativeAutofillPreservesFieldsAndBrandHighlight() {
        isolated()
        ActivityScenario.launch(BindingUserActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val student=activity.findViewById<EditText>(R.id.et_student_id)
                val password=activity.findViewById<EditText>(R.id.et_student_password)
                student.autofill(AutofillValue.forText("00123456789"))
                password.autofill(AutofillValue.forText("Synthetic-not-submitted!"))
                assertEquals("00123456789",student.text.toString())
                assertNotNull(password.transformationMethod)
                assertTrue(student.autofillHints!!.contains("username"))
                assertTrue(password.autofillHints!!.contains("password"))
                val attribute=android.util.TypedValue()
                assertTrue(activity.theme.resolveAttribute(android.R.attr.autofilledHighlight,attribute,true))
                assertEquals(R.drawable.autofill_brand_highlight,attribute.resourceId)
                student.requestFocus()
            }
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            android.os.SystemClock.sleep(700)
            val screenshot=InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
            File(context.getExternalFilesDir(null),"binding-autofill-synthetic.png").outputStream().use { screenshot.compress(Bitmap.CompressFormat.PNG,100,it) }
        }
    }
}
