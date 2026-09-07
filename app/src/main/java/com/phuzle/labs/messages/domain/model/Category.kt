package com.phuzle.labs.messages.domain.model

/** The eight inbox groupings from the design; [All] is a UI-only filter, never stored on a thread. */
enum class Category(val label: String, val channelId: String) {
    All("All", ""),
    Personal("Personal", NotificationChannelIds.PERSONAL),
    Transactions("Transactions", NotificationChannelIds.TRANSACTIONS),
    Otp("OTP", NotificationChannelIds.OTP),
    Ipo("IPO", NotificationChannelIds.IPO),
    Promotions("Promotions", NotificationChannelIds.PROMOTIONS),
    Others("Others", NotificationChannelIds.OTHERS),
    Unknown("Unknown", NotificationChannelIds.UNKNOWN);

    /** Matches the prototype's NONREPLIABLE list: only Personal and Unknown threads accept a reply. */
    val isReplyable: Boolean get() = this == Personal || this == Unknown

    companion object {
        /** Storable categories, i.e. every entry except the [All] filter chip. */
        val storable = listOf(Personal, Transactions, Otp, Ipo, Promotions, Others, Unknown)

        fun fromStoredName(name: String): Category =
            storable.firstOrNull { it.name == name } ?: Unknown
    }
}

object NotificationChannelIds {
    const val PERSONAL = "ch_personal"
    const val OTP = "ch_otp"
    const val TRANSACTIONS = "ch_transact"
    const val IPO = "ch_ipo"
    const val PROMOTIONS = "ch_promo"
    const val OTHERS = "ch_others"
    const val UNKNOWN = "ch_unknown"
    const val SYSTEM = "ch_system"
}
