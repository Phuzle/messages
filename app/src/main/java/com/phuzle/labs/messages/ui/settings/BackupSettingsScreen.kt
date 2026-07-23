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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
    var showDisconnectConfirm by remember { mutableStateOf(false) }
    val busy by viewModel.backupBusy.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = topBarContentPadding(68.dp), start = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        Column(Modifier.fillMaxWidth().background(tokens.surface, ShapeMedium).border(1.dp, tokens.border, ShapeMedium).padding(14.dp)) {
            Text("Local backup frequency", color = tokens.textPrimary, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
            Text(
                "Runs automatically in the background — \"Backup now\" below is just for an on-demand snapshot.",
                color = tokens.textTertiary, fontSize = 11.5.sp, modifier = Modifier.padding(top = 2.dp),
            )
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PillButton("Daily", settings.backupFrequency == "daily", { viewModel.setBackupFrequency("daily") }, modifier = Modifier.weight(1f))
                PillButton("Weekly", settings.backupFrequency == "weekly", { viewModel.setBackupFrequency("weekly") }, modifier = Modifier.weight(1f))
            }
        }

        // Google Drive: connect first, everything else (enable toggle, Wi-Fi-only opt-in, manual
        // backup/restore) only shows up once a real Google account is actually signed in — see
        // GoogleDriveBackupManager's doc comment for the Google Cloud Console setup this needs to
        // fully work (Drive API enabled + this account added as an OAuth test user).
        Column(Modifier.fillMaxWidth().background(tokens.surface, ShapeMedium).border(1.dp, tokens.border, ShapeMedium).padding(14.dp)) {
            if (settings.googleAccountEmail == null) {
                Text("Google Drive backup", color = tokens.textPrimary, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    "Connect a Google account to back up your messages to Drive and restore them on a new device.",
                    color = tokens.textTertiary, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp, bottom = 10.dp),
                )
                Text(
                    "Connect Google Drive", color = tokens.accentText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().background(tokens.accent, RoundedCornerShape(9.dp))
                        .clickable(onClick = viewModel::requestDriveSignIn).padding(vertical = 11.dp),
                )
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Connected to Google Drive", color = tokens.textPrimary, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                        Text(settings.googleAccountEmail, color = tokens.textTertiary, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                    }
                    Text(
                        "Disconnect", color = if (busy) tokens.textTertiary else tokens.danger, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable(enabled = !busy, onClick = { showDisconnectConfirm = true }).padding(4.dp),
                    )
                }
                Row(
                    Modifier.fillMaxWidth().padding(top = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Back up to Google Drive", color = tokens.textPrimary, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Also runs automatically, on the same schedule as local backup.",
                            color = tokens.textTertiary, fontSize = 11.5.sp, modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                    LabeledSwitch(checked = settings.cloudBackupConnected, onCheckedChange = { viewModel.toggleDriveEnabled() })
                }
                if (settings.cloudBackupConnected) {
                    Row(
                        Modifier.fillMaxWidth().padding(top = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text("Wi-Fi only", color = tokens.textPrimary, fontSize = 13.sp)
                            Text("Disabling this will use mobile data", color = tokens.textTertiary, fontSize = 11.5.sp, modifier = Modifier.padding(top = 2.dp))
                        }
                        LabeledSwitch(checked = settings.driveWifiOnly, onCheckedChange = { viewModel.toggleDriveWifiOnly() })
                    }
                    Row(
                        Modifier.fillMaxWidth().padding(top = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text("Backup now", color = tokens.textPrimary, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                            Text(lastBackupLabel(settings.lastDriveBackupAt), color = tokens.textTertiary, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                        }
                        BackupActionButton("Backup now", busy = busy, filled = true, onClick = viewModel::driveBackupNow)
                    }
                }
            }
        }

        Column(Modifier.fillMaxWidth().background(tokens.surface, ShapeMedium).border(1.dp, tokens.border, ShapeMedium).padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Backup now (local)", color = tokens.textPrimary, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                    Text(lastBackupLabel(settings.lastLocalBackupAt), color = tokens.textTertiary, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                }
                BackupActionButton("Backup now", busy = busy, filled = true, onClick = viewModel::backupNow)
            }
            Text(
                "Kept privately in this app's storage, not visible in your Files app. Use Export to save a copy you can move to another device.",
                color = tokens.textTertiary, fontSize = 11.5.sp, modifier = Modifier.padding(top = 10.dp),
            )
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Export a copy", color = tokens.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.weight(1f)
                        .border(1.dp, tokens.border, RoundedCornerShape(9.dp))
                        .clickable(onClick = viewModel::requestExportBackup).padding(vertical = 11.dp),
                )
                Text(
                    "Restore from file", color = tokens.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.weight(1f)
                        .border(1.dp, tokens.border, RoundedCornerShape(9.dp))
                        .clickable(onClick = viewModel::requestRestoreFromFile).padding(vertical = 11.dp),
                )
            }
            Text(
                "View & restore backups", color = tokens.accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                    .border(1.dp, tokens.border, RoundedCornerShape(9.dp))
                    .clickable(onClick = viewModel::openBackupList).padding(vertical = 11.dp),
            )
        }
    }

    if (showDisconnectConfirm) {
        AlertDialog(
            onDismissRequest = { showDisconnectConfirm = false },
            title = { Text("Disconnect Google Drive?") },
            text = { Text("You'll stop backing up to Drive automatically. Reconnecting later needs signing in again — nothing already backed up is deleted.") },
            confirmButton = {
                TextButton(onClick = { viewModel.disconnectGoogleDrive(); showDisconnectConfirm = false }) {
                    Text("Disconnect", color = tokens.danger)
                }
            },
            dismissButton = { TextButton(onClick = { showDisconnectConfirm = false }) { Text("Cancel") } },
        )
    }
}

private fun lastBackupLabel(timestamp: Long?): String =
    if (timestamp == null) "No backup yet" else "Last backup: ${DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(timestamp))}"

/** Backup/restore is async I/O with no other visible feedback — this disables the button and
 * swaps its label for a spinner while in flight, so a slow connection can't be mistaken for "my
 * tap didn't register" and tapped again into a queue of redundant runs (see AppViewModel.runBackupAction). */
@Composable
private fun BackupActionButton(label: String, busy: Boolean, filled: Boolean, onClick: () -> Unit) {
    val tokens = MessagesTheme.tokens
    val spinnerColor = if (filled) tokens.accentText else tokens.textPrimary
    Row(
        modifier = Modifier
            .let { if (filled) it.background(tokens.accent, RoundedCornerShape(9.dp)) else it.border(1.dp, tokens.border, RoundedCornerShape(9.dp)) }
            .clickable(enabled = !busy, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (busy) {
            CircularProgressIndicator(color = spinnerColor, strokeWidth = 2.dp, modifier = Modifier.size(14.dp))
        } else {
            Text(label, color = if (filled) tokens.accentText else tokens.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
