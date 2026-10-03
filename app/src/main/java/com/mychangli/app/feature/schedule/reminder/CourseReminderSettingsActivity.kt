package com.mychangli.app.feature.schedule.reminder

import android.Manifest
import androidx.compose.foundation.background
import androidx.activity.compose.BackHandler
import com.mychangli.app.core.theme.*
import androidx.core.app.NotificationManagerCompat
import android.app.NotificationManager
import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.mychangli.app.WidgetUpdateCoordinator
import com.mychangli.app.base.ComposeActivity
import com.mychangli.app.core.theme.AppTheme
import com.mychangli.app.feature.schedule.CourseScheduleRepository
import com.mychangli.app.feature.schedule.ScheduleCourse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CourseReminderSettingsActivity : ComposeActivity() {
    private var notificationPermissionGranted by mutableStateOf(false)
    private var exactAlarmGranted by mutableStateOf<Boolean?>(null)
    private var channelEnabled by mutableStateOf<Boolean?>(null)
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        notificationPermissionGranted = granted || hasNotificationPermission()
        if (notificationPermissionGranted) {
            CourseReminderPreferences.enabled = true
            lifecycleScope.launch(Dispatchers.IO) {
                CourseReminderScheduler.reschedule(this@CourseReminderSettingsActivity)
                WidgetUpdateCoordinator.updateAll(this@CourseReminderSettingsActivity)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        refreshPermissionState()
        setComposeContent {
            CourseReminderSettingsScreen(
                onBack = ::finish,
                notificationGranted = notificationPermissionGranted,
                exactGranted = exactAlarmGranted,
                channelEnabled = channelEnabled,
                openChannelSettings = { if (Build.VERSION.SDK_INT >= 26) startActivity(Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName).putExtra(Settings.EXTRA_CHANNEL_ID, ReminderChannels.currentId())) else openNotificationSettings() },
                refreshChannel = ::refreshPermissionState,
                requestNotification = ::requestNotificationPermission,
                openNotificationSettings = ::openNotificationSettings,
                openExactAlarmSettings = ::openExactAlarmSettings,
                openCourseSettings = {
                    startActivity(Intent(this, CourseReminderCoursesActivity::class.java))
                },
                openWidgets = {
                    startActivity(Intent(this, WidgetSetupActivity::class.java))
                },
            )
        }
    }

    override fun onResume() {
        super.onResume()
        refreshPermissionState()
    }

    private fun refreshPermissionState() {
        notificationPermissionGranted = hasNotificationPermission()
        exactAlarmGranted = runCatching { Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            getSystemService(AlarmManager::class.java).canScheduleExactAlarms() }.getOrNull()
        ReminderChannels.ensure(this)
        channelEnabled = runCatching { ReminderChannels.enabled(this) }.getOrNull()
    }

    private fun hasNotificationPermission() = NotificationManagerCompat.from(this).areNotificationsEnabled() &&
        (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 && !hasNotificationPermission()) notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) else openNotificationSettings()
    }

    private fun openNotificationSettings() {
        startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName))
    }

    private fun openExactAlarmSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            runCatching { startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:$packageName"))) }
        }
    }
}

@Composable
private fun CourseReminderSettingsScreen(
    onBack: () -> Unit,
    notificationGranted: Boolean,
    exactGranted: Boolean?,
    channelEnabled: Boolean?,
    openChannelSettings: () -> Unit,
    refreshChannel: () -> Unit,
    requestNotification: () -> Unit,
    openNotificationSettings: () -> Unit,
    openExactAlarmSettings: () -> Unit,
    openCourseSettings: () -> Unit,
    openWidgets: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var enabled by remember { mutableStateOf(CourseReminderPreferences.enabled) }
    var lead by remember { mutableStateOf(CourseReminderPreferences.leadMinutes) }
    var showDetails by remember { mutableStateOf(CourseReminderPreferences.showDetails) }
    var courses by remember { mutableStateOf<List<ScheduleCourse>>(emptyList()) }
    var explainPermission by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }

    fun reschedule(showMessage: Boolean = true) {
        scope.launch {
            CourseReminderScheduler.reschedule(context)
            WidgetUpdateCoordinator.updateAll(context)
            if (showMessage) message = "已按当前本地课表重新安排提醒"
        }
    }

    LaunchedEffect(Unit) { courses = CourseScheduleRepository.loadCourses(context) }
    LaunchedEffect(notificationGranted) {
        if (notificationGranted && CourseReminderPreferences.enabled) enabled = true
    }
    var page by remember { mutableStateOf("main") }
    var silent by remember { mutableStateOf(CourseReminderPreferences.silent) }
    var snooze by remember { mutableStateOf(CourseReminderPreferences.snoozeEnabled) }
    BackHandler(page != "main") { page = "main" }
    val state = if (!enabled) "已关闭" else if (notificationGranted && channelEnabled == true && courses.isNotEmpty()) "正常" else "待设置"
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(AppTheme.colors.overviewPageBackgroundColor)
            .statusBarsPadding().navigationBarsPadding().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { ReminderHeader(when(page) { "status" -> "提醒状态"; "mode" -> "提醒方式"; "lead" -> "提前时间"; else -> "上课提醒" }) { if (page == "main") onBack() else page = "main" } }
        if (page == "main") {
            item { SecondaryCard {
                SecondaryRow("上课提醒", control = { BrandSwitch(enabled) { checked ->
                    if (checked && !notificationGranted) explainPermission = true else {
                        enabled = checked; CourseReminderPreferences.enabled = checked; reschedule(false)
                    }
                } })
                SecondaryRow("提前时间", "${lead}分钟", { page = "lead" })
                SecondaryRow("显示教师和教室", control = { BrandSwitch(showDetails) {
                    showDetails = it; CourseReminderPreferences.showDetails = it
                } })
            } }
            item { SecondaryCard {
                SecondaryRow("课程提醒", onClick = openCourseSettings)
                SecondaryRow("提醒方式", if(silent) "静默" else "普通", { page = "mode" })
                SecondaryRow("桌面小组件", onClick = openWidgets)
                SecondaryRow("提醒状态", state, { page = "status" })
            } }
        } else if (page == "lead") {
            item { SecondaryCard { listOf(5,10,15,20,30).forEach { minutes ->
                SecondaryRow("${minutes}分钟", if(lead == minutes) "已选择" else "", {
                    lead = minutes; CourseReminderPreferences.leadMinutes = minutes; reschedule(false); page = "main"
                }, selected = lead == minutes)
            } } }
        } else if (page == "mode") {
            item { SecondaryCard {
                SecondaryRow("普通提醒", if(!silent) "已选择" else "", { silent = false; CourseReminderPreferences.silent = false; ReminderChannels.ensure(context); refreshChannel() }, selected = !silent)
                SecondaryRow("静默提醒", if(silent) "已选择" else "", { silent = true; CourseReminderPreferences.silent = true; ReminderChannels.ensure(context); refreshChannel() }, selected = silent)
                SecondaryRow("允许稍后提醒", control = { BrandSwitch(snooze) { snooze = it; CourseReminderPreferences.snoozeEnabled = it } })
                SecondaryRow("系统渠道设置", onClick = openChannelSettings)
            } }
            item { Text("静默提醒不播放声音或震动。通知中的“稍后5分钟 / 10分钟”延后当前课程提醒，课程结束或取消后停止。系统渠道和免打扰设置优先。", color = AppTheme.colors.secondaryTextColor) }
        } else {
            item { SecondaryCard {
                SecondaryRow("上课提醒", if(enabled) "已开启" else "已关闭")
                SecondaryRow("通知权限", if(notificationGranted) "已允许" else "未允许")
                SecondaryRow("通知渠道", when(channelEnabled) { true -> "已开启"; false -> "已关闭"; null -> "读取失败" })
                SecondaryRow("精确提醒", when { exactGranted == null -> "读取失败"; Build.VERSION.SDK_INT < 31 -> "无需额外授权"; exactGranted -> "可用"; else -> "未授权" })
                SecondaryRow("本地课程", if(courses.isEmpty()) "暂无课程" else "${courses.size}节")
                SecondaryRow("已安排提醒", "${CourseReminderPreferences.scheduledRequestCodes.size}项")
                SecondaryRow("管理通知权限", onClick = openNotificationSettings)
                SecondaryRow("管理通知渠道", onClick = openChannelSettings)
                if(Build.VERSION.SDK_INT >= 31) SecondaryRow("管理精确提醒权限", onClick = openExactAlarmSettings)
                SecondaryRow("重新安排提醒", onClick = { reschedule() })
            } }
            item { Text("依据本机课表安排通知。未获精确提醒能力时使用系统允许的普通调度，可能延迟；开启权限不能保证所有消息准时送达。", color = AppTheme.colors.secondaryTextColor) }
            if(message.isNotBlank()) item { Text(message, color = AppTheme.colors.functionalTextColor) }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }

    if (explainPermission) {
        AlertDialog(
            onDismissRequest = { explainPermission = false },
            title = { Text("允许上课提醒通知？") },
            text = { Text("开启后，我的长理会按本机课表在课程开始前发送通知。课程数据不会上传到服务器，你也可以随时在系统设置中关闭通知。") },
            confirmButton = { Button(onClick = { explainPermission = false; requestNotification() }) { Text("继续") } },
            dismissButton = { TextButton(onClick = { explainPermission = false }) { Text("暂不开启") } },
        )
    }
}

@Composable
internal fun ReminderHeader(title: String, onBack: () -> Unit) {
    SecondaryPageHeader(title, onBack)
}

@Composable
internal fun ReminderCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = AppTheme.colors.bgCardColor),
    ) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content) }
}

@Composable
private fun ReminderNavigationCard(
    title: String,
    subtitle: String,
    icon: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = AppTheme.colors.bgCardColor),
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                icon()
                Spacer(Modifier.padding(horizontal = 5.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, color = AppTheme.colors.primaryTextColor)
                Text(subtitle, color = AppTheme.colors.secondaryTextColor, fontSize = 13.sp)
            }
            Icon(Icons.Rounded.ChevronRight, "进入", tint = AppTheme.colors.secondaryTextColor)
        }
    }
}

@Composable
internal fun ReminderSwitch(title: String, subtitle: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, color = AppTheme.colors.primaryTextColor)
            Text(subtitle, color = AppTheme.colors.secondaryTextColor, fontSize = 12.sp)
        }
        BrandSwitch(checked = checked, onCheckedChange = onChecked)
    }
}
