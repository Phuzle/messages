package com.phuzle.labs.messages.core.push

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

data class UpdateInfo(val message: String)

/**
 * Drives the "update available" prompt off Firebase Remote Config rather than the Play Store's
 * own in-app-update API: the app isn't actually live on the Store yet (placeholder listing), so
 * there's no published version for Play to compare against. Once it ships for real, this can stay
 * as a lightweight nudge alongside (or instead of) `com.google.android.play:app-update`.
 *
 * Console-side setup: add a `latest_version_code` (Number) and optional `update_message` (String)
 * parameter in Remote Config, publish, and every install with an older [android.content.pm.PackageInfo.longVersionCode]
 * will see the dialog next time it opens.
 */
class UpdateChecker {
    private val remoteConfig: FirebaseRemoteConfig by lazy {
        FirebaseRemoteConfig.getInstance().apply {
            setConfigSettingsAsync(remoteConfigSettings { minimumFetchIntervalInSeconds = 3600 })
            setDefaultsAsync(
                mapOf(
                    "latest_version_code" to 0L,
                    "update_message" to "A new version of Messages is available on the Play Store.",
                ),
            )
        }
    }

    suspend fun checkForUpdate(currentVersionCode: Long): UpdateInfo? {
        val fetched = suspendCancellableCoroutine { cont ->
            remoteConfig.fetchAndActivate()
                .addOnCompleteListener { result -> cont.resume(result.isSuccessful) }
        }
        if (!fetched) return null

        val latest = remoteConfig.getLong("latest_version_code")
        if (latest <= currentVersionCode) return null
        return UpdateInfo(remoteConfig.getString("update_message"))
    }
}
