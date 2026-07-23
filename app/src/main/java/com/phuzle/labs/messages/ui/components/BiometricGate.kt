package com.phuzle.labs.messages.ui.components

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * The one real device-authentication check in the app (device biometric/PIN/pattern via
 * BiometricPrompt, not a hand-rolled lock screen) — reused by both Private Chats and the app-open
 * lock, which used to have their own copy-pasted (Passbook's real prompt) or entirely fake
 * (Private Chats' "Unlock" button, which used to just flip a boolean with no check at all) gates.
 *
 * [key] resets the pending/failed state when it changes (e.g. re-entering a screen after backing
 * out of a failed prompt). [content] renders once [onUnlocked] has actually fired.
 */
@Composable
fun BiometricGate(
    key: Any,
    title: String,
    subtitle: String,
    onUnlocked: () -> Unit,
    lockedContent: @Composable (retry: () -> Unit) -> Unit,
) {
    var authFailed by remember(key) { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(key, authFailed) {
        if (authFailed) return@LaunchedEffect
        val activity = context as? FragmentActivity ?: return@LaunchedEffect
        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onUnlocked()
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    authFailed = true
                }
            },
        )
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            .build()
        prompt.authenticate(promptInfo)
    }

    lockedContent { authFailed = false }
}
