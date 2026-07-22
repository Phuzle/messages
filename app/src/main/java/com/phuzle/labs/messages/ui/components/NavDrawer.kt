package com.phuzle.labs.messages.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phuzle.labs.messages.ui.theme.MessagesTheme

data class DrawerItem(val label: String, val onClick: () -> Unit)

@Composable
fun NavDrawer(visible: Boolean, onDismiss: () -> Unit, items: List<DrawerItem>, modifier: Modifier = Modifier) {
    val tokens = MessagesTheme.tokens
    AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut(), modifier = modifier) {
        Box(Modifier.fillMaxSize().background(tokens.overlayBg).clickable(onClick = onDismiss))
    }
    AnimatedVisibility(
        visible = visible,
        enter = slideInHorizontally(initialOffsetX = { -it }),
        exit = slideOutHorizontally(targetOffsetX = { -it }),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = 290.dp)
                .background(tokens.surface)
                .padding(horizontal = 16.dp, vertical = 24.dp),
        ) {
            Text("Messages", color = tokens.textPrimary, fontSize = 19.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
            items.forEach { item ->
                Text(
                    item.label,
                    color = tokens.textPrimary,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                        .clickable(onClick = item.onClick)
                        .padding(vertical = 12.dp, horizontal = 10.dp),
                )
            }
        }
    }
}
