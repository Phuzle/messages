package com.phuzle.labs.messages.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.phuzle.labs.messages.appContainer
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * Backup & Restore's "Local backup frequency" (Daily/Weekly) and "Back up to Google Drive" switch
 * used to be settings that nothing ever read — backing up only ever happened if the user tapped
 * "Backup now" themselves. This runs frequently enough (every 6h) to catch both cadences promptly,
 * but only actually backs up once the chosen interval has genuinely elapsed since the last one.
 */
class AutoBackupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val container = applicationContext.appContainer
        val settings = container.settingsRepository.settingsFlow.first()
        val intervalMillis = if (settings.backupFrequency == "weekly") TimeUnit.DAYS.toMillis(7) else TimeUnit.DAYS.toMillis(1)
        val now = System.currentTimeMillis()

        if (settings.lastLocalBackupAt == null || now - settings.lastLocalBackupAt >= intervalMillis) {
            container.backupManager.backupNow(container.database)
            container.settingsRepository.setLastLocalBackupAt(now)
        }

        if (settings.cloudBackupConnected && settings.googleAccountEmail != null) {
            val driveDue = settings.lastDriveBackupAt == null || now - settings.lastDriveBackupAt >= intervalMillis
            if (driveDue && (!settings.driveWifiOnly || container.isOnWifi())) {
                backupToDrive(container)
            }
        }
        return Result.success()
    }

    private suspend fun backupToDrive(container: com.phuzle.labs.messages.AppContainer) {
        val account = container.driveBackupManager.lastSignedInAccount() ?: return
        val token = container.driveBackupManager.accessToken(account) ?: return
        val gzipped = container.backupManager.gzipDatabaseSnapshot(container.database)
        container.driveBackupManager.uploadBackup(token, "messages-${System.currentTimeMillis()}.bak", gzipped) ?: return
        container.driveBackupManager.pruneOldBackups(token)
        container.settingsRepository.setLastDriveBackupAt(System.currentTimeMillis())
    }

    companion object {
        const val UNIQUE_WORK_NAME = "auto_backup"
    }
}
