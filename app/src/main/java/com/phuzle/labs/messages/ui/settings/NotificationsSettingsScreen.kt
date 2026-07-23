package com.phuzle.labs.messages.ui.settings
import com.phuzle.labs.messages.ui.components.topBarContentPadding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phuzle.labs.messages.domain.model.NotificationChannelIds
import com.phuzle.labs.messages.ui.AppViewModel
import com.phuzle.labs.messages.ui.components.SectionLabel
import com.phuzle.labs.messages.ui.components.SettingsCard
import com.phuzle.labs.messages.ui.components.SettingsRowDivider
import com.phuzle.labs.messages.ui.components.SettingsToggleRow
import com.phuzle.labs.messages.ui.model.AppUiState
import com.phuzle.labs.messages.ui.theme.MessagesTheme

private data class ChannelInfo(val id: String, val name: String, val importance: String, val behavior: String)

private val CHANNELS = listOf(
    ChannelInfo(NotificationChannelIds.PERSONAL, "Direct Messages", "High", "Ring, vibrate, heads-up"),
    ChannelInfo(NotificationChannelIds.OTP, "Authentication", "High", "Ring, vibrate, heads-up"),
    ChannelInfo(NotificationChannelIds.TRANSACTIONS, "Transactions", "Default", "Chime, status bar icon"),
    ChannelInfo(NotificationChannelIds.PROMOTIONS, "Promotional", "Low", "Silent, hidden from lock screen"),
)

private val LOCK_VISIBILITY_OPTIONS = listOf("full" to "Name & message", "nameOnly" to "Name only", "hidden" to "Hide content")

@Composable
fun NotificationsSettingsScreen(state: AppUiState, viewModel: AppViewModel) {
    val tokens = MessagesTheme.tokens
    val settings = state.settings

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = topBarContentPadding(68.dp), start = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        SettingsCard {
            SettingsToggleRow("Allow notifications", settings.notificationsAllowed, { viewModel.toggleNotificationsAllowed() })
            SettingsRowDivider()
            SettingsToggleRow("Wake screen for high priority", settings.wakeScreenForHighPriority, { viewModel.toggleWakeScreen() })
            SettingsRowDivider()
            SettingsToggleRow(
                "Quick action buttons", settings.quickActionButtons, { viewModel.toggleQuickActions() },
                subtitle = "Copy Code & quick-reply in notifications",
            )
        }

        Column {
            SectionLabel("Lock screen visibility", Modifier.padding(bottom = 8.dp))
            SettingsCard {
                LOCK_VISIBILITY_OPTIONS.forEachIndexed { index, (key, label) ->
                    if (index > 0) SettingsRowDivider()
                    val active = settings.lockScreenVisibility == key
                    Row(
                        Modifier.fillMaxWidth().clickable { viewModel.setLockScreenVisibility(key) }.padding(13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        val dotColor = if (active) tokens.accent else androidx.compose.ui.graphics.Color.Transparent
                        androidx.compose.foundation.layout.Box(
                            Modifier.size(16.dp).background(dotColor, CircleShape).border(2.dp, if (active) tokens.accent else tokens.border, CircleShape),
                        )
                        Text(label, color = tokens.textPrimary, fontSize = 14.sp)
                    }
                }
            }
        }

        Column {
            SectionLabel("Categories", Modifier.padding(bottom = 8.dp))
            SettingsCard {
                CHANNELS.forEachIndexed { index, channel ->
                    if (index > 0) SettingsRowDivider()
                    val enabled = when (channel.id) {
                        NotificationChannelIds.PERSONAL -> settings.channelPersonalEnabled
                        NotificationChannelIds.OTP -> settings.channelOtpEnabled
                        NotificationChannelIds.TRANSACTIONS -> settings.channelTransactEnabled
                        else -> settings.channelPromoEnabled
                    }
                    SettingsToggleRow(
                        title = channel.name,
                        checked = enabled,
                        onToggle = { viewModel.toggleChannel(channel.id) },
                        subtitle = "${channel.importance} · ${channel.behavior}",
                    )
                }
            }
        }
    }
}
