package com.phuzle.labs.messages.data.backup

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.phuzle.labs.messages.data.db.AppDatabase
import com.phuzle.labs.messages.data.db.DATABASE_FILE_NAME
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.KeyStore
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class LocalBackupFile(val fileName: String, val timestampMillis: Long)

/**
 * The PRD's "mandatory local tier": an encrypted, gzipped snapshot of the Room database, stored
 * app-privately. Keeps the last [MAX_BACKUPS] snapshots (each backupNow() call adds one, timestamped,
 * and the oldest beyond that count are pruned) rather than a single file, so a bad backup or a
 * restore mistake doesn't cost you every prior snapshot too.
 *
 * The AES layer here is deliberately AndroidKeyStore-backed, which is exactly why it's *not* reused
 * for Google Drive backups (see GoogleDriveBackupManager/DriveBackupMerger): that key is
 * non-exportable and tied to this specific device's secure hardware, so a backup encrypted with it
 * could never be decrypted again after a reinstall or on another device — the entire point of a
 * cloud backup. Drive backups instead ship gzip-only, relying on appDataFolder's own access control
 * (invisible to the user's own Drive UI and to every other app) as their confidentiality boundary.
 */
class LocalBackupManager(private val context: Context) {

    private val backupDir: File
        get() = File(context.filesDir, "backups").apply { mkdirs() }

    /** Newest-first. File names are `messages-<epochMillis>.bak` so sorting by name sorts by time. */
    private fun backupFiles(): List<File> =
        backupDir.listFiles { f -> f.name.startsWith("messages-") && f.name.endsWith(".bak") }
            ?.sortedByDescending { it.name } ?: emptyList()

    fun hasBackup(): Boolean = backupFiles().isNotEmpty()

    /** Newest-first, for the backup-list screen — every snapshot, not just the latest. */
    fun listBackups(): List<LocalBackupFile> = backupFiles().map { f ->
        val timestamp = f.name.removePrefix("messages-").removeSuffix(".bak").toLongOrNull() ?: f.lastModified()
        LocalBackupFile(f.name, timestamp)
    }

    /** Checkpoints the WAL and gzips the raw db file — the shared first half of both the local
     * (AES-encrypted-on-top) and Drive (shipped as-is) backup paths. */
    suspend fun gzipDatabaseSnapshot(db: AppDatabase): ByteArray {
        // PRAGMA wal_checkpoint returns a result row (busy/log/checkpointed counts), so it must go
        // through query()/rawQuery() — execSQL() rejects any statement that produces a result set.
        db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").use { it.moveToFirst() }
        val dbFile = context.getDatabasePath(DATABASE_FILE_NAME)
        return ByteArrayOutputStream().also { buffer ->
            GZIPOutputStream(buffer).use { it.write(dbFile.readBytes()) }
        }.toByteArray()
    }

    /** Gzips + AES/GCM-encrypts a new timestamped local snapshot, pruning older ones beyond [MAX_BACKUPS]. */
    suspend fun backupNow(db: AppDatabase) {
        val gzipped = gzipDatabaseSnapshot(db)
        val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, key()) }
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(gzipped)

        val newFile = File(backupDir, "messages-${System.currentTimeMillis()}.bak")
        newFile.outputStream().use { out ->
            out.write(iv.size)
            out.write(iv)
            out.write(ciphertext)
        }

        backupFiles().drop(MAX_BACKUPS).forEach { it.delete() }
    }

    /** Decrypts + gunzips the most recent local snapshot back over the live db file, then resets
     * the Room instance. */
    suspend fun restoreNow(): Boolean = backupFiles().firstOrNull()?.let { restoreFile(it) } ?: false

    /** Same as [restoreNow] but for a specific snapshot the user picked from the backup-list
     * screen, rather than always the newest one — e.g. restoring an older snapshot, or one that
     * migrated in from another device. */
    suspend fun restore(fileName: String): Boolean {
        val file = File(backupDir, fileName)
        if (!file.exists()) return false
        return restoreFile(file)
    }

    private fun restoreFile(file: File): Boolean {
        val raw = decryptLocalSnapshot(file.readBytes())
        writeOverLiveDatabase(raw)
        return true
    }

    private fun decryptLocalSnapshot(bytes: ByteArray): ByteArray {
        val ivSize = bytes[0].toInt()
        val iv = bytes.copyOfRange(1, 1 + ivSize)
        val ciphertext = bytes.copyOfRange(1 + ivSize, bytes.size)
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
        }
        val gzipped = cipher.doFinal(ciphertext)
        return GZIPInputStream(gzipped.inputStream()).use { it.readBytes() }
    }

    /** Gunzips a Drive-downloaded snapshot (no AES — see class doc) into raw sqlite bytes, for
     * DriveBackupMerger to open as a secondary database. Does *not* touch the live db. */
    fun gunzipDriveSnapshot(bytes: ByteArray): ByteArray = GZIPInputStream(bytes.inputStream()).use { it.readBytes() }

    /** Restores (overwrites, not merges) the live db from a Drive-downloaded snapshot's raw bytes. */
    fun writeOverLiveDatabase(rawSqliteBytes: ByteArray) {
        AppDatabase.closeAndReset()
        val dbFile = context.getDatabasePath(DATABASE_FILE_NAME)
        dbFile.writeBytes(rawSqliteBytes)
        File(dbFile.path + "-wal").delete()
        File(dbFile.path + "-shm").delete()
    }

    private fun key(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build()
        )
        return generator.generateKey()
    }

    companion object {
        private const val KEY_ALIAS = "messages_backup_key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val MAX_BACKUPS = 5
    }
}
