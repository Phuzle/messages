package com.phuzle.labs.messages

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.phuzle.labs.messages.core.sms.DefaultSmsAppHelper
import com.phuzle.labs.messages.ui.AppRoot
import com.phuzle.labs.messages.ui.AppViewModel
import com.phuzle.labs.messages.ui.AppViewModelFactory
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** [FragmentActivity], not plain ComponentActivity, because androidx.biometric's BiometricPrompt
 * (used to gate Passbook account details behind device auth) requires one. */
class MainActivity : FragmentActivity() {

    private val viewModel: AppViewModel by viewModels { AppViewModelFactory(appContainer) }

    private var keepSplashScreenOn = true

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }

    private val roleRequestLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        viewModel.setDefaultSmsAppStatus(DefaultSmsAppHelper.isDefaultSmsApp(this))
        // Contacts/notifications aren't part of the SMS role's own permission bundle, so they
        // still need their own runtime request — done here, after the role result comes back,
        // rather than launched back-to-back with it: firing two ActivityResultLauncher.launch()
        // calls without waiting for the first is unreliable (the earlier request can be
        // superseded before its dialog ever renders).
        requestRuntimePermissions()
    }

    private val driveSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        viewModel.handleDriveSignInResult(result.data)
    }

    private val exportBackupLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        viewModel.handleExportBackupResult(uri)
    }

    private val restoreFromFileLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        viewModel.handleRestoreFromFileResult(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        splashScreen.setKeepOnScreenCondition { keepSplashScreenOn }

        // Keep the native splash up until the first real combined app state has loaded (not
        // just the StateFlow's default seed value), so launch never flashes an empty dashboard.
        lifecycleScope.launch {
            viewModel.uiState.drop(1).first()
            keepSplashScreenOn = false
        }

        handleIntent(intent)
        setContent { AppRoot(viewModel) }

        // Permission/role prompts only fire from a user tap on AppRoot's gate screen (shown
        // whenever !isDefaultSmsApp) — required by Play Store's "Prominent Disclosure" policy for
        // apps requesting SMS/Call Log access, and also means we never pop a system dialog the
        // user didn't just ask for.
        lifecycleScope.launch {
            viewModel.smsPermissionRequests.collect { requestNeededPermissions() }
        }

        lifecycleScope.launch {
            viewModel.driveSignInRequests.collect { driveSignInLauncher.launch(appContainer.driveBackupManager.signInIntent()) }
        }
        lifecycleScope.launch {
            viewModel.exportBackupRequests.collect { suggestedName -> exportBackupLauncher.launch(suggestedName) }
        }
        lifecycleScope.launch {
            viewModel.restoreFromFileRequests.collect { restoreFromFileLauncher.launch(arrayOf("*/*")) }
        }
        // Not called directly here: it's chained after importHistoryOnce() (see AppViewModel)
        // so the "is there already something here" check reflects the post-sync state, matching
        // the intended startup sequence (disclosure -> sync -> drive restore offer -> dashboard).
        viewModel.reclassifyThreadsIfNeeded()
        viewModel.backfillPassbookIfNeeded()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        viewModel.setDefaultSmsAppStatus(DefaultSmsAppHelper.isDefaultSmsApp(this))
        viewModel.checkOtpHotSwap()
    }

    private fun handleIntent(intent: Intent?) {
        val threadId = intent?.getStringExtra(EXTRA_OPEN_THREAD_ID) ?: return
        viewModel.openThreadById(threadId)
    }

    private fun requestNeededPermissions() {
        if (!DefaultSmsAppHelper.isDefaultSmsApp(this)) {
            roleRequestLauncher.launch(DefaultSmsAppHelper.requestRoleIntent(this))
        } else {
            requestRuntimePermissions()
        }
    }

    /** SMS/RECEIVE/SEND permissions come bundled with the role grant above and don't need (and,
     * launched right alongside the role request, wouldn't reliably show) their own prompt — only
     * contacts and notifications are independent of that role and need asking here. */
    private fun requestRuntimePermissions() {
        val permissions = mutableListOf(Manifest.permission.READ_CONTACTS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions += Manifest.permission.POST_NOTIFICATIONS
        }
        permissionLauncher.launch(permissions.toTypedArray())
    }

    companion object {
        const val EXTRA_OPEN_THREAD_ID = "extra_open_thread_id"
    }
}
