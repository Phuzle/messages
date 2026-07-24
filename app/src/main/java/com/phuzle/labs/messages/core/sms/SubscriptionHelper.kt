package com.phuzle.labs.messages.core.sms

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.telephony.SubscriptionManager
import androidx.core.content.ContextCompat

/** One active SIM, as shown to the user (compose screen's SIM picker). */
data class SimOption(val subscriptionId: Int, val slotIndex: Int, val label: String)

/**
 * Wraps [SubscriptionManager] for devices with more than one active SIM. Everything here
 * degrades quietly on single-SIM devices and on devices/API levels that deny READ_PHONE_STATE:
 * [activeSims] returns an empty list rather than throwing, which every caller treats the same as
 * "nothing to pick from, just use the system default".
 */
object SubscriptionHelper {

    fun activeSims(context: Context): List<SimOption> {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            return emptyList()
        }
        val manager = context.getSystemService(SubscriptionManager::class.java) ?: return emptyList()
        return runCatching {
            manager.activeSubscriptionInfoList.orEmpty().mapIndexed { index, info ->
                val label = info.displayName?.toString()?.takeIf { it.isNotBlank() }
                    ?: info.carrierName?.toString()?.takeIf { it.isNotBlank() }
                    ?: "SIM ${index + 1}"
                SimOption(subscriptionId = info.subscriptionId, slotIndex = info.simSlotIndex, label = label)
            }
        }.getOrDefault(emptyList())
    }

    /**
     * Best-effort extraction of which subscription an incoming SMS_DELIVER intent arrived on.
     * [SubscriptionManager.EXTRA_SUBSCRIPTION_INDEX] is the documented key; the bare "subscription"
     * extra is what the platform used before that constant existed and is kept as a fallback since
     * some OEM/AOSP versions still only set the older key. Single-SIM emulators and devices set
     * neither, so null (meaning "no subscription known for this message") is a normal result, not
     * an error — callers must not substitute the default subscription here, since that would
     * misrepresent history that predates dual-SIM awareness as having arrived on a specific SIM.
     */
    fun subscriptionIdFromIntent(intent: Intent): Int? {
        val fromDocumentedExtra = intent.getIntExtra(SubscriptionManager.EXTRA_SUBSCRIPTION_INDEX, -1)
        if (fromDocumentedExtra != -1) return fromDocumentedExtra
        val fromLegacyExtra = intent.getIntExtra("subscription", -1)
        if (fromLegacyExtra != -1) return fromLegacyExtra
        return null
    }

    fun defaultSmsSubscriptionId(): Int? {
        val id = SubscriptionManager.getDefaultSmsSubscriptionId()
        return if (id == SubscriptionManager.INVALID_SUBSCRIPTION_ID) null else id
    }
}
