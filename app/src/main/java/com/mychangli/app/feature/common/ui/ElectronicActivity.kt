package com.mychangli.app.feature.common.ui

import androidx.activity.viewModels
import com.mychangli.app.base.ComposeActivity
import com.mychangli.app.feature.common.compose_ui.ElectronicScreen
import com.mychangli.app.feature.common.viewModel.ElectronicViewModel

/**
 * 电费查询
 */
class ElectronicActivity : ComposeActivity() {

    private val viewModel: ElectronicViewModel by viewModels()

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        setComposeContent {
            ElectronicScreen(
                viewModel = viewModel,
                onBack = { finish() }
            )
        }
    }
}
