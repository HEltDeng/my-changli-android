package com.mychangli.app.profileSettings.ui

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mychangli.app.BuildConfig
import com.mychangli.app.R
import com.mychangli.app.core.theme.AppSkinTheme
import com.mychangli.app.core.theme.AppTheme
import com.mychangli.app.core.update.AppUpdateChecker
import com.mychangli.app.core.update.AppUpdateNavigator
import com.mychangli.app.core.update.AppUpdatePrompt
import com.mychangli.app.core.update.VersionCheckResult
import com.mychangli.app.core.web.PublicWebsiteNavigator

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
@Preview
@Composable
fun AboutScreen(activity: Activity? = null, autoCheckUpdate: Boolean = false) {
    var checkMessage by remember { mutableStateOf("") }
    var availableUpdate by remember { mutableStateOf<VersionCheckResult.Available?>(null) }

    LaunchedEffect(autoCheckUpdate) {
        if (autoCheckUpdate) {
            checkMessage = "正在检查…"
            when (val result = AppUpdateChecker.check()) {
                VersionCheckResult.Latest -> checkMessage = "当前已是最新版本"
                VersionCheckResult.Unavailable -> checkMessage = "暂时无法检查更新"
                is VersionCheckResult.Available -> {
                    checkMessage = ""
                    availableUpdate = result
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "关于我的长理",
                        color = AppTheme.colors.titleTopColor,
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { activity?.finish() }) {
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
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(AppTheme.colors.bgPrimaryColor)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 20.dp),
        ) {
            Text(
                text = "我的长理",
                color = AppTheme.colors.primaryTextColor,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = "面向长沙理工大学同学的轻量校园生活工具，集中展示常用校园信息、课程服务与实用入口。",
                color = AppTheme.colors.secondaryTextColor,
                fontSize = 16.sp,
                lineHeight = 25.sp,
                modifier = Modifier.padding(top = 10.dp, bottom = 20.dp),
            )
            Surface(
                color = AppTheme.colors.bgCardColor,
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "当前版本",
                        color = AppTheme.colors.secondaryTextColor,
                        fontSize = 14.sp,
                    )
                    Text(
                        text = BuildConfig.VERSION_NAME,
                        color = AppTheme.colors.primaryTextColor,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    if (checkMessage.isNotBlank()) {
                        Text(
                            text = checkMessage,
                            color = AppTheme.colors.secondaryTextColor,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(top = 10.dp),
                        )
                    }
                }
            }
            Button(
                onClick = {
                    activity?.let {
                        PublicWebsiteNavigator.openHomeInApp(it)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 18.dp),
            ) {
                Text("访问我的长理官网")
            }
            Text(
                text = "产品介绍、最新消息和详细说明请前往官网查看。",
                color = AppTheme.colors.secondaryTextColor,
                fontSize = 14.sp,
                lineHeight = 21.sp,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }

    availableUpdate?.let { update ->
        AppUpdatePrompt(
            update = update,
            onLater = { availableUpdate = null },
            onPrimaryAction = {
                availableUpdate = null
                checkMessage = if (
                    activity != null && AppUpdateNavigator.open(activity, update.version.download_url)
                ) {
                    ""
                } else {
                    "暂时无法打开更新地址"
                }
            },
        )
    }
}
