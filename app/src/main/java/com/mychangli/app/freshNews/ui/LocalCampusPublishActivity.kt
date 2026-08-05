package com.mychangli.app.freshNews.ui

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mychangli.app.core.theme.AppSkinTheme
import com.mychangli.app.freshNews.ui.compose.LocalCampusPublishScreen
import com.mychangli.app.freshNews.viewModel.LocalCampusPublishViewModel

class LocalCampusPublishActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppSkinTheme {
                val viewModel: LocalCampusPublishViewModel = viewModel()
                LocalCampusPublishScreen(
                    viewModel = viewModel,
                    onClose = ::finish,
                    onPublished = {
                        setResult(Activity.RESULT_OK)
                        finish()
                    }
                )
            }
        }
    }
}
