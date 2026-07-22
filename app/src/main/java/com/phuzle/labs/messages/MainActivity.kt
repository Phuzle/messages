package com.phuzle.labs.messages

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.phuzle.labs.messages.core.sms.DefaultSmsAppHelper
import com.phuzle.labs.messages.ui.AppRoot
import com.phuzle.labs.messages.ui.AppViewModel
import com.phuzle.labs.messages.ui.AppViewModelFactory

class MainActivity : ComponentActivity() {

    private val viewModel: AppViewModel by viewModels { AppViewModelFactory(appContainer) }

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }

    private val roleRequestLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        viewModel.setDefaultSmsAppStatus(DefaultSmsAppHelper.isDefaultSmsApp(this))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNeededPermissions()
        handleIntent(intent)
        setContent { AppRoot(viewModel) }
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
