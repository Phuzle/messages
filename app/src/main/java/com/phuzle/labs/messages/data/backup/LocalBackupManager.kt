package com.phuzle.labs.messages.data.backup

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
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
 * The PRD's "mandatory local tier": a gzipped snapshot of the Room database. On API 29+ these go
 * into the shared `Downloads/Messages Backups/` collection via MediaStore — no storage permission
 * needed to write or read back the app's own entries there, and critically, files in that shared
 * collection are NOT deleted when the app is uninstalled. That distinction used to be missing
 * entirely: every backup lived in `context.filesDir`, encrypted with an AndroidKeyStore key, both
 * of which Android deletes the instant the app is uninstalled — meaning "local backup" protected
 * against Room corruption or `pm clear`, but not against the single most common real reason
 * someone needs to restore from a backup at all (accidental uninstall, factory reset, new phone).
 * A backup that can't survive that isn't the safety net its name promises.
 *
 * Below API 29 (no MediaStore-without-permission), and for any backup already sitting in the old
 * location from before this fix, the original app-private + AndroidKeyStore-AES path is kept as a
 * fallback — see [legacyBackupFiles] — so nothing already backed up is orphaned, only new backups
 * on modern devices actually get the durability fix. Because the two paths use different formats
 * (legacy is AES+gzip; MediaStore is gzip-only, since a hardware-tied key defeats the entire point
 * of a portable backup — see below), [restore] tells them apart by which store actually has that
 * filename, not by inspecting the bytes.
 */
class LocalBackupManager(private val context: Context) {

    private val legacyBackupDir: File
        get() = File(context.filesDir, "backups").apply { mkdirs() }

    /** Newest-first. File names are `messages-<epochMillis>.bak` so sorting by name sorts by time. */
    private fun legacyBackupFiles(): List<File> =
        legacyBackupDir.listFiles { f -> f.name.startsWith("messages-") && f.name.endsWith(".bak") }
            ?.sortedByDescending { it.name } ?: emptyList()

    private fun usesMediaStore() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    /** (MediaStore row id, file name, timestamp), newest first. Always empty below API 29 — see
     * class doc. Querying without READ_EXTERNAL_STORAGE only ever returns entries this app itself
     * created, which is exactly what's wanted here (every row will be one of our own backups). */
    private fun mediaStoreBackups(): List<Triple<Long, String, Long>> {
        if (!usesMediaStore()) return emptyList()
        val result = mutableListOf<Triple<Long, String, Long>>()
        runCatching {
            val projection = arrayOf(MediaStore.Downloads._ID, MediaStore.Downloads.DISPLAY_NAME)
            val selection = "${MediaStore.Downloads.RELATIVE_PATH} = ? AND ${MediaStore.Downloads.DISPLAY_NAME} LIKE ?"
            val args = arrayOf(BACKUP_RELATIVE_PATH, "messages-%.bak")
            context.contentResolver.query(MediaStore.Downloads.EXTERNAL_CONTENT_URI, projection, selection, args, null)?.use { cursor ->
                val idIdx = cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID)
                val nameIdx = cursor.getColumnIndexOrThrow(MediaStore.Downloads.DISPLAY_NAME)
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idIdx)
                    val name = cursor.getString(nameIdx)
                    val timestamp = name.removePrefix("messages-").removeSuffix(".bak").toLongOrNull() ?: 0L
                    result += Triple(id, name, timestamp)
                }
            }
        }
        return result.sortedByDescending { it.third }
    }

    private fun mediaStoreUriFor(id: Long): Uri = ContentUris.withAppendedId(MediaStore.Downloads.EXTERNAL_CONTENT_URI, id)

    fun hasBackup(): Boolean = mediaStoreBackups().isNotEmpty() || legacyBackupFiles().isNotEmpty()

    /** Storage & Data's "space used" — the live db file (+ its not-yet-checkpointed WAL, which is
     * real on-disk space) plus every local backup snapshot kept on this device, in either store. */
    fun totalStorageBytes(): Long {
        val dbFile = context.getDatabasePath(DATABASE_FILE_NAME)
        val liveBytes = dbFile.length() + File(dbFile.path + "-wal").length() + File(dbFile.path + "-shm").length()
        val legacyBytes = legacyBackupFiles().sumOf { it.length() }
        val mediaStoreBytes = mediaStoreBackups().sumOf { (id, _, _) ->
            runCatching { context.contentResolver.openAssetFileDescriptor(mediaStoreUriFor(id), "r")?.use { it.length } ?: 0L }.getOrDefault(0L)
        }
        return liveBytes + legacyBytes + mediaStoreBytes
    }

    /** Newest-first across both stores, for the backup-list screen — every snapshot, not just the
     * latest. Deduped by file name defensively (timestamps in the name make real collisions
     * practically impossible, but a device that crossed the API 29 boundary between installs is
     * the one scenario where both stores could ever be non-empty at once). */
    fun listBackups(): List<LocalBackupFile> {
        val fromMediaStore = mediaStoreBackups().map { (_, name, timestamp) -> LocalBackupFile(name, timestamp) }
        val fromLegacy = legacyBackupFiles().map { f ->
            val timestamp = f.name.removePrefix("messages-").removeSuffix(".bak").toLongOrNull() ?: f.lastModified()
            LocalBackupFile(f.name, timestamp)
        }
        return (fromMediaStore + fromLegacy).distinctBy { it.fileName }.sortedByDescending { it.timestampMillis }
    }

    /** Checkpoints the WAL and gzips the raw db file — the shared first half of both the local
     * and Drive backup paths. */
    suspend fun gzipDatabaseSnapshot(db: AppDatabase): ByteArray {
        // PRAGMA wal_checkpoint returns a result row (busy/log/checkpointed counts), so it must go
        // through query()/rawQuery() — execSQL() rejects any statement that produces a result set.
        db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").use { it.moveToFirst() }
        val dbFile = context.getDatabasePath(DATABASE_FILE_NAME)
        return ByteArrayOutputStream().also { buffer ->
            GZIPOutputStream(buffer).use { it.write(dbFile.readBytes()) }
        }.toByteArray()
    }

    /** Gzips a new timestamped local snapshot into whichever store this device supports, pruning
     * older ones in that same store beyond [MAX_BACKUPS]. */
    suspend fun backupNow(db: AppDatabase) {
        val gzipped = gzipDatabaseSnapshot(db)
        val fileName = "messages-${System.currentTimeMillis()}.bak"
        if (usesMediaStore()) {
            writeToMediaStore(fileName, gzipped)
            mediaStoreBackups().drop(MAX_BACKUPS).forEach { (id, _, _) -> runCatching { context.contentResolver.delete(mediaStoreUriFor(id), null, null) } }
        } else {
            writeToLegacy(fileName, gzipped)
            legacyBackupFiles().drop(MAX_BACKUPS).forEach { it.delete() }
        }
    }

    private fun writeToMediaStore(fileName: String, gzipped: ByteArray) {
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, "application/octet-stream")
            put(MediaStore.Downloads.RELATIVE_PATH, BACKUP_RELATIVE_PATH)
        }
        val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return
        context.contentResolver.openOutputStream(uri)?.use { it.write(gzipped) }
    }

    /** No AES here (unlike [encryptForLegacy]) — a device-hardware-tied key would make a backup
     * meant to survive a reinstall undecryptable the moment it actually needs to be restored,
     * which is the same reasoning Drive backups already used (see class doc and
     * GoogleDriveBackupManager) applied to this store for the same reason. */
    private fun writeToLegacy(fileName: String, gzipped: ByteArray) {
        val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, key()) }
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(gzipped)
        File(legacyBackupDir, fileName).outputStream().use { out ->
            out.write(iv.size)
            out.write(iv)
            out.write(ciphertext)
        }
    }

    /** Decrypts + gunzips the most recent local snapshot back over the live db file, then resets
     * the Room instance. */
    suspend fun restoreNow(): Boolean = listBackups().maxByOrNull { it.timestampMillis }?.let { restore(it.fileName) } ?: false

    /** Same as [restoreNow] but for a specific snapshot the user picked from the backup-list
     * screen, rather than always the newest one — e.g. restoring an older snapshot, or one that
     * migrated in from another device. */
    suspend fun restore(fileName: String): Boolean {
        val mediaStoreMatch = mediaStoreBackups().firstOrNull { it.second == fileName }
        if (mediaStoreMatch != null) {
            val gzipped = context.contentResolver.openInputStream(mediaStoreUriFor(mediaStoreMatch.first))?.use { it.readBytes() } ?: return false
            writeOverLiveDatabase(GZIPInputStream(gzipped.inputStream()).use { it.readBytes() })
            return true
        }
        val legacyFile = File(legacyBackupDir, fileName)
        if (!legacyFile.exists()) return false
        writeOverLiveDatabase(decryptLocalSnapshot(legacyFile.readBytes()))
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
        private const val BACKUP_RELATIVE_PATH = "Download/Messages Backups/"
    }
}
