package com.phuzle.labs.messages.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.phuzle.labs.messages.ui.theme.MessagesTheme

/** A pulsing placeholder bubble shown while [com.phuzle.labs.messages.ui.AppViewModel.loadOlderMessages] fetches the next page. */
@Composable
fun MessageSkeletonRow(alignEnd: Boolean, modifier: Modifier = Modifier) {
    val tokens = MessagesTheme.tokens
    val transition = rememberInfiniteTransition(label = "message_skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(animation = tween(700), repeatMode = RepeatMode.Reverse),
        label = "message_skeleton_alpha",
    )
    Row(modifier.fillMaxWidth(), horizontalArrangement = if (alignEnd) Arrangement.End else Arrangement.Start) {
        androidx.compose.foundation.layout.Box(
            Modifier
                .width(if (alignEnd) 140.dp else 190.dp)
                .height(38.dp)
                .background(tokens.surfaceAlt.copy(alpha = alpha), RoundedCornerShape(16.dp)),
        )
    }
}
