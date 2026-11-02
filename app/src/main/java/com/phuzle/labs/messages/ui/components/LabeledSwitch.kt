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

/**
 * The design's custom 52x32 toggle track — not the Material3 [androidx.compose.material3.Switch].
 * The clickable area is 52x48 so the control meets the 48dp touch-target minimum.
 */
@Composable
fun LabeledSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val tokens = MessagesTheme.tokens
    val trackColor = if (checked) tokens.accentDeep else tokens.switchTrackOff
    val knobOffset by animateDpAsState(if (checked) 24.dp else 4.dp, label = "switch_knob")

    Box(
        modifier = modifier
            .size(width = 52.dp, height = 48.dp)
            .clickable { onCheckedChange(!checked) },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(width = 52.dp, height = 32.dp)
                .background(trackColor, RoundedCornerShape(50)),
            contentAlignment = Alignment.TopStart,
        ) {
            Box(
                modifier = Modifier
                    .offset(x = knobOffset, y = 4.dp)
                    .size(24.dp)
                    .background(Color.White, CircleShape),
            )
        }
    }
}
