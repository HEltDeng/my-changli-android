package com.mychangli.app.core.network.campustools

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mychangli.app.core.PlanetApplication
import com.mychangli.app.common.data.local.mmkv.StudentInfoManager
import com.mychangli.app.core.privacy.PrivacyConsentManager
import java.io.File
import java.util.UUID
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Optional, minimal school identity sync. No password/session/response enters this object. */
object SchoolBindingSync {
    private val context get() = PlanetApplication.appContext
    private val prefs get() = context.getSharedPreferences("school_sync_v1", 0)
    private val pending get() = File(context.noBackupFilesDir, "pending-school-binding.json")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO.limitedParallelism(1))
    private val mutex = Mutex()
    private val gson = Gson()
    private var enrolled = false
    val updates = kotlinx.coroutines.flow.MutableStateFlow(0L)
    @Volatile internal var lastFailure: String = ""
    private fun changed() { updates.value = updates.value + 1 }
    fun hasDecision() = prefs.contains("allowed")
    fun allowed() = prefs.getBoolean("allowed", false)
    fun status(): String = when {
        !allowed() -> if (prefs.getBoolean("withdraw_pending", false)) "已关闭，联网后完成撤回" else "未开启"
        prefs.getString("owner", "") != CampusToolsSession.currentUser?.id.orEmpty() -> "当前账号尚未验证学校绑定"
        pending.exists() -> "已开启，等待同步"
        prefs.getBoolean("synced", false) -> "已同步"
        else -> "已开启，学校绑定成功后同步"
    }
    fun decide(allow: Boolean) {
        val owner = CampusToolsSession.currentUser?.id.orEmpty()
        prefs.edit().putBoolean("allowed", allow).putString("owner", owner).apply()
        changed()
        if (!allow) { unbind(withdraw = true); return }
        if (StudentInfoManager.hasCompleteBinding && !prefs.getBoolean("owner_changed", false)) {
            queueBinding(StudentInfoManager.studentId, owner, "CLIENT_EXISTING_BINDING")
        }
    }
    fun verified(student: String, ownerAtLogin: String?) {
        if (!allowed() || CampusToolsSession.currentUser?.id.orEmpty() != ownerAtLogin.orEmpty()) return
        prefs.edit().putString("owner", ownerAtLogin.orEmpty()).putBoolean("owner_changed", false).apply()
        queueBinding(student, ownerAtLogin.orEmpty(), "CLIENT_SCHOOL_LOGIN")
    }
    private fun queueBinding(student: String, owner: String, source: String) {
        scope.launch { mutex.withLock {
            if (!allowed() || CampusToolsSession.currentUser?.id.orEmpty() != owner) return@withLock
            val event = mapOf("eventId" to UUID.randomUUID().toString(), "action" to "BIND",
                "schoolId" to "csust", "studentId" to student, "source" to source,
                "consentVersion" to 1, "verifiedClientAt" to System.currentTimeMillis(), "owner" to owner)
            val file = android.util.AtomicFile(pending)
            val stream = file.startWrite()
            try { stream.write(gson.toJson(event).toByteArray(Charsets.UTF_8)); file.finishWrite(stream) }
            catch (e: Exception) { file.failWrite(stream); throw e }
            prefs.edit().putBoolean("synced", false).apply()
            sendLocked()
        } }
    }
    fun unbind(withdraw: Boolean = false) {
        prefs.edit().putBoolean("synced", false).putBoolean("withdraw_pending", true)
            .putString("end_action", if (withdraw) "WITHDRAW" else "UNBIND").apply()
        scope.launch { mutex.withLock { pending.delete(); sendLocked() } }
    }
    fun accountChanged(previous: String?, current: String?) {
        if (previous == current) return
        if (previous == null && prefs.getString("owner", "") == "" && !prefs.getBoolean("owner_changed", false)
            && allowed() && StudentInfoManager.hasCompleteBinding) {
            prefs.edit().putString("owner", current.orEmpty()).apply()
            queueBinding(StudentInfoManager.studentId, current.orEmpty(), "CLIENT_EXISTING_BINDING")
        } else {
            prefs.edit().putBoolean("owner_changed", true).apply()
            unbind()
        }
    }
    fun foreground() { scope.launch { mutex.withLock { sendLocked() } } }
    private suspend fun sendLocked() {
        if (!PrivacyConsentManager.hasAppConsent(context)) return
        try {
            val ending = prefs.getBoolean("withdraw_pending", false)
            if (!ending && (!allowed() || !pending.exists())) return
            val data: MutableMap<String, Any?> = if (ending) mutableMapOf(
                "eventId" to (prefs.getString("end_id", null) ?: UUID.randomUUID().toString().also { prefs.edit().putString("end_id", it).commit() }),
                "action" to prefs.getString("end_action", "WITHDRAW"))
            else gson.fromJson(android.util.AtomicFile(pending).openRead().bufferedReader().use { it.readText() }, object : TypeToken<MutableMap<String, Any?>>() {}.type)
            val owner = data.remove("owner") as? String
            if (!ending && (owner != CampusToolsSession.currentUser?.id.orEmpty() || owner != prefs.getString("owner", ""))) return
            for (key in listOf("consentVersion", "verifiedClientAt")) (data[key] as? Number)?.let { data[key] = it.toLong() }
            data["installationId"] = InstallationIdentity.id
            data["installationSecret"] = InstallationIdentity.secret
            if (!ending) data["afterOperationId"] = prefs.getString("completed_end_id", null)
            val token = if (ending || owner.isNullOrEmpty()) null else CampusToolsSession.token ?: return
            if (!enrolled) {
                val enrollment = CampusToolsApiClient.unauthenticated.deviceAuth(DeviceAuthRequest(
                    installationId = InstallationIdentity.id, installationSecret = InstallationIdentity.secret,
                    versionName = com.mychangli.app.BuildConfig.VERSION_NAME, versionCode = com.mychangli.app.BuildConfig.VERSION_CODE))
                if (!enrollment.ok) return
                enrolled = true
            }
            val response = CampusToolsApiClient.unauthenticated.schoolBinding(token?.let { "Bearer $it" }, data)
            if (response.ok) {
                lastFailure = ""
                if (ending) prefs.edit().putBoolean("withdraw_pending", false).putString("completed_end_id", data["eventId"] as String).remove("end_id").apply()
                else { pending.delete(); prefs.edit().putBoolean("synced", true).apply() }
                if (ending && allowed() && pending.exists()) sendLocked()
            }
        } catch (e: CancellationException) { throw e } catch (e: Exception) {
            lastFailure = e.javaClass.simpleName + (if (e is retrofit2.HttpException) ":${e.code()}" else "")
            // Diagnostic type/code only: never retain a request, credential or school identity.
        }
        finally { changed() }
    }
}
