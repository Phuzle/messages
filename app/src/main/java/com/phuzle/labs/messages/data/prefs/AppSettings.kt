package com.phuzle.labs.messages.data.prefs

/** Every toggle/enum from the prototype's `state` object that isn't a thread or passbook row. */
data class AppSettings(
    val themeMode: String = "system", // system | light | dark | amoled | sepia
    val accentHex: String = "#3E6DF2",
    val swipeLeftAction: String = "archive", // archive | delete | toggleRead | none
    val swipeRightAction: String = "toggleRead",
    val channelPersonalEnabled: Boolean = true,
    val channelOtpEnabled: Boolean = true,
    val channelTransactEnabled: Boolean = true,
    val channelPromoEnabled: Boolean = true,
    val notificationsAllowed: Boolean = true,
    val wakeScreenForHighPriority: Boolean = true,
    val quickActionButtons: Boolean = true,
    val lockScreenVisibility: String = "full", // full | nameOnly | hidden
    val appLockEnabled: Boolean = false,
    val appLockMethod: String = "fingerprint", // fingerprint | face | pin
    val signature: String = "",
    val showCharCount: Boolean = false,
    val inAppBrowser: Boolean = true,
    val backupFrequency: String = "daily", // daily | weekly
    val cloudBackupConnected: Boolean = false,
    val otpEvictionEnabled: Boolean = false,
    val lastLocalBackupAt: Long? = null,
    val lastLocalRestoreAt: Long? = null,
    val historyImported: Boolean = false,
    /** Layer 3 of the PRD's pipeline — off by default, since it means sending (PII-scrubbed)
     * message text to a server. See server/README.md. */
    val cloudFallbackEnabled: Boolean = false,
    val serverBaseUrl: String = "http://10.0.2.2:8080/",
)
