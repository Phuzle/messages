package com.phuzle.labs.messages.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phuzle.labs.messages.ui.theme.IconSizes
import com.phuzle.labs.messages.ui.theme.MessagesTheme
import com.phuzle.labs.messages.ui.theme.ShapeMedium

data class MenuItem(val label: String, val icon: ImageVector, val danger: Boolean = false, val onClick: () -> Unit)

@Composable
fun OverflowMenu(visible: Boolean, onDismiss: () -> Unit, items: List<MenuItem>, modifier: Modifier = Modifier) {
    val tokens = MessagesTheme.tokens
    // Stays composed through its own exit animation, so the popover eases out instead of vanishing.
    val state = remember { MutableTransitionState(false) }
    state.targetState = visible
    if (!state.currentState && !state.targetState) return
    androidx.activity.compose.BackHandler(enabled = visible, onBack = onDismiss)
    Box(modifier.fillMaxSize().statusBarsPadding()) {
        Box(
            Modifier.fillMaxSize()
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
        )
        AnimatedVisibility(
            visibleState = state,
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 64.dp, end = 10.dp),
            enter = fadeIn(tween(140)) + scaleIn(tween(170), initialScale = 0.88f, transformOrigin = TransformOrigin(1f, 0f)),
            exit = fadeOut(tween(100)) + scaleOut(tween(120), targetScale = 0.92f, transformOrigin = TransformOrigin(1f, 0f)),
        ) {
            Column(
                modifier = Modifier
                    .widthIn(min = 180.dp, max = 240.dp)
                    // A floating popover reads as a distinct layer above content in a way an inline
                    // list row doesn't — same deliberate exception to this app's zero-elevation rule
                    // as GlassBar's top/bottom bars (same 6.dp), not a new one-off.
                    .shadow(elevation = 6.dp, shape = ShapeMedium)
                    .background(tokens.surface, ShapeMedium)
                    .border(1.dp, tokens.border, ShapeMedium),
            ) {
                items.forEachIndexed { index, item ->
                    if (index > 0) SettingsRowDivider()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = item.onClick)
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            item.icon,
                            contentDescription = null,
                            tint = if (item.danger) tokens.danger else tokens.textSecondary,
                            modifier = Modifier.size(IconSizes.Medium),
                        )
                        Text(
                            item.label,
                            color = if (item.danger) tokens.danger else tokens.textPrimary,
                            fontSize = 13.5.sp,
                            modifier = Modifier.padding(start = 12.dp),
                        )
                    }
                }
            }
        }
    }
}
