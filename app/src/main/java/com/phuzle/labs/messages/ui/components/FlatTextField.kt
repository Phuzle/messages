package com.phuzle.labs.messages.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phuzle.labs.messages.ui.theme.MessagesTheme
import com.phuzle.labs.messages.ui.theme.ShapeSmall

/**
 * The design's flat pill/box inputs (search bar, reply bar, compose fields, signature) — plain
 * [BasicTextField] styled by hand since Material3's TextField brings its own underline/label
 * chrome that doesn't match this system.
 */
@Composable
fun FlatTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    filled: Boolean = false,
    fontSize: androidx.compose.ui.unit.TextUnit = 14.sp,
    singleLine: Boolean = true,
) {
    val tokens = MessagesTheme.tokens
    val textStyle = TextStyle(color = tokens.textPrimary, fontSize = fontSize)
    val background = if (filled) Modifier.background(tokens.inputBg, ShapeSmall).padding(horizontal = 12.dp, vertical = 10.dp) else Modifier
    Box(modifier = modifier.then(background)) {
        if (value.isEmpty()) {
            Text(placeholder, color = tokens.textTertiary, fontSize = fontSize)
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = textStyle,
            singleLine = singleLine,
            cursorBrush = SolidColor(tokens.accent),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
