package com.phuzle.labs.messages.data.backup

import android.accounts.Account
import android.content.Context
import android.content.Intent
import android.util.Log
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
 *   3. An Android OAuth 2.0 Client ID (APIs & Services -> Credentials -> Create Credentials ->
 *      OAuth client ID -> Android) registered for *every* signing certificate this app is ever
 *      actually installed with — package name com.phuzle.labs.messages plus that certificate's
 *      SHA-1. A debug build and a Play-distributed release are signed with genuinely different
 *      certificates (Play App Signing re-signs the uploaded build with a Google-held key, not
 *      whatever local/CI keystore produced the AAB), so "it works from a debug build" proves
 *      nothing about whether the Play release's certificate has ever been registered. Get that
 *      exact SHA-1 from Play Console -> Setup -> App integrity -> App signing key certificate —
 *      not from `keytool` on the local release keystore, which is the *upload* key and only
 *      matches production if Play App Signing was opted out of. Missing this client is the
 *      textbook cause of sign-in failing ONLY on Play-installed builds: it surfaces as
 *      ApiException status 10 (DEVELOPER_ERROR) — see handleSignInResult's logging below.
 * None of these three can be done from code — they need interactive Google Cloud Console access.
 * Without (1) or (2), sign-in itself still succeeds (basic profile/email scope only), but any
 * Drive API call below fails with a 403 from Google, which surfaces as an ordinary toast the same
 * way any other network failure in this app does — not a crash, not a silent no-op. Without (3),
 * sign-in fails outright before ever reaching this app's own code.
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
     * entirely and silently proceed on basic-profile-only sign-in with no Drive consent at all).
     *
     * The failure branch is logged with the raw ApiException status code specifically because
     * "sign-in doesn't work" reports from real devices are otherwise undiagnosable — status 10
     * (DEVELOPER_ERROR) almost always means the OAuth client's registered SHA-1 doesn't match the
     * certificate this build was actually signed with (Play App Signing re-signs release builds
     * with its own key, distinct from the upload key, so the SHA-1 registered in the Google Cloud
     * Console/Firebase project needs to be the *App signing certificate* from Play Console ->
     * Setup -> App integrity, not the local debug/upload keystore's); status 12501 is a genuine
     * user cancel; anything else warrants pulling logcat off the affected device. */
    fun handleSignInResult(data: Intent?): GoogleSignInAccount? = runCatching {
        GoogleSignIn.getSignedInAccountFromIntent(data).getResult(ApiException::class.java)
    }.onFailure { e ->
        val statusCode = (e as? ApiException)?.statusCode
        val statusName = statusCode?.let { com.google.android.gms.common.api.CommonStatusCodes.getStatusCodeString(it) }
        Log.w("GoogleDriveBackupManager", "Interactive sign-in failed: status=$statusCode ($statusName)", e)
    }.getOrNull()

    suspend fun signOut() = suspendCancellableCoroutine<Unit> { cont ->
        signInClient().signOut().addOnCompleteListener { if (cont.isActive) cont.resume(Unit) }
    }

    /** No UI shown — an optimization attempt, not a guarantee: it succeeds for free when Play
     * Services can resolve the account+scope with zero interaction, but for a scoped permission
     * like Drive (as opposed to basic profile/email) it commonly fails with ApiException(4)
     * SIGN_IN_REQUIRED even for an account that granted this exact access before — most reliably
     * right after this app's own data was cleared, which wipes whatever local session state let a
     * previous silent attempt short-circuit. That failure is routine, not exceptional, so it's
     * logged at a level worth keeping (not filtered out) but callers must treat null as "can't
     * tell silently, fall back to an interactive sign-in" — never as "no backup exists" (see
     * AppViewModel.checkFirstLaunchDriveRestore, which used to make exactly that mistake). */
    suspend fun silentSignIn(): GoogleSignInAccount? = suspendCancellableCoroutine { cont ->
        signInClient().silentSignIn()
            .addOnSuccessListener { account -> if (cont.isActive) cont.resume(account) }
            .addOnFailureListener {
                Log.i("GoogleDriveBackupManager", "silentSignIn couldn't resolve without interaction: $it")
                if (cont.isActive) cont.resume(null)
            }
    }

    /** The account to actually use for a Drive API call — every caller that used to call
     * [lastSignedInAccount] alone should call this instead. [lastSignedInAccount] is a bare local
     * cache read (no network, no Play Services round-trip) and can come back null even minutes
     * after a real successful sign-in in the exact same app process — this app's own
     * "connected"/e-mail settings are the durable record of whether the user connected Drive, but
     * they aren't themselves a live, usable [GoogleSignInAccount]. [silentSignIn] is the same
     * "try to resolve for free" fallback used at startup (see checkFirstLaunchDriveRestore) and can
     * succeed here even when the bare cache read just failed. Still returns null if both fail —
     * callers are responsible for falling back to an interactive sign-in from there if they can. */
    suspend fun resolveConnectedAccount(): GoogleSignInAccount? = lastSignedInAccount() ?: silentSignIn()

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
