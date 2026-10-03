package com.mychangli.app.core.privacy

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.mychangli.app.core.network.campustools.InstallationTelemetry

@Composable
fun DeviceStatisticsConsentDialog(onClose: () -> Unit) {
    AlertDialog(
        onDismissRequest = { InstallationTelemetry.decide(false); onClose() },
        title = { Text("可选设备与使用统计") },
        text = { Text("同意后，上报手机厂商、品牌、系统型号、Android 与 App 版本、安装标识及前台使用时间，用于安装去重、机型适配和使用统计。另将 Android ID 在本应用内派生，服务端再次加密派生，用于有限范围的设备去重；它不是永久手机编号，也不用于登录。不会采集 IMEI、通讯录或位置。\n\n设备与安装资料保留至撤回或注销清理；逐次使用事件保留 180 天。可在“设置—隐私与合规—设备与使用统计”关闭并撤回，删除可选设备和活跃资料；最小安装服务身份保留以支持账号与业务。拒绝不影响课表、成绩等功能。") },
        confirmButton = { TextButton(onClick = { InstallationTelemetry.decide(true); onClose() }) { Text("同意并开启") } },
        dismissButton = { TextButton(onClick = { InstallationTelemetry.decide(false); onClose() }) { Text("关闭并撤回 / 不开启") } },
    )
}

