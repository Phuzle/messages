package com.phuzle.labs.messages.ui.settings
import com.phuzle.labs.messages.ui.components.topBarContentPadding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.phuzle.labs.messages.ui.AppViewModel
import com.phuzle.labs.messages.ui.components.PillButton
import com.phuzle.labs.messages.ui.components.SectionLabel
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
            SectionLabel("Theme", Modifier.padding(bottom = 8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeMode.entries.forEach { mode ->
                    PillButton(label = mode.label, active = state.themeMode == mode, onClick = { viewModel.setThemeMode(mode.key) })
                }
            }
        }
        Column {
            SectionLabel("Accent color", Modifier.padding(bottom = 8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ACCENT_OPTIONS.forEach { option ->
                    val swatch = if (MessagesTheme.isDark) option.dark else option.light
                    val ring = if (state.settings.accentHex == option.hex) tokens.textPrimary else Color.Transparent
                    androidx.compose.foundation.layout.Box(
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
