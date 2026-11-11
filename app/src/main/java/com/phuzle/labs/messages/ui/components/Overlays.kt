package com.phuzle.labs.messages.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phuzle.labs.messages.ui.theme.IconSizes
import com.phuzle.labs.messages.ui.theme.MessagesTheme
import com.phuzle.labs.messages.ui.theme.ShapeLarge
import com.phuzle.labs.messages.ui.theme.ShapeMedium

/*
 * The app's two overlay shapes, so that every one behaves and looks the same:
 *
 *  - AppBottomSheet — pick one of several actions, or adjust a few options. Slides up from the
 *    bottom edge over a fading scrim.
 *  - AppDialog / AppDialogHost — a yes/no question or a small form. A centered card that fades and
 *    scales in over the same scrim.
 *
 * Both stay composed while their exit animation runs (so they ease out instead of vanishing), close
 * on a scrim tap or the system Back, and swallow taps on their own surface. Anything whose content
 * comes from nullable state should cache it with [rememberLast] so the content doesn't blank out
 * during that exit.
 */

/** Keeps showing the last non-null [value] — lets an overlay's content survive its own exit animation. */
@Composable
fun <T : Any> rememberLast(value: T?): T? {
    var last by remember { mutableStateOf(value) }
    if (value != null) last = value
    return last
}

private const val SCRIM_IN_MS = 200
private const val SCRIM_OUT_MS = 160
private const val SHEET_IN_MS = 260
private const val SHEET_OUT_MS = 200
private const val DIALOG_IN_MS = 200
private const val DIALOG_OUT_MS = 140

@Composable
private fun OverlayHost(
    visible: Boolean,
    onDismiss: () -> Unit,
    contentAlignment: Alignment,
    enter: EnterTransition,
    exit: ExitTransition,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val tokens = MessagesTheme.tokens
    val state = remember { MutableTransitionState(false) }
    state.targetState = visible
    if (!state.currentState && !state.targetState) return

    BackHandler(enabled = visible, onBack = onDismiss)
    Box(modifier.fillMaxSize()) {
        AnimatedVisibility(
            visibleState = state,
            enter = fadeIn(tween(SCRIM_IN_MS)),
            exit = fadeOut(tween(SCRIM_OUT_MS)),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(tokens.overlayBg)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
            )
        }
        Box(Modifier.fillMaxSize(), contentAlignment = contentAlignment) {
            AnimatedVisibility(visibleState = state, enter = enter, exit = exit) { content() }
        }
    }
}

/** Taps that land on an overlay's own surface must not fall through to the scrim behind it. */
private fun Modifier.consumeTaps(): Modifier = pointerInput(Unit) { detectTapGestures { } }

@Composable
fun AppBottomSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    maxHeight: Dp = Dp.Unspecified,
    content: @Composable ColumnScope.() -> Unit,
) {
    val tokens = MessagesTheme.tokens
    OverlayHost(
        visible = visible,
        onDismiss = onDismiss,
        contentAlignment = Alignment.BottomCenter,
        enter = slideInVertically(tween(SHEET_IN_MS)) { it } + fadeIn(tween(SHEET_IN_MS)),
        exit = slideOutVertically(tween(SHEET_OUT_MS)) { it } + fadeOut(tween(SHEET_OUT_MS)),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxHeight)
                .background(tokens.surface, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .consumeTaps()
                .navigationBarsPadding()
                .padding(bottom = 12.dp),
        ) {
            Box(
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 10.dp)
                    .size(width = 32.dp, height = 4.dp)
                    .background(tokens.textTertiary.copy(alpha = 0.5f), RoundedCornerShape(4.dp)),
            )
            if (title != null) {
                Text(
                    title,
                    color = tokens.textPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 14.dp),
                )
                SettingsRowDivider()
            }
            content()
        }
    }
}

/** One tappable action in a sheet: an icon and a label, 56dp tall. */
@Composable
fun SheetActionRow(label: String, icon: ImageVector, onClick: () -> Unit, color: Color = MessagesTheme.tokens.textPrimary) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = 56.dp)
            .padding(horizontal = 24.dp),
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(IconSizes.Medium))
        Text(label, color = color, fontSize = 16.sp, modifier = Modifier.padding(start = 16.dp))
    }
}

/** The centered card every dialog sits on; build custom ones (a text field, a countdown) with this
 * and the plain [AppDialog] for questions. */
@Composable
fun AppDialogHost(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    maxWidth: Dp = 320.dp,
    contentPadding: Modifier = Modifier.padding(24.dp),
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable ColumnScope.() -> Unit,
) {
    val tokens = MessagesTheme.tokens
    OverlayHost(
        visible = visible,
        onDismiss = onDismiss,
        contentAlignment = Alignment.Center,
        enter = fadeIn(tween(DIALOG_IN_MS)) + scaleIn(tween(DIALOG_IN_MS), initialScale = 0.92f),
        exit = fadeOut(tween(DIALOG_OUT_MS)) + scaleOut(tween(DIALOG_OUT_MS), targetScale = 0.96f),
        modifier = modifier.imePadding().padding(horizontal = 28.dp, vertical = 24.dp),
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = maxWidth)
                .fillMaxWidth()
                .background(tokens.modalBg, ShapeLarge)
                .consumeTaps()
                .then(contentPadding),
            horizontalAlignment = horizontalAlignment,
            content = content,
        )
    }
}

/** A question with a confirming action and a way out. [destructive] paints the confirm button in
 * the danger color — every delete-style confirmation reads the same. */
@Composable
fun AppDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    title: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    message: String? = null,
    dismissLabel: String = "Cancel",
    destructive: Boolean = false,
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    // The strings may be derived from state that is cleared by the very action that closed the
    // dialog ("Delete 0 chats?"); hold on to what was last shown while it eases out.
    var shownTitle by remember { mutableStateOf(title) }
    var shownMessage by remember { mutableStateOf(message) }
    var shownConfirm by remember { mutableStateOf(confirmLabel) }
    if (visible) {
        shownTitle = title
        shownMessage = message
        shownConfirm = confirmLabel
    }
    val tokens = MessagesTheme.tokens
    AppDialogHost(visible = visible, onDismiss = onDismiss) {
        Text(shownTitle, color = tokens.modalText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        shownMessage?.let {
            Text(it, color = tokens.modalText.copy(alpha = 0.72f), fontSize = 14.sp, lineHeight = 20.sp, modifier = Modifier.padding(top = 8.dp))
        }
        content?.invoke(this)
        Column(Modifier.padding(top = 22.dp)) {
            DialogPrimaryButton(shownConfirm, onConfirm, destructive)
            DialogSecondaryButton(dismissLabel, onDismiss)
        }
    }
}

@Composable
fun DialogPrimaryButton(label: String, onClick: () -> Unit, destructive: Boolean = false, modifier: Modifier = Modifier) {
    val tokens = MessagesTheme.tokens
    Text(
        label,
        color = if (destructive) Color.White else tokens.onModalAccent,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        modifier = modifier
            .fillMaxWidth()
            .clip(ShapeMedium)
            .background(if (destructive) tokens.danger else tokens.modalAccent, ShapeMedium)
            .clickable(onClick = onClick)
            .heightIn(min = 52.dp)
            .wrapContentHeight(Alignment.CenterVertically),
    )
}

@Composable
fun DialogSecondaryButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val tokens = MessagesTheme.tokens
    Text(
        label,
        color = tokens.modalText.copy(alpha = 0.6f),
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = 48.dp)
            .wrapContentHeight(Alignment.CenterVertically),
    )
}
