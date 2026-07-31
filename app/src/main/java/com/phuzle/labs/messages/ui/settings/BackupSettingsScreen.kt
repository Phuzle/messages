package com.phuzle.labs.messages.ui.settings
import com.phuzle.labs.messages.ui.components.topBarContentPadding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.style.TextAlign
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

    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(top = topBarContentPadding(68.dp), start = 16.dp, end = 16.dp, bottom = 108.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            Column(Modifier.fillMaxWidth().background(tokens.surface, ShapeMedium).border(1.dp, tokens.border, ShapeMedium).padding(14.dp)) {
                Text("Local backup frequency", color = tokens.textPrimary, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    "Runs automatically in the background — the Backup Now button below is just for an on-demand snapshot.",
                    color = tokens.textTertiary, fontSize = 11.5.sp, modifier = Modifier.padding(top = 2.dp),
                )
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PillButton("Daily", settings.backupFrequency == "daily", { viewModel.setBackupFrequency("daily") }, modifier = Modifier.weight(1f), icon = Icons.Filled.Today)
                    PillButton("Weekly", settings.backupFrequency == "weekly", { viewModel.setBackupFrequency("weekly") }, modifier = Modifier.weight(1f), icon = Icons.Filled.DateRange)
                }
            }

            // Google Drive: connect first, everything else (enable toggle, Wi-Fi-only opt-in) only
            // shows up once a real Google account is actually signed in — see
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
                        textAlign = TextAlign.Center,
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
                                "Included automatically whenever you back up, on the same schedule as local backup.",
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
                            Icon(Icons.Filled.Wifi, contentDescription = null, tint = tokens.textSecondary, modifier = Modifier.size(20.dp).padding(end = 12.dp))
                            Column {
                                Text("Wi-Fi only", color = tokens.textPrimary, fontSize = 13.sp)
                                Text("Disabling this will use mobile data", color = tokens.textTertiary, fontSize = 11.5.sp, modifier = Modifier.padding(top = 2.dp))
                            }
                            LabeledSwitch(checked = settings.driveWifiOnly, onCheckedChange = { viewModel.toggleDriveWifiOnly() })
                        }
                        Text(
                            lastBackupLabel("Google Drive", settings.lastDriveBackupAt),
                            color = tokens.textTertiary, fontSize = 12.sp, modifier = Modifier.padding(top = 12.dp),
                        )
                    }
                }
            }

            Column(Modifier.fillMaxWidth().background(tokens.surface, ShapeMedium).border(1.dp, tokens.border, ShapeMedium).padding(14.dp)) {
                Text("Local backup", color = tokens.textPrimary, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    "Saved to Downloads/Messages Backups on this device, so it's still there to restore from even if the app itself is uninstalled.",
                    color = tokens.textTertiary, fontSize = 11.5.sp, modifier = Modifier.padding(top = 4.dp),
                )
                Text(lastBackupLabel("Local", settings.lastLocalBackupAt), color = tokens.textTertiary, fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp))
                Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Export a copy", color = tokens.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                            .border(1.dp, tokens.border, RoundedCornerShape(9.dp))
                            .clickable(onClick = viewModel::requestExportBackup).padding(vertical = 11.dp),
                    )
                    Text(
                        "Restore from file", color = tokens.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                            .border(1.dp, tokens.border, RoundedCornerShape(9.dp))
                            .clickable(onClick = viewModel::requestRestoreFromFile).padding(vertical = 11.dp),
                    )
                }
                Text(
                    "View & restore backups", color = tokens.accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                        .border(1.dp, tokens.border, RoundedCornerShape(9.dp))
                        .clickable(onClick = viewModel::openBackupList).padding(vertical = 11.dp),
                )
            }
        }

        // The one "Backup now" action for the whole screen (see AppViewModel.backupNow) — always
        // backs up locally, and to Drive too when that's enabled above, replacing what used to be
        // two separate buttons the user had to tap one at a time.
        Box(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .background(tokens.bg)
                .border(1.dp, tokens.border, androidx.compose.ui.graphics.RectangleShape)
                .navigationBarsPadding()
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().background(tokens.accent, ShapeMedium)
                    .clickable(enabled = !busy, onClick = viewModel::backupNow)
                    .padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (busy) {
                    CircularProgressIndicator(color = tokens.accentText, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                } else {
                    Text("Backup Now", color = tokens.accentText, fontSize = 14.5.sp, fontWeight = FontWeight.Bold)
                }
            }
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

private fun lastBackupLabel(target: String, timestamp: Long?): String =
    if (timestamp == null) "$target: no backup yet" else "$target: ${DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(timestamp))}"
