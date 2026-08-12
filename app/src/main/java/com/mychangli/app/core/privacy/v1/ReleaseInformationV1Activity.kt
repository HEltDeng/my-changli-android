package com.mychangli.app.core.privacy

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mychangli.app.R
import com.mychangli.app.core.Route
import com.mychangli.app.core.theme.AppSkinTheme
import com.mychangli.app.core.theme.AppTheme

enum class ReleaseDocumentType(val value: String, val title: String) {
    PRIVACY("privacy", "隐私政策（V1 发布前审阅稿）"),
    AGREEMENT("agreement", "用户协议（V1 发布前审阅稿）"),
    THIRD_PARTY("third_party", "第三方 SDK / 服务"),
    PERSONAL_INFORMATION("personal_information", "个人信息清单"),
    ACCOUNT_DATA("account_data", "账号与数据管理");

    companion object {
        fun from(value: String?): ReleaseDocumentType =
            entries.firstOrNull { it.value == value } ?: PRIVACY
    }
}

private data class V1InformationSection(val title: String, val body: String)

class ReleaseInformationActivity : ComponentActivity() {
    companion object {
        private const val EXTRA_TYPE = "release_document_type"

        fun intent(context: Context, type: ReleaseDocumentType): Intent =
            Intent(context, ReleaseInformationActivity::class.java)
                .putExtra(EXTRA_TYPE, type.value)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val type = ReleaseDocumentType.from(intent.getStringExtra(EXTRA_TYPE))
        setContent { AppSkinTheme { V1InformationScreen(this, type) } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun V1InformationScreen(activity: ComponentActivity, type: ReleaseDocumentType) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(type.title, color = AppTheme.colors.titleTopColor) },
                navigationIcon = {
                    IconButton(onClick = activity::finish) {
                        Icon(
                            painter = painterResource(R.drawable.ic_back),
                            contentDescription = "返回",
                            tint = AppTheme.colors.titleTopColor,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppTheme.colors.bgTopBarColor,
                ),
            )
        },
    ) { paddingValues ->
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxSize()
                .background(AppTheme.colors.bgPrimaryColor)
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            Surface(
                color = AppTheme.colors.bgCardColor,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = NON_OFFICIAL_STATEMENT,
                    color = AppTheme.colors.primaryTextColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(18.dp),
                )
            }
            v1InformationSections(type).forEach { section ->
                Text(
                    text = section.title,
                    color = AppTheme.colors.primaryTextColor,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = section.body,
                    color = AppTheme.colors.secondaryTextColor,
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
                )
            }
            if (type == ReleaseDocumentType.ACCOUNT_DATA) {
                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = { Route.goAccountCenter(activity) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("管理我的长理账号") }
                OutlinedButton(
                    onClick = { AccountDataManager.clearSchoolBinding() },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("清理旧版本学校凭据与会话") }
                OutlinedButton(
                    onClick = { AccountDataManager.clearLocalLearningData() },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("清除本地课表与学习缓存") }
            }
            Text(
                text = "V1.0 发布前审阅稿 · privacyConsentVersion ${PrivacyConsentManager.CURRENT_APP_CONSENT_VERSION}",
                color = AppTheme.colors.greyTextColor,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 10.dp, bottom = 28.dp),
            )
        }
    }
}

private fun v1InformationSections(type: ReleaseDocumentType): List<V1InformationSection> =
    when (type) {
        ReleaseDocumentType.PRIVACY -> listOf(
            V1InformationSection(
                "V1 数据边界",
                "本版本不提供学校账号绑定、学校官方接口、校园地图、真实体育预约或真实支付，也不会要求用户输入学号、学校密码、验证码、学校 Cookie 或学校 token。",
            ),
            V1InformationSection(
                "自有服务",
                "用户主动登录、发布失物招领、提交反馈或使用资料库时，相关账号信息和用户提交内容仅发送至配置完成的我的长理自有后端。生产地址未配置时客户端保持关闭，不回连开发地址。",
            ),
            V1InformationSection(
                "本地数据与权限",
                "手动课表、课程提醒、桌面小组件和记账数据保存在设备本地。联网用于用户主动使用的自有服务和外部公开网页；通知、开机接收及精确闹钟仅服务课程提醒。",
            ),
        )
        ReleaseDocumentType.AGREEMENT -> listOf(
            V1InformationSection(
                "服务性质",
                "我的长理是个人开发者独立开发的非官方校园生活辅助工具，不代表学校，不构成学校授权或合作。",
            ),
            V1InformationSection(
                "首发限制",
                "V1 不提供学校官方业务履约、真实体育预约或真实支付。服务未配置或远端关闭时，应用不会伪造成功结果。",
            ),
            V1InformationSection(
                "用户内容",
                "用户应仅发布有权提交的内容，不得发布违法、侵权或不必要的敏感信息。失物招领支持举报与后台治理。",
            ),
        )
        ReleaseDocumentType.THIRD_PARTY -> listOf(
            V1InformationSection(
                "Android 本地组件",
                "本版本使用 AndroidX、Compose、Material、Room、MMKV、Retrofit、OkHttp、Coil、Glide 等应用运行与界面组件。调试工具不进入 Release。",
            ),
            V1InformationSection(
                "明确不包含",
                "V1 Release 不包含来源或授权尚未闭合的学校接口、地图定位二进制或支付 SDK。",
            ),
            V1InformationSection(
                "外部公开网页",
                "用户主动打开四六级、普通话或资料链接时，由对应网页运营方处理其网络请求。",
            ),
        )
        ReleaseDocumentType.PERSONAL_INFORMATION -> listOf(
            V1InformationSection(
                "发送至自有后端",
                "我的长理账号信息、installationId，以及用户主动提交的失物招领内容、可选图片、反馈和资料库交互信息。",
            ),
            V1InformationSection(
                "不会发送至自有后端",
                "学号、学校密码、验证码、学校 Cookie、学校 token、成绩、考试和学校账号会话。V1 不提供采集这些信息的入口。",
            ),
            V1InformationSection(
                "仅在本地",
                "手动课表、课程提醒、桌面小组件设置及记账数据。自有账号 token 由 Android Keystore 加密保存。",
            ),
        )
        ReleaseDocumentType.ACCOUNT_DATA -> listOf(
            V1InformationSection(
                "我的长理账号",
                "账号中心提供登录状态管理、退出和后端已支持的账号数据操作；服务未配置时不会假装操作成功。",
            ),
            V1InformationSection(
                "旧版本清理",
                "升级用户可单独清除旧完整版本遗留的学校凭据和会话。该操作不清除我的长理自有账号。",
            ),
            V1InformationSection(
                "本地学习数据",
                "用户可清除本地课表、课程提醒数据和学习缓存。",
            ),
        )
    }
