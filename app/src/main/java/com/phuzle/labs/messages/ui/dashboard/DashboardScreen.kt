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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phuzle.labs.messages.ui.AppViewModel
import com.phuzle.labs.messages.ui.components.BarInset
import com.phuzle.labs.messages.ui.components.CategoryChip
import com.phuzle.labs.messages.ui.components.FlatTextField
import com.phuzle.labs.messages.ui.components.GlassBar
import com.phuzle.labs.messages.ui.components.HamburgerIcon
import com.phuzle.labs.messages.ui.components.SearchGlyph
import com.phuzle.labs.messages.ui.components.ThreadRow
import com.phuzle.labs.messages.ui.model.AppUiState
import com.phuzle.labs.messages.ui.model.DashboardTab
import com.phuzle.labs.messages.ui.theme.MessagesTheme

@Composable
fun DashboardScreen(state: AppUiState, viewModel: AppViewModel) {
    val tokens = MessagesTheme.tokens
    val isMessages = state.activeTab == DashboardTab.Messages
    val statusBarInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val topContentPadding = (if (isMessages) 104.dp else 68.dp) + statusBarInset
    val bottomContentPadding = 90.dp + navBarInset

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
                    ) { HamburgerIcon(tokens.textPrimary) }

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
                            SearchGlyph(tokens.textTertiary)
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
                    ) { Text("⋮", color = tokens.textPrimary, fontSize = 18.sp) }
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

        GlassBar(modifier = Modifier.align(Alignment.BottomCenter), height = 64.dp, hairlineAtBottom = false, inset = BarInset.Bottom) {
            Row(Modifier.fillMaxSize()) {
                BottomTabButton(
                    label = "Messages",
                    active = isMessages,
                    hasUnreadDot = state.hasUnread,
                    onClick = viewModel::openMessagesTab,
                    modifier = Modifier.weight(1f),
                ) { color -> Box(Modifier.width(20.dp).height(16.dp).background(color, RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp, bottomEnd = 5.dp, bottomStart = 0.dp))) }
                BottomTabButton(
                    label = "Passbook",
                    active = state.activeTab == DashboardTab.Passbook,
                    hasUnreadDot = false,
                    onClick = viewModel::openPassbookTab,
                    modifier = Modifier.weight(1f),
                ) { color -> Box(Modifier.width(20.dp).height(15.dp).border(2.dp, color, RoundedCornerShape(3.dp))) }
                BottomTabButton(
                    label = "Reminders",
                    active = state.activeTab == DashboardTab.Reminders,
                    hasUnreadDot = false,
                    onClick = viewModel::openRemindersTab,
                    modifier = Modifier.weight(1f),
                ) { color ->
                    Box(
                        Modifier.size(16.dp).rotate(45f).background(
                            color,
                            RoundedCornerShape(topStartPercent = 50, topEndPercent = 50, bottomEndPercent = 50, bottomStartPercent = 0),
                        ),
                    )
                }
            }
        }

        if (isMessages) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 80.dp + navBarInset)
                    .size(52.dp)
                    .background(tokens.accent, RoundedCornerShape(16.dp))
                    .clickable(onClick = viewModel::openCompose),
                contentAlignment = Alignment.Center,
            ) {
                Text("+", color = tokens.accentText, fontSize = 26.sp, fontWeight = FontWeight.Light)
            }
        }
    }
}

@Composable
private fun BottomTabButton(
    label: String,
    active: Boolean,
    hasUnreadDot: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable (Color) -> Unit,
) {
    val tokens = MessagesTheme.tokens
    val color = if (active) tokens.accent else tokens.textTertiary
    Box(modifier.clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            icon(color)
            Text(label, color = color, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
        }
        if (hasUnreadDot) {
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(start = 20.dp)
                    .size(8.dp)
                    .background(tokens.danger, CircleShape),
            )
        }
    }
}
