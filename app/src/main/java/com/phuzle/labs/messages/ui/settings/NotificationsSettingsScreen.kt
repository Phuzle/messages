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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.phuzle.labs.messages.domain.model.Category
import com.phuzle.labs.messages.domain.model.NotificationChannelIds
import com.phuzle.labs.messages.ui.AppViewModel
import com.phuzle.labs.messages.ui.components.PillButton
import com.phuzle.labs.messages.ui.components.SectionLabel
import com.phuzle.labs.messages.ui.components.SettingsCard
import com.phuzle.labs.messages.ui.components.SettingsIconBadge
import com.phuzle.labs.messages.ui.components.SettingsNavRow
import com.phuzle.labs.messages.ui.components.SettingsRowDivider
import com.phuzle.labs.messages.ui.components.SettingsToggleRow
import com.phuzle.labs.messages.ui.components.iconForCategory
import com.phuzle.labs.messages.ui.model.AppUiState
import com.phuzle.labs.messages.ui.theme.MessagesTheme

private data class ChannelInfo(val id: String, val name: String, val category: Category)

// Every inbox category gets its own row here — Others and Unknown used to silently share the
// Promotional channel, which meant toggling/prioritizing one toggled all three together. Each now
// has its own real OS channel (see NotificationChannels.kt) so this list can show all seven
// without any of them being a no-op.
private val CHANNELS = listOf(
    ChannelInfo(NotificationChannelIds.PERSONAL, "Direct Messages", Category.Personal),
    ChannelInfo(NotificationChannelIds.OTP, "Authentication", Category.Otp),
    ChannelInfo(NotificationChannelIds.TRANSACTIONS, "Transactions", Category.Transactions),
    ChannelInfo(NotificationChannelIds.IPO, "IPO Updates", Category.Ipo),
    ChannelInfo(NotificationChannelIds.PROMOTIONS, "Promotional", Category.Promotions),
    ChannelInfo(NotificationChannelIds.OTHERS, "Others", Category.Others),
    ChannelInfo(NotificationChannelIds.UNKNOWN, "Unknown", Category.Unknown),
)

// Per-category notification priority: the OS importance level, which drives whether it
// heads-up/rings, just chimes, or stays silent and off the lock screen. See
// NotificationChannels.updateChannelPriority for why changing it deletes and recreates the
// underlying channel.
private val PRIORITY_OPTIONS = listOf("high" to "High", "default" to "Default", "low" to "Low")

private fun behaviorFor(priority: String): String = when (priority) {
    "high" -> "Ring, vibrate, heads-up"
    "low" -> "Silent, hidden from lock screen"
    else -> "Chime, status bar icon"
}

private fun enabledFor(channel: ChannelInfo, settings: com.phuzle.labs.messages.data.prefs.AppSettings): Boolean =
    when (channel.id) {
        NotificationChannelIds.PERSONAL -> settings.channelPersonalEnabled
        NotificationChannelIds.OTP -> settings.channelOtpEnabled
        NotificationChannelIds.TRANSACTIONS -> settings.channelTransactEnabled
        NotificationChannelIds.IPO -> settings.channelIpoEnabled
        NotificationChannelIds.OTHERS -> settings.channelOthersEnabled
        NotificationChannelIds.UNKNOWN -> settings.channelUnknownEnabled
        else -> settings.channelPromoEnabled
    }

private fun priorityFor(channel: ChannelInfo, settings: com.phuzle.labs.messages.data.prefs.AppSettings): String =
    when (channel.id) {
        NotificationChannelIds.PERSONAL -> settings.channelPersonalPriority
        NotificationChannelIds.OTP -> settings.channelOtpPriority
        NotificationChannelIds.TRANSACTIONS -> settings.channelTransactPriority
        NotificationChannelIds.IPO -> settings.channelIpoPriority
        NotificationChannelIds.OTHERS -> settings.channelOthersPriority
        NotificationChannelIds.UNKNOWN -> settings.channelUnknownPriority
        else -> settings.channelPromoPriority
    }

private val LOCK_VISIBILITY_OPTIONS = listOf("full" to "Name & message", "nameOnly" to "Name only", "hidden" to "Hide content")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsSettingsScreen(state: AppUiState, viewModel: AppViewModel) {
    val tokens = MessagesTheme.tokens
    val settings = state.settings
    var sheetChannel by remember { mutableStateOf<ChannelInfo?>(null) }

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
                    val enabled = enabledFor(channel, settings)
                    val priority = priorityFor(channel, settings)
                    SettingsNavRow(
                        title = channel.name,
                        hint = if (enabled) "${PRIORITY_OPTIONS.first { it.first == priority }.second} · ${behaviorFor(priority)}" else "Off",
                        onClick = { sheetChannel = channel },
                        leading = { SettingsIconBadge(icon = iconForCategory(channel.category)) },
                    )
                }
            }
        }
    }

    sheetChannel?.let { channel ->
        val enabled = enabledFor(channel, settings)
        val priority = priorityFor(channel, settings)
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(onDismissRequest = { sheetChannel = null }, sheetState = sheetState) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).navigationBarsPadding().padding(bottom = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
                    SettingsIconBadge(icon = iconForCategory(channel.category), modifier = Modifier.padding(end = 12.dp))
                    Text(channel.name, color = tokens.textPrimary, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                }
                SettingsCard {
                    SettingsToggleRow(
                        title = "Allow notifications",
                        checked = enabled,
                        onToggle = { viewModel.toggleChannel(channel.id) },
                    )
                }
                if (enabled) {
                    Text(
                        "Priority", color = tokens.textTertiary, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp, modifier = Modifier.padding(top = 18.dp, bottom = 8.dp),
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PRIORITY_OPTIONS.forEach { (key, label) ->
                            PillButton(
                                label = label,
                                active = priority == key,
                                onClick = { viewModel.setChannelPriority(channel.id, key) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                    Text(
                        behaviorFor(priority), color = tokens.textTertiary, fontSize = 12.sp,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }
    }
}
