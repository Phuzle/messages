package com.phuzle.labs.messages.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import com.phuzle.labs.messages.ui.theme.IconSizes
import com.phuzle.labs.messages.ui.theme.MessagesTheme

/**
 * The top-bar content every multi-select-capable list swaps to once at least one row is selected
 * — a close button, the running count, a screen-specific set of bulk-action icons, and a trigger
 * for the "Select all / Invert selection / Deselect all" overflow. Shared instead of duplicated
 * per screen (Dashboard, Archived, Private Chats, Recycle Bin) so multi-select behaves and looks
 * identical everywhere it appears, instead of each list inventing its own variant.
 *
 * [showMenu]/[onMenuClick] are hoisted rather than owned here, and [SelectionOverflowMenu] is a
 * separate composable the caller renders OUTSIDE its GlassBar — GlassBar's `Modifier.shadow`
 * clips its content by default (clip defaults to `elevation > 0.dp`), which would silently make
 * an overflow popup nested in here invisible and untouchable the moment it's taller than the bar.
 */
@Composable
fun SelectionTopBar(
    count: Int,
    onClose: () -> Unit,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val tokens = MessagesTheme.tokens
    Row(modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).roundClickable(onClick = onClose), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Close, contentDescription = "Cancel selection", tint = tokens.textPrimary, modifier = Modifier.size(IconSizes.Medium))
        }
        Text(
            "$count selected", color = tokens.textPrimary, fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f).padding(start = 4.dp),
        )
        actions()
        Box(Modifier.size(40.dp).roundClickable(onClick = onMenuClick), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.MoreVert, contentDescription = "Selection options", tint = tokens.textPrimary, modifier = Modifier.size(IconSizes.Medium))
        }
    }
}

/** The popup [SelectionTopBar]'s overflow button opens — render this as a sibling of GlassBar
 * (e.g. directly in the screen's outer Box), never nested inside it. No "deselect all" entry here
 * — that's just the X button, which is always visible right next to this menu's trigger. */
@Composable
fun SelectionOverflowMenu(
    visible: Boolean,
    onDismiss: () -> Unit,
    onSelectAll: () -> Unit,
    onInvertSelection: () -> Unit,
) {
    OverflowMenu(
        visible = visible,
        onDismiss = onDismiss,
        items = listOf(
            MenuItem("Select all", icon = Icons.Filled.SelectAll) { onSelectAll(); onDismiss() },
            MenuItem("Invert selection", icon = Icons.Filled.SwapHoriz) { onInvertSelection(); onDismiss() },
        ),
    )
}
