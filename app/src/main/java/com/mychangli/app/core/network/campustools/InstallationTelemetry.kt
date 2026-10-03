package com.mychangli.app.core.network.campustools

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Build
import android.os.Bundle
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mychangli.app.BuildConfig
import com.mychangli.app.core.PlanetApplication
import com.mychangli.app.core.privacy.PrivacyConsentManager
import java.io.File
import java.util.UUID
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Optional foreground-only telemetry. Never starts a background service or invalidates login. */
object InstallationTelemetry {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private var job: Job? = null
    private val gson = Gson()
    private val context get() = PlanetApplication.appContext
    private val prefs get() = context.getSharedPreferences("optional_device_statistics_v1", 0)
    private val pendingFile get() = File(context.noBackupFilesDir, "pending-device-event.json")
    private val withdrawalFile get() = File(context.noBackupFilesDir, "pending-device-withdrawal.json")
    private var enrolled = false
    private var started = 0
    fun choice(context: Context): Int = context.getSharedPreferences("optional_device_statistics_v1", 0).getInt("choice", 0)
    fun enabled(): Boolean = choice(context) == 1 && PrivacyConsentManager.hasAppConsent(context)

    fun register(application: Application) {
        application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityStarted(activity: Activity) { if (started++ == 0) foreground() }
            override fun onActivityStopped(activity: Activity) { started = (started - 1).coerceAtLeast(0) }
            override fun onActivityCreated(a: Activity, b: Bundle?) {}
            override fun onActivityResumed(a: Activity) {}
            override fun onActivityPaused(a: Activity) {}
            override fun onActivitySaveInstanceState(a: Activity, b: Bundle) {}
            override fun onActivityDestroyed(a: Activity) {}
        })
    }
    fun decide(allow: Boolean) {
        val wasAllowed = choice(context) == 1
        val withdrawalNeeded = wasAllowed || prefs.getBoolean("withdraw_pending", false)
        check(prefs.edit().putInt("choice", if (allow) 1 else 2)
            .putBoolean("withdraw_pending", !allow && withdrawalNeeded).commit())
        job?.cancel()
        if (!allow) pendingFile.delete()
        job = scope.launch {
            mutex.withLock {
                pendingFile.delete()
                if (allow) {
                    withdrawalFile.delete()
                    prefs.edit().remove("last_sent").remove("activated").apply()
                    sendLocked()
                } else if (withdrawalNeeded) {
                    // Minimal deletion instruction contains no collected device profile.
                    prefs.edit().putBoolean("withdraw_pending", true).apply()
                    sendWithdrawal()
                }
            }
        }
    }
    fun foreground() {
        if (!enabled()) pendingFile.delete()
        if (!enabled() && !prefs.getBoolean("withdraw_pending", false)) return
        if (job?.isActive == true) return
        job = scope.launch {
            mutex.withLock {
                if (prefs.getBoolean("withdraw_pending", false)) sendWithdrawal()
                if (enabled()) sendLocked()
            }
        }
    }
    private fun base(kind: String): MutableMap<String, Any?> = mutableMapOf(
        "eventId" to UUID.randomUUID().toString(), "kind" to kind,
        "clientAt" to System.currentTimeMillis(), "protocolVersion" to 2,
        "packageName" to BuildConfig.APPLICATION_ID,
    )
    private fun readPending(file: File = pendingFile): Map<String, Any?> {
        val raw: Map<String, Any?> = try { gson.fromJson(file.readText(),
            object : TypeToken<Map<String, Any?>>() {}.type)
        } catch (e: Exception) { file.delete(); throw e }
        val integerFields = setOf("clientAt", "protocolVersion", "consentVersion", "sdkInt", "appVersionCode", "firstInstallAt")
        return raw.mapValues { (key, value) ->
            if (key in integerFields && value is Number) value.toLong() else value
        }
    }
    private suspend fun post(event: Map<String, Any?>): Boolean {
        val payload = event + mapOf("installationId" to InstallationIdentity.id,
            "installationSecret" to InstallationIdentity.secret)
        // Dedicated call: telemetry errors must never clear the account session.
        return try { CampusToolsApiClient.authenticated.installationEvent(payload).ok }
        catch (e: retrofit2.HttpException) {
            if (e.code() !in setOf(401, 403)) throw e
            // Expired/disabled account sessions cannot bind accounts, but installation proof
            // still authorizes this installation's own observations and withdrawal.
            CampusToolsApiClient.unauthenticated.installationEvent(payload).ok
        }
    }
    private suspend fun sendWithdrawal() {
        try {
            pendingFile.delete()
            if (!withdrawalFile.exists()) withdrawalFile.writeText(gson.toJson(base("WITHDRAW")))
            val event = readPending(withdrawalFile)
            if (post(event)) {
                withdrawalFile.delete()
                prefs.edit().putBoolean("withdraw_pending", false).remove("activated").remove("last_sent").apply()
            }
        } catch (e: CancellationException) { throw e } catch (_: Exception) { /* Retry next foreground. */ }
    }
    private suspend fun sendLocked() {
        if (!enabled()) return
        try {
            if (!enrolled) {
                val result = CampusToolsApiClient.unauthenticated.deviceAuth(DeviceAuthRequest(
                    installationId = InstallationIdentity.id, installationSecret = InstallationIdentity.secret,
                    versionName = BuildConfig.VERSION_NAME, versionCode = BuildConfig.VERSION_CODE))
                if (!result.ok) return
                enrolled = true
            }
            if (!enabled()) return
            val now = System.currentTimeMillis()
            val versionChanged = prefs.getInt("version", 0) != BuildConfig.VERSION_CODE
            if (!pendingFile.exists()) {
                if (!versionChanged && now - prefs.getLong("last_sent", 0) in 0 until 15*60*1000) return
                // Build and Android ID APIs are called ONLY inside this consent gate.
                val event = base(if (prefs.getBoolean("activated", false)) "FOREGROUND" else "ACTIVATE")
                event.putAll(mapOf("consentVersion" to 1, "manufacturer" to Build.MANUFACTURER,
                    "brand" to Build.BRAND, "modelRaw" to Build.MODEL,
                    "androidVersion" to Build.VERSION.RELEASE, "sdkInt" to Build.VERSION.SDK_INT,
                    "appVersionName" to BuildConfig.VERSION_NAME, "appVersionCode" to BuildConfig.VERSION_CODE,
                    "deviceScopedId" to DeviceIdentity.deviceScopedId(context),
                    "installationOrigin" to InstallationIdentity.origin,
                    "firstInstallAt" to context.packageManager.getPackageInfo(context.packageName, 0).firstInstallTime))
                pendingFile.writeText(gson.toJson(event))
            }
            val event = readPending()
            if (!enabled()) { pendingFile.delete(); return }
            if (post(event)) {
                pendingFile.delete()
                prefs.edit().putLong("last_sent", now).putInt("version", BuildConfig.VERSION_CODE)
                    .putBoolean("activated", true).apply()
            }
        } catch (e: CancellationException) { throw e } catch (_: Exception) { /* Stable event survives retries. */ }
    }
}
