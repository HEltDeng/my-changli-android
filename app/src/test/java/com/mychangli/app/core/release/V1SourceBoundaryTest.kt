package com.mychangli.app.core.release

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class V1SourceBoundaryTest {
    private fun appPath(relative: String): File =
        if (File("src/main").isDirectory) File(relative) else File("app", relative)

    private fun appFile(relative: String): File = appPath(relative).also {
        check(it.exists()) { "Missing app file: $relative (cwd=${System.getProperty("user.dir")})" }
    }

    @Test
    fun `unverified binaries and their package references are absent`() {
        assertFalse(appPath("libs/CSUSTDataGet-1.1.4-no-sensitive-logs.aar").exists())
        assertFalse(appPath("libs/Lite3DMap.jar").exists())

        val sourceRoot = appFile("src/main/java")
        val source = sourceRoot.walkTopDown()
            .filter { it.isFile && it.extension in setOf("kt", "java") }
            .joinToString("\n") { it.readText() }
        assertFalse(source.contains("com.dcelysia.csust_spider"))
        assertFalse(source.contains("com.example.csustdataget"))
        assertFalse(source.contains("com.amap.api"))

        val build = appFile("build.gradle.kts").readText()
        assertFalse(build.contains("implementation(files("))
        assertFalse(appFile("../settings.gradle.kts").readText().contains("flatDir"))
    }

    @Test
    fun `closed feature activities permissions and deep links are absent`() {
        val manifest = appFile("src/main/AndroidManifest.xml").readText()
        listOf(
            "SportsReservationActivity",
            "BindingUserActivity",
            "CampusMapActivity",
            "ElectronicActivity",
            "MoocActivity",
        ).forEach { assertFalse(manifest.contains(it)) }
        listOf(
            "ACCESS_FINE_LOCATION",
            "ACCESS_COARSE_LOCATION",
            "ACCESS_WIFI_STATE",
            "CHANGE_WIFI_STATE",
            "android.permission.CAMERA",
            "com.amap.api.v2.apikey",
        ).forEach { assertFalse(manifest.contains(it)) }
        assertFalse(manifest.contains("<data"))
        assertTrue(manifest.contains("LostFoundActivity"))
        assertFalse(appPath("src/main/res/layout/activity_binding_user.xml").exists())
    }

    @Test
    fun `account surfaces do not collect sports or school credentials`() {
        val accountCenter = appFile(
            "src/main/java/com/mychangli/app/core/account/ui/AccountCenterActivity.kt",
        ).readText()
        listOf(
            "reservationProfile()",
            "updateReservationProfile(",
            "deleteReservationProfile()",
            "预约识别学号",
            "教务绑定",
        ).forEach { assertFalse(accountCenter.contains(it)) }

        val accountGate = appFile(
            "src/main/java/com/mychangli/app/core/account/ui/AccountGateScreen.kt",
        ).readText()
        assertTrue(accountGate.contains("首发版不提供学校账号绑定"))
        assertTrue(accountGate.contains("不会采集学校密码"))
    }

    @Test
    fun `release identity endpoint and privacy gate are explicit`() {
        val build = appFile("build.gradle.kts").readText()
        assertTrue(build.contains("applicationId = \"com.mychangli.app\""))
        assertTrue(build.contains("versionCode = 32"))
        assertTrue(build.contains("versionName = \"1.0.0\""))
        assertTrue(build.contains("https://campus-tools.invalid/api/v1/"))

        val main = appFile("src/main/java/com/mychangli/app/core/MainActivity.kt").readText()
        assertTrue(main.contains("PrivacyConsentScreen"))
        assertTrue(main.contains("PrivacyDeclinedScreen"))
    }
}
