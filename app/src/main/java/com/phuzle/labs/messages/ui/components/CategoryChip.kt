package com.phuzle.labs.messages.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phuzle.labs.messages.ui.model.CategoryChipUi
import com.phuzle.labs.messages.ui.theme.MessagesTheme
import com.phuzle.labs.messages.ui.theme.ShapePill
import com.phuzle.labs.messages.ui.theme.categoryChipColors

/** One UI-style full-pill toggle chips; the active one gets a checkmark alongside the bg change. */
@Composable
fun CategoryChip(chip: CategoryChipUi, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = categoryChipColors(chip.active, MessagesTheme.tokens)
    Row(
        modifier = modifier
            .clip(ShapePill)
            .background(colors.background, ShapePill)
            .border(1.dp, colors.border, ShapePill)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (chip.active) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = colors.content, modifier = Modifier.size(14.dp).padding(end = 3.dp))
        }
        Text(text = chip.label, color = colors.content, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
    }
}
