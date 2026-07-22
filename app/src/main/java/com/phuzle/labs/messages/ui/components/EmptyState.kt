package com.phuzle.labs.messages.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phuzle.labs.messages.ui.theme.MessagesTheme
import com.phuzle.labs.messages.ui.theme.ShapePill

/** The one empty-state layout used everywhere a list can legitimately have nothing in it. */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    detail: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val tokens = MessagesTheme.tokens
    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier.size(64.dp).background(tokens.surfaceAlt, CircleShape),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, contentDescription = null, tint = tokens.textTertiary, modifier = Modifier.size(28.dp)) }
        Text(title, color = tokens.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 14.dp))
        Text(detail, color = tokens.textTertiary, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
        if (actionLabel != null && onAction != null) {
            Text(
                actionLabel,
                color = tokens.accentText,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .padding(top = 18.dp)
                    .clip(ShapePill)
                    .background(tokens.accent, ShapePill)
                    .clickable(onClick = onAction)
                    .padding(horizontal = 20.dp, vertical = 10.dp),
            )
        }
    }
}
