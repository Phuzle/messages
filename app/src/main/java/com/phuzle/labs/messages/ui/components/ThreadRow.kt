package com.phuzle.labs.messages.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phuzle.labs.messages.ui.model.ThreadUi
import com.phuzle.labs.messages.ui.theme.MessagesTheme
import com.phuzle.labs.messages.ui.theme.SwipeActionMeta
import com.phuzle.labs.messages.ui.theme.swipeActionMeta

/**
 * A single inbox row. Swiping past the threshold in either direction fires that side's configured
 * action (archive/delete/toggle-read/none) via Material3's [SwipeToDismissBox], which also gives
 * us the settle-back-to-center spring the prototype relies on for non-removing actions for free.
 *
 * Tapping/long-pressing the avatar is a separate gesture target from the rest of the row: a tap
 * opens that chat's profile directly ([onAvatarClick]), a long-press starts multi-select
 * ([onAvatarLongPress]) — mirroring how most inbox apps let you jump into "select mode" from the
 * avatar without disturbing the row's own open/action-sheet gestures. Once [selectionMode] is on
 * (someone long-pressed an avatar), every tap anywhere on any row — including its avatar — toggles
 * that row's selection instead, and swiping is disabled to avoid an ambiguous double-meaning.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ThreadRow(
    thread: ThreadUi,
    swipeRightActionKey: String,
    swipeLeftActionKey: String,
    onOpen: () -> Unit,
    onLongPress: () -> Unit,
    onSwipeRight: () -> Unit,
    onSwipeLeft: () -> Unit,
    modifier: Modifier = Modifier,
    selectionMode: Boolean = false,
    selected: Boolean = false,
    onToggleSelect: () -> Unit = {},
    onAvatarClick: () -> Unit = {},
    onAvatarLongPress: () -> Unit = {},
) {
    val tokens = MessagesTheme.tokens
    val dismissState = rememberSwipeToDismissBoxState(
        // Below Material3's 50% default so a normal reveal-and-release swipe actually commits —
        // 0.6f (tried previously) made a deliberate swipe that clearly reveals the action label
        // spring back without firing, which read as "the swipe doesn't work."
        positionalThreshold = { totalWidth -> totalWidth * 0.35f },
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> onSwipeRight()
                SwipeToDismissBoxValue.EndToStart -> onSwipeLeft()
                SwipeToDismissBoxValue.Settled -> {}
            }
            false // never let the box actually remove itself; the backing list re-filters instead
        },
    )
    val rightPanel = swipeActionMeta(swipeRightActionKey, thread.unread, tokens)
    val leftPanel = swipeActionMeta(swipeLeftActionKey, thread.unread, tokens)

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        enableDismissFromStartToEnd = !selectionMode && rightPanel.label != "—",
        enableDismissFromEndToStart = !selectionMode && leftPanel.label != "—",
        backgroundContent = {
            val panel = when (dismissState.dismissDirection) {
                SwipeToDismissBoxValue.StartToEnd -> rightPanel
                SwipeToDismissBoxValue.EndToStart -> leftPanel
                SwipeToDismissBoxValue.Settled -> SwipeActionMeta("", tokens.bg, null)
            }
            val alignment = if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd) {
                Alignment.CenterStart
            } else {
                Alignment.CenterEnd
            }
            Box(
                modifier = Modifier.fillMaxSize().background(panel.color).padding(horizontal = 22.dp),
                contentAlignment = alignment,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (alignment == Alignment.CenterEnd) {
                        Text(panel.label, color = androidx.compose.ui.graphics.Color.White, fontSize = 12.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                    }
                    panel.icon?.let {
                        Icon(
                            it,
                            contentDescription = panel.label,
                            tint = androidx.compose.ui.graphics.Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp).size(18.dp),
                        )
                    }
                    if (alignment == Alignment.CenterStart) {
                        Text(panel.label, color = androidx.compose.ui.graphics.Color.White, fontSize = 12.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                    }
                }
            }
        },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (selected) tokens.accentSoft else tokens.bg)
                .combinedClickable(
                    onClick = if (selectionMode) onToggleSelect else onOpen,
                    onLongClick = if (selectionMode) null else onLongPress,
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.combinedClickable(
                    onClick = if (selectionMode) onToggleSelect else onAvatarClick,
                    onLongClick = if (selectionMode) null else onAvatarLongPress,
                ),
            ) {
                if (selectionMode) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(if (selected) tokens.accent else tokens.surfaceAlt, CircleShape)
                            .border(1.dp, if (selected) tokens.accent else tokens.border, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (selected) {
                            Icon(Icons.Filled.Check, contentDescription = "Selected", tint = tokens.accentText, modifier = Modifier.size(20.dp))
                        }
                    }
                } else {
                    AvatarBubble(thread.initials, thread.avatarColor, thread.isBusiness, photoUri = thread.photoUri)
                }
            }
            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween) {
                    Text(
                        highlightedText(thread.displayName, thread.displayNameMatch),
                        color = tokens.textPrimary,
                        fontWeight = thread.nameWeight,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Text(thread.timeLabel, color = tokens.textTertiary, fontSize = 12.sp)
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                ) {
                    Text(
                        highlightedText(thread.preview, thread.previewMatch),
                        color = tokens.textSecondary,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (thread.unread) {
                        Box(
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .size(8.dp)
                                .background(tokens.accent, androidx.compose.foundation.shape.CircleShape),
                        )
                    }
                }
            }
        }
    }
}
