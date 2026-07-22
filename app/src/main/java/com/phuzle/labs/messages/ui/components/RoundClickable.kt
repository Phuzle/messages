package com.phuzle.labs.messages.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip

/**
 * Clips both the ripple and the hit target to a circle. Plain `clickable()` bounds its ripple to
 * the modifier chain's rectangular layout box, which reads oddly on icon-only buttons (menu,
 * overflow, back, close, FAB) that are visually round — the touch feedback should be too.
 */
fun Modifier.roundClickable(onClick: () -> Unit): Modifier = this.clip(CircleShape).clickable(onClick = onClick)
