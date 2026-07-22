package com.phuzle.labs.messages.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * The PRD describes Passbook as loading from a `bank_accounts` SQLite view fed by background
 * triggers off real bank SMS. No such ingestion pipeline exists yet, so this table is seeded
 * once with the same sample accounts the prototype ships with.
 */
@Entity(tableName = "bank_accounts")
data class BankAccountEntity(
    @PrimaryKey val id: String,
    val name: String,
    val last4: String,
    val type: String,
    val balanceCents: Long,
)
