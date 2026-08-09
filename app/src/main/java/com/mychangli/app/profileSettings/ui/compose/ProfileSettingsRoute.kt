package com.mychangli.app.profileSettings.ui.compose

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Feedback
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.mychangli.app.BuildConfig
import com.mychangli.app.R
import com.mychangli.app.common.data.local.mmkv.StudentInfoManager
import com.mychangli.app.common.data.local.mmkv.UserInfoManager
import com.mychangli.app.core.PlanetApplication
import com.mychangli.app.core.Route
import com.mychangli.app.core.privacy.ReleaseDocumentType
import com.mychangli.app.core.network.campustools.AccountSessionState
import com.mychangli.app.core.network.campustools.CampusToolsSession
import com.mychangli.app.core.theme.AppSkinTheme
import com.mychangli.app.core.theme.AppTheme
import com.mychangli.app.profileSettings.ui.model.SettingItemUiModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

private const val OPEN_SOURCE_URL = "https://github.com/CreaMakers/changli-planet-app"

private data class ProfileBindingSnapshot(
    val studentId: String,
    val isBound: Boolean
)

private fun readProfileBindingSnapshot(): ProfileBindingSnapshot {
    val isBound = StudentInfoManager.hasCompleteBinding
    return ProfileBindingSnapshot(
        studentId = StudentInfoManager.studentId.takeIf { isBound }.orEmpty(),
        isBound = isBound
    )
}

@Composable
fun ProfileSettingsRoute(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var bindingSnapshot by remember { mutableStateOf(readProfileBindingSnapshot()) }
    val studentId = bindingSnapshot.studentId
    val isBound = bindingSnapshot.isBound
    val studentIdLabel = remember(studentId) { maskedStudentId(studentId) }
    val accountState by CampusToolsSession.state.collectAsState()
    val accountUser = (accountState as? AccountSessionState.SignedIn)?.user
    val avatarUrl = UserInfoManager.userAvatar.takeIf { it.isNotBlank() }
    var showCacheDialog by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                bindingSnapshot = readProfileBindingSnapshot()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    ProfileSettingsScreen(
        modifier = modifier,
        items = createSettingItems(),
        studentIdLabel = studentIdLabel,
        displayName = accountUser?.nickname ?: "长理同学",
        accountLabel = accountUser?.phoneMasked ?: "我的长理账号",
        avatarData = avatarUrl,
        isBound = isBound,
        onItemClick = { item ->
            if (item.id == "4") {
                showCacheDialog = true
            } else {
                handleSettingItemClick(context, item)
            }
        },
        onPrimaryClick = {
            // 切换学号 / 绑定学号均直接跳转绑定页，旧数据等绑定成功后再清理。
            if (isBound) {
                Route.goBindingUser(context, isSwitchAccount = true)
            } else {
                Route.goBindingUser(context)
            }
        }
    )

    if (showCacheDialog) {
        ConfirmDialog(
            title = "清除工具缓存",
            content = "确定清除实用工具产生的本地缓存吗？",
            onDismiss = { showCacheDialog = false },
            onConfirm = {
                PlanetApplication.clearContentCache()
                showCacheDialog = false
            }
        )
    }
}

@Composable
private fun ProfileSettingsScreen(
    items: List<SettingItemUiModel>,
    studentIdLabel: String,
    avatarData: Any?,
    isBound: Boolean,
    onItemClick: (SettingItemUiModel.Option) -> Unit,
    onPrimaryClick: () -> Unit,
    displayName: String = "长理同学",
    accountLabel: String = "我的长理账号",
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppTheme.colors.bgPrimaryColor)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "我的",
            color = AppTheme.colors.primaryTextColor,
            fontSize = 30.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(top = 16.dp, start = 4.dp)
        )
        Text(
            text = "账号与应用设置",
            color = AppTheme.colors.secondaryTextColor,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 2.dp, start = 4.dp)
        )

        Spacer(modifier = Modifier.height(18.dp))
        ProfileHeader(
            studentIdLabel = studentIdLabel,
            displayName = displayName,
            accountLabel = accountLabel,
            avatarData = avatarData,
            isBound = isBound
        )

        Spacer(modifier = Modifier.height(16.dp))
        SettingsCard(
            items = items,
            isBound = isBound,
            onItemClick = onItemClick,
            onPrimaryClick = onPrimaryClick
        )
        Spacer(modifier = Modifier.height(112.dp))
    }
}

@Composable
private fun ProfileHeader(
    studentIdLabel: String,
    displayName: String,
    accountLabel: String,
    avatarData: Any?,
    isBound: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = AppTheme.colors.bgTopBarColor,
        border = BorderStroke(1.dp, AppTheme.colors.outlineLowContrastColor)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(avatarData)
                    .crossfade(true)
                    .placeholder(R.drawable.ic_default_avatar)
                    .error(R.drawable.ic_default_avatar)
                    .fallback(R.drawable.ic_default_avatar)
                    .build(),
                contentDescription = "用户头像",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp)
            ) {
                Text(
                    text = displayName,
                    color = AppTheme.colors.primaryTextColor,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = accountLabel,
                    color = AppTheme.colors.greyTextColor,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 4.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = studentIdLabel,
                    color = AppTheme.colors.greyTextColor,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 3.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Surface(
                    color = AppTheme.colors.bgButtonLowlightColor,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.padding(top = 10.dp)
                ) {
                    Text(
                        text = if (isBound) "已绑定学号" else "未绑定学号",
                        color = AppTheme.colors.functionalTextColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsCard(
    items: List<SettingItemUiModel>,
    isBound: Boolean,
    onItemClick: (SettingItemUiModel.Option) -> Unit,
    onPrimaryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = AppTheme.colors.bgCardColor,
        border = BorderStroke(1.dp, AppTheme.colors.outlineLowContrastColor)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp)
        ) {
            items.forEach { item ->
                when (item) {
                    is SettingItemUiModel.Header -> SettingHeaderItem(item)
                    is SettingItemUiModel.Option -> SettingOptionItem(
                        item = item,
                        onClick = { onItemClick(item) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onPrimaryClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppTheme.colors.bgButtonColor,
                    contentColor = AppTheme.colors.textButtonColor
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = if (isBound) "切换学号" else "绑定学号",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SettingHeaderItem(item: SettingItemUiModel.Header) {
    Text(
        text = item.title,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = AppTheme.colors.greyTextColor,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 4.dp)
    )
}

@Composable
private fun SettingOptionItem(
    item: SettingItemUiModel.Option,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 10.dp)
    ) {
        item.icon?.let { imageVector ->
            Surface(
                modifier = Modifier.size(36.dp),
                shape = RoundedCornerShape(10.dp),
                color = AppTheme.colors.bgButtonLowlightColor
            ) {
                Icon(
                    imageVector = imageVector,
                    contentDescription = null,
                    tint = AppTheme.colors.functionalTextColor,
                    modifier = Modifier.padding(8.dp)
                )
            }
            Spacer(modifier = Modifier.size(12.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                fontSize = 16.sp,
                color = AppTheme.colors.primaryTextColor,
            )
            item.subtitle?.let { subtitle ->
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = AppTheme.colors.greyTextColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        Icon(
            imageVector = Icons.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = AppTheme.colors.iconSecondaryColor,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun ConfirmDialog(
    title: String,
    content: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        containerColor = AppTheme.colors.bgCardColor,
        titleContentColor = AppTheme.colors.primaryTextColor,
        textContentColor = AppTheme.colors.greyTextColor,
        onDismissRequest = onDismiss,
        title = { Text(text = title, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
        text = { Text(text = content, fontSize = 16.sp) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("确定", color = AppTheme.colors.functionalTextColor, fontSize = 16.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = AppTheme.colors.greyTextColor, fontSize = 16.sp)
            }
        }
    )
}

private fun handleSettingItemClick(
    context: android.content.Context,
    item: SettingItemUiModel.Option
) {
    when (item.id) {
        "11" -> Route.goAccountCenter(context)
        "5" -> Route.goBindingUser(context)
        "6" -> Route.goSkinSecletion(context)
        "9" -> Route.goWebView(context, OPEN_SOURCE_URL)
        "10" -> Route.goFeedback(context)
        "12" -> Route.goCourseReminders(context)
        "13" -> Route.goAbout(context)
        "14" -> Route.goReleaseInformation(context, ReleaseDocumentType.PRIVACY)
        "15" -> Route.goReleaseInformation(context, ReleaseDocumentType.AGREEMENT)
        "16" -> Route.goReleaseInformation(context, ReleaseDocumentType.THIRD_PARTY)
        "17" -> Route.goReleaseInformation(context, ReleaseDocumentType.PERSONAL_INFORMATION)
        "18" -> Route.goReleaseInformation(context, ReleaseDocumentType.ACCOUNT_DATA)
    }
}

private fun createSettingItems(): List<SettingItemUiModel> = buildList {
    add(SettingItemUiModel.Header("主要设置"))
    add(SettingItemUiModel.Option("4", "清除缓存", Icons.Outlined.DeleteOutline))
    add(SettingItemUiModel.Option("11", "我的长理账号", Icons.Outlined.AccountCircle))
    add(SettingItemUiModel.Option("5", "绑定学号", Icons.Outlined.AccountCircle))
    add(
        SettingItemUiModel.Option(
            id = "12",
            title = "上课提醒 & 桌面小组件",
            icon = Icons.Outlined.NotificationsActive,
        )
    )
    if (!BuildConfig.TOOLS_API_BASE_URL.contains(".invalid")) {
        add(SettingItemUiModel.Option("6", "主题设置", Icons.Outlined.Palette))
    }
    add(SettingItemUiModel.Option("10", "意见反馈", Icons.Outlined.Feedback))
    add(SettingItemUiModel.Header("隐私与合规"))
    add(SettingItemUiModel.Option("13", "关于我的长理", Icons.Outlined.Info))
    add(SettingItemUiModel.Option("14", "隐私政策", Icons.Outlined.Info))
    add(SettingItemUiModel.Option("15", "用户协议", Icons.Outlined.Info))
    add(SettingItemUiModel.Option("16", "第三方 SDK / 服务清单", Icons.Outlined.Info))
    add(SettingItemUiModel.Option("17", "个人信息清单", Icons.Outlined.Info))
    add(SettingItemUiModel.Option("18", "账号与数据管理", Icons.Outlined.AccountCircle))
}

private fun maskedStudentId(studentId: String): String {
    if (studentId.isBlank()) return "尚未绑定学号"
    if (studentId.length <= 4) return "学号 ${"•".repeat(studentId.length)}"

    val visiblePrefixLength = if (studentId.length >= 8) 4 else 2
    val visibleSuffixLength = 2
    val hiddenLength = (studentId.length - visiblePrefixLength - visibleSuffixLength).coerceAtLeast(2)
    return buildString {
        append("学号 ")
        append(studentId.take(visiblePrefixLength))
        append("•".repeat(hiddenLength))
        append(studentId.takeLast(visibleSuffixLength))
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfileSettingsRoutePreview() {
    AppSkinTheme {
        ProfileSettingsScreen(
            items = createSettingItems(),
            studentIdLabel = "尚未绑定学号",
            avatarData = null,
            isBound = false,
            onItemClick = {},
            onPrimaryClick = {}
        )
    }
}
