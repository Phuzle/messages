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
import com.phuzle.labs.messages.ui.components.PillButton
import com.phuzle.labs.messages.ui.model.AppUiState
import com.phuzle.labs.messages.ui.theme.MessagesTheme
import com.phuzle.labs.messages.ui.theme.ShapeMedium
import java.text.DateFormat
import java.util.Date

@Composable
fun BackupSettingsScreen(state: AppUiState, viewModel: AppViewModel) {
    val tokens = MessagesTheme.tokens
    val settings = state.settings

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = topBarContentPadding(68.dp), start = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        Column(Modifier.fillMaxWidth().background(tokens.surface, ShapeMedium).border(1.dp, tokens.border, ShapeMedium).padding(14.dp)) {
            Text("Local backup frequency", color = tokens.textPrimary, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PillButton("Daily", settings.backupFrequency == "daily", { viewModel.setBackupFrequency("daily") }, modifier = Modifier.weight(1f))
                PillButton("Weekly", settings.backupFrequency == "weekly", { viewModel.setBackupFrequency("weekly") }, modifier = Modifier.weight(1f))
            }
            Row(
                Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Back up to Google Drive", color = tokens.textPrimary, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                    Text("Wi-Fi only · appDataFolder", color = tokens.textTertiary, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                }
                LabeledSwitch(checked = settings.cloudBackupConnected, onCheckedChange = { viewModel.toggleCloud() })
            }
        }

        Column(
            Modifier.fillMaxWidth().background(tokens.surface, ShapeMedium).border(1.dp, tokens.border, ShapeMedium).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Backup now", color = tokens.textPrimary, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                    Text(lastBackupLabel(settings.lastLocalBackupAt), color = tokens.textTertiary, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                }
                Text(
                    "Backup now", color = tokens.accentText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.background(tokens.accent, androidx.compose.foundation.shape.RoundedCornerShape(9.dp))
                        .clickable(onClick = viewModel::backupNow).padding(horizontal = 16.dp, vertical = 9.dp),
                )
            }
            Row(
                Modifier.fillMaxWidth().padding(top = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("Restore from backup", color = tokens.textPrimary, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                    Text(lastRestoreLabel(settings.lastLocalRestoreAt), color = tokens.textTertiary, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                }
                Text(
                    "Restore", color = tokens.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.border(1.dp, tokens.border, androidx.compose.foundation.shape.RoundedCornerShape(9.dp))
                        .clickable(onClick = viewModel::restoreNow).padding(horizontal = 16.dp, vertical = 9.dp),
                )
            }
        }
    }
}

private fun lastBackupLabel(timestamp: Long?): String =
    if (timestamp == null) "No backup yet" else "Last backup: ${DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(timestamp))}"

private fun lastRestoreLabel(timestamp: Long?): String =
    if (timestamp == null) "From latest local backup" else "Last restore: ${DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(timestamp))}"
