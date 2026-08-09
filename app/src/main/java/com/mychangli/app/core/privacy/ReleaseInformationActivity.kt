package com.mychangli.app.core.privacy

import android.app.Activity
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    PRIVACY("privacy", "隐私政策（草案）"),
    AGREEMENT("agreement", "用户协议（草案）"),
    THIRD_PARTY("third_party", "第三方 SDK / 服务"),
    PERSONAL_INFORMATION("personal_information", "个人信息清单"),
    ACCOUNT_DATA("account_data", "账号与数据管理");

    companion object {
        fun from(value: String?): ReleaseDocumentType =
            entries.firstOrNull { it.value == value } ?: PRIVACY
    }
}

data class ReleaseInformationSection(val title: String, val body: String)

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
        setContent {
            AppSkinTheme {
                ReleaseInformationScreen(activity = this, type = type)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReleaseInformationScreen(activity: Activity, type: ReleaseDocumentType) {
    var confirmAction by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf("") }
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
                    lineHeight = 22.sp,
                    modifier = Modifier.padding(18.dp),
                )
            }
            releaseInformationSections(type).forEach { section ->
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
                AccountDataActions(
                    onOpenAccount = { Route.goAccountCenter(activity) },
                    onClearBinding = { confirmAction = "binding" },
                    onClearLearning = { confirmAction = "learning" },
                    onOpenPrivacy = {
                        Route.goReleaseInformation(activity, ReleaseDocumentType.PRIVACY)
                    },
                )
            }
            if (message.isNotBlank()) {
                Text(message, color = AppTheme.colors.functionalTextColor, fontSize = 14.sp)
            }
            Text(
                text = "版本：DRAFT / privacyConsentVersion ${PrivacyConsentManager.CURRENT_APP_CONSENT_VERSION}",
                color = AppTheme.colors.greyTextColor,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 10.dp, bottom = 28.dp),
            )
        }
    }

    if (confirmAction != null) {
        val clearBinding = confirmAction == "binding"
        AlertDialog(
            onDismissRequest = { confirmAction = null },
            title = { Text(if (clearBinding) "清理学校绑定？" else "清除本地学习数据？") },
            text = {
                Text(
                    if (clearBinding) {
                        "将删除本机学号、加密教务密码、学校会话/Cookie、学校资料缓存和学习数据。此操作不会注销“我的长理”自有账号。"
                    } else {
                        "将清除本机课表、成绩、考试、MOOC 和小组件缓存，不会删除教务绑定或自有账号。"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (clearBinding) {
                        AccountDataManager.clearSchoolBinding()
                        message = "学校绑定与相关本地数据已清理"
                    } else {
                        AccountDataManager.clearLocalLearningData()
                        message = "本地学习数据已清理"
                    }
                    confirmAction = null
                }) { Text("确认清理") }
            },
            dismissButton = {
                TextButton(onClick = { confirmAction = null }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun AccountDataActions(
    onOpenAccount: () -> Unit,
    onClearBinding: () -> Unit,
    onClearLearning: () -> Unit,
    onOpenPrivacy: () -> Unit,
) {
    Spacer(Modifier.height(4.dp))
    Button(onClick = onOpenAccount, modifier = Modifier.fillMaxWidth()) {
        Text("管理自有账号 / 退出 / 注销 / 预约资料")
    }
    OutlinedButton(onClick = onClearBinding, modifier = Modifier.fillMaxWidth()) {
        Text("清理学校绑定与学校会话")
    }
    OutlinedButton(onClick = onClearLearning, modifier = Modifier.fillMaxWidth()) {
        Text("清除本地学习数据与缓存")
    }
    TextButton(onClick = onOpenPrivacy, modifier = Modifier.fillMaxWidth()) {
        Text("查看隐私政策")
    }
}

private fun releaseInformationSections(type: ReleaseDocumentType): List<ReleaseInformationSection> =
    when (type) {
        ReleaseDocumentType.PRIVACY -> listOf(
            ReleaseInformationSection(
                "文档状态",
                "这是发布前本地草案，不是最终法律文本。生产后端、域名、保存期限、服务器位置和本地依赖授权仍待专项审计确认。",
            ),
            ReleaseInformationSection(
                "三类数据流",
                "A. 手机号、账号密码、昵称、用户 ID、随机 installationId，以及用户主动提交的预约、失物招领、反馈等数据会发送至“我的长理”后端。\n\nB. 学号、教务密码、验证码、学校 Cookie/session 与查询请求直接用于学校服务；当前可审计客户端逻辑未将这些学校凭据发送到“我的长理”后端。\n\nC. 课表、成绩、考试、MOOC、校园卡/电量查询结果等主要保存在设备本地；具体字段见个人信息清单。",
            ),
            ReleaseInformationSection(
                "权限与第三方",
                "定位只在用户进入校园地图并单独同意后由高德相关服务处理；相机、图片、通知只在用户主动使用对应功能时请求。所有联网服务会自然获得 IP 和基础网络日志。",
            ),
            ReleaseInformationSection(
                "删除与撤回",
                "用户可在“账号与数据管理”退出/注销自有账号、清理学校绑定、学校会话、课程与查询缓存，并管理预约资料。反馈等部分服务端数据尚无用户自助删除接口，属于待生产后端审计确认事项。",
            ),
        )
        ReleaseDocumentType.AGREEMENT -> listOf(
            ReleaseInformationSection(
                "服务性质",
                "我的长理提供非官方校园辅助功能，不代表学校，不保证学校或第三方服务持续可用。学校页面、认证系统和外部查询服务由对应运营方提供。",
            ),
            ReleaseInformationSection(
                "用户责任",
                "请仅使用本人有权使用的账号和资料，不提交违法、侵权或不必要的敏感信息。请在可信设备上保存学校凭据，并妥善保护自有账号密码。",
            ),
            ReleaseInformationSection(
                "功能边界",
                "第一版不提供真实支付，广告保持关闭。尚未部署的服务会显示“服务暂未开放”，不会以旧项目或测试服务冒充生产服务。",
            ),
            ReleaseInformationSection(
                "草案说明",
                "本协议为发布前工程草案。争议处理、运营主体联系方式和正式生效日期需在生产主体与基础设施确定后补充。",
            ),
        )
        ReleaseDocumentType.THIRD_PARTY -> listOf(
            ReleaseInformationSection(
                "高德相关服务",
                "用于校园地图与定位。仅在用户进入地图并单独同意定位后启用；可能处理位置、IP、设备和网络信息。Lite3DMap.jar 的来源、版本与分发授权仍为 BLOCKER。",
            ),
            ReleaseInformationSection(
                "长沙理工大学外部服务",
                "统一身份认证、教务、MOOC、选课、校园卡、电量、官网和图书馆页面由学校相关系统提供。用户主动使用时，学校账号、Cookie 和查询参数直接发送到相应学校服务。",
            ),
            ReleaseInformationSection(
                "其他外部页面",
                "四六级查询由教育考试相关外部网站提供，普通话查询由教育政务外部网站提供，源码/资源链接可能打开 GitHub。页面会自然获得 IP、浏览器标识和用户主动填写的数据。",
            ),
            ReleaseInformationSection(
                "不作为数据接收方列示的依赖",
                "AndroidX、Compose、Material、Room、MMKV、图片/UI 库等普通本地依赖不会仅因被引用就被虚构为个人信息接收方；若实际联网行为发生变化，将在后续版本更新清单。",
            ),
        )
        ReleaseDocumentType.PERSONAL_INFORMATION -> listOf(
            ReleaseInformationSection(
                "发送至“我的长理”后端",
                "自有账号：手机号、账号密码、昵称、用户 ID、随机 installationId。\n体育预约：用户主动填写的姓名、学号、场馆、日期、时段、场地。\n社区工具：失物招领内容、地点、时间、可选图片，用户反馈和资源投诉。\n服务端保存期限、服务器位置、备份及日志删除策略：待生产后端审计确认。",
            ),
            ReleaseInformationSection(
                "直接发送给学校/外部服务",
                "学校服务：学号、教务密码、验证码、Cookie/session、查询条件，以及学校返回的课表、成绩、考试、MOOC、校园卡/电量等结果。\n高德：用户单独同意后的定位及 SDK 运行所需网络/设备信息。\n外部 Web 页面：IP、浏览器标识和用户主动输入内容。",
            ),
            ReleaseInformationSection(
                "仅在设备本地处理或缓存",
                "加密保存的教务密码和 MOOC Cookie；明文或结构化缓存的学号、课表、成绩、考试、MOOC、宿舍/电量、学校头像与资料；自有账号 token 使用 Android Keystore 加密保存。图片选择与裁剪可先在本机处理，用户发布时才上传目标服务。",
            ),
            ReleaseInformationSection(
                "设备、IP、日志与通知",
                "installationId 是 App 随机 UUID，不是硬件序列号。所有网络接收方会自然获得 IP。Release 不记录网络响应正文；Debug 只记录不含 body 且已脱敏的基础请求信息。通知只在用户开启提醒等功能后申请。",
            ),
        )
        ReleaseDocumentType.ACCOUNT_DATA -> listOf(
            ReleaseInformationSection(
                "自有账号",
                "账号中心提供切换账号、退出登录、注销账号和预约资料删除。账号注销与预约资料删除依赖已配置且可用的“我的长理”后端；服务未开放时不会假装已删除。",
            ),
            ReleaseInformationSection(
                "学校绑定与本地数据",
                "可独立清理学号、加密教务密码、学校 Cookie/session、学校资料缓存、课程/成绩/考试/MOOC 数据。清理学校绑定不会注销自有账号。",
            ),
            ReleaseInformationSection(
                "当前限制",
                "反馈、投诉等部分服务端数据未发现用户自助删除接口；生产后端需补齐或提供明确联系渠道。本页不会编造尚不存在的删除能力。",
            ),
        )
    }
