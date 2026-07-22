package com.phuzle.labs.messages.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phuzle.labs.messages.ui.model.OtpModalUi
import com.phuzle.labs.messages.ui.theme.JetBrainsMonoFontFamily
import com.phuzle.labs.messages.ui.theme.MessagesTheme
import com.phuzle.labs.messages.ui.theme.ShapeMedium

/** The 30-second OTP hot-swap modal: pops up when the app resumes to a fresh OTP. */
@Composable
fun OtpModal(otp: OtpModalUi?, onCopy: () -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    if (otp == null) return
    val tokens = MessagesTheme.tokens
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(tokens.overlayBg)
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .background(tokens.modalBg, ShapeMedium)
                .padding(horizontal = 22.dp, vertical = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                otp.senderLabel,
                color = tokens.modalText.copy(alpha = 0.6f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.6.sp,
            )
            SelectionContainer {
                Text(
                    otp.code,
                    color = tokens.modalText,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 4.sp,
                    fontFamily = JetBrainsMonoFontFamily,
                    modifier = Modifier.padding(vertical = 16.dp),
                )
            }
            Button(
                onClick = onCopy,
                colors = ButtonDefaults.buttonColors(containerColor = tokens.accent, contentColor = tokens.accentText),
                shape = ShapeMedium,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (otp.copied) "Copied ✓" else "Copy to Clipboard", fontWeight = FontWeight.Bold, fontSize = 14.5.sp)
            }
            TextButton(onClick = onDismiss) {
                Text("Dismiss", color = tokens.modalText.copy(alpha = 0.55f), fontSize = 13.sp)
            }
        }
    }
}
