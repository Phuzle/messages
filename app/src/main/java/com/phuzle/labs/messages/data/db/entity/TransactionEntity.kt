package com.phuzle.labs.messages.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: String,
    val merchant: String,
    val accountLast4: String,
    val amountCents: Long,
    val time: Long,
    val isCredit: Boolean,
)
