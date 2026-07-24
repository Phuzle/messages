package com.phuzle.labs.messages.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.phuzle.labs.messages.domain.model.Category

/** Business senders get a rounded-square avatar, personal contacts a circle — per the prototype.
 * When [photoUri] resolves to a saved contact's photo, it's shown instead of the icon-on-color
 * placeholder. Without a photo, the placeholder is a filled icon (not initials — initials read as
 * meaningless for a bare phone number, which is most senders here) that varies by [category], so
 * an OTP sender and a promo sender are visually distinguishable at a glance even before you read
 * the preview text. [color] is expected to already be a deterministic per-sender hash (see
 * AvatarPalette.forSeed), not chosen here. */
@Composable
fun AvatarBubble(
    category: Category,
    color: Color,
    isBusiness: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
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
        Icon(
            imageVector = iconForCategory(category),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(size * 0.5f),
        )
    }
}

private fun iconForCategory(category: Category): ImageVector = when (category) {
    Category.Otp -> Icons.Filled.Lock
    Category.Transactions -> Icons.Filled.AccountBalanceWallet
    Category.Promotions -> Icons.Filled.Sell
    Category.Others -> Icons.Filled.Business
    Category.Personal, Category.Unknown, Category.All -> Icons.Filled.Person
}
