package com.mychangli.app.core.privacy

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mychangli.app.core.theme.AppTheme

const val NON_OFFICIAL_STATEMENT =
    "本应用由个人开发者独立开发，是面向校园生活场景的非官方辅助工具，与长沙理工大学不存在官方隶属或授权关系。"

@Composable
fun PrivacyConsentScreen(
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenAgreement: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.bgPrimaryColor)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 28.dp),
    ) {
        Text(
            text = "欢迎使用我的长理",
            color = AppTheme.colors.primaryTextColor,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "请先了解应用身份与个人信息处理方式",
            color = AppTheme.colors.secondaryTextColor,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 8.dp),
        )
        Surface(
            color = AppTheme.colors.bgCardColor,
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.padding(20.dp),
            ) {
                Text(
                    text = NON_OFFICIAL_STATEMENT,
                    color = AppTheme.colors.primaryTextColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 24.sp,
                )
                Text(
                    text = "同意后，应用才会执行远程配置、版本检查、自有账号恢复等非必要联网。通知权限仍只在你主动启用提醒时另行申请。",
                    color = AppTheme.colors.secondaryTextColor,
                    fontSize = 14.sp,
                    lineHeight = 21.sp,
                )
                Text(
                    text = "用户主动使用校园查询时，可输入本人学校账号及按学校登录流程出现的验证码；学号、学校密码、验证码、学校 Cookie 和学校 token 不会发送到“我的长理”自有后端。",
                    color = AppTheme.colors.secondaryTextColor,
                    fontSize = 14.sp,
                    lineHeight = 21.sp,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onOpenPrivacy) { Text("隐私政策") }
                    TextButton(onClick = onOpenAgreement) { Text("用户协议") }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onAccept,
            colors = ButtonDefaults.buttonColors(
                containerColor = AppTheme.colors.functionalTextColor,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        ) { Text("同意并继续", fontWeight = FontWeight.Bold) }
        OutlinedButton(
            onClick = onDecline,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
                .height(52.dp),
        ) { Text("暂不同意") }
        Text(
            text = "暂不同意不会反复弹窗。你仍可查看本地协议说明，之后可主动重新选择。",
            color = AppTheme.colors.greyTextColor,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}

@Composable
fun PrivacyDeclinedScreen(
    onReviewDecision: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenAgreement: () -> Unit,
    onExit: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.bgPrimaryColor)
            .statusBarsPadding()
            .padding(28.dp),
    ) {
        Text(
            text = "已选择暂不同意",
            color = AppTheme.colors.primaryTextColor,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "应用不会启动非必要联网或个人信息功能。你可以查看本地说明、重新选择，或退出应用。",
            color = AppTheme.colors.secondaryTextColor,
            fontSize = 15.sp,
            lineHeight = 22.sp,
            modifier = Modifier.padding(top = 12.dp, bottom = 20.dp),
        )
        Button(onClick = onReviewDecision, modifier = Modifier.fillMaxWidth()) {
            Text("重新选择")
        }
        Row(modifier = Modifier.padding(top = 8.dp)) {
            TextButton(onClick = onOpenPrivacy) { Text("隐私政策") }
            TextButton(onClick = onOpenAgreement) { Text("用户协议") }
        }
        TextButton(onClick = onExit) { Text("退出应用") }
    }
}
