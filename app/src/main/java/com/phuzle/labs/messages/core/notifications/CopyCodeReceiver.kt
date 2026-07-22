package com.phuzle.labs.messages.core.notifications

import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.core.content.getSystemService

/** The OTP notification's "Copy Code" action target — writes straight to the clipboard, no app launch. */
class CopyCodeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val code = intent.getStringExtra(EXTRA_CODE) ?: return
        val clipboard = context.getSystemService<ClipboardManager>() ?: return
        clipboard.setPrimaryClip(ClipData.newPlainText("OTP code", code))
    }

    companion object {
        const val EXTRA_CODE = "extra_code"
    }
}
