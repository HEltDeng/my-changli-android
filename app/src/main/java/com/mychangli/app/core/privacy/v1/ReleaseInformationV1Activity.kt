package com.mychangli.app.core.privacy

import android.content.Context
import androidx.compose.runtime.collectAsState
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.annotation.StringRes
import com.mychangli.app.R
import com.mychangli.app.core.Route
import com.mychangli.app.core.theme.AppSkinTheme
import com.mychangli.app.core.theme.AppTheme

enum class ReleaseDocumentType(val value: String, @param:StringRes val titleRes: Int) {
    STATISTICS("statistics", R.string.statistics_settings_title),
    PRIVACY("privacy", R.string.release_document_title_privacy),
    AGREEMENT("agreement", R.string.release_document_title_agreement),
    THIRD_PARTY("third_party", R.string.release_document_title_third_party),
    PERSONAL_INFORMATION("personal_information", R.string.release_document_title_personal_information),
    ACCOUNT_DATA("account_data", R.string.release_document_title_account_data);

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
                title = { Text(stringResource(type.titleRes), color = AppTheme.colors.titleTopColor) },
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
            if (type == ReleaseDocumentType.PRIVACY) {
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
            }
            v1InformationSections(
                type = type,
                androidComponentsDescription = stringResource(
                    R.string.release_third_party_android_components,
                ),
            ).forEach { section ->
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
            if (type == ReleaseDocumentType.PRIVACY) {
                Text("可选项目均不影响无关校园查询；基础协议同意不代表开启可选同步。", color = AppTheme.colors.secondaryTextColor)
                OptionalDataEntry(activity)
            }
            if (type == ReleaseDocumentType.STATISTICS) OptionalDataControls()
            if (type == ReleaseDocumentType.ACCOUNT_DATA) {
                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = { Route.goAccountCenter(activity) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("管理我的长理账号") }
                OutlinedButton(
                    onClick = { AccountDataManager.clearSchoolBinding() },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("解除学校账号绑定并清理会话") }
                OutlinedButton(
                    onClick = { AccountDataManager.clearLocalLearningData() },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("清除本地课表与学习缓存") }
            }
            Text(
                text = stringResource(R.string.release_document_footer),
                color = AppTheme.colors.greyTextColor,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 10.dp, bottom = 28.dp),
            )
        }
    }
}

private fun v1InformationSections(
    type: ReleaseDocumentType,
    androidComponentsDescription: String,
): List<V1InformationSection> =
    when (type) {
        ReleaseDocumentType.STATISTICS -> listOf(
            V1InformationSection("安装与使用统计", "用于统计安装与使用情况，帮助优化机型适配和功能体验。开启后处理品牌、型号、系统与 App 版本、安装及设备辅助标识和前台使用记录。它不是下载次数，也不代表绝对人数。逐次记录最多保留 180 天；关闭并撤回会停止采集、清理待发数据，联网后删除可选设备及使用记录。"),
            V1InformationSection("学校及学号同步", "单独开启后，学校绑定成功或你确认现有有效绑定时，同步学校、学号、绑定状态、来源和必要时间，用于账号识别与服务支持。以当前我的长理账号关联，学号保留前导零。学校校验发生在客户端，不是后台独立的官方实名认证。学校密码、Cookie、验证码、令牌、课表和成绩均不上传自有后端。解绑保留历史关联至撤回或注销；关闭并撤回删除本安装的学校关联。"),
        )
        ReleaseDocumentType.PRIVACY -> listOf(
            V1InformationSection(
                "学校账号与校园查询",
                "用户主动绑定学校账号时，App 会处理学号、学校密码及按学校登录流程出现的验证码，并直接连接长沙理工大学统一认证、教务、网络教学和校园卡服务，用于查询用户本人的课表、成绩、考试、空教室、MOOC、校历、电量与校园卡相关信息。学校密码、Cookie/session 和校园查询结果不会发送至“我的长理”自有后端。学校及学号仅在单独同意后同步；其他学校信息仍在本机处理。",
            ),
            V1InformationSection(
                "传输与本地保存",
                "统一认证等支持 HTTPS 的学校服务使用 HTTPS。学校现有教务 xk.csust.edu.cn 与网络教学 pt.csust.edu.cn 仍要求 HTTP，本版本仅对这两个明确域名允许兼容访问；该链路的 Cookie/session 与返回数据存在被同一网络中的第三方观察或篡改的固有风险。学校密码由 Android Keystore + AES-GCM 加密保存在本机，运行会话以本机处理为主；课表、成绩、考试、校历和 MOOC 等必要缓存保存在设备本地。请仅在可信网络和可信设备使用。",
            ),
            V1InformationSection(
                "自有服务",
                "本次安装的随机标识和安装凭证用于建立自有服务身份，独立于平台登录账号。在单独同意“安装与使用统计”后，App 才读取并上报厂商、品牌、系统型号、Android/API 版本、App 版本及前台使用事件，并在本机将 ANDROID_ID 作应用隔离派生，服务端再使用仓库外密钥 HMAC 派生保存设备辅助标识。该标识仍是可关联数据，仅在应用签名、系统用户和设备等范围内辅助去重，不用于登录或恢复注销账号。原始 ANDROID_ID 仅在本机参与应用隔离派生，不上传；不读取 IMEI、IMSI、硬件序列号、通讯录、广告标识、应用列表和精确位置。拒绝不影响课表、成绩等无关功能。可在“隐私与合规—隐私政策—安装与使用统计”关闭并撤回；撤回立即停止采集、清除待发统计资料，在线提交删除请求，离线时下次前台联网重试。服务端删除可选机型、设备关联及前台事件，保留最小安装服务身份和历史激活事实。逐次事件最多保留 180 天，设备资料保留至撤回或关联账号注销清理；备份按既定灾备保留流程限制访问。学校密码和校园查询结果不上传自有后端。另经单独确认，仅同步学校标识、字符串学号、绑定状态、客户端校验来源及必要时间，用于当前平台账号识别与服务支持；学校同步与设备统计分别控制。",
            ),
            V1InformationSection(
                "本地数据与权限",
                "学校绑定、学校缓存、手动课表、课程提醒、桌面小组件和记账数据保存在设备本地。联网用于学校查询、自有服务和用户主动打开的外部网页；通知、开机接收及精确闹钟仅服务课程提醒。用户可在账号与数据管理中解绑学校账号并清除本地学习数据。",
            ),
        )
        ReleaseDocumentType.AGREEMENT -> listOf(
            V1InformationSection(
                "服务性质",
                "我的长理是个人开发者独立开发的非官方校园生活辅助工具，不代表学校，不构成学校授权或合作。",
            ),
            V1InformationSection(
                "首发限制",
                "学校查询仅作为用户本人访问学校既有服务的非官方辅助入口。V1 不提供校园地图、真实体育预约或真实支付；服务不可用时应用不会伪造查询、预约或支付成功结果。",
            ),
            V1InformationSection(
                "用户内容",
                "用户应仅发布有权提交的内容，不得发布违法、侵权或不必要的敏感信息。失物招领支持举报与后台治理。",
            ),
        )
        ReleaseDocumentType.THIRD_PARTY -> listOf(
            V1InformationSection(
                "Android 本地组件",
                androidComponentsDescription,
            ),
            V1InformationSection(
                "校园服务依赖",
                "学校登录与校园查询使用经原权利人项目专项授权的 CSUSTDataGet 1.1.4 安全日志关闭衍生构建。该授权允许本项目使用、必要修改、依赖构建及随 APK/AAB 分发，不等于长沙理工大学官方授权，也不被表述为通用开源许可证。",
            ),
            V1InformationSection(
                "明确不包含",
                "V1 不包含 Lite3DMap、AMap 地图/定位 SDK、支付 SDK或真实体育履约实现。",
            ),
            V1InformationSection(
                "外部公开网页",
                "用户主动打开四六级、普通话或资料链接时，由对应网页运营方处理其网络请求。",
            ),
        )
        ReleaseDocumentType.PERSONAL_INFORMATION -> listOf(
            V1InformationSection(
                "发送至自有后端",
                "本次安装的随机标识和安装凭证用于建立自有服务身份，独立于平台登录账号。在单独同意“安装与使用统计”后，App 才读取并上报厂商、品牌、系统型号、Android/API 版本、App 版本及前台使用事件，并在本机将 ANDROID_ID 作应用隔离派生，服务端再使用仓库外密钥 HMAC 派生保存设备辅助标识。该标识仍是可关联数据，仅在应用签名、系统用户和设备等范围内辅助去重，不用于登录或恢复注销账号。原始 ANDROID_ID 仅在本机参与应用隔离派生，不上传；不读取 IMEI、IMSI、硬件序列号、通讯录、广告标识、应用列表和精确位置。拒绝不影响课表、成绩等无关功能。可在“隐私与合规—隐私政策—安装与使用统计”关闭并撤回；撤回立即停止采集、清除待发统计资料，在线提交删除请求，离线时下次前台联网重试。服务端删除可选机型、设备关联及前台事件，保留最小安装服务身份和历史激活事实。逐次事件最多保留 180 天，设备资料保留至撤回或关联账号注销清理；备份按既定灾备保留流程限制访问。学校密码和校园查询结果不上传自有后端。另经单独确认，仅同步学校标识、字符串学号、绑定状态、客户端校验来源及必要时间，用于当前平台账号识别与服务支持；学校同步与设备统计分别控制。",
            ),
            V1InformationSection(
                "不会发送至自有后端",
                "学校密码、验证码、学校 Cookie/session、学校 token，以及课表、成绩、考试、MOOC、校历、电量和校园卡查询结果。它们仅在用户主动使用相应功能时发往对应学校服务或在本机处理。",
            ),
            V1InformationSection(
                "仅在本地",
                "未同意同步的学校账号标识、经 Android Keystore + AES-GCM 加密的学校密码、学校会话、校园查询缓存、手动课表、课程提醒、桌面小组件设置及记账数据。自有账号 token 也由 Android Keystore 加密保存。",
            ),
        )
        ReleaseDocumentType.ACCOUNT_DATA -> listOf(
            V1InformationSection(
                "我的长理账号",
                "账号中心提供登录状态管理、退出和后端已支持的账号数据操作；服务未配置时不会假装操作成功。",
            ),
            V1InformationSection(
                "学校账号解绑",
                "用户可单独解除学校账号绑定并清除学校凭据、Cookie/session 和校园查询缓存。该操作不清除“我的长理”自有账号。",
            ),
            V1InformationSection(
                "本地学习数据",
                "用户可清除本地课表、课程提醒数据和学习缓存。",
            ),
        )
    }

@Composable
private fun OptionalDataEntry(activity: ComponentActivity) {
    val owner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    var status by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
    var schoolStatus by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
    androidx.compose.runtime.DisposableEffect(owner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                status = if (com.mychangli.app.core.network.campustools.InstallationTelemetry.choice(activity) == 1) "已开启" else "已关闭"
                schoolStatus = com.mychangli.app.core.network.campustools.SchoolBindingSync.status()
            }
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    Button(onClick = { Route.goReleaseInformation(activity, ReleaseDocumentType.STATISTICS) },
        modifier = Modifier.fillMaxWidth()) { Text("安装与使用统计 · " + status) }
    OutlinedButton(onClick = { Route.goReleaseInformation(activity, ReleaseDocumentType.STATISTICS) },
        modifier = Modifier.fillMaxWidth()) { Text("学校及学号同步 · " + schoolStatus) }
}

@Composable
private fun OptionalDataControls() {
    val updates = com.mychangli.app.core.network.campustools.SchoolBindingSync.updates.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    var statistics by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(com.mychangli.app.core.network.campustools.InstallationTelemetry.choice(context) == 1) }
    var school by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(com.mychangli.app.core.network.campustools.SchoolBindingSync.allowed()) }
    var schoolStatus by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(com.mychangli.app.core.network.campustools.SchoolBindingSync.status()) }
    androidx.compose.runtime.LaunchedEffect(updates.value) { schoolStatus = com.mychangli.app.core.network.campustools.SchoolBindingSync.status() }
    val accepted = PrivacyConsentManager.hasAppConsent(context)
    if (!accepted) Text("请先在欢迎页确认基础政策；可选项目可随后在本页单独开启。", color = AppTheme.colors.functionalTextColor)
    Text("安装与使用统计：" + if (statistics) "已开启" else "已关闭")
    androidx.compose.material3.Switch(checked = statistics, enabled = accepted, onCheckedChange = {
        statistics = it
        com.mychangli.app.core.network.campustools.InstallationTelemetry.decide(it)
    })
    Text("学校及学号同步：" + schoolStatus)
    androidx.compose.material3.Switch(checked = school, enabled = accepted, onCheckedChange = {
        school = it
        com.mychangli.app.core.network.campustools.SchoolBindingSync.decide(it)
        schoolStatus = com.mychangli.app.core.network.campustools.SchoolBindingSync.status()
    })
    if (school && accepted) OutlinedButton(onClick = {
        com.mychangli.app.core.network.campustools.SchoolBindingSync.decide(true)
        schoolStatus = com.mychangli.app.core.network.campustools.SchoolBindingSync.status()
    }, modifier = Modifier.fillMaxWidth()) { Text("确认同步当前账号的有效学校绑定") }
    Text("关闭即停止对应采集并请求撤回；离线时删除请求将在下次前台联网发送。不会解除本机学校绑定。", color = AppTheme.colors.secondaryTextColor, fontSize = 13.sp)
}
