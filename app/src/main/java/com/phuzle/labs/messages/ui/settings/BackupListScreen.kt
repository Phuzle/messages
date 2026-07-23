package com.phuzle.labs.messages.ui.settings
import com.phuzle.labs.messages.ui.components.topBarContentPadding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudQueue
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.phuzle.labs.messages.ui.AppViewModel
import com.phuzle.labs.messages.ui.components.BackBarScaffold
import com.phuzle.labs.messages.ui.components.EmptyState
import com.phuzle.labs.messages.ui.model.DriveBackupUi
import com.phuzle.labs.messages.ui.model.LocalBackupUi
import com.phuzle.labs.messages.ui.theme.MessagesTheme
import com.phuzle.labs.messages.ui.theme.ShapeMedium
import java.text.DateFormat
import java.util.Date

/**
 * Lets the user pick *any* backup — local or Drive, not just the newest of each — and restore that
 * specific one. Replaces the old "Restore" (local, most-recent-only) and "Merge" (Drive,
 * most-recent-only) single-button actions, so migrating in from another device (whose last backup
 * isn't necessarily this device's newest local one) is actually possible.
 */
@Composable
fun BackupListScreen(viewModel: AppViewModel) {
    val tokens = MessagesTheme.tokens
    val listState by viewModel.backupListState.collectAsStateWithLifecycle()
    var pendingLocalRestore by remember { mutableStateOf<LocalBackupUi?>(null) }

    BackBarScaffold(title = "Backups", onBack = viewModel::goBack) {
        if (listState.local.isEmpty() && listState.drive.isEmpty() && !listState.loading) {
            EmptyState(
                icon = Icons.Filled.CloudQueue,
                title = "No backups yet",
                detail = "Back up locally or to Google Drive from Backup & Restore, then any snapshot you make will show up here to restore from.",
                modifier = Modifier.padding(top = topBarContentPadding(68.dp)),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = topBarContentPadding(68.dp), bottom = 24.dp, start = 16.dp, end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (listState.loading) {
                    item {
                        Row(Modifier.fillMaxWidth().padding(vertical = 20.dp), horizontalArrangement = Arrangement.Center) {
                            CircularProgressIndicator(color = tokens.accent, modifier = Modifier.padding(end = 8.dp))
                        }
                    }
                }
                item {
                    Text(
                        "On this device", color = tokens.textSecondary, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
                    )
                }
                if (listState.local.isEmpty() && !listState.loading) {
                    item { Text("No local backups yet", color = tokens.textTertiary, fontSize = 12.5.sp) }
                }
                items(listState.local, key = { "local-" + it.fileName }) { backup ->
                    BackupRow(
                        title = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(backup.timestampMillis)),
                        subtitle = "Local snapshot",
                        onRestore = { pendingLocalRestore = backup },
                    )
                }

                item {
                    Text(
                        "Google Drive", color = tokens.textSecondary, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 14.dp, bottom = 2.dp),
                    )
                }
                if (!listState.driveConnected) {
                    item { Text("Not connected to Google Drive", color = tokens.textTertiary, fontSize = 12.5.sp) }
                } else if (listState.drive.isEmpty() && !listState.loading) {
                    item { Text("No Drive backups yet", color = tokens.textTertiary, fontSize = 12.5.sp) }
                }
                items(listState.drive, key = { "drive-" + it.id }) { backup ->
                    BackupRow(
                        title = formatDriveTime(backup.createdTime),
                        subtitle = "Google Drive · merges in, nothing local is removed",
                        onRestore = { viewModel.restoreDriveBackup(backup.id) },
                    )
                }
            }
        }
    }

    val target = pendingLocalRestore
    if (target != null) {
        AlertDialog(
            onDismissRequest = { pendingLocalRestore = null },
            title = { Text("Restore this backup?") },
            text = {
                Text(
                    "This replaces everything currently on this device with the snapshot from " +
                        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(target.timestampMillis)) +
                        ". This can't be undone.",
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.restoreLocalBackup(target.fileName); pendingLocalRestore = null }) {
                    Text("Restore", color = tokens.danger)
                }
            },
            dismissButton = { TextButton(onClick = { pendingLocalRestore = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun BackupRow(title: String, subtitle: String, onRestore: () -> Unit) {
    val tokens = MessagesTheme.tokens
    Row(
        Modifier.fillMaxWidth().background(tokens.surface, ShapeMedium).border(1.dp, tokens.border, ShapeMedium).padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = tokens.textPrimary, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = tokens.textTertiary, fontSize = 11.5.sp, modifier = Modifier.padding(top = 2.dp))
        }
        Text(
            "Restore", color = tokens.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.border(1.dp, tokens.border, RoundedCornerShape(9.dp))
                .clickable(onClick = onRestore).padding(horizontal = 16.dp, vertical = 9.dp),
        )
    }
}

private fun formatDriveTime(iso: String): String = runCatching {
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date.from(java.time.Instant.parse(iso)))
}.getOrDefault(iso)
