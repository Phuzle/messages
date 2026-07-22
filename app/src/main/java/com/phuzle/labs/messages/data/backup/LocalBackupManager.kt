package com.phuzle.labs.messages.data.backup

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.phuzle.labs.messages.data.db.AppDatabase
import com.phuzle.labs.messages.data.db.DATABASE_FILE_NAME
import java.io.File
import java.security.KeyStore
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * The PRD's "mandatory local tier": an encrypted, gzipped snapshot of the Room database, stored
 * app-privately. The Google Drive cloud tier is out of scope for this pass (see plan) — the
 * "Back up to Google Drive" switch is preference-only, no network call happens here.
 */
class LocalBackupManager(private val context: Context) {

    private val backupFile: File
        get() = File(context.filesDir, "backups/messages.bak").apply { parentFile?.mkdirs() }

    fun hasBackup(): Boolean = backupFile.exists()

    /** Checkpoints the WAL, then gzips + AES/GCM-encrypts the raw db file into [backupFile]. */
    suspend fun backupNow(db: AppDatabase) {
        db.openHelper.writableDatabase.execSQL("PRAGMA wal_checkpoint(FULL)")
        val dbFile = context.getDatabasePath(DATABASE_FILE_NAME)
        val gzipped = java.io.ByteArrayOutputStream().also { buffer ->
            GZIPOutputStream(buffer).use { it.write(dbFile.readBytes()) }
        }.toByteArray()

        val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, key()) }
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(gzipped)

        backupFile.outputStream().use { out ->
            out.write(iv.size)
            out.write(iv)
            out.write(ciphertext)
        }
    }

    /** Decrypts + gunzips the latest snapshot back over the live db file, then resets the Room instance. */
    suspend fun restoreNow(): Boolean {
        if (!backupFile.exists()) return false
        val bytes = backupFile.readBytes()
        val ivSize = bytes[0].toInt()
        val iv = bytes.copyOfRange(1, 1 + ivSize)
        val ciphertext = bytes.copyOfRange(1 + ivSize, bytes.size)

        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
        }
        val gzipped = cipher.doFinal(ciphertext)
        val raw = GZIPInputStream(gzipped.inputStream()).use { it.readBytes() }

        AppDatabase.closeAndReset()
        val dbFile = context.getDatabasePath(DATABASE_FILE_NAME)
        dbFile.writeBytes(raw)
        File(dbFile.path + "-wal").delete()
        File(dbFile.path + "-shm").delete()
        return true
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
    }
}
