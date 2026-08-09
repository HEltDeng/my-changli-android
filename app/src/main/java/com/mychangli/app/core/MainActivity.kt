package com.mychangli.app.core

import android.app.Activity
import android.app.Application
import android.content.ComponentCallbacks
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.mychangli.app.common.api.DrawerController
import com.mychangli.app.common.cache.CommonInfo
import com.mychangli.app.common.pool.TabAnimationPool
import com.mychangli.app.common.redux.action.UserAction
import com.mychangli.app.common.redux.store.UserStore
import com.mychangli.app.core.main.navigation.MainTabNavigator
import com.mychangli.app.core.main.ui.MainScreen
import com.mychangli.app.core.network.campustools.AccountSessionState
import com.mychangli.app.core.network.campustools.CampusToolsSession
import com.mychangli.app.core.network.campustools.RemoteConfigurationStore
import com.mychangli.app.core.privacy.PrivacyConsentManager
import com.mychangli.app.core.privacy.PrivacyConsentManager.AppConsentState
import com.mychangli.app.core.privacy.PrivacyConsentScreen
import com.mychangli.app.core.privacy.PrivacyDeclinedScreen
import com.mychangli.app.core.privacy.ReleaseDocumentType
import com.mychangli.app.feature.lostfound.data.LostFoundRepository
import com.mychangli.app.feature.sports.data.SportsReservationRepository
import com.mychangli.app.core.theme.AppSkinTheme
import com.mychangli.app.core.theme.AppTheme
import com.mychangli.app.core.update.AppUpdateChecker
import com.mychangli.app.core.update.AppUpdateCooldown
import com.mychangli.app.core.update.AppUpdatePrompt
import com.mychangli.app.core.update.VersionCheckResult
import com.mychangli.app.utils.event.AppEventBus
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity(), DrawerController {
    private val store by lazy { UserStore() }
    private val mainTabNavigator by lazy(LazyThreadSafetyMode.NONE) { MainTabNavigator() }

    override fun onResume() {
        super.onResume()
        if (!PrivacyConsentManager.hasAppConsent(this)) return
        store.dispatch(UserAction.initilaize())  //初始化用户信息，对游客模式无影响
        lifecycleScope.launch(Dispatchers.IO) {
            runCatching { RemoteConfigurationStore.refreshIfStale(30_000L) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setCustomDensity(this, application, 412)
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                AppEventBus.selectEvent.collect { event ->
                    mainTabNavigator.select(event.eventType)
                }
            }
        }
//        if (PlanetApplication.Companion.accessToken.isNullOrEmpty() && !PlanetApplication.Companion.is_tourist) {
//            Route.goLogin(this@MainActivity)
//            finish()
//            return
//       }
        ////  Route.goHome(this@MainActivity)
        CommonInfo.startTime = System.currentTimeMillis()
        enableEdgeToEdge()
        val start = System.currentTimeMillis()
        PlanetApplication.startTime = System.currentTimeMillis()
        setContent {
            AppSkinTheme {
                var privacyState by remember {
                    mutableStateOf(PrivacyConsentManager.appConsentState(this@MainActivity))
                }
                when (privacyState) {
                    AppConsentState.UNDECIDED -> PrivacyConsentScreen(
                        onAccept = {
                            PrivacyConsentManager.acceptAppConsent(this@MainActivity)
                            privacyState = AppConsentState.ACCEPTED
                            runConsentedLifecycleWork()
                        },
                        onDecline = {
                            PrivacyConsentManager.declineAppConsent(this@MainActivity)
                            privacyState = AppConsentState.DECLINED
                        },
                        onOpenPrivacy = {
                            Route.goReleaseInformation(
                                this@MainActivity,
                                ReleaseDocumentType.PRIVACY,
                            )
                        },
                        onOpenAgreement = {
                            Route.goReleaseInformation(
                                this@MainActivity,
                                ReleaseDocumentType.AGREEMENT,
                            )
                        },
                    )
                    AppConsentState.DECLINED -> PrivacyDeclinedScreen(
                        onReviewDecision = {
                            PrivacyConsentManager.clearDeclinedDecision(this@MainActivity)
                            privacyState = AppConsentState.UNDECIDED
                        },
                        onOpenPrivacy = {
                            Route.goReleaseInformation(
                                this@MainActivity,
                                ReleaseDocumentType.PRIVACY,
                            )
                        },
                        onOpenAgreement = {
                            Route.goReleaseInformation(
                                this@MainActivity,
                                ReleaseDocumentType.AGREEMENT,
                            )
                        },
                        onExit = { finishAffinity() },
                    )
                    AppConsentState.ACCEPTED -> {
                        val accountState by CampusToolsSession.state.collectAsState()
                        var availableUpdate by remember {
                            mutableStateOf<VersionCheckResult.Available?>(null)
                        }
                        LaunchedEffect(Unit) {
                            CampusToolsSession.restore()
                        }
                        LaunchedEffect(Unit) {
                            delay(1_800)
                            val result = AppUpdateChecker.check()
                            val update = result as? VersionCheckResult.Available
                            if (
                                update != null &&
                                AppUpdateCooldown.isPromptAllowed(
                                    this@MainActivity,
                                    update.version.latest_version_code,
                                )
                            ) {
                                availableUpdate = update
                            }
                        }
                        LaunchedEffect(accountState) {
                            val signedIn = accountState as? AccountSessionState.SignedIn
                            if (signedIn != null) {
                                SportsReservationRepository.activateAccount(signedIn.user.id)
                                LostFoundRepository.activateAccount(signedIn.user.id)
                            } else if (accountState is AccountSessionState.SignedOut) {
                                LostFoundRepository.activateGuest()
                            }
                        }
                        Surface(color = AppTheme.colors.bgPrimaryColor) {
                            MainScreen(navigator = mainTabNavigator)
                        }
                        availableUpdate?.let { update ->
                            AppUpdatePrompt(
                                update = update,
                                onLater = {
                                    AppUpdateCooldown.recordDismissed(
                                        this@MainActivity,
                                        update.version.latest_version_code,
                                    )
                                    availableUpdate = null
                                },
                                onSimulateUpdate = {
                                    availableUpdate = null
                                    Toast.makeText(
                                        this@MainActivity,
                                        "本地体验版未配置真实 APK 下载，不会执行安装",
                                        Toast.LENGTH_LONG,
                                    ).show()
                                },
                            )
                        }
                    }
                }
            }
        }
        if (PrivacyConsentManager.hasAppConsent(this)) notifyLocalScheduleComponents()
        Log.d("MainActivity", "用时 ${System.currentTimeMillis() - start}")
        // 版本检查仅访问“我的长理”后端；本地阶段不下载或安装 APK。
    }

    override fun onDestroy() {
        super.onDestroy()
        TabAnimationPool.clear()
    }

    override fun onStart() {
        super.onStart()
        if (!PrivacyConsentManager.hasAppConsent(this)) return
        // 账号系统已取消，不再拉取自研后端的用户资料。
        // 但从老版本升级上来时，MMKV 中原本由自研后端写入的头像/昵称已被
        // migrateLegacyCacheIfNeeded 清除，这里用已绑定的学号/密码做一次静默 SSO
        // 重新登录 + getLoginUser，把 MOOC(网络教学平台) 的用户名/头像写回 UserInfoManager。
        // 若未绑定、或 UserInfoManager.account 已有值，action 内部会直接跳过，无网络开销。
        store.dispatch(UserAction.RefreshMoocProfileSilently)
    }

    private fun runConsentedLifecycleWork() {
        store.dispatch(UserAction.initilaize())
        lifecycleScope.launch(Dispatchers.IO) {
            runCatching { RemoteConfigurationStore.refreshIfStale(30_000L) }
        }
        store.dispatch(UserAction.RefreshMoocProfileSilently)
        notifyLocalScheduleComponents()
    }

    private fun notifyLocalScheduleComponents() {
        sendBroadcast(Intent(this, com.mychangli.app.ScheduleSystemReceiver::class.java).apply {
            action = com.mychangli.app.WidgetUpdateCoordinator.ACTION_REFRESH_SCHEDULE
        })
    }

    override fun openDrawer() {
    }

    private fun setCustomDensity(activity: Activity, application: Application, designWidthDp: Int) {
        val appDisplayMetrics = application.resources.displayMetrics
        val targetDensity = appDisplayMetrics.widthPixels / designWidthDp.toFloat()
        val targetDensityDpi = (targetDensity * 160).toInt()
        var nonCompatScaleDensity = appDisplayMetrics.scaledDensity

        application.registerComponentCallbacks(object : ComponentCallbacks {
            override fun onConfigurationChanged(newConfig: Configuration) {
                if (newConfig.fontScale > 0) {
                    nonCompatScaleDensity = application.resources.displayMetrics.scaledDensity
                }
            }

            override fun onLowMemory() = Unit
        })

        val targetScaleDensity =
            targetDensity * (nonCompatScaleDensity / appDisplayMetrics.density)

        appDisplayMetrics.density = targetDensity
        appDisplayMetrics.densityDpi = targetDensityDpi
        appDisplayMetrics.scaledDensity = targetScaleDensity

        activity.resources.displayMetrics.density = targetDensity
        activity.resources.displayMetrics.densityDpi = targetDensityDpi
        activity.resources.displayMetrics.scaledDensity = targetScaleDensity
    }
}
