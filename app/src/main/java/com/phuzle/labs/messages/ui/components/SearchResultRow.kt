package com.phuzle.labs.messages.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phuzle.labs.messages.ui.model.SearchResultUi
import com.phuzle.labs.messages.ui.theme.MessagesTheme
import com.phuzle.labs.messages.ui.theme.ShapeMedium

/** One matching *message* in search results: who, when, and a one-line excerpt with the match in view. */
@Composable
fun SearchMessageRow(result: SearchResultUi.Message, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val tokens = MessagesTheme.tokens
    val snippet = rememberHighlightedText(result.snippet, result.snippetMatch)
    Row(
        modifier = modifier
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .fillMaxWidth()
            .clip(ShapeMedium)
            .clickable(onClick = onClick)
            .heightIn(min = 72.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AvatarBubble(result.category, result.avatarColor, result.isBusiness, size = 44.dp, photoUri = result.photoUri)
        Column(Modifier.weight(1f).padding(start = 14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    result.displayName, color = tokens.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false),
                )
                Text(result.timeLabel, color = tokens.textTertiary, fontSize = 12.sp, modifier = Modifier.padding(start = 8.dp))
            }
            Text(
                if (result.outgoing) {
                    buildAnnotatedString {
                        pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                        append("You")
                        pop()
                        append(": ")
                        append(snippet)
                    }
                } else {
                    snippet
                },
                color = tokens.textSecondary, fontSize = 14.sp, lineHeight = 20.sp, maxLines = 1,
                overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

/** Small caption above a group of results ("Contacts", "Messages · 42"). */
@Composable
fun SearchSectionHeader(label: String, modifier: Modifier = Modifier) {
    Text(
        label.uppercase(),
        color = MessagesTheme.tokens.textTertiary,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.5.sp,
        modifier = modifier.fillMaxWidth().background(MessagesTheme.tokens.bg).padding(start = 28.dp, end = 16.dp, top = 14.dp, bottom = 6.dp),
    )
}
