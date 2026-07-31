package com.phuzle.labs.messages.core.util

import android.content.Context
import java.io.File

/**
 * Detects a genuine fresh install/reinstall, independent of what Android's Auto Backup silently
 * restores. Auto Backup restores this app's DataStore settings (historyImported,
 * driveRestorePromptShown, theme, ...) from the cloud on reinstall for any device signed into a
 * Google account — while messages.db is deliberately excluded from that same backup (see
 * res/xml/backup_rules.xml) so real message content is never restored outside the app's own
 * explicit Drive-backup opt-in. Without this marker, a real uninstall+reinstall looks identical to
 * "already fully set up" purely from the settings' point of view, and the Play Store-required
 * prominent-disclosure/sync/Drive-restore startup sequence silently never runs again.
 *
 * The fix: a marker file in [Context.getNoBackupFilesDir], which Android guarantees is never
 * included in any backup — cloud or device-transfer — regardless of allowBackup/
 * dataExtractionRules. Its mere absence is therefore a reliable "this specific installation has
 * never completed startup" signal, completely independent of whatever DataStore came back as.
 */
class FreshInstallMarker(private val context: Context) {
    private val markerFile: File get() = File(context.noBackupFilesDir, "startup_complete")

    /** True until [markComplete] has been called for this specific installation. */
    fun isFreshInstall(): Boolean = !markerFile.exists()

    fun markComplete() {
        runCatching { markerFile.writeText("1") }
    }
}
