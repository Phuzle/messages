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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phuzle.labs.messages.ui.theme.MessagesTheme

data class DrawerItem(val label: String, val icon: DrawerIconType, val onClick: () -> Unit)

@Composable
fun NavDrawer(
    visible: Boolean,
    onDismiss: () -> Unit,
    items: List<DrawerItem>,
    secondaryItems: List<DrawerItem>,
    modifier: Modifier = Modifier,
) {
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
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 24.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 20.dp)) {
                AppLogo()
                Text(
                    "Messages",
                    color = tokens.textPrimary,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }

            items.forEach { item -> DrawerRow(item, tokens.textPrimary) }

            SettingsRowDivider(Modifier.padding(vertical = 12.dp))

            secondaryItems.forEach { item -> DrawerRow(item, tokens.textSecondary) }
        }
    }
}

@Composable
private fun DrawerRow(item: DrawerItem, contentColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = item.onClick)
            .padding(vertical = 12.dp, horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DrawerIcon(item.icon, contentColor)
        Text(
            item.label,
            color = contentColor,
            fontSize = 14.5.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 14.dp),
        )
    }
}
