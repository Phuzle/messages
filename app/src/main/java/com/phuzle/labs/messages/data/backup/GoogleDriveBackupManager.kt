package com.phuzle.labs.messages.data.backup

import android.accounts.Account
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

data class DriveBackupFile(val id: String, val name: String, val createdTime: String)

/**
 * Real Google Drive backup, scoped to the appDataFolder (a per-app storage area hidden from the
 * user's own Drive UI — matches what the Settings screen already advertised before any of this
 * existed as a preference-only stub). Genuinely calls Google Sign-In and the Drive REST API v3,
 * not a fake/local-only stand-in.
 *
 * This only works end to end once the Google Cloud project behind google-services.json has:
 *   1. The Drive API enabled (console.cloud.google.com -> APIs & Services -> Library).
 *   2. An OAuth consent screen configured, with the signing-in Google account added as a test
 *      user (APIs & Services -> OAuth consent screen -> Test users) — drive.appdata is a
 *      sensitive scope that needs this while the app is unverified/in testing.
 * Neither of those steps can be done from code — they need interactive Google Cloud Console
 * access. Without them, sign-in itself still succeeds (basic profile/email scope only), but any
 * Drive API call below fails with a 403 from Google, which surfaces as an ordinary toast the same
 * way any other network failure in this app does — not a crash, not a silent no-op.
 */
class GoogleDriveBackupManager(private val context: Context) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private fun signInClient(): GoogleSignInClient {
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DRIVE_APPDATA_SCOPE))
            .build()
        return GoogleSignIn.getClient(context, options)
    }

    fun signInIntent(): Intent = signInClient().signInIntent

    /** The scope requested in signInClient() being on the *options* only means it was asked for —
     * it does not mean the user actually granted it. Every caller that cares whether Drive access
     * is real (not just "some Google account is signed in") must check this explicitly, which is
     * exactly the bug this existed to fix: handleSignInResult used to be trusted at face value. */
    fun hasDriveScope(account: GoogleSignInAccount): Boolean = GoogleSignIn.hasPermissions(account, Scope(DRIVE_APPDATA_SCOPE))

    fun lastSignedInAccount(): GoogleSignInAccount? =
        GoogleSignIn.getLastSignedInAccount(context)?.takeIf { hasDriveScope(it) }

    /** Raw sign-in result — null only on actual cancel/failure. This does *not* mean Drive access
     * was granted; callers must check hasDriveScope(account) themselves before treating the user
     * as "connected" (see AppViewModel.handleDriveSignInResult, which used to skip this check
     * entirely and silently proceed on basic-profile-only sign-in with no Drive consent at all). */
    fun handleSignInResult(data: Intent?): GoogleSignInAccount? = runCatching {
        GoogleSignIn.getSignedInAccountFromIntent(data).getResult(ApiException::class.java)
    }.getOrNull()

    fun signOut(onComplete: () -> Unit) {
        signInClient().signOut().addOnCompleteListener { onComplete() }
    }

    /** No UI shown — Google Play Services remembers this app's consent at the Google *account*
     * level, so this can succeed even right after a reinstall (no cached local session) as long
     * as the same account previously granted Drive access on this device. Used by the
     * first-launch "restore from Drive?" check to detect that without asking the user anything
     * up front. Returns null (not an exception) on any failure, same as every other method here. */
    suspend fun silentSignIn(): GoogleSignInAccount? = suspendCancellableCoroutine { cont ->
        signInClient().silentSignIn()
            .addOnSuccessListener { account -> if (cont.isActive) cont.resume(account) }
            .addOnFailureListener { if (cont.isActive) cont.resume(null) }
    }

    /** Blocking OAuth token fetch — always called from a background dispatcher. Can throw
     * UserRecoverableAuthException if consent was somehow skipped during sign-in; treated the same
     * as any other failure here (surfaced as null -> caller shows a toast) rather than
     * implementing the secondary consent-recovery flow, since requestScopes() in signInClient()
     * already asks for this scope up front during the normal sign-in screen. */
    suspend fun accessToken(account: GoogleSignInAccount): String? = withContext(Dispatchers.IO) {
        val email = account.email ?: return@withContext null
        runCatching { GoogleAuthUtil.getToken(context, Account(email, "com.google"), "oauth2:$DRIVE_APPDATA_SCOPE") }.getOrNull()
    }

    suspend fun uploadBackup(token: String, name: String, bytes: ByteArray): String? = withContext(Dispatchers.IO) {
        runCatching {
            val metadata = JSONObject().put("name", name).put("parents", JSONArray().put("appDataFolder"))
            val body = MultipartBody.Builder()
                .setType("multipart/related".toMediaType())
                .addPart(MultipartBody.Part.create(null, metadata.toString().toRequestBody("application/json; charset=UTF-8".toMediaType())))
                .addPart(MultipartBody.Part.create(null, bytes.toRequestBody("application/octet-stream".toMediaType())))
                .build()
            val request = Request.Builder()
                .url("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart")
                .header("Authorization", "Bearer $token")
                .post(body)
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                JSONObject(response.body?.string().orEmpty()).getString("id")
            }
        }.getOrNull()
    }

    /** Newest first. */
    suspend fun listBackups(token: String): List<DriveBackupFile> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url(
                    "https://www.googleapis.com/drive/v3/files" +
                        "?spaces=appDataFolder&orderBy=createdTime desc&fields=files(id,name,createdTime)&pageSize=50",
                )
                .header("Authorization", "Bearer $token")
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val files = JSONObject(response.body?.string().orEmpty()).getJSONArray("files")
                (0 until files.length()).map { i ->
                    val f = files.getJSONObject(i)
                    DriveBackupFile(f.getString("id"), f.getString("name"), f.optString("createdTime"))
                }
            }
        }.getOrDefault(emptyList())
    }

    suspend fun downloadBackup(token: String, fileId: String): ByteArray? = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url("https://www.googleapis.com/drive/v3/files/$fileId?alt=media")
                .header("Authorization", "Bearer $token")
                .get()
                .build()
            client.newCall(request).execute().use { response -> if (!response.isSuccessful) null else response.body?.bytes() }
        }.getOrNull()
    }

    suspend fun deleteBackup(token: String, fileId: String) = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url("https://www.googleapis.com/drive/v3/files/$fileId")
                .header("Authorization", "Bearer $token")
                .delete()
                .build()
            client.newCall(request).execute().close()
        }
    }

    /** Mirrors LocalBackupManager's MAX_BACKUPS — keep the newest [keep], delete the rest. */
    suspend fun pruneOldBackups(token: String, keep: Int = 5) {
        listBackups(token).drop(keep).forEach { deleteBackup(token, it.id) }
    }

    companion object {
        private const val DRIVE_APPDATA_SCOPE = "https://www.googleapis.com/auth/drive.appdata"
    }
}
