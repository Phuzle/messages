package com.phuzle.labs.messages.ui.privatechats

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phuzle.labs.messages.ui.AppViewModel
import com.phuzle.labs.messages.ui.components.BackBarScaffold
import com.phuzle.labs.messages.ui.components.SimpleThreadRow
import com.phuzle.labs.messages.ui.model.AppUiState
import com.phuzle.labs.messages.ui.theme.MessagesTheme
import com.phuzle.labs.messages.ui.theme.ShapeMedium

private fun appLockMethodLabel(key: String) = when (key) {
    "face" -> "Face Unlock"
    "pin" -> "PIN"
    else -> "Fingerprint"
}

@Composable
fun PrivateChatsScreen(state: AppUiState, viewModel: AppViewModel) {
    val tokens = MessagesTheme.tokens
    BackBarScaffold(title = "Private Chats", onBack = viewModel::goBack) {
        if (state.privateChatsUnlockedThisSession) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 68.dp, start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.privateThreads, key = { it.id }) { item ->
                    SimpleThreadRow(item = item, onClick = { viewModel.openThreadById(item.id) })
                }
                if (state.privateThreads.isEmpty()) {
                    item {
                        Text(
                            "No private chats yet. Use a chat's long-press menu to move it here.",
                            color = tokens.textTertiary, fontSize = 13.5.sp, textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxSize().padding(top = 40.dp),
                        )
                    }
                }
            }
        } else {
            Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(
                        Modifier.width(34.dp).height(22.dp).background(tokens.surfaceAlt, RoundedCornerShape(5.dp)).border(1.dp, tokens.border, RoundedCornerShape(5.dp)),
                    )
                    Text("Private chats are locked", color = tokens.textSecondary, fontSize = 14.sp)
                    Text(
                        "Unlock with ${appLockMethodLabel(state.settings.appLockMethod)}",
                        color = tokens.accentText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .background(tokens.accent, ShapeMedium)
                            .clickable(onClick = viewModel::unlockPrivateChats)
                            .padding(horizontal = 20.dp, vertical = 11.dp),
                    )
                }
            }
        }
    }
}
