package com.mychangli.app.feature.activities

import androidx.annotation.Keep
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.mychangli.app.BuildConfig
import com.mychangli.app.common.data.local.mmkv.StudentInfoManager
import com.mychangli.app.core.network.campustools.*
import com.mychangli.app.core.security.SchoolSessionCoordinator
import com.mychangli.app.core.security.SchoolSessionResult
import com.dcelysia.csust_spider.mooc.data.remote.repository.MoocRepository
import com.dcelysia.csust_spider.core.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import java.util.UUID
import java.util.concurrent.TimeUnit

@Keep data class CampusActivity(
    val id: String, val public_id: String, val title: String, val category: String,
    val organizer_name: String, val college_id: String, val location: String,
    val summary: String, val description: String, val latest_update: String, val contact: String,
    val start_at: Long, val end_at: Long, val signup_start_at: Long, val signup_end_at: Long,
    val capacity: Int?, val registered_count: Int, val state: String, val state_label: String,
    val registration_status: String?, val requires_phone: Boolean,
    val can_register: Boolean, val can_cancel: Boolean, val duration_minutes: Long,
)
@Keep data class ActivityIdentity(val student_number: String, val student_name: String, val college_id: String, val phone: String? = null)

/** Preview transport is isolated to activities. Existing platform sessions are never replaced. */
object ActivityRepository {
    private val gson = Gson()
    private val preview get() = BuildConfig.ACTIVITY_API_BASE_URL != BuildConfig.CAMPUS_TOOLS_API_BASE_URL
    private val mutex = Mutex()
    private var previewToken: String? = null
    val changes = MutableStateFlow(0L)
    private val client = OkHttpClient.Builder().connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS).followRedirects(false).followSslRedirects(false)
        .apply { if (BuildConfig.ACTIVITY_API_BASE_URL.startsWith("http://127.0.0.1:")) proxy(java.net.Proxy.NO_PROXY) }.build()

    private suspend fun request(path: String, method: String = "GET", data: Any? = null, token: String? = null): com.google.gson.JsonElement = withContext(Dispatchers.IO) {
        val body = data?.let { gson.toJson(it).toRequestBody("application/json; charset=utf-8".toMediaType()) }
        val builder = Request.Builder().url(BuildConfig.ACTIVITY_API_BASE_URL + path).method(method, body)
        token?.let { builder.header("Authorization", "Bearer $it") }
        client.newCall(builder.build()).execute().use { response ->
            val json = runCatching { JsonParser.parseString(response.body?.string().orEmpty()).asJsonObject }.getOrNull()
            if (!response.isSuccessful || json?.get("ok")?.asBoolean != true) {
                if (response.code == 401 && preview) previewToken = null
                error(json?.getAsJsonObject("error")?.get("message")?.asString ?: "活动暂时无法加载，请重新加载")
            }
            json.get("data")
        }
    }

    private suspend fun token(): String = mutex.withLock {
        if (!preview) {
            CampusToolsSession.restore()
            return@withLock CampusToolsSession.requireToken()
        }
        previewToken ?: request("auth/device", "POST", mapOf(
            "installationId" to InstallationIdentity.id, "installationSecret" to InstallationIdentity.secret,
            "versionName" to BuildConfig.VERSION_NAME, "versionCode" to BuildConfig.VERSION_CODE,
        )).asJsonObject.get("token").asString.also { previewToken = it }
    }

    suspend fun list(mine: Boolean = false, recent: Boolean = false): List<CampusActivity> {
        val data = request(if (mine) "activities/mine" else "activities?recent=$recent", token = token())
        return data.asJsonObject.getAsJsonArray("items").map { gson.fromJson(it, CampusActivity::class.java) }
    }
    suspend fun detail(id: String): CampusActivity = gson.fromJson(request("activities/$id", token = token()), CampusActivity::class.java)
    suspend fun identity(): ActivityIdentity? {
        val data = request("activities/profile", token = token())
        return if (data.isJsonNull) null else gson.fromJson(data, ActivityIdentity::class.java)
    }

    suspend fun readSchoolIdentity(): ActivityIdentity = withContext(Dispatchers.IO) {
        check(StudentInfoManager.hasCompleteBinding) { "请先在我的页面绑定学校账号" }
        check(SchoolSessionCoordinator.instance.ensureReady() == SchoolSessionResult.Ready) { "学校会话暂不可用，请重新绑定或稍后再试" }
        val result = MoocRepository.instance.getLoginUser().filter { it !is Resource.Loading }.first()
        check(result is Resource.Success) { "暂时无法取得学校身份，请稍后重试" }
        val sso = result.data ?: error("学校身份信息为空")
        check(sso.userAccount == StudentInfoManager.studentId) { "学校账号已变化，请重新绑定后报名" }
        check(sso.userName.isNotBlank() && sso.deptName.isNotBlank()) { "学校暂未提供完整姓名和学院，请稍后重试" }
        val college = sso.deptName.split('/').lastOrNull { it.isNotBlank() }?.trim().orEmpty()
        check(college.isNotBlank()) { "学校暂未提供学院，请稍后重试" }
        ActivityIdentity(StudentInfoManager.studentId, sso.userName, college)
    }

    suspend fun confirmIdentity(identity: ActivityIdentity) {
        check(identity.student_number == StudentInfoManager.studentId) { "学校账号已变化，请重新确认" }
        val t = token()
        request("school-binding", "POST", mapOf(
            "installationId" to InstallationIdentity.id, "installationSecret" to InstallationIdentity.secret,
            "eventId" to UUID.randomUUID().toString(), "action" to "BIND", "schoolId" to "csust",
            "studentId" to identity.student_number, "source" to "CLIENT_SCHOOL_LOGIN", "consentVersion" to 1,
            "verifiedClientAt" to System.currentTimeMillis(),
        ), t)
        request("activities/profile", "PUT", mapOf(
            "student_number" to identity.student_number, "student_name" to identity.student_name,
            "college_id" to identity.college_id, "source" to "CLIENT_SCHOOL_LOGIN", "consent" to true,
        ), t)
        changes.value++
    }

    suspend fun register(id: String, phone: String? = null): CampusActivity {
        check(StudentInfoManager.hasCompleteBinding) { "请先绑定学校账号" }
        val p = identity() ?: error("请先确认学校身份")
        check(p.student_number == StudentInfoManager.studentId) { "学校账号已变化，请重新确认身份" }
        val data = if (phone == null) emptyMap<String, Any>() else mapOf("phone" to phone, "phone_confirmed" to true)
        return gson.fromJson(request("activities/$id/register", "POST", data, token()), CampusActivity::class.java).also { changes.value++ }
    }
    suspend fun cancel(id: String): CampusActivity = gson.fromJson(request("activities/$id/registration", "DELETE", token = token()), CampusActivity::class.java).also { changes.value++ }
}
