package com.mychangli.app.core.privacy

import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.mychangli.app.BuildConfig
import com.mychangli.app.core.network.campustools.*
import com.mychangli.app.feature.schedule.reminder.*
import org.junit.*
import org.junit.Assert.*
import java.io.File

class CandidateSecondaryFlowTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun shot(name: String) {
        compose.waitForIdle()
        android.os.SystemClock.sleep(600)
        val bitmap=InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        File(context.getExternalFilesDir(null),name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) }
    }
    @Test fun reminderNavigationAndSilentChannel() {
        ActivityScenario.launch(CourseReminderSettingsActivity::class.java).use {
            compose.onNodeWithText("仅根据本机课表发送系统通知").assertDoesNotExist()
            compose.onNodeWithText("管理精确提醒权限").assertDoesNotExist()
            shot("reminders-after.png")
            compose.onNodeWithText("提醒方式").performClick()
            compose.onNodeWithText("静默提醒").performClick()
            val channel=context.getSystemService(android.app.NotificationManager::class.java).getNotificationChannel(ReminderChannels.SILENT)
            assertNull(channel.sound);assertFalse(channel.shouldVibrate())
            shot("reminder-mode-after.png")
            compose.onNodeWithContentDescription("返回").performClick()
            compose.onNodeWithText("提醒状态").performClick()
            compose.onNodeWithText("管理精确提醒权限").assertIsDisplayed()
            shot("reminder-status-after.png")
        }
    }
    @Test fun legacyStatisticsRouteShowsWithdrawalNotEnableSwitches() {
        ActivityScenario.launch<ReleaseInformationActivity>(ReleaseInformationActivity.intent(context,ReleaseDocumentType.STATISTICS)).use {
            compose.onNodeWithText("数据授权管理").assertIsDisplayed()
            compose.onAllNodes(isToggleable()).assertCountEquals(0)
            shot("data-authorization-after.png")
        }
    }
    @Test fun explicitConfirmationPreservesRefusalAndInstallation() {
        Assume.assumeTrue(BuildConfig.DEBUG && BuildConfig.CAMPUS_TOOLS_API_BASE_URL.startsWith("http://127.0.0.1:"))
        val p=context.getSharedPreferences("optional_device_statistics_v1",0)
        val school=context.getSharedPreferences("school_sync_v1",0)
        val identity=InstallationIdentity.id
        for(choice in listOf(2,3)) {
            p.edit().putInt("choice",choice).commit()
            school.edit().putBoolean("allowed",false).commit()
            PrivacyConsentManager.confirmCurrentPolicy(context,true)
            assertEquals(choice,InstallationTelemetry.choice(context)); assertFalse(SchoolBindingSync.allowed())
        }
        p.edit().putInt("choice",0).commit();school.edit().remove("allowed").commit()
        PrivacyConsentManager.confirmCurrentPolicy(context,true)
        assertTrue(InstallationTelemetry.enabled());assertTrue(SchoolBindingSync.allowed())
        PrivacyConsentManager.confirmCurrentPolicy(context,false)
        assertFalse(InstallationTelemetry.enabled());assertFalse(SchoolBindingSync.allowed())
        assertEquals(identity,InstallationIdentity.id)
    }
}
