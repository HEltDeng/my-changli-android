package com.mychangli.app.core.release

import java.io.File
import java.security.MessageDigest
import org.junit.Assert.assertEquals
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
    fun `authorized campus dependency is fixed while excluded binaries stay absent`() {
        val csustDataGet = appFile("libs/CSUSTDataGet-1.1.4-no-sensitive-logs.aar")
        assertFalse(appPath("libs/Lite3DMap.jar").exists())

        val sha256 = MessageDigest.getInstance("SHA-256")
            .digest(csustDataGet.readBytes())
            .joinToString("") { "%02X".format(it) }
        assertEquals(
            "9B03523FA04E9C8B3F5CAAAD49DC282B17EB2AC82E69A49E89764B21ABF71C29",
            sha256,
        )

        val sourceRoot = appFile("src/main/java")
        val source = sourceRoot.walkTopDown()
            .filter { it.isFile && it.extension in setOf("kt", "java") }
            .joinToString("\n") { it.readText() }
        assertTrue(source.contains("com.dcelysia.csust_spider"))
        assertFalse(source.contains("com.amap.api"))

        val build = appFile("build.gradle.kts").readText()
        assertTrue(build.contains("CSUSTDataGet-1.1.4-no-sensitive-logs.aar"))
        assertFalse(build.contains("Lite3DMap.jar"))
    }

    @Test
    fun `closed feature activities permissions and deep links are absent`() {
        val manifest = appFile("src/main/AndroidManifest.xml").readText()
        listOf(
            "SportsReservationActivity",
            "CampusMapActivity",
            "SportsReservationActivity",
        ).forEach { assertFalse(manifest.contains(it)) }
        listOf(
            "BindingUserActivity",
            "ElectronicActivity",
            "MoocActivity",
            "ScoreInquiryActivity",
            "ExamArrangementActivity",
        ).forEach { assertTrue(manifest.contains(it)) }
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
        assertTrue(appPath("src/main/res/layout/activity_binding_user.xml").exists())

        val networkConfig = appFile("src/main/res/xml/netconfig.xml").readText()
        assertTrue(networkConfig.contains("<base-config cleartextTrafficPermitted=\"false\">"))
        assertTrue(networkConfig.contains("xk.csust.edu.cn"))
        assertTrue(networkConfig.contains("pt.csust.edu.cn"))
        assertFalse(networkConfig.contains("includeSubdomains=\"true\""))
    }

    @Test
    fun `own account remains separate from school credentials`() {
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
        assertTrue(accountGate.contains("学校账号相互独立"))
        assertTrue(accountGate.contains("不会发送至“我的长理”自有后端"))
    }

    @Test
    fun `release identity endpoint and privacy gate are explicit`() {
        val build = appFile("build.gradle.kts").readText()
        assertTrue(build.contains("applicationId = \"com.mychangli.app\""))
        assertTrue(build.contains("versionCode = 32"))
        assertTrue(build.contains("versionName = \"1.0.0\""))
        assertTrue(build.contains("https://api.mychangliapp.cn/api/v1/"))
        assertTrue(build.contains("https://api.mychangliapp.cn/api/v1/legacy-disabled/"))
        assertTrue(build.contains("Release endpoints must not contain placeholder or loopback hosts"))

        val main = appFile("src/main/java/com/mychangli/app/core/MainActivity.kt").readText()
        assertTrue(main.contains("PrivacyConsentScreen"))
        assertTrue(main.contains("PrivacyDeclinedScreen"))

        val campusTools = appFile(
            "src/main/java/com/mychangli/app/core/network/campustools/CampusToolsApi.kt",
        ).readText()
        assertTrue(campusTools.contains("deviceAuth(DeviceAuthRequest(installationId))"))
        assertTrue(campusTools.contains("get() = token ?: deviceToken"))
        assertTrue(campusTools.contains("Only phone accounts can enter the visible account session"))

        val legacyNetwork = appFile(
            "src/main/java/com/mychangli/app/utils/RetrofitUtils.kt",
        ).readText()
        assertTrue(legacyNetwork.contains("LegacyServiceGuardInterceptor()"))
    }

    @Test
    fun `release user surface removes local demo components and draft labels`() {
        val releaseManifest = appFile("src/release/AndroidManifest.xml").readText()
        listOf(
            "BuddyClubActivity",
            "CampusActivityPlaceholderActivity",
            "LocalCampusPublishActivity",
            "LocalCampusPostDetailActivity",
        ).forEach { activity ->
            assertTrue(releaseManifest.contains(activity))
        }
        assertEquals(4, Regex("tools:node=\"remove\"").findAll(releaseManifest).count())

        val releaseStrings = appFile("src/main/res/values/strings.xml").readText()
        assertTrue(releaseStrings.contains("<string name=\"app_name\">我的长理</string>"))
        assertTrue(releaseStrings.contains("<string name=\"release_document_title_privacy\">隐私政策</string>"))
        assertFalse(releaseStrings.contains("发布前审阅稿"))

        val debugStrings = appFile("src/debug/res/values/strings.xml").readText()
        assertTrue(debugStrings.contains("<string name=\"app_name\">我的长理 QA</string>"))
    }
}
