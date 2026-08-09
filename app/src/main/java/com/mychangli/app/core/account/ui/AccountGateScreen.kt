package com.mychangli.app.core.account.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mychangli.app.core.theme.AppTheme
import com.mychangli.app.core.account.AccountInputRules
import com.mychangli.app.core.Route
import com.mychangli.app.core.privacy.ReleaseDocumentType
import com.mychangli.app.feature.common.compose_ui.ImeCompatibleTextField
import kotlinx.coroutines.launch

@Composable
fun AccountLoadingScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.bgPrimaryColor),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = AppTheme.colors.functionalTextColor)
    }
}

@Composable
fun AccountGateScreen(
    onLogin: suspend (phone: String, password: String) -> Unit,
    onRegister: suspend (phone: String, password: String, nickname: String) -> Unit,
    onAuthenticated: () -> Unit = {},
    onContinueAsGuest: (() -> Unit)? = null,
    reasonText: String = "登录后即可使用体育预约、失物招领、资料库和其他个人服务。课表与上课提醒可继续本地使用。",
) {
    var registerMode by rememberSaveable { mutableStateOf(false) }
    var phone by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var nickname by rememberSaveable { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.bgPrimaryColor)
            .statusBarsPadding()
            .imePadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 28.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(AppTheme.colors.functionalTextColor),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "长理",
                    color = AppTheme.colors.bgCardColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                )
            }
            Spacer(Modifier.height(22.dp))
            Text(
                text = if (registerMode) "创建我的长理账号" else "登录我的长理账号",
                color = AppTheme.colors.primaryTextColor,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = reasonText,
                color = AppTheme.colors.secondaryTextColor,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 8.dp),
            )

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                color = AppTheme.colors.bgSecondaryColor,
                shape = RoundedCornerShape(18.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "我的长理由个人开发者独立开发，是非官方校园辅助工具，与长沙理工大学不存在官方隶属或授权关系。",
                        color = AppTheme.colors.secondaryTextColor,
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                    )
                    TextButton(
                        onClick = {
                            Route.goReleaseInformation(context, ReleaseDocumentType.PRIVACY)
                        },
                        modifier = Modifier.align(Alignment.End),
                    ) { Text("查看完整隐私说明") }
                }
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                color = AppTheme.colors.bgCardColor,
                shape = RoundedCornerShape(22.dp),
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.padding(18.dp),
                ) {
                    if (registerMode) {
                        ImeCompatibleTextField(
                            value = nickname,
                            onValueChange = { nickname = it.take(30) },
                            label = "昵称",
                            placeholder = "例如：长理同学",
                        )
                    }
                    ImeCompatibleTextField(
                        value = phone,
                        onValueChange = { phone = it.filter(Char::isDigit).take(11) },
                        label = "手机号",
                        placeholder = "请输入 11 位手机号",
                        numeric = true,
                    )
                    ImeCompatibleTextField(
                        value = password,
                        onValueChange = { password = it.take(200) },
                        label = "密码",
                        placeholder = if (registerMode) "至少 8 位" else "请输入密码",
                        password = true,
                    )
                    if (errorMessage.isNotBlank()) {
                        Text(
                            text = errorMessage,
                            color = AppTheme.colors.errorRedColor,
                            fontSize = 13.sp,
                        )
                    }
                    Button(
                        enabled = !submitting && AccountInputRules.canSubmit(
                            phone = phone,
                            password = password,
                            nickname = nickname,
                            registering = registerMode,
                        ),
                        onClick = {
                            errorMessage = ""
                            submitting = true
                            scope.launch {
                                runCatching {
                                    if (registerMode) onRegister(phone, password, nickname)
                                    else onLogin(phone, password)
                                }.onSuccess {
                                    onAuthenticated()
                                }.onFailure {
                                    errorMessage = it.message ?: "操作失败，请检查网络后重试"
                                }
                                submitting = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppTheme.colors.functionalTextColor,
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                    ) {
                        if (submitting) {
                            CircularProgressIndicator(
                                color = AppTheme.colors.bgCardColor,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(20.dp),
                            )
                        } else {
                            Text(if (registerMode) "注册并进入" else "登录")
                        }
                    }
                    TextButton(
                        onClick = {
                            registerMode = !registerMode
                            errorMessage = ""
                        },
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    ) {
                        Text(
                            if (registerMode) "已有账号，直接登录" else "没有账号，创建一个",
                            color = AppTheme.colors.functionalTextColor,
                        )
                    }
                    if (onContinueAsGuest != null) {
                        TextButton(
                            onClick = onContinueAsGuest,
                            enabled = !submitting,
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                        ) {
                            Text("暂不登录", color = AppTheme.colors.secondaryTextColor)
                        }
                    }
                }
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                color = AppTheme.colors.bgSecondaryColor,
                shape = RoundedCornerShape(18.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "教务绑定只用于你主动查询课表、成绩与考试信息。学校账号密码、验证码、Cookie 和会话不会上传到我的长理后端，也不影响体育预约、失物招领或资料库使用。",
                        color = AppTheme.colors.secondaryTextColor,
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
