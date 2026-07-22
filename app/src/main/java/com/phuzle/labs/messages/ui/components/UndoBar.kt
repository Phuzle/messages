package com.phuzle.labs.messages.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phuzle.labs.messages.ui.theme.MessagesTheme
import com.phuzle.labs.messages.ui.theme.ShapeMedium

/** The undo affordance for reversible destructive actions (delete/archive/send) — auto-dismisses
 * on the ViewModel side after a few seconds; this is purely the presentation. */
@Composable
fun UndoBar(message: String?, onUndo: () -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    if (message == null) return
    val tokens = MessagesTheme.tokens
    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .background(tokens.modalBg, ShapeMedium)
            .clickable(onClick = onDismiss)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(message, color = tokens.modalText, fontSize = 13.5.sp, modifier = Modifier.weight(1f))
        Text(
            "UNDO",
            color = tokens.accent,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable(onClick = onUndo).padding(start = 12.dp),
        )
    }
}
