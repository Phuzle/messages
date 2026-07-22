package com.phuzle.labs.messages.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

/** Business senders get a rounded-square avatar, personal contacts a circle — per the prototype.
 * When [photoUri] resolves to a saved contact's photo, it's shown instead of the initials bubble. */
@Composable
fun AvatarBubble(
    initials: String,
    color: Color,
    isBusiness: Boolean,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 44.dp,
    photoUri: String? = null,
) {
    val shape = if (isBusiness) RoundedCornerShape(10.dp) else RoundedCornerShape(50)
    if (photoUri != null) {
        AsyncImage(
            model = photoUri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier.size(size).clip(shape),
        )
        return
    }
    Box(
        modifier = modifier.size(size).background(color, shape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initials,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            fontSize = (size.value * 0.34f).sp,
        )
    }
}
