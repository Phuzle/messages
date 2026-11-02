package com.phuzle.labs.messages.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import com.phuzle.labs.messages.ui.theme.IconSizes
import com.phuzle.labs.messages.ui.theme.MessagesTheme
import com.phuzle.labs.messages.ui.theme.ShapePill
import com.phuzle.labs.messages.ui.theme.categoryChipColors

/** One UI-style full-pill toggle chips; the active one gets a checkmark alongside the bg change. */
@Composable
fun CategoryChip(chip: CategoryChipUi, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = categoryChipColors(chip.active, MessagesTheme.tokens)
    // 40dp visual pill inside a 48dp tap target: the clickable wraps the extra 4dp above and below.
    Box(modifier = modifier.heightIn(min = 48.dp).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
    Row(
        modifier = Modifier
            .height(40.dp)
            .clip(ShapePill)
            .background(colors.background, ShapePill)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (chip.active) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = colors.content, modifier = Modifier.size(IconSizes.Small).padding(end = 6.dp))
        }
        Text(text = chip.label, color = colors.content, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
    }
    }
}
