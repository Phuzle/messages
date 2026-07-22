package com.phuzle.labs.messages.ui.settings

import androidx.compose.runtime.Composable
import com.phuzle.labs.messages.ui.AppViewModel
import com.phuzle.labs.messages.ui.components.BackBarScaffold
import com.phuzle.labs.messages.ui.model.AppUiState
import com.phuzle.labs.messages.ui.model.SettingsSub

@Composable
fun SettingsScreen(state: AppUiState, viewModel: AppViewModel) {
    val title = state.settingsSub?.title ?: "Settings"
    BackBarScaffold(title = title, onBack = viewModel::goBack) {
        when (state.settingsSub) {
            null -> SettingsHomeScreen(state, viewModel)
            SettingsSub.Notifications -> NotificationsSettingsScreen(state, viewModel)
            SettingsSub.Appearance -> AppearanceSettingsScreen(state, viewModel)
            SettingsSub.Privacy -> PrivacySettingsScreen(state, viewModel)
            SettingsSub.Chats -> ChatsSettingsScreen(state, viewModel)
            SettingsSub.Backup -> BackupSettingsScreen(state, viewModel)
            SettingsSub.Storage -> StorageSettingsScreen(state, viewModel)
            SettingsSub.About -> AboutSettingsScreen(state, viewModel)
        }
    }
}
