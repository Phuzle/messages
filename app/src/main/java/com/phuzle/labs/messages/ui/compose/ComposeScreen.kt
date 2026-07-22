package com.phuzle.labs.messages.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phuzle.labs.messages.ui.AppViewModel
import com.phuzle.labs.messages.ui.components.BarInset
import com.phuzle.labs.messages.ui.components.FlatTextField
import com.phuzle.labs.messages.ui.components.GlassBar
import com.phuzle.labs.messages.ui.components.PillButton
import com.phuzle.labs.messages.ui.components.topBarContentPadding
import com.phuzle.labs.messages.ui.model.AppUiState
import com.phuzle.labs.messages.ui.theme.MessagesTheme
import com.phuzle.labs.messages.ui.theme.ShapeMedium

@Composable
fun ComposeScreen(state: AppUiState, viewModel: AppViewModel) {
    val tokens = MessagesTheme.tokens

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().navigationBarsPadding()
                .padding(top = topBarContentPadding(70.dp), start = 16.dp, end = 16.dp, bottom = 16.dp),
        ) {
            FlatTextField(
                value = state.composeTo,
                onValueChange = viewModel::onComposeToChange,
                placeholder = "To: name or number",
                fontSize = 15.sp,
                modifier = Modifier.padding(bottom = 10.dp),
            )
            com.phuzle.labs.messages.ui.components.SettingsRowDivider()
            Spacer(Modifier.height(12.dp))
            FlatTextField(
                value = state.composeBody,
                onValueChange = viewModel::onComposeBodyChange,
                placeholder = "Type a message",
                fontSize = 15.sp,
                singleLine = false,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )
            if (state.settings.showCharCount) {
                Text(
                    "${state.composeBody.length} characters", color = tokens.textTertiary, fontSize = 11.sp,
                    modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.End,
                )
            }
            Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                state.scheduleOptions.forEach { option ->
                    PillButton(label = option.label, active = option.active, onClick = { viewModel.setComposeSchedule(option.key) })
                }
            }
            Text(
                if (state.composeScheduleKey != null) "Schedule" else "Send",
                color = tokens.accentText,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = 12.dp)
                    .background(tokens.accent, ShapeMedium)
                    .clickable(onClick = viewModel::sendCompose)
                    .padding(horizontal = 22.dp, vertical = 10.dp),
            )
        }

        GlassBar(modifier = Modifier.align(Alignment.TopCenter), height = 56.dp, inset = BarInset.Top) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("New message", color = tokens.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Icon(
                    Icons.Filled.Close, contentDescription = "Close", tint = tokens.textPrimary,
                    modifier = Modifier.size(22.dp).clickable(onClick = viewModel::closeCompose),
                )
            }
        }
    }
}
