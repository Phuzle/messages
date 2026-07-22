package com.phuzle.labs.messages.ui.settings
import com.phuzle.labs.messages.ui.components.topBarContentPadding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phuzle.labs.messages.ui.AppViewModel
import com.phuzle.labs.messages.ui.components.FlatTextField
import com.phuzle.labs.messages.ui.components.PillButton
import com.phuzle.labs.messages.ui.components.SectionLabel
import com.phuzle.labs.messages.ui.components.SettingsCard
import com.phuzle.labs.messages.ui.components.SettingsRowDivider
import com.phuzle.labs.messages.ui.components.SettingsToggleRow
import com.phuzle.labs.messages.ui.model.AppUiState
import com.phuzle.labs.messages.ui.model.SwipeAction
import com.phuzle.labs.messages.ui.theme.MessagesTheme
import com.phuzle.labs.messages.ui.theme.ShapeMedium

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChatsSettingsScreen(state: AppUiState, viewModel: AppViewModel) {
    val tokens = MessagesTheme.tokens
    val settings = state.settings

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = topBarContentPadding(68.dp), start = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        Column {
            SectionLabel("Swipe actions", Modifier.padding(bottom = 8.dp))
            Column(
                Modifier.fillMaxWidth().background(tokens.surface, ShapeMedium).border(1.dp, tokens.border, ShapeMedium)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Column {
                    Text("Swipe left", color = tokens.textSecondary, fontSize = 13.sp, modifier = Modifier.padding(bottom = 8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        SwipeAction.entries.forEach { action ->
                            PillButton(action.label, settings.swipeLeftAction == action.key, { viewModel.setSwipeAction(left = true, action = action.key) })
                        }
                    }
                }
                Column {
                    Text("Swipe right", color = tokens.textSecondary, fontSize = 13.sp, modifier = Modifier.padding(bottom = 8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        SwipeAction.entries.forEach { action ->
                            PillButton(action.label, settings.swipeRightAction == action.key, { viewModel.setSwipeAction(left = false, action = action.key) })
                        }
                    }
                }
            }
        }

        Column {
            SectionLabel("Default signature", Modifier.padding(bottom = 8.dp))
            FlatTextField(
                value = settings.signature,
                onValueChange = viewModel::onSignatureChange,
                placeholder = "Added to the end of messages you send",
                filled = true,
                fontSize = 13.5.sp,
                singleLine = false,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            )
        }

        SettingsCard {
            SettingsToggleRow("Show character count", settings.showCharCount, { viewModel.toggleCharCount() })
            SettingsRowDivider()
            SettingsToggleRow("Open links in in-app browser", settings.inAppBrowser, { viewModel.toggleInAppBrowser() })
        }
    }
}
