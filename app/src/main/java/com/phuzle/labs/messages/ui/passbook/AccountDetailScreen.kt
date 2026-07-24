package com.phuzle.labs.messages.ui.passbook

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phuzle.labs.messages.ui.AppViewModel
import com.phuzle.labs.messages.ui.components.BackBarScaffold
import com.phuzle.labs.messages.ui.components.BiometricGate
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

    BackBarScaffold(title = "•• $last4", onBack = viewModel::goBack) {
        if (!unlocked) {
            BiometricGate(
                key = last4,
                title = "Unlock account details",
                subtitle = "Confirm it's you to view •• $last4",
                onUnlocked = { unlocked = true },
            ) { retry ->
                EmptyState(
                    icon = Icons.Filled.Lock,
                    title = "Locked",
                    detail = "Confirm it's you to view this account's details.",
                    modifier = Modifier.padding(top = topBarContentPadding(68.dp)),
                    actionLabel = "Try again",
                    onAction = retry,
                )
            }
        } else {
            LazyColumn(
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
                            account?.netLabel ?: "—",
                            color = if (account?.netIsCredit == true) tokens.success else if (account != null) tokens.danger else tokens.textPrimary,
                            fontSize = 28.sp, fontWeight = FontWeight.Bold,
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
                            state.transactions.forEachIndexed { index, tx ->
                                if (index > 0) {
                                    Spacer(Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(1.dp).background(tokens.border))
                                }
                                Row(
                                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            Modifier.size(34.dp)
                                                .background(if (tx.isCredit) tokens.success.copy(alpha = 0.14f) else tokens.danger.copy(alpha = 0.14f), CircleShape),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Icon(
                                                if (tx.isCredit) Icons.Filled.ArrowDownward else Icons.Filled.ArrowUpward,
                                                contentDescription = null,
                                                tint = if (tx.isCredit) tokens.success else tokens.danger,
                                                modifier = Modifier.size(16.dp),
                                            )
                                        }
                                        Column(Modifier.padding(start = 10.dp)) {
                                            Text(tx.merchant, color = tokens.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                            Text(tx.timeLabel, color = tokens.textTertiary, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                                        }
                                    }
                                    Text(
                                        tx.amountLabel, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                                        color = if (tx.isCredit) tokens.success else tokens.danger,
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
