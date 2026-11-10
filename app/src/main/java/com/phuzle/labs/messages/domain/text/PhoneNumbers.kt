package com.phuzle.labs.messages.domain.text

object PhoneNumbers {
    /**
     * Whether two address strings are the same party: real phone numbers compare by their last ten
     * digits (so "+91 98765 43210" matches "09876543210"), anything shorter or non-numeric — a
     * shortcode, an alphanumeric sender like "HDFCBK" — compares as plain case-insensitive text.
     */
    fun same(a: String, b: String): Boolean {
        val da = a.filter(Char::isDigit)
        val db = b.filter(Char::isDigit)
        return if (da.length >= 7 && db.length >= 7) da.takeLast(10) == db.takeLast(10) else a.trim().equals(b.trim(), ignoreCase = true)
    }
}
