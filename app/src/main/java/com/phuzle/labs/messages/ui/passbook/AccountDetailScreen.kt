package com.phuzle.labs.messages.ui.passbook

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.phuzle.labs.messages.ui.AppViewModel
import com.phuzle.labs.messages.ui.components.BackBarScaffold
import com.phuzle.labs.messages.ui.components.EmptyState
import com.phuzle.labs.messages.ui.components.topBarContentPadding
import com.phuzle.labs.messages.ui.model.AppUiState
import com.phuzle.labs.messages.ui.theme.MessagesTheme
import com.phuzle.labs.messages.ui.theme.ShapeMedium

/**
 * Recent activity used to sit inline at the bottom of the Passbook tab; it lives here now, one
 * tap away from an account card. When the user has app-lock enabled (Settings > Privacy — the
 * same toggle that gates Private Chats), this account's transaction history is real financial
 * detail, so it's held behind a BiometricPrompt (falls back to device PIN/pattern) before it
 * renders, rather than inventing a separate lock mechanism just for this screen.
 */
@Composable
fun AccountDetailScreen(state: AppUiState, viewModel: AppViewModel) {
    val tokens = MessagesTheme.tokens
    val last4 = state.selectedAccountLast4 ?: return
    val account = state.accounts.firstOrNull { it.last4 == last4 }
    val appLockEnabled = state.settings.appLockEnabled
    var unlocked by remember(last4) { mutableStateOf(!appLockEnabled) }
    var authFailed by remember(last4) { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(last4, appLockEnabled) {
        if (unlocked) return@LaunchedEffect
        val activity = context as? FragmentActivity ?: return@LaunchedEffect
        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    unlocked = true
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    authFailed = true
                }
            },
        )
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock account details")
            .setSubtitle("Confirm it's you to view •• $last4")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            .build()
        prompt.authenticate(promptInfo)
    }

    BackBarScaffold(title = "•• $last4", onBack = viewModel::goBack) {
        when {
            !unlocked && authFailed -> EmptyState(
                icon = Icons.Filled.Lock,
                title = "Couldn't verify it's you",
                detail = "Account details stay locked until you authenticate.",
                modifier = Modifier.padding(top = topBarContentPadding(68.dp)),
                actionLabel = "Try again",
                onAction = { authFailed = false },
            )
            !unlocked -> EmptyState(
                icon = Icons.Filled.Lock,
                title = "Locked",
                detail = "Waiting for authentication…",
                modifier = Modifier.padding(top = topBarContentPadding(68.dp)),
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = topBarContentPadding(80.dp), start = 16.dp, end = 16.dp, bottom = 24.dp),
            ) {
                item {
                    Column(
                        Modifier.fillMaxWidth().background(tokens.surface, ShapeMedium).border(1.dp, tokens.border, ShapeMedium)
                            .padding(16.dp),
                    ) {
                        Text("Account ending $last4", color = tokens.textSecondary, fontSize = 13.sp)
                        Text(
                            account?.netLabel ?: "—", color = tokens.textPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                        Text(
                            "${account?.transactionCount ?: 0} ${if (account?.transactionCount == 1) "transaction" else "transactions"} captured",
                            color = tokens.textTertiary, fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    Text(
                        "RECENT ACTIVITY", color = tokens.textSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.4.sp, modifier = Modifier.padding(top = 20.dp, bottom = 10.dp),
                    )
                }
                if (state.transactions.isEmpty()) {
                    item {
                        Text(
                            "No transactions for this account yet.", color = tokens.textTertiary, fontSize = 13.sp,
                            modifier = Modifier.padding(vertical = 12.dp),
                        )
                    }
                } else {
                    item {
                        Column(Modifier.fillMaxWidth().background(tokens.surface, ShapeMedium).border(1.dp, tokens.border, ShapeMedium)) {
                            state.transactions.forEach { tx ->
                                Row(
                                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Column {
                                        Text(tx.merchant, color = tokens.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                        Text(tx.timeLabel, color = tokens.textTertiary, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                                    }
                                    Text(
                                        tx.amountLabel, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                                        color = if (tx.isCredit) tokens.accent else tokens.textPrimary,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
