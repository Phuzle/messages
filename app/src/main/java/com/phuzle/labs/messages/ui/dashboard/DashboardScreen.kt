package com.phuzle.labs.messages.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.outlined.Chat as ChatOutlined
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.AccountBalanceWallet as AccountBalanceWalletOutlined
import androidx.compose.material.icons.outlined.Notifications as NotificationsOutlined
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phuzle.labs.messages.ui.AppViewModel
import com.phuzle.labs.messages.ui.components.BarInset
import com.phuzle.labs.messages.ui.components.CategoryChip
import com.phuzle.labs.messages.ui.components.FlatTextField
import com.phuzle.labs.messages.ui.components.GlassBar
import com.phuzle.labs.messages.ui.components.ThreadRow
import com.phuzle.labs.messages.ui.model.AppUiState
import com.phuzle.labs.messages.ui.model.DashboardTab
import com.phuzle.labs.messages.ui.theme.MessagesTheme

private const val BOTTOM_BAR_HEIGHT = 60

@Composable
fun DashboardScreen(state: AppUiState, viewModel: AppViewModel) {
    val tokens = MessagesTheme.tokens
    val isMessages = state.activeTab == DashboardTab.Messages
    val statusBarInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val topContentPadding = (if (isMessages) 104.dp else 68.dp) + statusBarInset
    val bottomContentPadding = (BOTTOM_BAR_HEIGHT + 26).dp + navBarInset

    Box(Modifier.fillMaxSize()) {
        when (state.activeTab) {
            DashboardTab.Messages -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = topContentPadding, bottom = bottomContentPadding),
            ) {
                items(state.threads, key = { it.id }) { thread ->
                    ThreadRow(
                        thread = thread,
                        swipeRightActionKey = state.settings.swipeRightAction,
                        swipeLeftActionKey = state.settings.swipeLeftAction,
                        onOpen = { viewModel.openThreadById(thread.id) },
                        onLongPress = { viewModel.openActionSheet(thread.id) },
                        onSwipeRight = { viewModel.onSwipeRight(thread.id) },
                        onSwipeLeft = { viewModel.onSwipeLeft(thread.id) },
                    )
                }
            }
            DashboardTab.Passbook -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = topContentPadding, bottom = bottomContentPadding, start = 16.dp, end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.accounts, key = { it.id }) { account ->
                    Column(
                        Modifier.fillMaxWidth().background(tokens.surface, RoundedCornerShape(12.dp))
                            .border(1.dp, tokens.border, RoundedCornerShape(12.dp)).padding(16.dp),
                    ) {
                        Text("${account.name} · •• ${account.last4}", color = tokens.textSecondary, fontSize = 13.sp)
                        Text(account.balanceLabel, color = tokens.textPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp))
                        Text(account.type.uppercase(), color = tokens.textTertiary, fontSize = 11.5.sp, letterSpacing = 0.4.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                }
                item {
                    Text(
                        "RECENT ACTIVITY", color = tokens.textSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.4.sp, modifier = Modifier.padding(top = 10.dp),
                    )
                }
                item {
                    Column(Modifier.fillMaxWidth().background(tokens.surface, RoundedCornerShape(12.dp)).border(1.dp, tokens.border, RoundedCornerShape(12.dp))) {
                        state.transactions.forEachIndexed { index, tx ->
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column {
                                    Text(tx.merchant, color = tokens.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                    Text("${tx.accountLabel} · ${tx.timeLabel}", color = tokens.textTertiary, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
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
            DashboardTab.Reminders -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = topContentPadding, bottom = bottomContentPadding, start = 16.dp, end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.reminders, key = { it.id }) { reminder ->
                    Column(
                        Modifier.fillMaxWidth().background(tokens.surface, RoundedCornerShape(12.dp))
                            .border(1.dp, tokens.border, RoundedCornerShape(12.dp)).padding(horizontal = 16.dp, vertical = 14.dp),
                    ) {
                        Text(reminder.title, color = tokens.textPrimary, fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold)
                        Text(reminder.detail, color = tokens.textSecondary, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
                        Text(
                            reminder.timeLabel.uppercase(), color = tokens.accent, fontSize = 11.5.sp, fontWeight = FontWeight.Bold,
                            letterSpacing = 0.3.sp, modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                }
            }
        }

        Column(Modifier.align(Alignment.TopCenter)) {
            GlassBar(height = 52.dp, inset = BarInset.Top) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(40.dp).clickable(onClick = viewModel::toggleDrawer),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Filled.Menu, contentDescription = "Menu", tint = tokens.textPrimary, modifier = Modifier.size(22.dp)) }

                    when (state.activeTab) {
                        DashboardTab.Messages -> Row(
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .background(tokens.inputBg, RoundedCornerShape(10.dp))
                                .padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(Icons.Filled.Search, contentDescription = null, tint = tokens.textTertiary, modifier = Modifier.size(16.dp))
                            FlatTextField(
                                value = state.searchQuery,
                                onValueChange = viewModel::onSearchChange,
                                placeholder = "Search messages",
                                modifier = Modifier.weight(1f),
                            )
                        }
                        DashboardTab.Passbook -> Text("Passbook", color = tokens.textPrimary, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        DashboardTab.Reminders -> Text("Reminders", color = tokens.textPrimary, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    }

                    Box(
                        Modifier.size(40.dp).clickable(onClick = viewModel::toggleOverflowMenu),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Filled.MoreVert, contentDescription = "More options", tint = tokens.textPrimary, modifier = Modifier.size(22.dp)) }
                }
            }
            if (isMessages) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(tokens.bg)
                        .border(androidx.compose.foundation.BorderStroke(1.dp, tokens.barBorder))
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    state.categories.forEach { chip ->
                        CategoryChip(chip = chip, onClick = { viewModel.setCategory(chip.category) })
                    }
                }
            }
        }

        GlassBar(
            modifier = Modifier.align(Alignment.BottomCenter),
            height = BOTTOM_BAR_HEIGHT.dp,
            hairlineAtBottom = false,
            inset = BarInset.Bottom,
        ) {
            Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                BottomTabButton(
                    label = "Messages",
                    icon = Icons.AutoMirrored.Filled.Chat,
                    iconOutlined = Icons.AutoMirrored.Outlined.ChatOutlined,
                    active = isMessages,
                    hasUnreadDot = state.hasUnread,
                    onClick = viewModel::openMessagesTab,
                    modifier = Modifier.weight(1f),
                )
                BottomTabButton(
                    label = "Passbook",
                    icon = Icons.Filled.AccountBalanceWallet,
                    iconOutlined = Icons.Outlined.AccountBalanceWalletOutlined,
                    active = state.activeTab == DashboardTab.Passbook,
                    hasUnreadDot = false,
                    onClick = viewModel::openPassbookTab,
                    modifier = Modifier.weight(1f),
                )
                BottomTabButton(
                    label = "Reminders",
                    icon = Icons.Filled.Notifications,
                    iconOutlined = Icons.Outlined.NotificationsOutlined,
                    active = state.activeTab == DashboardTab.Reminders,
                    hasUnreadDot = false,
                    onClick = viewModel::openRemindersTab,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        if (isMessages) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = (BOTTOM_BAR_HEIGHT + 16).dp + navBarInset)
                    .size(52.dp)
                    .background(tokens.accent, RoundedCornerShape(16.dp))
                    .clickable(onClick = viewModel::openCompose),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Compose", tint = tokens.accentText, modifier = Modifier.size(26.dp))
            }
        }
    }
}

@Composable
private fun BottomTabButton(
    label: String,
    icon: ImageVector,
    iconOutlined: ImageVector,
    active: Boolean,
    hasUnreadDot: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = MessagesTheme.tokens
    val color = if (active) tokens.accent else tokens.textTertiary
    Box(modifier.fillMaxSize().clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Box {
                Icon(if (active) icon else iconOutlined, contentDescription = label, tint = color, modifier = Modifier.size(23.dp))
                if (hasUnreadDot) {
                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 2.dp, y = (-2).dp)
                            .size(7.dp)
                            .background(tokens.danger, CircleShape),
                    )
                }
            }
            Text(label, color = color, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
