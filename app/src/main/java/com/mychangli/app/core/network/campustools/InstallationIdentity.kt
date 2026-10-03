package com.mychangli.app.core.network.campustools

import android.util.AtomicFile
import com.mychangli.app.core.PlanetApplication
import com.tencent.mmkv.MMKV
import org.json.JSONObject
import java.io.File
import java.util.UUID

/** One process-wide, atomic, account-independent identity in Android noBackupFilesDir. */
object InstallationIdentity {
    private var cached: JSONObject? = null
    @Synchronized private fun record(): JSONObject {
        cached?.let { return it }
        val context = PlanetApplication.appContext
        val file = AtomicFile(File(context.noBackupFilesDir, "installation-v2.json"))
        if (file.baseFile.exists()) {
            val saved = JSONObject(file.openRead().bufferedReader().use { it.readText() })
            require(saved.getString("id").isNotBlank() && saved.getString("secret").length >= 36)
            return saved.also { cached = it }
        }
        val legacy = MMKV.mmkvWithID("campus_tools_session_v1")
        val legacyId = legacy.decodeString("installation_id")?.takeIf { it.isNotBlank() }
        val marker = context.getSharedPreferences("installation_migration", 0)
        val identityWasMigrated = marker.getBoolean("v2", false)
        // A missing no-backup identity after migration is a fresh/restore instance.
        // Do not carry its platform session to a new installation.
        if (identityWasMigrated) {
            legacy.removeValuesForKeys(arrayOf("access_token", "device_access_token", "account_user", "installation_id"))
        }
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        val upgraded = info.firstInstallTime < info.lastUpdateTime
        val adopted = legacyId?.takeIf { !identityWasMigrated && upgraded }
        val value = JSONObject().put("id", adopted ?: UUID.randomUUID().toString())
            .put("secret", UUID.randomUUID().toString() + UUID.randomUUID().toString())
            .put("origin", if (adopted != null || upgraded) "UPGRADE" else "NEW")
        val stream = file.startWrite()
        try {
            stream.write(value.toString().toByteArray(Charsets.UTF_8))
            file.finishWrite(stream)
        } catch (e: Exception) {
            file.failWrite(stream)
            throw e
        }
        check(marker.edit().putBoolean("v2", true).commit())
        return value.also { cached = it }
    }
    val id: String get() = record().getString("id")
    val secret: String get() = record().getString("secret")
    val origin: String get() = record().getString("origin")
}

