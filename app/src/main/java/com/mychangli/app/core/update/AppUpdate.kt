package com.mychangli.app.core.update

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import com.mychangli.app.BuildConfig
import com.mychangli.app.core.network.campustools.CampusToolsApiClient
import com.mychangli.app.core.network.campustools.RemoteAppVersion
import com.mychangli.app.core.theme.AppTheme

sealed interface VersionCheckResult {
    data object Latest : VersionCheckResult
    data class Available(
        val version: RemoteAppVersion,
        val mandatory: Boolean,
    ) : VersionCheckResult
    data object Unavailable : VersionCheckResult
}

object VersionPolicy {
    fun evaluate(
        currentVersionCode: Int,
        updateAvailable: Boolean,
        latest: RemoteAppVersion?,
    ): VersionCheckResult {
        if (latest == null || latest.latest_version_code < 1) return VersionCheckResult.Latest
        if (!updateAvailable || latest.latest_version_code <= currentVersionCode) {
            return VersionCheckResult.Latest
        }
        val mandatory = latest.force_update || currentVersionCode < latest.minimum_version_code
        return VersionCheckResult.Available(latest, mandatory)
    }
}

/**
 * Pure cooldown decision logic for the automatic update prompt.
 *
 * Kept Android-free so unit tests can cover the exact rules:
 * - a dismissed version is suppressed for [cooldownMillis];
 * - a newer version immediately resets the cooldown;
 * - manual checks are never subject to this policy (callers choose to bypass).
 */
data class UpdatePromptCooldownPolicy(
    val cooldownMillis: Long,
    private val nowMillis: Long,
) {
    fun shouldPrompt(
        dismissedVersionCode: Int,
        dismissedAtMillis: Long,
        candidateVersionCode: Int,
    ): Boolean {
        if (candidateVersionCode != dismissedVersionCode) return true
        return nowMillis - dismissedAtMillis >= cooldownMillis
    }
}

object AppUpdateChecker {
    suspend fun check(): VersionCheckResult = runCatching {
        val envelope = CampusToolsApiClient.unauthenticated.versionCheck(BuildConfig.VERSION_CODE)
        val data = envelope.data.takeIf { envelope.ok } ?: return@runCatching VersionCheckResult.Unavailable
        VersionPolicy.evaluate(BuildConfig.VERSION_CODE, data.updateAvailable, data.latestVersion)
    }.getOrDefault(VersionCheckResult.Unavailable)
}

/**
 * Local cooldown for the automatic cold-start update prompt.
 *
 * Manual checks and taps on the service-page update card always bypass the
 * cooldown. Only the automatic prompt records a dismissal, keyed by the
 * server version code, so a newly published version is never suppressed by an
 * old dismissal.
 */
object AppUpdateCooldown {
    private const val PREF_NAME = "app_update_prompt_cooldown_v1"
    private const val KEY_DISMISSED_VERSION_CODE = "dismissed_version_code"
    private const val KEY_DISMISSED_AT_MILLIS = "dismissed_at_millis"
    const val DEFAULT_COOLDOWN_MILLIS: Long = 24L * 60L * 60L * 1000L

    fun isPromptAllowed(context: Context, versionCode: Int): Boolean {
        val prefs = prefs(context)
        val dismissedVersionCode = prefs.getInt(KEY_DISMISSED_VERSION_CODE, -1)
        val dismissedAt = prefs.getLong(KEY_DISMISSED_AT_MILLIS, 0L)
        return UpdatePromptCooldownPolicy(
            cooldownMillis = DEFAULT_COOLDOWN_MILLIS,
            nowMillis = System.currentTimeMillis(),
        ).shouldPrompt(
            dismissedVersionCode = dismissedVersionCode,
            dismissedAtMillis = dismissedAt,
            candidateVersionCode = versionCode,
        )
    }

    fun recordDismissed(context: Context, versionCode: Int) {
        prefs(context).edit()
            .putInt(KEY_DISMISSED_VERSION_CODE, versionCode)
            .putLong(KEY_DISMISSED_AT_MILLIS, System.currentTimeMillis())
            .apply()
    }

    fun clearForVersion(context: Context, versionCode: Int) {
        prefs(context).edit()
            .putInt(KEY_DISMISSED_VERSION_CODE, -1)
            .putLong(KEY_DISMISSED_AT_MILLIS, 0L)
            .apply()
    }

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
}

@Composable
fun AppUpdatePrompt(
    update: VersionCheckResult.Available,
    onLater: () -> Unit,
    onSimulateUpdate: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onLater,
        containerColor = AppTheme.colors.bgCardColor,
        title = { Text("发现新版本 ${update.version.latest_version_name}", fontWeight = FontWeight.Bold) },
        text = {
            Text(
                buildString {
                    append(update.version.update_message.ifBlank { "包含体验与稳定性改进。" })
                    if (update.mandatory) append("\n\n当前为本地规则演示，不会锁定或替换应用。")
                    append("\n\n更新来源：我的长理本地后端。")
                }
            )
        },
        confirmButton = { TextButton(onClick = onSimulateUpdate) { Text("立即更新") } },
        dismissButton = { TextButton(onClick = onLater) { Text("稍后再说") } },
    )
}
