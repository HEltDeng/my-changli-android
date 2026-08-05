package com.mychangli.app.core.account.ui

import android.os.Bundle
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mychangli.app.base.ComposeActivity
import com.mychangli.app.core.Route
import com.mychangli.app.core.network.campustools.AccountSessionState
import com.mychangli.app.core.network.campustools.CampusToolsSession
import com.mychangli.app.core.network.campustools.authenticatedCall
import com.mychangli.app.core.theme.AppTheme
import com.mychangli.app.feature.common.compose_ui.ImeCompatibleTextField
import kotlinx.coroutines.launch

class AccountCenterActivity : ComposeActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!com.mychangli.app.core.account.AccountAccess.isSignedIn) {
            com.mychangli.app.core.account.AccountAccess.openOrRequestLogin(
                this,
                android.content.Intent(this, AccountCenterActivity::class.java),
            )
            finish()
            return
        }
        setComposeContent {
            AccountCenterScreen(
                onBack = ::finish,
                onSignedOut = { Route.goHomeForcibly(this) },
            )
        }
    }
}

@Composable
private fun AccountCenterScreen(
    onBack: () -> Unit,
    onSignedOut: () -> Unit,
) {
    val state by CampusToolsSession.state.collectAsState()
    val user = (state as? AccountSessionState.SignedIn)?.user
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var nickname by remember(user?.id) { mutableStateOf(user?.nickname.orEmpty()) }
    var reservationStudentId by remember(user?.id) {
        mutableStateOf(user?.reservationStudentId.orEmpty())
    }
    var reservationName by remember(user?.id) {
        mutableStateOf(user?.reservationName.orEmpty())
    }
    var reservationProfileMode by remember(user?.id) { mutableStateOf("OPTIONAL") }
    var reservationProfileConfirmed by remember(user?.id) { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var confirmAction by remember { mutableStateOf<String?>(null) }
    var deletePassword by remember { mutableStateOf("") }

    LaunchedEffect(user?.id) {
        if (user != null) {
            runCatching { authenticatedCall { it.reservationProfile() } }
                .onSuccess { reservationProfileMode = it.rules.reservationProfileMode }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Rounded.ArrowBack,
                    contentDescription = "返回",
                    tint = AppTheme.colors.primaryTextColor,
                )
            }
            Text(
                text = "账号与安全",
                color = AppTheme.colors.primaryTextColor,
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        if (user == null) {
            Text(
                "账号已退出",
                color = AppTheme.colors.secondaryTextColor,
                modifier = Modifier.padding(20.dp),
            )
            return@Column
        }

        Surface(
            color = AppTheme.colors.bgCardColor,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.padding(18.dp),
            ) {
                Text("我的长理账号", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                AccountRow("手机号", user.phoneMasked ?: "未提供")
                ImeCompatibleTextField(
                    value = nickname,
                    onValueChange = { nickname = it.take(30) },
                    label = "昵称",
                )
                Button(
                    enabled = nickname.isNotBlank() && nickname != user.nickname,
                    onClick = {
                        scope.launch {
                            runCatching { CampusToolsSession.updateNickname(nickname) }
                                .onSuccess { message = "昵称已更新" }
                                .onFailure { message = it.message ?: "更新失败" }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppTheme.colors.functionalTextColor
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("保存昵称")
                }
                if (message.isNotBlank()) {
                    Text(
                        message,
                        color = AppTheme.colors.secondaryTextColor,
                        fontSize = 13.sp,
                    )
                }
            }
        }

        Surface(
            color = AppTheme.colors.bgSecondaryColor,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp),
        ) {
            Text(
                text = "教务绑定与本账号严格分离。退出或切换我的长理账号不会删除你本机保存的教务绑定；学校密码与 Cookie 不会发送到我的长理后端。",
                color = AppTheme.colors.secondaryTextColor,
                fontSize = 13.sp,
                lineHeight = 20.sp,
                modifier = Modifier.padding(16.dp),
            )
        }

        Surface(
            color = AppTheme.colors.bgCardColor,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(18.dp),
            ) {
                Text("预约资料", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(
                    "请填写真实姓名和预约识别学号，用于场馆预约核验、账号查找及后续如适用的退款处理。若资料不真实、不完整或无法核验，可能影响预约服务、人工核验、账户查找及退款处理。",
                    color = AppTheme.colors.secondaryTextColor,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                )
                Text(
                    "这不是学校统一认证，不会登录教务系统，也不会读取课表、成绩或其他教务数据。",
                    color = AppTheme.colors.secondaryTextColor,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                )
                if (reservationProfileMode == "OFF") {
                    Text(
                        "当前预约资料收集已关闭，无需填写。",
                        color = AppTheme.colors.commonColor,
                        fontSize = 13.sp,
                    )
                } else {
                    ImeCompatibleTextField(
                        value = reservationName,
                        onValueChange = {
                            reservationName = it.take(40)
                            reservationProfileConfirmed = false
                        },
                        label = "真实姓名",
                        placeholder = "请填写用于预约核验的真实姓名",
                    )
                    ImeCompatibleTextField(
                        value = reservationStudentId,
                        onValueChange = {
                            reservationStudentId = it
                                .filter { char -> char.isLetterOrDigit() || char == '_' || char == '-' }
                                .take(32)
                            reservationProfileConfirmed = false
                        },
                        label = "预约识别学号",
                        placeholder = "由你自主填写",
                    )
                    Row(
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Checkbox(
                            checked = reservationProfileConfirmed,
                            onCheckedChange = { reservationProfileConfirmed = it },
                        )
                        Text(
                            "我已确认所填写的姓名和学号真实、准确，并同意仅用于预约核验、账号查找及相关订单服务。",
                            color = AppTheme.colors.primaryTextColor,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    }
                    Button(
                        enabled = reservationName.trim().length >= 2 &&
                            reservationStudentId.length >= 6 &&
                            reservationProfileConfirmed,
                        onClick = {
                            scope.launch {
                                runCatching {
                                    CampusToolsSession.updateReservationProfile(
                                        reservationStudentId,
                                        reservationName,
                                    )
                                }.onSuccess {
                                    reservationProfileConfirmed = false
                                    message = "预约资料已保存；之后新预约会使用新资料"
                                }.onFailure { message = it.message ?: "预约资料保存失败" }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("确认并保存预约资料")
                    }
                }
                if (user.reservationStudentId != null) {
                    TextButton(
                        onClick = { confirmAction = "deleteProfile" },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("删除预约资料", color = AppTheme.colors.errorRedColor)
                    }
                }
                Text(
                    "修改或删除不会改变历史预约中已经保存的核验资料快照。",
                    color = AppTheme.colors.secondaryTextColor,
                    fontSize = 12.sp,
                )
            }
        }

        OutlinedButton(
            onClick = { confirmAction = "switch" },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp),
        ) {
            Text("切换账号")
        }
        TextButton(
            onClick = { confirmAction = "logout" },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("退出登录", color = AppTheme.colors.secondaryTextColor)
        }
        TextButton(
            onClick = { confirmAction = "delete" },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
        ) {
            Text("注销账号", color = AppTheme.colors.errorRedColor)
        }
    }

    when (confirmAction) {
        "logout", "switch" -> AlertDialog(
            onDismissRequest = { confirmAction = null },
            title = { Text(if (confirmAction == "switch") "切换账号" else "退出登录") },
            text = {
                Text(
                    if (confirmAction == "switch") {
                        "退出当前账号后可登录其他账号，本地教务绑定不会被删除。"
                    } else {
                        "确认退出我的长理账号？"
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        CampusToolsSession.logout()
                        onSignedOut()
                    }
                }) { Text("确认") }
            },
            dismissButton = {
                TextButton(onClick = { confirmAction = null }) { Text("取消") }
            },
        )
        "delete" -> AlertDialog(
            onDismissRequest = { confirmAction = null },
            title = { Text("注销我的长理账号") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("账号注销后无法恢复；有效预约会被取消。请输入当前账号密码确认。")
                    ImeCompatibleTextField(
                        value = deletePassword,
                        onValueChange = { deletePassword = it.take(200) },
                        label = "账号密码",
                        password = true,
                    )
                    if (message.isNotBlank()) {
                        Text(message, color = AppTheme.colors.errorRedColor, fontSize = 13.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = deletePassword.length >= 8,
                    onClick = {
                        scope.launch {
                            runCatching { CampusToolsSession.deleteAccount(deletePassword) }
                                .onSuccess { onSignedOut() }
                                .onFailure { message = it.message ?: "注销失败" }
                        }
                    },
                ) { Text("确认注销", color = AppTheme.colors.errorRedColor) }
            },
            dismissButton = {
                TextButton(onClick = { confirmAction = null }) { Text("取消") }
            },
        )
        "deleteProfile" -> AlertDialog(
            onDismissRequest = { confirmAction = null },
            title = { Text("删除预约资料？") },
            text = {
                Text("删除后，新预约将不再使用当前姓名和识别学号；历史预约快照不会改变。")
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        runCatching { CampusToolsSession.deleteReservationProfile() }
                            .onSuccess {
                                reservationStudentId = ""
                                reservationName = ""
                                reservationProfileConfirmed = false
                                message = "预约资料已删除"
                                confirmAction = null
                            }
                            .onFailure { message = it.message ?: "删除失败" }
                    }
                }) { Text("确认删除", color = AppTheme.colors.errorRedColor) }
            },
            dismissButton = {
                TextButton(onClick = { confirmAction = null }) { Text("取消") }
            },
        )
        else -> Unit
    }
}

@Composable
private fun AccountRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            label,
            color = AppTheme.colors.secondaryTextColor,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.padding(4.dp))
        Text(value, color = AppTheme.colors.primaryTextColor)
    }
}
