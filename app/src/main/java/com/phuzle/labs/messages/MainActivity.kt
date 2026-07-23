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

        requestNeededPermissions()
        handleIntent(intent)
        setContent { AppRoot(viewModel) }

        lifecycleScope.launch {
            viewModel.driveSignInRequests.collect { driveSignInLauncher.launch(appContainer.driveBackupManager.signInIntent()) }
        }
        lifecycleScope.launch {
            viewModel.exportBackupRequests.collect { suggestedName -> exportBackupLauncher.launch(suggestedName) }
        }
        lifecycleScope.launch {
            viewModel.restoreFromFileRequests.collect { restoreFromFileLauncher.launch(arrayOf("*/*")) }
        }
        viewModel.checkFirstLaunchDriveRestore()
        viewModel.reclassifyThreadsIfNeeded()
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
        val permissions = mutableListOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_SMS,
            Manifest.permission.READ_CONTACTS,
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions += Manifest.permission.POST_NOTIFICATIONS
        }
        permissionLauncher.launch(permissions.toTypedArray())

        if (!DefaultSmsAppHelper.isDefaultSmsApp(this)) {
            roleRequestLauncher.launch(DefaultSmsAppHelper.requestRoleIntent(this))
        }
    }

    companion object {
        const val EXTRA_OPEN_THREAD_ID = "extra_open_thread_id"
    }
}
