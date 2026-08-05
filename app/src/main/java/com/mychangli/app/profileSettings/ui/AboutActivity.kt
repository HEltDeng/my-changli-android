package com.mychangli.app.profileSettings.ui

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mychangli.app.R
import com.mychangli.app.core.theme.AppSkinTheme
import com.mychangli.app.core.theme.AppTheme
import com.mychangli.app.core.update.AppUpdateChecker
import com.mychangli.app.core.update.AppUpdatePrompt
import com.mychangli.app.core.update.VersionCheckResult
import kotlinx.coroutines.launch

class AboutActivity : ComponentActivity() {
    companion object {
        const val EXTRA_AUTO_CHECK_UPDATE = "extra_auto_check_update"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val autoCheck = intent?.getBooleanExtra(EXTRA_AUTO_CHECK_UPDATE, false) ?: false
        setContent {
            AppSkinTheme {
                AboutScreen(activity = this, autoCheckUpdate = autoCheck)
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Preview()
@Composable
fun AboutScreen(activity: Activity? = null, autoCheckUpdate: Boolean = false) {
    val scope = rememberCoroutineScope()
    var checking by remember { mutableStateOf(false) }
    var checkMessage by remember { mutableStateOf("") }
    var availableUpdate by remember { mutableStateOf<VersionCheckResult.Available?>(null) }
    var autoCheckLaunched by remember { mutableStateOf(false) }
    LaunchedEffect(autoCheckUpdate, autoCheckLaunched) {
        if (autoCheckUpdate && !autoCheckLaunched) {
            autoCheckLaunched = true
            checking = true
            checkMessage = "正在检查…"
            when (val result = AppUpdateChecker.check()) {
                VersionCheckResult.Latest -> checkMessage = "已是最新版本"
                VersionCheckResult.Unavailable -> checkMessage = "暂时无法检查更新"
                is VersionCheckResult.Available -> {
                    checkMessage = ""
                    availableUpdate = result
                }
            }
            checking = false
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.drawBehind {
                    val strokeWidth = 2.dp.toPx()
                    val y = size.height - strokeWidth / 2
                    drawLine(
                        color = Color.Black,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = strokeWidth
                    )
                },
                title = { Text("关于我们", color = AppTheme.colors.titleTopColor) },
                navigationIcon = {
                    IconButton(onClick = { activity?.finish() }) {
                        Icon(
                            painterResource(id = R.drawable.ic_back),
                            contentDescription = "返回",
                            tint = AppTheme.colors.titleTopColor
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppTheme.colors.bgTopBarColor,
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(AppTheme.colors.bgPrimaryColor)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            CheckUpdateSection(
                checking = checking,
                checkMessage = checkMessage,
                onCheck = {
                    checking = true
                    checkMessage = "正在检查…"
                    scope.launch {
                        when (val result = AppUpdateChecker.check()) {
                            VersionCheckResult.Latest -> checkMessage = "已是最新版本"
                            VersionCheckResult.Unavailable -> checkMessage = "暂时无法检查更新"
                            is VersionCheckResult.Available -> {
                                checkMessage = ""
                                availableUpdate = result
                            }
                        }
                        checking = false
                    }
                },
            )

            Text(
                text = "这是什么",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = AppTheme.colors.primaryTextColor ,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = "我的长理是一款面向长沙理工大学师生的非官方校园工具，目标是把课表、成绩、校园服务、资料和同学交流集中到一个更简洁的入口。应用基于 CreaMakers 的开源校园项目二次开发。",
                fontSize = 16.sp,
                color = AppTheme.colors.primaryTextColor ,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            Text(
                text = "产品方向",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = AppTheme.colors.primaryTextColor ,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // 特色列表
            FeatureItem(
                title = "信息更直接",
                description = "减少在多个学校网站之间来回查找，把常用查询和办事入口放在清晰的位置。"
            )

            FeatureItem(
                title = "稳定好用",
                description = "优先打磨高频校园工具的稳定性、响应速度和日常使用体验。"
            )

            FeatureItem(
                title = "资源共享",
                description = "提供资料库入口，逐步汇集课程资料、考试资料和学校官方服务。"
            )

            FeatureItem(
                title = "克制扩展",
                description = "只增加真实需要、维护成本可控的校园工具，不以社交或功能堆叠为目标。"
            )

            // 标题3
            Text(
                text = "核心功能",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = AppTheme.colors.primaryTextColor ,
                modifier = Modifier.padding(top = 24.dp, bottom = 16.dp)
            )

            Text(
                text = "我的长理",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = AppTheme.colors.primaryTextColor ,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = "应用以高频校园信息查询和日常工具为核心，主要包括：",
                fontSize = 16.sp,
                color = AppTheme.colors.primaryTextColor ,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // 功能列表
            BulletPoint(text = "成绩查询：学生可以直接在App内查看学期成绩。")
            BulletPoint(text = "课表定制：用户可以根据个人课表定制学习计划，方便日程管理。")
            BulletPoint(text = "资料库：集中访问往年试题、馆藏检索和学校官方资源。")
            BulletPoint(text = "失物招领：在本机浏览、发布和管理寻物与招领信息。")
            BulletPoint(text = "场地预约：体验羽毛球和乒乓球的本地预约模拟流程。")

            Text(
                text = "开源说明：本应用基于 CreaMakers/changli-planet-app（MIT License）修改，原作者版权与许可文件保留在项目中。长沙理工大学校名与校徽归学校相关权利人所有；本应用不代表学校官方立场。",
                fontSize = 16.sp,
                color = AppTheme.colors.primaryTextColor ,
                modifier = Modifier.padding(top = 16.dp, bottom = 24.dp)
            )
        }
    }
    availableUpdate?.let { update ->
        AppUpdatePrompt(
            update = update,
            onLater = { availableUpdate = null },
            onSimulateUpdate = {
                availableUpdate = null
                checkMessage = "本地体验版未配置真实 APK 下载，不会执行安装"
            },
        )
    }
}

@Composable
private fun CheckUpdateSection(
    checking: Boolean,
    checkMessage: String,
    onCheck: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(bottom = 24.dp)) {
        Button(
            enabled = !checking,
            onClick = onCheck,
        ) {
            Text("检查更新")
        }
        Text(
            text = "检查是否有新版本",
            fontSize = 14.sp,
            color = AppTheme.colors.greyTextColor,
            modifier = Modifier.padding(top = 6.dp),
        )
        if (checkMessage.isNotBlank()) {
            Text(
                text = checkMessage,
                fontSize = 14.sp,
                color = AppTheme.colors.greyTextColor,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

@Composable
fun FeatureItem(title: String, description: String) {
    Column(modifier = Modifier.padding(bottom = 16.dp)) {
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = AppTheme.colors.primaryTextColor ,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = description,
            fontSize = 16.sp,
            color = AppTheme.colors.primaryTextColor
        )
    }
}

@Composable
fun BulletPoint(text: String) {
    Row(modifier = Modifier.padding(bottom = 8.dp)) {
        Text(
            text = "• ",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = AppTheme.colors.primaryTextColor
        )
        Text(
            text = text,
            fontSize = 16.sp,
            color = AppTheme.colors.primaryTextColor
        )
    }
}
