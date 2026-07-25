package com.phuzle.labs.messages.domain.model

/** Passbook and Reminders are fully implemented (data layer, classifier, screens) but hidden from
 * the UI for now — real-world categorization quirks mean the ledger isn't reliable enough yet to
 * show. Flip this back on once that's sorted; nothing else needs to change (the bottom nav bar,
 * drawer entries, and dashboard tabs all key off this one flag). */
object FeatureFlags {
    const val PASSBOOK_AND_REMINDERS_ENABLED = false
}
