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
import androidx.compose.material.icons.filled.ShowChart
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
import androidx.compose.ui.graphics.lerp
import coil.compose.AsyncImage
import com.phuzle.labs.messages.ui.theme.MessagesTheme
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
    size: Dp = 52.dp,
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
        modifier = modifier.size(size).background(mutedAvatarColor(color, MessagesTheme.isDark), shape),
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

/** Same category glyph AvatarBubble uses, reused wherever else a category needs a quick visual
 * (e.g. NotificationsSettingsScreen's channel list). */
fun iconForCategory(category: Category): ImageVector = when (category) {
    Category.Otp -> Icons.Filled.Lock
    Category.Transactions -> Icons.Filled.AccountBalanceWallet
    Category.Ipo -> Icons.Filled.ShowChart
    Category.Promotions -> Icons.Filled.Sell
    Category.Others -> Icons.Filled.Business
    Category.Personal, Category.Unknown, Category.All -> Icons.Filled.Person
}

/**
 * The per-sender palette is saturated; this pulls it toward a slate midtone at render time so a long
 * inbox of avatars reads calm. The palette itself (and its deterministic per-sender hash) is untouched.
 */
private fun mutedAvatarColor(color: Color, dark: Boolean): Color =
    lerp(color, if (dark) Color(0xFF4A516B) else Color(0xFF6F7794), 0.42f)
