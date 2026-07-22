package com.phuzle.labs.messages.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phuzle.labs.messages.ui.theme.MessagesTheme
import com.phuzle.labs.messages.ui.theme.ShapeSmall

/**
 * The design's flat pill/box inputs (search bar, reply bar, compose fields, signature) — plain
 * [BasicTextField] styled by hand since Material3's TextField brings its own underline/label
 * chrome that doesn't match this system. When [singleLine] is false this is a real growing
 * textarea (Enter inserts a newline, never submits) capped at [maxHeight] with internal scroll.
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
    maxHeight: androidx.compose.ui.unit.Dp = 120.dp,
) {
    val tokens = MessagesTheme.tokens
    val textStyle = TextStyle(color = tokens.textPrimary, fontSize = fontSize)
    val background = if (filled) Modifier.background(tokens.inputBg, ShapeSmall).padding(horizontal = 12.dp, vertical = 10.dp) else Modifier
    val fieldModifier = if (singleLine) Modifier.fillMaxWidth() else Modifier.fillMaxWidth().heightIn(max = maxHeight).verticalScroll(rememberScrollState())
    Box(modifier = modifier.then(background)) {
        if (value.isEmpty()) {
            Text(placeholder, color = tokens.textTertiary, fontSize = fontSize)
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = textStyle,
            singleLine = singleLine,
            keyboardOptions = KeyboardOptions(imeAction = if (singleLine) ImeAction.Default else ImeAction.None),
            cursorBrush = SolidColor(tokens.accent),
            modifier = fieldModifier,
        )
    }
}
