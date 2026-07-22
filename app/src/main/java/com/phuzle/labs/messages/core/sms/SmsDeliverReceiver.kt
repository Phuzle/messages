package com.phuzle.labs.messages.core.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.phuzle.labs.messages.appContainer
import com.phuzle.labs.messages.domain.categorization.TransactionExtractor
import com.phuzle.labs.messages.domain.model.Category
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
                val contactName = container.contactLookup.displayNameFor(sender)
                val displayName = contactName ?: sender
                val isBusiness = contactName == null
                val photoUri = if (contactName != null) container.contactLookup.photoUriFor(sender) else null
                val category = container.classifier.classify(sender, body)

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
