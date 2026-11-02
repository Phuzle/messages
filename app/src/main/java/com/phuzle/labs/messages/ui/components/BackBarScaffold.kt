package com.phuzle.labs.messages.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phuzle.labs.messages.ui.theme.IconSizes
import com.phuzle.labs.messages.ui.theme.MessagesTheme

/** The back-arrow + title bar shared by every pushed sub-screen (Recycle Bin, Archived, Settings subs, ...).
 * [topBarContent], when non-null, replaces the default back+title row entirely — used to swap in a
 * multi-select bar (see SelectionTopBar) without every caller reimplementing this scaffold. */
@Composable
fun BackBarScaffold(
    title: String,
    onBack: () -> Unit,
    topBarContent: (@Composable () -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val tokens = MessagesTheme.tokens
    Box(Modifier.fillMaxSize()) {
        content()
        GlassBar(modifier = Modifier.align(Alignment.TopCenter), height = 64.dp, inset = BarInset.Top) {
            if (topBarContent != null) {
                topBarContent()
            } else {
                Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(48.dp).roundClickable(onClick = onBack), contentAlignment = Alignment.Center) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = tokens.textPrimary, modifier = Modifier.size(IconSizes.Medium))
                    }
                    Text(title, color = tokens.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}
