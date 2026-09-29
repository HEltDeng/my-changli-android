package com.mychangli.app.overview.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.mychangli.app.core.Route
import com.mychangli.app.core.network.campustools.RemoteConfigurationStore
import com.mychangli.app.core.theme.AppSkinTheme
import com.mychangli.app.feature.common.compose_ui.FunctionDestination
import com.mychangli.app.feature.common.compose_ui.openFunctionShortcut
import com.mychangli.app.feature.common.compose_ui.primaryFunctionShortcuts
import com.mychangli.app.overview.ui.compose.OverviewScreen
import com.mychangli.app.overview.viewmodel.OverviewViewModel

class OverviewFragment : Fragment() {
    companion object {
        @JvmStatic
        fun newInstance() = OverviewFragment()
    }

    private val viewModel: OverviewViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                AppSkinTheme {
                    val appConfigState by RemoteConfigurationStore.appConfig.collectAsState()
                    OverviewScreen(
                        viewModel = viewModel,
                        onBindClick = { Route.goBindingUser(requireContext()) },
                        onQuickActionClick = { actionId ->
                            val shortcut = primaryFunctionShortcuts(appConfigState.envelope.config)
                                .firstOrNull { it.id == actionId }
                            when {
                                shortcut != null -> openFunctionShortcut(requireContext(), shortcut)
                                actionId == FunctionDestination.ScoreInquiry.name -> Route.goScoreInquiry(requireContext())
                                actionId == FunctionDestination.Electronic.name -> Route.goElectronic(requireContext())
                            }
                        }
                    )
                }
            }
        }
    }
}
