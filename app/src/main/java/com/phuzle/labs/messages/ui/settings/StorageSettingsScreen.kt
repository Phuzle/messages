package com.phuzle.labs.messages.ui.settings
import com.phuzle.labs.messages.ui.components.topBarContentPadding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import com.phuzle.labs.messages.ui.components.LabeledSwitch
import com.phuzle.labs.messages.ui.components.SettingsCard
import com.phuzle.labs.messages.ui.components.SettingsRowDivider
import com.phuzle.labs.messages.ui.model.AppUiState
import com.phuzle.labs.messages.ui.theme.MessagesTheme
import com.phuzle.labs.messages.ui.theme.ShapeMedium

@Composable
fun StorageSettingsScreen(state: AppUiState, viewModel: AppViewModel) {
    val tokens = MessagesTheme.tokens

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = topBarContentPadding(68.dp), start = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        Column(Modifier.fillMaxWidth().background(tokens.surface, ShapeMedium).border(1.dp, tokens.border, ShapeMedium).padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("24-hour OTP eviction", color = tokens.textPrimary, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                    Text("Permanently purge OTP codes after 24h", color = tokens.textTertiary, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                }
                LabeledSwitch(checked = state.settings.otpEvictionEnabled, onCheckedChange = { viewModel.toggleOtpEviction() })
            }
        }

        SettingsCard {
            CountNavRow("Archived", state.archivedThreads.size, viewModel::openArchivedScreen)
            SettingsRowDivider()
            CountNavRow("Recycle Bin", state.deletedThreads.size, viewModel::openRecycleBin)
        }

        Text(
            "Archived threads stay until you unarchive them. Deleted threads are purged automatically after 30 days.",
            color = tokens.textTertiary, fontSize = 12.sp,
        )
    }
}

@Composable
private fun CountNavRow(title: String, count: Int, onClick: () -> Unit) {
    val tokens = MessagesTheme.tokens
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, color = tokens.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("$count", color = tokens.textTertiary, fontSize = 13.sp)
            com.phuzle.labs.messages.ui.components.ChevronIcon()
        }
    }
}
