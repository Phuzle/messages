package com.phuzle.labs.messages.ui.settings
import com.phuzle.labs.messages.ui.components.topBarContentPadding

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phuzle.labs.messages.ui.AppViewModel
import com.phuzle.labs.messages.ui.components.PillButton
import com.phuzle.labs.messages.ui.components.SectionLabel
import com.phuzle.labs.messages.ui.components.SettingsCard
import com.phuzle.labs.messages.ui.components.SettingsRowDivider
import com.phuzle.labs.messages.ui.model.AppUiState
import com.phuzle.labs.messages.ui.theme.MessagesTheme
import com.phuzle.labs.messages.ui.theme.ShapeMedium

private val APP_LOCK_METHODS = listOf("fingerprint" to "Fingerprint", "face" to "Face Unlock", "pin" to "PIN")

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PrivacySettingsScreen(state: AppUiState, viewModel: AppViewModel) {
    val tokens = MessagesTheme.tokens
    val settings = state.settings

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = topBarContentPadding(68.dp), start = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        Column {
            SectionLabel("App lock", Modifier.padding(bottom = 8.dp))
            Column(Modifier.fillMaxWidth().background(tokens.surface, ShapeMedium).border(1.dp, tokens.border, ShapeMedium).padding(14.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Require authentication to open app", color = tokens.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    com.phuzle.labs.messages.ui.components.LabeledSwitch(checked = settings.appLockEnabled, onCheckedChange = { viewModel.toggleAppLock() })
                }
                if (settings.appLockEnabled) {
                    Row(
                        Modifier.fillMaxWidth().padding(top = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        APP_LOCK_METHODS.forEach { (key, label) ->
                            PillButton(label = label, active = settings.appLockMethod == key, onClick = { viewModel.setAppLockMethod(key) }, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        Column {
            SectionLabel("Private chats", Modifier.padding(bottom = 8.dp))
            Row(
                Modifier.fillMaxWidth().background(tokens.surface, ShapeMedium).border(1.dp, tokens.border, ShapeMedium)
                    .clickable(onClick = viewModel::openPrivateChatsScreen).padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("View private chats", color = tokens.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${state.privateThreads.size}", color = tokens.textTertiary, fontSize = 13.sp)
                    com.phuzle.labs.messages.ui.components.ChevronIcon()
                }
            }
        }

        Column {
            SectionLabel("Blocked contacts", Modifier.padding(bottom = 8.dp))
            SettingsCard {
                if (state.blockedList.isEmpty()) {
                    Text("No blocked contacts.", color = tokens.textTertiary, fontSize = 13.sp, modifier = Modifier.padding(14.dp))
                } else {
                    state.blockedList.forEachIndexed { index, blocked ->
                        if (index > 0) SettingsRowDivider()
                        Row(
                            Modifier.fillMaxWidth()
                                .combinedClickable(onClick = {}, onLongClick = { viewModel.copyNumber(blocked.number) })
                                .padding(13.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(blocked.number, color = tokens.textPrimary, fontSize = 14.sp)
                            Text(
                                "Unblock", color = tokens.accent, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .border(1.dp, tokens.border, androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                                    .clickable { viewModel.unblockNumber(blocked.number) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
