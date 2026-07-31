package com.phuzle.labs.messages.core.push

import android.content.Context
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability

/**
 * Drives the "update available" prompt off the Play Store's own In-App Update API (the flexible
 * flow), now that Messages is actually live in closed testing. Play itself shows the "download
 * this update?" system UI ([checkForUpdate]'s [AppUpdateManager.startUpdateFlowForResult] call) —
 * this class only has to notice when a flexible update *finishes* downloading in the background,
 * since Play never restarts the app on its own for that flow. See [AppViewModel]'s updateInfo/
 * [com.phuzle.labs.messages.ui.components.UpdateAvailableDialog] for the "restart to finish
 * installing" prompt this feeds.
 */
class UpdateChecker(context: Context) {
    private val manager: AppUpdateManager by lazy { AppUpdateManagerFactory.create(context.applicationContext) }

    /** Called once per Activity creation. Starts Play's own download-confirmation UI via
     * [launcher] when a flexible update is newly available, or calls [onReadyToInstall]
     * immediately if one had already finished downloading before this check ran (e.g. the app was
     * killed mid-flow last time). */
    fun checkForUpdate(launcher: ActivityResultLauncher<IntentSenderRequest>, onReadyToInstall: () -> Unit) {
        manager.appUpdateInfo.addOnSuccessListener { info ->
            when {
                info.installStatus() == InstallStatus.DOWNLOADED -> onReadyToInstall()
                info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                    info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) -> {
                    manager.startUpdateFlowForResult(info, launcher, AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build())
                }
            }
        }
    }

    /** Fires [onReadyToInstall] the moment Play finishes downloading a flexible update in the
     * background. Register once (e.g. Activity.onCreate) and [unregisterListener] in onDestroy —
     * an unregistered listener leaks the Activity it was created with. */
    fun registerListener(onReadyToInstall: () -> Unit): InstallStateUpdatedListener {
        val listener = InstallStateUpdatedListener { state ->
            if (state.installStatus() == InstallStatus.DOWNLOADED) onReadyToInstall()
        }
        manager.registerListener(listener)
        return listener
    }

    fun unregisterListener(listener: InstallStateUpdatedListener) = manager.unregisterListener(listener)

    /** The dialog's "Restart Now" — installs the already-downloaded update, which restarts the app. */
    fun completeUpdate() {
        manager.completeUpdate()
    }
}
