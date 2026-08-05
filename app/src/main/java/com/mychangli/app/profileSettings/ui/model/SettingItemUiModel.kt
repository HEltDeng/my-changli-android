package com.mychangli.app.profileSettings.ui.model

import androidx.compose.ui.graphics.vector.ImageVector

sealed class SettingItemUiModel {
    data class Header(val title: String) : SettingItemUiModel()
    data class Option(
        val id: String,
        val title: String,
        val icon: ImageVector? = null,
        val subtitle: String? = null,
    ) : SettingItemUiModel()
}
