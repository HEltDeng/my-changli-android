package com.mychangli.app.core.web

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.mychangli.app.core.theme.AppSkinTheme
import com.mychangli.app.core.theme.AppTheme
import java.net.URI

private const val OFFICIAL_WEBSITE_HOST = "mychangliapp.cn"
private const val EXTRA_INITIAL_URL = "official_website_initial_url"

internal fun isOfficialWebsiteUrl(rawUrl: String): Boolean {
    val uri = runCatching { URI(rawUrl) }.getOrNull() ?: return false
    return uri.scheme.equals("https", ignoreCase = true) &&
        uri.host.equals(OFFICIAL_WEBSITE_HOST, ignoreCase = true) &&
        uri.userInfo == null &&
        (uri.port == -1 || uri.port == 443)
}

class OfficialWebsiteActivity : ComponentActivity() {
    companion object {
        fun createIntent(context: Context, url: String): Intent {
            require(isOfficialWebsiteUrl(url)) { "Only the official HTTPS website can open in app" }
            return Intent(context, OfficialWebsiteActivity::class.java)
                .putExtra(EXTRA_INITIAL_URL, url)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val initialUrl = intent.getStringExtra(EXTRA_INITIAL_URL)
            ?.takeIf(::isOfficialWebsiteUrl)
            ?: run {
                finish()
                return
            }
        setContent {
            AppSkinTheme {
                OfficialWebsiteScreen(
                    initialUrl = initialUrl,
                    onClose = ::finish,
                    onOpenExternal = ::openExternal,
                )
            }
        }
    }

    private fun openExternal(rawUrl: String) {
        val uri = runCatching { Uri.parse(rawUrl) }.getOrNull()
        val allowedScheme = uri?.scheme?.lowercase() in setOf("https", "http", "mailto", "tel")
        if (uri == null || !allowedScheme) {
            Toast.makeText(this, "该链接无法安全打开", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            startActivity(
                Intent(Intent.ACTION_VIEW, uri)
                    .addCategory(Intent.CATEGORY_BROWSABLE),
            )
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, "未找到可打开该链接的应用", Toast.LENGTH_SHORT).show()
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OfficialWebsiteScreen(
    initialUrl: String,
    onClose: () -> Unit,
    onOpenExternal: (String) -> Unit,
) {
    val context = LocalContext.current
    var webView by remember { mutableStateOf<WebView?>(null) }
    var progress by remember { mutableIntStateOf(0) }
    var mainFrameFailed by remember { mutableStateOf(false) }

    fun navigateBack() {
        val currentWebView = webView
        if (currentWebView?.canGoBack() == true) currentWebView.goBack() else onClose()
    }

    BackHandler(onBack = ::navigateBack)
    DisposableEffect(Unit) {
        onDispose {
            webView?.apply {
                stopLoading()
                webChromeClient = null
                webViewClient = WebViewClient()
                removeAllViews()
                destroy()
            }
            webView = null
        }
    }

    Scaffold(
        containerColor = AppTheme.colors.overviewPageBackgroundColor,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "我的长理官网",
                        color = AppTheme.colors.titleTopColor,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = ::navigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "返回",
                            tint = AppTheme.colors.titleTopColor,
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            mainFrameFailed = false
                            webView?.reload()
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = "刷新网页",
                            tint = AppTheme.colors.titleTopColor,
                        )
                    }
                    IconButton(
                        onClick = { onOpenExternal(PublicWebsiteNavigator.HOME_URL) },
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                            contentDescription = "在浏览器中打开",
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(AppTheme.colors.overviewPageBackgroundColor),
        ) {
            AndroidView(
                factory = {
                    WebView(context).apply {
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            allowFileAccess = false
                            allowContentAccess = false
                            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                            javaScriptCanOpenWindowsAutomatically = false
                            setSupportMultipleWindows(false)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                safeBrowsingEnabled = true
                            }
                        }
                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, false)
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(
                                view: WebView,
                                request: WebResourceRequest,
                            ): Boolean {
                                val target = request.url.toString()
                                if (isOfficialWebsiteUrl(target)) return false
                                onOpenExternal(target)
                                return true
                            }

                            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                                mainFrameFailed = false
                            }

                            override fun onPageFinished(view: WebView, url: String) {
                                progress = 100
                            }

                            override fun onReceivedError(
                                view: WebView,
                                request: WebResourceRequest,
                                error: WebResourceError,
                            ) {
                                if (request.isForMainFrame) mainFrameFailed = true
                            }

                            override fun onReceivedHttpError(
                                view: WebView,
                                request: WebResourceRequest,
                                errorResponse: WebResourceResponse,
                            ) {
                                if (request.isForMainFrame && errorResponse.statusCode >= 400) {
                                    mainFrameFailed = true
                                }
                            }

                            override fun onReceivedSslError(
                                view: WebView,
                                handler: SslErrorHandler,
                                error: SslError,
                            ) {
                                handler.cancel()
                                mainFrameFailed = true
                            }
                        }
                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView, newProgress: Int) {
                                progress = newProgress
                            }
                        }
                        setDownloadListener { url, _, _, _, _ -> onOpenExternal(url) }
                        loadUrl(initialUrl)
                        webView = this
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )

            if (progress in 0..99 && !mainFrameFailed) {
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter),
                    color = AppTheme.colors.commonColor,
                )
            }

            if (mainFrameFailed) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(AppTheme.colors.overviewPageBackgroundColor)
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = "官网加载失败",
                        color = AppTheme.colors.primaryTextColor,
                        fontSize = 18.sp,
                    )
                    Text(
                        text = "请检查网络连接后重试",
                        color = AppTheme.colors.greyTextColor,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 8.dp, bottom = 18.dp),
                    )
                    Button(
                        onClick = {
                            mainFrameFailed = false
                            webView?.reload()
                        },
                    ) {
                        Text("重新加载")
                    }
                }
            }
        }
    }
}
