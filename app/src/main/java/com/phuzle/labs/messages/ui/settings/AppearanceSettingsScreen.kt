package com.phuzle.labs.messages.ui.settings
import com.phuzle.labs.messages.ui.components.topBarContentPadding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phuzle.labs.messages.ui.AppViewModel
import com.phuzle.labs.messages.ui.components.PillButton
import com.phuzle.labs.messages.ui.components.SectionLabel
import com.phuzle.labs.messages.ui.components.SettingsCard
import com.phuzle.labs.messages.ui.model.AppUiState
import com.phuzle.labs.messages.ui.theme.ACCENT_OPTIONS
import com.phuzle.labs.messages.ui.theme.MessagesTheme
import com.phuzle.labs.messages.ui.theme.ThemeMode

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppearanceSettingsScreen(state: AppUiState, viewModel: AppViewModel) {
    val tokens = MessagesTheme.tokens
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = topBarContentPadding(68.dp), start = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        Column {
            SectionLabel("Preview", Modifier.padding(bottom = 8.dp))
            ThemePreviewCard()
        }
        Column {
            SectionLabel("Theme", Modifier.padding(bottom = 8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeMode.entries.forEach { mode ->
                    PillButton(label = mode.label, active = state.themeMode == mode, onClick = { viewModel.setThemeMode(mode.key) })
                }
            }
        }
        Column {
            SectionLabel("Accent color", Modifier.padding(bottom = 8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ACCENT_OPTIONS.forEach { option ->
                    val swatch = if (MessagesTheme.isDark) option.dark else option.light
                    val ring = if (state.settings.accentHex == option.hex) tokens.textPrimary else Color.Transparent
                    Box(
                        Modifier
                            .size(38.dp)
                            .background(swatch, CircleShape)
                            .border(3.dp, ring, CircleShape)
                            .clickable { viewModel.setAccent(option.hex) },
                    )
                }
            }
        }
    }
}

/** A live mock-up of what the picked theme/accent actually looks like on real content — a
 * message exchange and an inbox row, the two places color choices show up most — so switching
 * options here shows the result immediately, in place, instead of needing to back out to the
 * dashboard/a thread to see what changed. Built entirely from [MessagesTheme.tokens], so it
 * re-renders live the instant a theme/accent tap changes them, same as the rest of the app. */
@Composable
private fun ThemePreviewCard() {
    val tokens = MessagesTheme.tokens
    SettingsCard(modifier = Modifier.padding(14.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                Column(
                    Modifier.widthIn(max = 220.dp).background(tokens.surfaceAlt, RoundedCornerShape(16.dp)).padding(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    Text("Hey, how's it going?", color = tokens.textPrimary, fontSize = 14.sp, lineHeight = 19.sp)
                    Text("9:41 AM", color = tokens.textPrimary.copy(alpha = 0.65f), fontSize = 10.sp, modifier = Modifier.align(Alignment.End).padding(top = 4.dp))
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Column(
                    Modifier.widthIn(max = 220.dp).background(tokens.accent, RoundedCornerShape(16.dp)).padding(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    Text("Looking good, thanks!", color = tokens.accentText, fontSize = 14.sp, lineHeight = 19.sp)
                    Text("9:42 AM", color = tokens.accentText.copy(alpha = 0.65f), fontSize = 10.sp, modifier = Modifier.align(Alignment.End).padding(top = 4.dp))
                }
            }
        }
        com.phuzle.labs.messages.ui.components.SettingsRowDivider()
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).background(tokens.accentSoft, CircleShape))
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text("Jordan Reyes", color = tokens.textPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text("You: Looking good, thanks!", color = tokens.textSecondary, fontSize = 13.sp, maxLines = 1)
            }
            Box(Modifier.size(18.dp).background(tokens.accent, CircleShape))
        }
    }
}
