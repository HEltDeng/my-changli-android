package com.mychangli.app.core

import androidx.compose.foundation.layout.fillMaxSize
import android.Manifest
import android.app.Activity
import android.app.Application
import android.content.ComponentCallbacks
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.mychangli.app.common.api.DrawerController
import com.mychangli.app.BuildConfig
import com.mychangli.app.common.cache.CommonInfo
import com.mychangli.app.common.pool.TabAnimationPool
import com.mychangli.app.common.redux.action.UserAction
import com.mychangli.app.common.redux.store.UserStore
import com.mychangli.app.core.main.navigation.MainTabNavigator
import com.mychangli.app.core.main.ui.MainScreen
import com.mychangli.app.core.network.campustools.AccountSessionState
import com.mychangli.app.core.network.campustools.CampusToolsSession
import com.mychangli.app.core.network.campustools.RemoteConfigurationStore
import com.mychangli.app.core.notifications.AnnouncementNotificationCoordinator
import com.mychangli.app.core.notifications.AnnouncementPopupDialog
import com.mychangli.app.core.notifications.NotificationPermissionPreferences
import com.mychangli.app.core.network.campustools.InstallationTelemetry
import com.mychangli.app.core.privacy.PrivacyConsentManager
import com.mychangli.app.core.privacy.PrivacyConsentManager.AppConsentState
import com.mychangli.app.core.privacy.PrivacyConsentScreen
import com.mychangli.app.core.privacy.PrivacyDeclinedScreen
import com.mychangli.app.core.privacy.ReleaseDocumentType
import com.mychangli.app.feature.lostfound.data.LostFoundRepository
import com.mychangli.app.core.theme.AppSkinTheme
import com.mychangli.app.core.theme.AppTheme
import com.mychangli.app.core.update.AppUpdateCooldown
import com.mychangli.app.core.update.AppUpdateCoordinator
import com.mychangli.app.core.update.AppUpdatePolicyController
import com.mychangli.app.core.update.AppUpdatePolicyState
import com.mychangli.app.core.update.AppUpdatePrompt
import com.mychangli.app.core.update.VersionCheckResult
import com.mychangli.app.feature.common.ui.AnnouncementDetailActivity
import com.mychangli.app.utils.event.AppEventBus
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity(), DrawerController {
    private var notificationsAllowed by mutableStateOf(false)
    private fun readNotifications() = androidx.core.app.NotificationManagerCompat.from(this).areNotificationsEnabled()
    private val store by lazy { UserStore() }
    private val mainTabNavigator by lazy(LazyThreadSafetyMode.NONE) { MainTabNavigator() }

    override fun onResume() {
        super.onResume()
        notificationsAllowed = readNotifications()
        AppUpdateCoordinator.resumePendingInstall(this)
        if (!PrivacyConsentManager.hasAppConsent(this)) return
        store.dispatch(UserAction.initilaize())  //初始化用户信息，对游客模式无影响
        lifecycleScope.launch(Dispatchers.IO) {
            val bootstrap = runCatching {
                RemoteConfigurationStore.refreshIfStale(30_000L)
            }.getOrNull()
            runCatching {
                AnnouncementNotificationCoordinator.process(
                    this@MainActivity,
                    bootstrap?.announcements
                        ?: RemoteConfigurationStore.bootstrap.value?.announcements.orEmpty(),
                )
            }
        }
        lifecycleScope.launch {
            AppUpdatePolicyController.refresh(forceRefresh = true)
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
                var showNoticeList by remember { mutableStateOf(false) }
                val announcementPopup by AnnouncementNotificationCoordinator.popup.collectAsState()
                val notificationPermissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission(),
                ) { notificationsAllowed = readNotifications() }
                when (privacyState) {
                    AppConsentState.UNDECIDED -> PrivacyConsentScreen(
                        onAccept = {
                            PrivacyConsentManager.confirmCurrentPolicy(this@MainActivity, includeOptional = true)
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
                        onCoreOnly = {
                            PrivacyConsentManager.confirmCurrentPolicy(this@MainActivity, includeOptional = false)
                            privacyState = AppConsentState.ACCEPTED
                            runConsentedLifecycleWork()
                        },
                    )
                    AppConsentState.ACCEPTED -> {
                        val accountState by CampusToolsSession.state.collectAsState()
                        val updatePolicyState by AppUpdatePolicyController.state.collectAsState()
                        var suppressedOptionalVersion by remember { mutableStateOf<Int?>(null) }
                        LaunchedEffect(Unit) {
                            CampusToolsSession.restore()
                            com.mychangli.app.core.network.campustools.SchoolBindingSync.foreground()
                            InstallationTelemetry.foreground()
                            if (AppUpdatePolicyController.state.value is AppUpdatePolicyState.IDLE) {
                                AppUpdatePolicyController.refresh(forceRefresh = true)
                            }
                        }
                        LaunchedEffect(accountState) {
                            val signedIn = accountState as? AccountSessionState.SignedIn
                            if (signedIn != null) {
                                LostFoundRepository.activateAccount(signedIn.user.id)
                            } else if (accountState is AccountSessionState.SignedOut) {
                                LostFoundRepository.activateGuest()
                            }
                        }
                        val forceUpdate = when (val state = updatePolicyState) {
                            is AppUpdatePolicyState.FORCE_UPDATE -> state.update
                            is AppUpdatePolicyState.CHECKING ->
                                (state.previous as? VersionCheckResult.Available)
                                    ?.takeIf { it.mandatory }
                            else -> null
                        }
                        val optionalUpdate = (updatePolicyState as? AppUpdatePolicyState.OPTIONAL_UPDATE)
                            ?.update
                            ?.takeIf {
                                suppressedOptionalVersion != it.version.latest_version_code &&
                                AppUpdateCooldown.isPromptAllowed(
                                    this@MainActivity,
                                    it.version.latest_version_code,
                                )
                        }
                        when {
                            forceUpdate != null -> {
                                AppUpdatePrompt(
                                    update = forceUpdate,
                                    context = this@MainActivity,
                                    onLater = {},
                                    onRefreshPolicy = {
                                        lifecycleScope.launch {
                                            AppUpdatePolicyController.refresh(forceRefresh = true)
                                        }
                                    },
                                    onPrimaryAction = {
                                        AppUpdateCoordinator.begin(
                                            this@MainActivity,
                                            forceUpdate.version,
                                        )
                                    },
                                )
                            }

                            else -> {
                                if (notificationsAllowed) Surface(color = AppTheme.colors.bgPrimaryColor) {
                                    MainScreen(navigator = mainTabNavigator)
                                }
                                optionalUpdate?.takeIf { notificationsAllowed }?.let { update ->
                                    AppUpdatePrompt(
                                        update = update,
                                        context = this@MainActivity,
                                        onLater = {
                                            AppUpdateCooldown.recordDismissed(
                                                this@MainActivity,
                                                update.version.latest_version_code,
                                            )
                                            suppressedOptionalVersion =
                                                update.version.latest_version_code
                                        },
                                        onRefreshPolicy = {
                                            lifecycleScope.launch {
                                                AppUpdatePolicyController.refresh(forceRefresh = true)
                                            }
                                        },
                                        onPrimaryAction = {
                                            AppUpdateCoordinator.begin(this@MainActivity, update.version)
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
                if (privacyState == AppConsentState.ACCEPTED && !notificationsAllowed) {
                    androidx.activity.compose.BackHandler { finishAffinity() }
                    Surface(color = AppTheme.colors.overviewPageBackgroundColor, modifier = androidx.compose.ui.Modifier.fillMaxSize()) {}
                    AlertDialog(
                        containerColor = AppTheme.colors.bgCardColor,
                        onDismissRequest = { finishAffinity() },
                        properties = androidx.compose.ui.window.DialogProperties(dismissOnClickOutside = false),
                        title = { Text("开启通知提醒") },
                        text = { androidx.compose.foundation.layout.Column {
                            Text("用于接收上课提醒和重要服务通知")
                            TextButton(onClick = { showNoticeList = true }) { Text("查看重要服务公告") }
                        } },
                        dismissButton = { TextButton(onClick = { finishAffinity() }) { Text("退出应用") } },
                        confirmButton = { androidx.compose.material3.Button(onClick = {
                            val prefs = getSharedPreferences("notification_permission_v1", MODE_PRIVATE)
                            val requested = prefs.getBoolean("system_requested", false)
                            if (Build.VERSION.SDK_INT >= 33 && !requested) {
                                prefs.edit().putBoolean("system_requested", true).apply()
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName))
                        }) { Text("开启通知") } },
                    )
                }
                if(showNoticeList) {
                    val notices = RemoteConfigurationStore.bootstrap.value?.announcements.orEmpty()
                    AlertDialog(onDismissRequest = { showNoticeList = false }, title = { Text("重要服务公告") },
                        text = { androidx.compose.foundation.layout.Column {
                            if(notices.isEmpty()) Text("暂无已加载公告，请联网后重试")
                            notices.forEach { notice -> TextButton(onClick = {
                                startActivity(AnnouncementDetailActivity.intent(this@MainActivity, notice.title, notice.content, notice.publishedAt))
                            }) { Text(notice.title) } }
                        } }, confirmButton = { TextButton(onClick = { showNoticeList = false }) { Text("返回") } })
                }
                announcementPopup?.takeIf { notificationsAllowed }?.let { popup ->
                    AnnouncementPopupDialog(
                        announcement = popup,
                        onDismiss = {
                            AnnouncementNotificationCoordinator.dismissPopup(popup.id)
                        },
                        onOpenDetail = {
                            AnnouncementNotificationCoordinator.dismissPopup(popup.id)
                            startActivity(
                                AnnouncementDetailActivity.intent(
                                    context = this@MainActivity,
                                    title = popup.title,
                                    content = popup.content,
                                    publishedAt = popup.publishedAt,
                                ),
                            )
                        },
                    )
                }
            }
        }
        if (PrivacyConsentManager.hasAppConsent(this)) notifyLocalScheduleComponents()
        Log.d("MainActivity", "用时 ${System.currentTimeMillis() - start}")
        // 版本检查与自动更新共用 AppUpdateCoordinator；安装始终交给系统 PackageInstaller 确认。
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
            val bootstrap = runCatching {
                RemoteConfigurationStore.refreshIfStale(30_000L)
            }.getOrNull()
            runCatching {
                AnnouncementNotificationCoordinator.process(
                    this@MainActivity,
                    bootstrap?.announcements
                        ?: RemoteConfigurationStore.bootstrap.value?.announcements.orEmpty(),
                )
            }
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
