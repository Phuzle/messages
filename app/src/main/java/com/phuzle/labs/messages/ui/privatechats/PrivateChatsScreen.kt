package com.phuzle.labs.messages.ui.privatechats
import com.phuzle.labs.messages.ui.components.topBarContentPadding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phuzle.labs.messages.ui.AppViewModel
import com.phuzle.labs.messages.ui.components.BackBarScaffold
import com.phuzle.labs.messages.ui.components.BiometricGate
import com.phuzle.labs.messages.ui.components.EmptyState
import com.phuzle.labs.messages.ui.components.SimpleThreadRow
import com.phuzle.labs.messages.ui.model.AppUiState
import com.phuzle.labs.messages.ui.theme.MessagesTheme
import com.phuzle.labs.messages.ui.theme.ShapeMedium

@Composable
fun PrivateChatsScreen(state: AppUiState, viewModel: AppViewModel) {
    val tokens = MessagesTheme.tokens
    BackBarScaffold(title = "Private Chats", onBack = viewModel::goBack) {
        if (state.privateChatsUnlockedThisSession) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = topBarContentPadding(68.dp), start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (state.privateThreads.isEmpty()) {
                    item {
                        EmptyState(
                            icon = Icons.Filled.LockOpen,
                            title = "No private chats yet",
                            detail = "Use a chat's long-press menu to move it here.",
                            modifier = Modifier.fillMaxSize().padding(top = 20.dp),
                        )
                    }
                } else {
                    items(state.privateThreads, key = { it.id }) { item ->
                        SimpleThreadRow(item = item, onClick = { viewModel.openThreadById(item.id) })
                    }
                }
            }
        } else {
            BiometricGate(
                key = "private_chats",
                title = "Unlock private chats",
                subtitle = "Confirm it's you to view your private chats",
                onUnlocked = viewModel::unlockPrivateChats,
            ) { retry ->
                Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Box(
                            Modifier.size(48.dp).background(tokens.surfaceAlt, RoundedCornerShape(14.dp)).border(1.dp, tokens.border, RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Filled.Lock, contentDescription = null, tint = tokens.textSecondary, modifier = Modifier.size(22.dp))
                        }
                        Text("Private chats are locked", color = tokens.textSecondary, fontSize = 14.sp)
                        Text(
                            "Unlock",
                            color = tokens.accentText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .background(tokens.accent, ShapeMedium)
                                .clickable(onClick = retry)
                                .padding(horizontal = 20.dp, vertical = 11.dp),
                        )
                    }
                }
            }
        }
    }
}
