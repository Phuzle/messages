package com.phuzle.labs.messages.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phuzle.labs.messages.ui.theme.MessagesTheme
import com.phuzle.labs.messages.ui.theme.ShapeMedium

/** The one full-width filled CTA button — see DESIGN.md's Buttons section. A flat
 * Text().background().clickable() composable on purpose, not Material3's Button: that brings its
 * own elevation/ripple/padding defaults this app's zero-elevation flat system doesn't use. Use
 * this instead of hand-rolling the same background/clickable/padding chain per screen, and instead
 * of Material3 Button for any primary action (sign in, continue, save, send, ...). */
@Composable
fun PrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    busy: Boolean = false,
) {
    val tokens = MessagesTheme.tokens
    if (busy) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .background(tokens.accent, ShapeMedium)
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(color = tokens.accentText, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
        }
        return
    }
    Text(
        label,
        color = tokens.accentText,
        fontSize = 14.5.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        modifier = modifier
            .fillMaxWidth()
            .background(if (enabled) tokens.accent else tokens.surfaceAlt, ShapeMedium)
            .let { if (enabled) it.clickable(onClick = onClick) else it }
            .padding(vertical = 14.dp),
    )
}

/** A plain text tertiary action — "Skip"/"Cancel" under a [PrimaryButton]. No background, no border. */
@Composable
fun SecondaryTextButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val tokens = MessagesTheme.tokens
    Text(
        label,
        color = tokens.textTertiary,
        fontSize = 13.5.sp,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
    )
}
