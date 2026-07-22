package com.phuzle.labs.messages.ui.settings
import com.phuzle.labs.messages.ui.components.topBarContentPadding

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.phuzle.labs.messages.ui.AppViewModel
import com.phuzle.labs.messages.ui.components.SettingsCard
import com.phuzle.labs.messages.ui.components.SettingsCategoryIcon
import com.phuzle.labs.messages.ui.components.SettingsNavRow
import com.phuzle.labs.messages.ui.components.SettingsRowDivider
import com.phuzle.labs.messages.ui.model.AppUiState
import com.phuzle.labs.messages.ui.model.SettingsSub

private data class SettingsCategoryInfo(val sub: SettingsSub, val hint: String)

private val CATEGORIES = listOf(
    SettingsCategoryInfo(SettingsSub.Notifications, "Channels, previews, lock screen"),
    SettingsCategoryInfo(SettingsSub.Appearance, "Theme & accent color"),
    SettingsCategoryInfo(SettingsSub.Privacy, "App lock, private chats, blocking"),
    SettingsCategoryInfo(SettingsSub.Chats, "Swipe actions, signature"),
    SettingsCategoryInfo(SettingsSub.Backup, "Local & cloud backup"),
    SettingsCategoryInfo(SettingsSub.Storage, "Archive, recycle bin, OTP eviction"),
    SettingsCategoryInfo(SettingsSub.About, "About, contact, credits"),
)

@Composable
fun SettingsHomeScreen(state: AppUiState, viewModel: AppViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = topBarContentPadding(68.dp), start = 16.dp, end = 16.dp),
    ) {
        SettingsCard {
            CATEGORIES.forEachIndexed { index, category ->
                if (index > 0) SettingsRowDivider()
                SettingsNavRow(
                    title = category.sub.title,
                    hint = category.hint,
                    onClick = { viewModel.openSettingsSub(category.sub) },
                    leading = { SettingsCategoryIcon(category.sub) },
                )
            }
        }
    }
}
