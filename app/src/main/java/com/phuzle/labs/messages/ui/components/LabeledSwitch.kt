package com.phuzle.labs.messages.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.phuzle.labs.messages.ui.theme.MessagesTheme

/** The design's custom 40x22 toggle track — not the Material3 [androidx.compose.material3.Switch]. */
@Composable
fun LabeledSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val tokens = MessagesTheme.tokens
    val trackColor = if (checked) tokens.accent else tokens.switchTrackOff
    val knobOffset by animateDpAsState(if (checked) 20.dp else 2.dp, label = "switch_knob")

    Box(
        modifier = modifier
            .size(width = 40.dp, height = 22.dp)
            .background(trackColor, RoundedCornerShape(50))
            .clickable { onCheckedChange(!checked) },
        contentAlignment = Alignment.TopStart,
    ) {
        Box(
            modifier = Modifier
                .offset(x = knobOffset, y = 2.dp)
                .size(18.dp)
                .background(Color.White, CircleShape),
        )
    }
}
