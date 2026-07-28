package com.phuzle.labs.messages.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phuzle.labs.messages.ui.theme.MessagesTheme
import com.phuzle.labs.messages.ui.theme.ShapeSmall

/**
 * The design's flat pill/box inputs (search bar, reply bar, compose fields, signature) — plain
 * [BasicTextField] styled by hand since Material3's TextField brings its own underline/label
 * chrome that doesn't match this system. When [singleLine] is false this is a real growing
 * textarea (Enter inserts a newline, never submits), either capped at [maxHeight] with internal
 * scroll, or — when [fillHeight] is set — filling whatever height [modifier] gives the outer Box
 * instead (see ComposeScreen's message body, where the old fixed cap left a large dead-tap-zone
 * below a short capped field even though the surrounding screen had much more room).
 * [scrollState] lets a caller that needs [fillHeight] hoist the scroll position out (e.g. to draw
 * its own scrollbar against it); omitted, one is created internally as before.
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
    focusRequester: FocusRequester? = null,
    fillHeight: Boolean = false,
    scrollState: ScrollState? = null,
) {
    val tokens = MessagesTheme.tokens
    val textStyle = TextStyle(color = tokens.textPrimary, fontSize = fontSize)
    val background = if (filled) Modifier.background(tokens.inputBg, ShapeSmall).padding(horizontal = 12.dp, vertical = 10.dp) else Modifier
    val resolvedScrollState = scrollState ?: rememberScrollState()
    var fieldModifier = when {
        singleLine -> Modifier.fillMaxWidth()
        fillHeight -> Modifier.fillMaxSize().verticalScroll(resolvedScrollState)
        else -> Modifier.fillMaxWidth().heightIn(max = maxHeight).verticalScroll(resolvedScrollState)
    }
    if (focusRequester != null) fieldModifier = fieldModifier.focusRequester(focusRequester)

    // Cursor position is tracked here, locally, rather than driven straight off [value] — every
    // caller hoists [value] through a StateFlow (dashboard search in particular recombines
    // against a DB query on every keystroke), and that round trip lands back on this composable
    // with real latency. The plain-String BasicTextField overload can't tell "this is my own
    // keystroke echoed back" apart from "an external value change" once that happens, and resets
    // the cursor to the start. Keeping selection in local state that's only overwritten when the
    // text itself actually changes for a reason other than our own typing (a draft loading, the
    // search field being cleared by its X button, an emoji picker insert, ...) fixes that for
    // every field that goes through here — search, compose, reply bar, signature.
    var fieldValue by remember { mutableStateOf(TextFieldValue(value)) }
    if (fieldValue.text != value) {
        fieldValue = TextFieldValue(value, selection = TextRange(value.length))
    }

    Box(modifier = modifier.then(background)) {
        if (value.isEmpty()) {
            Text(placeholder, color = tokens.textTertiary, fontSize = fontSize)
        }
        BasicTextField(
            value = fieldValue,
            onValueChange = { new ->
                fieldValue = new
                if (new.text != value) onValueChange(new.text)
            },
            textStyle = textStyle,
            singleLine = singleLine,
            keyboardOptions = KeyboardOptions(imeAction = if (singleLine) ImeAction.Default else ImeAction.None),
            cursorBrush = SolidColor(tokens.accent),
            modifier = fieldModifier,
        )
    }
}
