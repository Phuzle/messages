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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phuzle.labs.messages.ui.model.OtpModalUi
import com.phuzle.labs.messages.ui.theme.JetBrainsMonoFontFamily
import com.phuzle.labs.messages.ui.theme.MessagesTheme
import com.phuzle.labs.messages.ui.theme.ShapeMedium
import kotlinx.coroutines.delay

private const val HOT_SWAP_WINDOW_MS = 30_000L

/** The 30-second OTP hot-swap modal: pops up when the app resumes to a fresh OTP. The countdown
 * shown here is purely cosmetic — AppViewModel.checkOtpHotSwap is what actually clears otpModal
 * once the window expires, this just mirrors that deadline so the overlay doesn't sit there with
 * no indication it's about to (or already did) time out. */
@Composable
fun OtpModal(otp: OtpModalUi?, onCopy: () -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    if (otp == null) return
    val tokens = MessagesTheme.tokens

    var remainingMs by remember(otp.expiresAtMillis) {
        mutableLongStateOf((otp.expiresAtMillis - System.currentTimeMillis()).coerceAtLeast(0))
    }
    LaunchedEffect(otp.expiresAtMillis) {
        while (remainingMs > 0) {
            delay(250)
            remainingMs = (otp.expiresAtMillis - System.currentTimeMillis()).coerceAtLeast(0)
        }
    }

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
            LinearProgressIndicator(
                progress = { (remainingMs.toFloat() / HOT_SWAP_WINDOW_MS).coerceIn(0f, 1f) },
                color = tokens.accent,
                trackColor = tokens.modalText.copy(alpha = 0.12f),
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            )
            Text(
                "Auto-dismisses in ${(remainingMs / 1000).toInt() + 1}s",
                color = tokens.modalText.copy(alpha = 0.45f),
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}
