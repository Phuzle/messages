package com.phuzle.labs.messages.ui.format

import java.text.NumberFormat
import java.util.Locale

private val currencyFormat: NumberFormat = NumberFormat.getCurrencyInstance(Locale.US)

/** Account balance display: "$3,482.19". */
fun formatCentsPlain(cents: Long): String = currencyFormat.format(cents / 100.0)

/** Transaction row display: "+$2,400.00" / "-$42.10". */
fun formatCentsSigned(cents: Long): String {
    val amount = currencyFormat.format(kotlin.math.abs(cents) / 100.0)
    return if (cents >= 0) "+$amount" else "-$amount"
}
