package com.mychangli.app.core.account.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.mychangli.app.base.ComposeActivity
import com.mychangli.app.core.Route
import com.mychangli.app.core.network.campustools.CampusToolsSession

class AccountRequiredActivity : ComposeActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (CampusToolsSession.currentUser != null && !CampusToolsSession.token.isNullOrBlank()) {
            openTargetAndFinish()
            return
        }
        setComposeContent {
            AccountGateScreen(
                onLogin = CampusToolsSession::login,
                onRegister = CampusToolsSession::register,
                onAuthenticated = ::openTargetAndFinish,
                onContinueAsGuest = ::finish,
            )
        }
    }

    private fun openTargetAndFinish() {
        val target = intent.getStringExtra(EXTRA_TARGET_URI)
            ?.let { runCatching { Intent.parseUri(it, Intent.URI_INTENT_SCHEME) }.getOrNull() }
            ?.takeIf { it.component?.packageName == packageName }
        if (target != null) startActivity(target) else Route.goHome(this)
        finish()
    }

    companion object {
        private const val EXTRA_TARGET_URI = "account_target_uri"

        fun intent(context: Context, target: Intent): Intent =
            Intent(context, AccountRequiredActivity::class.java)
                .putExtra(EXTRA_TARGET_URI, target.toUri(Intent.URI_INTENT_SCHEME))
    }
}
