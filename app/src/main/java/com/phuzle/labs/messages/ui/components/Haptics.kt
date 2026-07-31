package com.phuzle.labs.messages.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/** Wraps [action] so every long-press across the app gives the same short vibration a long-press
 * ought to feel like — `combinedClickable`'s `onLongClick` doesn't do this on its own, unlike a
 * plain button press, which Android already haptics through its ripple/indication. */
@Composable
fun withLongPressHaptic(action: () -> Unit): () -> Unit {
    val haptic = LocalHapticFeedback.current
    return { haptic.performHapticFeedback(HapticFeedbackType.LongPress); action() }
}
