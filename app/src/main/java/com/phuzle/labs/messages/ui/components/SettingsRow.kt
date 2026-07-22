package com.phuzle.labs.messages.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phuzle.labs.messages.ui.theme.MessagesTheme
import com.phuzle.labs.messages.ui.theme.ShapeMedium

/** The chevron used on every navigable settings/list row. */
@Composable
fun ChevronIcon(modifier: Modifier = Modifier) {
    Icon(
        Icons.AutoMirrored.Filled.KeyboardArrowRight,
        contentDescription = null,
        tint = MessagesTheme.tokens.textTertiary,
        modifier = modifier.size(20.dp),
    )
}

/** A bordered flat card wrapping a column of rows — the design's zero-elevation settings groups. */
@Composable
fun SettingsCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val tokens = MessagesTheme.tokens
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(tokens.surface, ShapeMedium)
            .border(1.dp, tokens.border, ShapeMedium),
        content = content,
    )
}

@Composable
fun SettingsRowDivider(modifier: Modifier = Modifier) {
    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MessagesTheme.tokens.border),
    )
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        color = MessagesTheme.tokens.textTertiary,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.5.sp,
        modifier = modifier,
    )
}

@Composable
fun SettingsToggleRow(
    title: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = MessagesTheme.tokens.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            if (subtitle != null) {
                Text(subtitle, color = MessagesTheme.tokens.textTertiary, fontSize = 12.sp)
            }
        }
        LabeledSwitch(checked = checked, onCheckedChange = onToggle)
    }
}

@Composable
fun SettingsNavRow(
    title: String,
    hint: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leading: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading()
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            Text(title, color = MessagesTheme.tokens.textPrimary, fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold)
            Text(hint, color = MessagesTheme.tokens.textTertiary, fontSize = 12.sp)
        }
        ChevronIcon()
    }
}
