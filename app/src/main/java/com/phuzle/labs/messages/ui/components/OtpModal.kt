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
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phuzle.labs.messages.ui.model.OtpModalUi
import com.phuzle.labs.messages.ui.theme.JetBrainsMonoFontFamily
import com.phuzle.labs.messages.ui.theme.MessagesTheme
import com.phuzle.labs.messages.ui.theme.ShapeLarge
import com.phuzle.labs.messages.ui.theme.ShapeMedium
import com.phuzle.labs.messages.ui.theme.ShapePill
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
                .widthIn(max = 296.dp)
                .background(tokens.modalBg, ShapeLarge)
                .padding(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 20.dp),
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
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 4.sp,
                    fontFamily = JetBrainsMonoFontFamily,
                    modifier = Modifier.padding(top = 16.dp, bottom = 20.dp),
                )
            }
            Button(
                onClick = onCopy,
                colors = ButtonDefaults.buttonColors(containerColor = tokens.modalAccent, contentColor = tokens.onModalAccent),
                shape = ShapeMedium,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            ) {
                Text(if (otp.copied) "Copied ✓" else "Copy to Clipboard", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("Dismiss", color = tokens.modalText.copy(alpha = 0.6f), fontSize = 14.sp)
            }
            LinearProgressIndicator(
                progress = { (remainingMs.toFloat() / HOT_SWAP_WINDOW_MS).coerceIn(0f, 1f) },
                color = tokens.modalAccent,
                trackColor = tokens.modalText.copy(alpha = 0.12f),
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp).height(6.dp).clip(ShapePill),
            )
            Text(
                "Auto-dismisses in ${(remainingMs / 1000).toInt() + 1}s",
                color = tokens.modalText.copy(alpha = 0.45f),
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}
