package com.phuzle.labs.messages.core.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.phuzle.labs.messages.appContainer
import com.phuzle.labs.messages.domain.categorization.TransactionExtractor
import com.phuzle.labs.messages.domain.model.Category
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * The one component that only exists while we hold the default-SMS-app role. Reassembles the
 * (possibly multi-part) message, runs it through Layer-1 classification, persists it, and
 * notifies — the real path described in PRD section 2's "Inbound Interception Lifecycle".
 */
class SmsDeliverReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_DELIVER_ACTION) return
        val parts = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        if (parts.isEmpty()) return

        val sender = parts[0].originatingAddress ?: return
        val body = parts.joinToString(separator = "") { it.messageBody ?: "" }
        val timestamp = parts[0].timestampMillis

        val pendingResult = goAsync()
        val container = context.appContainer
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // The default SMS app must write incoming messages into the system provider
                // itself — SMS_DELIVER is only a notification that a message arrived, not an
                // automatic insert. Without this, every message received while this app holds the
                // default role would live only in this app's private database: invisible to any
                // other app, and gone system-wide on uninstall or a switch back to another SMS
                // app. Best-effort — a provider failure must never block storing/notifying locally.
                runCatching {
                    val values = android.content.ContentValues().apply {
                        put(Telephony.Sms.ADDRESS, sender)
                        put(Telephony.Sms.BODY, body)
                        put(Telephony.Sms.DATE, timestamp)
                        put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_INBOX)
                        put(Telephony.Sms.READ, 0)
                        put(Telephony.Sms.SEEN, 0)
                    }
                    context.contentResolver.insert(Telephony.Sms.CONTENT_URI, values)
                }.onFailure { Log.w("SmsDeliverReceiver", "Couldn't record incoming message in the system SMS provider", it) }

                val contactName = container.contactLookup.displayNameFor(sender)
                val displayName = contactName ?: sender
                val isBusiness = contactName == null
                val photoUri = if (contactName != null) container.contactLookup.photoUriFor(sender) else null
                var category = container.classifier.classify(sender, body)

                val settings = container.settingsRepository.settingsFlow.first()

                // Layer 3: only for what Layer 1 couldn't confidently place, only when the user
                // opted in, and only after PiiScrubber redacts the body (see CloudClassifierClient).
                if (category == Category.Unknown && settings.cloudFallbackEnabled) {
                    container.cloudClassifierClient.classify(settings.serverBaseUrl, body)?.let { category = it }
                }

                val (thread, message) = container.threadRepository.recordIncomingMessage(
                    sender = sender,
                    displayName = displayName,
                    isBusiness = isBusiness,
                    category = category,
                    body = body,
                    timestampMillis = timestamp,
                    photoUri = photoUri,
                )

                if (category == Category.Transactions) {
                    TransactionExtractor.extract(body, container.regexRules.amountPattern, fallbackMerchant = displayName)?.let { tx ->
                        container.passbookRepository.recordTransaction(
                            merchant = tx.merchant,
                            accountLast4 = tx.accountLast4,
                            amountCents = tx.amountCents,
                            isCredit = tx.isCredit,
                            timestampMillis = timestamp,
                        )
                    }
                }

                if (settings.cloudFallbackEnabled) {
                    container.cloudClassifierClient.extractReminder(settings.serverBaseUrl, body, timestamp)?.let { reminder ->
                        container.passbookRepository.insertReminder(reminder.title, reminder.detail, reminder.dueAtEpochMillis)
                    }
                }

                if (!container.threadRepository.isBlocked(sender)) {
                    val otpCode = if (category == Category.Otp) container.regexRules.extractCode(body) else null
                    container.messageNotifier.notifyIncoming(thread, message, category, otpCode)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
