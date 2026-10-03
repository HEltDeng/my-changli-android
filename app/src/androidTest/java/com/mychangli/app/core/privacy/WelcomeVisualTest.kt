package com.mychangli.app.core.privacy

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.test.platform.app.InstrumentationRegistry
import com.mychangli.app.core.theme.AppSkinTheme
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
import java.io.File

/** Isolated visual host: does not clear app consent, identity, credentials or business data. */
class WelcomeVisualTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    @Test fun restoredWelcomeHasOnlyOriginalActions() {
        var accepted = false
        compose.activityRule.scenario.onActivity {
            it.enableEdgeToEdge()
            it.window.isNavigationBarContrastEnforced = false
        }
        compose.setContent { AppSkinTheme { PrivacyConsentScreen(
            onAccept = { accepted = true }, onDecline = {}, onOpenPrivacy = {}, onOpenAgreement = {}
        ) } }
        compose.onNodeWithText("欢迎使用我的长理").assertIsDisplayed()
        compose.onNodeWithText("同意并继续").assertIsDisplayed()
        compose.onNodeWithText("暂不同意").assertIsDisplayed()
        compose.onNodeWithText("隐私政策").assertIsDisplayed()
        compose.onNodeWithText("用户协议").assertIsDisplayed()
        compose.onNodeWithText("了解详情").assertDoesNotExist()
        compose.onNodeWithText("参与安装与使用统计，帮助改进适配与功能。").assertDoesNotExist()
        compose.onNodeWithText("学校绑定成功后同步学校及学号，用于账号识别与服务支持（可选）。").assertDoesNotExist()
        compose.waitForIdle()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        // Semantics can be ready before the emulator's first SurfaceFlinger frame.
        android.os.SystemClock.sleep(800)
        val target = File(compose.activity.getExternalFilesDir(null), "welcome-restored-1200x2664.png")
        val screenshot = compose.onRoot().captureToImage().asAndroidBitmap()
        target.outputStream().use { screenshot.compress(Bitmap.CompressFormat.PNG, 100, it) }
        compose.onNodeWithText("同意并继续").performClick()
        assertTrue(accepted)
    }
}
