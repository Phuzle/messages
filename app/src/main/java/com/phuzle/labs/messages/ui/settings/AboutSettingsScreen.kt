package com.phuzle.labs.messages.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phuzle.labs.messages.BuildConfig
import com.phuzle.labs.messages.ui.AppViewModel
import com.phuzle.labs.messages.ui.components.SectionLabel
import com.phuzle.labs.messages.ui.components.SettingsCard
import com.phuzle.labs.messages.ui.components.SettingsRowDivider
import com.phuzle.labs.messages.ui.components.topBarContentPadding
import com.phuzle.labs.messages.ui.model.AppUiState
import com.phuzle.labs.messages.ui.theme.MessagesTheme

private const val SUPPORT_EMAIL = "support@phuzle.com"
private const val PRIVACY_POLICY_URL = "https://docs.phuzle.com/messages/privacy"
private const val TERMS_URL = "https://docs.phuzle.com/messages/terms"

@Composable
fun AboutSettingsScreen(state: AppUiState, viewModel: AppViewModel) {
    val tokens = MessagesTheme.tokens
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(top = topBarContentPadding(68.dp), start = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        Column(
            Modifier.fillMaxWidth().background(tokens.surface, com.phuzle.labs.messages.ui.theme.ShapeMedium)
                .border(1.dp, tokens.border, com.phuzle.labs.messages.ui.theme.ShapeMedium)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            com.phuzle.labs.messages.ui.components.AppLogo(size = 56.dp, cornerRadius = 14.dp)
            Text("Messages", color = tokens.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp))
            Text("Version ${BuildConfig.VERSION_NAME}", color = tokens.textTertiary, fontSize = 12.5.sp, modifier = Modifier.padding(top = 2.dp))
            Text(
                "A smart SMS client that automatically sorts messages, surfaces one-time codes instantly, and keeps a running passbook of your accounts.",
                color = tokens.textSecondary,
                fontSize = 13.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp),
            )
        }

        Column {
            SectionLabel("Contact", Modifier.padding(bottom = 8.dp))
            SettingsCard {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$SUPPORT_EMAIL"))
                            context.startActivity(Intent.createChooser(intent, "Email support"))
                        }
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text("Email support", color = tokens.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text(SUPPORT_EMAIL, color = tokens.textTertiary, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                    }
                    com.phuzle.labs.messages.ui.components.ChevronIcon()
                }
                SettingsRowDivider()
                Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Package", color = tokens.textSecondary, fontSize = 13.sp)
                    Text(BuildConfig.APPLICATION_ID, color = tokens.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Column {
            SectionLabel("Legal", Modifier.padding(bottom = 8.dp))
            SettingsCard {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { com.phuzle.labs.messages.core.util.openUrl(context, state.settings.inAppBrowser, PRIVACY_POLICY_URL) }
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Privacy Policy", color = tokens.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    com.phuzle.labs.messages.ui.components.ChevronIcon()
                }
                SettingsRowDivider()
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { com.phuzle.labs.messages.core.util.openUrl(context, state.settings.inAppBrowser, TERMS_URL) }
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Terms of Service", color = tokens.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    com.phuzle.labs.messages.ui.components.ChevronIcon()
                }
            }
        }

        Column {
            SectionLabel("Made by", Modifier.padding(bottom = 8.dp))
            Column(
                Modifier.fillMaxWidth().background(tokens.surface, com.phuzle.labs.messages.ui.theme.ShapeMedium)
                    .border(1.dp, tokens.border, com.phuzle.labs.messages.ui.theme.ShapeMedium)
                    .padding(14.dp),
            ) {
                Text("Made with care by the Phuzle team.", color = tokens.textPrimary, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    "Thanks for trying Messages — feedback is always welcome.",
                    color = tokens.textTertiary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}
