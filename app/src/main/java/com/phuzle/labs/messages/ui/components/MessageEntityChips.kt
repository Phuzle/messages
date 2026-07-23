package com.phuzle.labs.messages.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Password
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phuzle.labs.messages.domain.text.DetectedEntity

/**
 * One row of small action chips per detected phone/URL/email/code in a message body — a primary
 * action (Call/Open/Email) plus a Copy button for everything except a code, which is Copy-only
 * since "opening" a verification code doesn't mean anything.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MessageEntityChips(
    entities: List<DetectedEntity>,
    contentColor: Color,
    onOpenUrl: (String) -> Unit,
    onCall: (String) -> Unit,
    onEmail: (String) -> Unit,
    onCopy: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (entities.isEmpty()) return
    FlowRow(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        entities.forEach { entity ->
            when (entity.type) {
                DetectedEntity.Type.Url -> {
                    EntityChip("Open", Icons.AutoMirrored.Filled.OpenInNew, contentColor, onClick = { onOpenUrl(entity.value) })
                    CopyIconChip(contentColor, onClick = { onCopy(entity.value) })
                }
                DetectedEntity.Type.Email -> {
                    EntityChip("Email", Icons.Filled.Email, contentColor, onClick = { onEmail(entity.value) })
                    CopyIconChip(contentColor, onClick = { onCopy(entity.value) })
                }
                DetectedEntity.Type.Phone -> {
                    EntityChip("Call", Icons.Filled.Call, contentColor, onClick = { onCall(entity.value) })
                    CopyIconChip(contentColor, onClick = { onCopy(entity.value) })
                }
                DetectedEntity.Type.Code -> {
                    EntityChip("Copy code", Icons.Filled.Password, contentColor, onClick = { onCopy(entity.value) })
                }
            }
        }
    }
}

@Composable
private fun EntityChip(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, contentColor: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .background(contentColor.copy(alpha = 0.14f), RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Icon(icon, contentDescription = label, tint = contentColor, modifier = Modifier.size(13.dp))
        Text(label, color = contentColor, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun CopyIconChip(contentColor: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .background(contentColor.copy(alpha = 0.14f), RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.ContentCopy, contentDescription = "Copy", tint = contentColor, modifier = Modifier.size(13.dp))
    }
}
