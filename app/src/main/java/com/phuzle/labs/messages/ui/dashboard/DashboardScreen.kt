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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import com.phuzle.labs.messages.domain.model.Category
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.outlined.Chat as ChatOutlined
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.AccountBalanceWallet as AccountBalanceWalletOutlined
import androidx.compose.material.icons.outlined.Notifications as NotificationsOutlined
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phuzle.labs.messages.ui.AppViewModel
import com.phuzle.labs.messages.ui.components.BarInset
import com.phuzle.labs.messages.ui.components.CategoryChip
import com.phuzle.labs.messages.ui.components.FlatTextField
import com.phuzle.labs.messages.ui.components.EmptyState
import com.phuzle.labs.messages.ui.components.GlassBar
import com.phuzle.labs.messages.ui.components.ThreadRow
import com.phuzle.labs.messages.ui.components.roundClickable
import com.phuzle.labs.messages.ui.model.AccountUi
import com.phuzle.labs.messages.ui.model.AppUiState
import com.phuzle.labs.messages.ui.model.DashboardTab
import com.phuzle.labs.messages.ui.model.ReminderUi
import com.phuzle.labs.messages.ui.theme.MessagesTheme
import com.phuzle.labs.messages.ui.theme.ShapeMedium
import com.phuzle.labs.messages.ui.theme.ShapeSmall

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
            DashboardTab.Messages -> if (state.threads.isEmpty()) {
                EmptyState(
                    icon = Icons.AutoMirrored.Filled.Chat,
                    title = if (state.searchQuery.isNotBlank() || state.activeCategory != Category.All || state.unreadOnly) "No matching messages" else "No messages yet",
                    detail = if (state.searchQuery.isNotBlank() || state.activeCategory != Category.All || state.unreadOnly) {
                        "Try a different search, category, or filter."
                    } else {
                        "Incoming texts will show up here automatically."
                    },
                    modifier = Modifier.padding(top = topContentPadding, bottom = bottomContentPadding),
                )
            } else {
                LazyColumn(
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
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
            }
            DashboardTab.Passbook -> if (state.accounts.isEmpty()) {
                EmptyState(
                    icon = Icons.Filled.AccountBalanceWallet,
                    title = "No transactions yet",
                    detail = "Bank and card messages are captured automatically as they arrive — nothing to show until then.",
                    modifier = Modifier.padding(top = topContentPadding, bottom = bottomContentPadding),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = topContentPadding, bottom = bottomContentPadding, start = 16.dp, end = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(state.accounts, key = { it.last4 }) { account ->
                        AccountCard(account = account, onClick = { viewModel.openAccountDetail(account.last4) }, modifier = Modifier.animateItem())
                    }
                }
            }
            DashboardTab.Reminders -> if (state.reminders.isEmpty()) {
                EmptyState(
                    icon = Icons.Filled.NotificationsNone,
                    title = "No reminders yet",
                    detail = "Reminders will appear here once we can reliably detect due dates and follow-ups from your messages.",
                    modifier = Modifier.padding(top = topContentPadding, bottom = bottomContentPadding),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = topContentPadding, bottom = bottomContentPadding, start = 16.dp, end = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(state.reminders, key = { it.id }) { reminder ->
                        ReminderCard(reminder, onDismiss = { viewModel.dismissReminder(reminder.id) }, modifier = Modifier.animateItem())
                    }
                }
            }
        }

        Column(Modifier.align(Alignment.TopCenter)) {
            GlassBar(height = 52.dp, inset = BarInset.Top) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(40.dp).roundClickable(onClick = viewModel::toggleDrawer),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Filled.Menu, contentDescription = "Menu", tint = tokens.textPrimary, modifier = Modifier.size(22.dp)) }

                    when (state.activeTab) {
                        DashboardTab.Messages -> Row(
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .background(tokens.inputBg, ShapeSmall)
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
                        Modifier.size(40.dp).roundClickable(onClick = viewModel::toggleOverflowMenu),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Filled.MoreVert, contentDescription = "More options", tint = tokens.textPrimary, modifier = Modifier.size(22.dp)) }
                }
            }
            if (isMessages) {
                val categoryScrollState = rememberScrollState()
                val coroutineScope = rememberCoroutineScope()
                var rowWidth by remember { mutableIntStateOf(0) }
                val chipBounds = remember { mutableStateMapOf<Category, Pair<Int, Int>>() }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(tokens.bg)
                        .border(androidx.compose.foundation.BorderStroke(1.dp, tokens.barBorder))
                        .onGloballyPositioned { rowWidth = it.size.width }
                        .horizontalScroll(categoryScrollState)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    state.categories.forEach { chip ->
                        CategoryChip(
                            chip = chip,
                            modifier = Modifier.onGloballyPositioned { coords ->
                                chipBounds[chip.category] = coords.positionInParent().x.toInt() to coords.size.width
                            },
                            onClick = {
                                viewModel.setCategory(chip.category)
                                chipBounds[chip.category]?.let { (x, width) ->
                                    val target = (x + width / 2) - rowWidth / 2
                                    coroutineScope.launch {
                                        categoryScrollState.animateScrollTo(target.coerceIn(0, categoryScrollState.maxValue))
                                    }
                                }
                            },
                        )
                    }
                }
            }
        }

        GlassBar(
            modifier = Modifier.align(Alignment.BottomCenter),
            height = BOTTOM_BAR_HEIGHT.dp,
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

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = (BOTTOM_BAR_HEIGHT + 16).dp + navBarInset)
                .size(56.dp)
                .background(tokens.accent, CircleShape)
                .roundClickable(onClick = viewModel::openCompose),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Compose", tint = tokens.accentText, modifier = Modifier.size(26.dp))
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
            Box(
                modifier = Modifier
                    .background(if (active) tokens.accentSoft else androidx.compose.ui.graphics.Color.Transparent, CircleShape)
                    .padding(horizontal = 18.dp, vertical = 4.dp),
            ) {
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

@Composable
private fun AccountCard(account: AccountUi, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val tokens = MessagesTheme.tokens
    val amountColor = if (account.netIsCredit) tokens.accent else tokens.textPrimary
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(tokens.surface, ShapeMedium)
            .border(1.dp, if (account.selected) tokens.accent else tokens.border, ShapeMedium)
            .clip(ShapeMedium)
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text("•• ${account.last4}", color = tokens.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(
                "${account.transactionCount} transaction${if (account.transactionCount == 1) "" else "s"}",
                color = tokens.textTertiary, fontSize = 12.5.sp, modifier = Modifier.padding(top = 3.dp),
            )
        }
        Text(account.netLabel, color = amountColor, fontSize = 17.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ReminderCard(reminder: ReminderUi, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val tokens = MessagesTheme.tokens
    Row(
        modifier.fillMaxWidth().background(tokens.surface, ShapeMedium)
            .border(1.dp, tokens.border, ShapeMedium).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(Modifier.weight(1f)) {
            Text(reminder.title, color = tokens.textPrimary, fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold)
            Text(reminder.detail, color = tokens.textSecondary, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
            Text(
                reminder.timeLabel.uppercase(), color = tokens.accent, fontSize = 11.5.sp, fontWeight = FontWeight.Bold,
                letterSpacing = 0.3.sp, modifier = Modifier.padding(top = 6.dp),
            )
        }
        Box(
            Modifier.size(28.dp).roundClickable(onClick = onDismiss),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Check, contentDescription = "Dismiss reminder", tint = tokens.textTertiary, modifier = Modifier.size(18.dp))
        }
    }
}
