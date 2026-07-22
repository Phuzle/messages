package com.phuzle.labs.messages.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phuzle.labs.messages.ui.model.CategoryChipUi
import com.phuzle.labs.messages.ui.theme.MessagesTheme
import com.phuzle.labs.messages.ui.theme.ShapeSmall
import com.phuzle.labs.messages.ui.theme.categoryChipColors

/** The prototype's shadcn-style flat toggle chips — no Material `TabRow` indicators. */
@Composable
fun CategoryChip(chip: CategoryChipUi, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = categoryChipColors(chip.active, MessagesTheme.tokens)
    Text(
        text = chip.label,
        color = colors.content,
        fontSize = 12.5.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier
            .background(colors.background, ShapeSmall)
            .border(1.dp, colors.border, ShapeSmall)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
    )
}
