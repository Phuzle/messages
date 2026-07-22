package com.phuzle.labs.messages.core.cloud

import retrofit2.http.Body
import retrofit2.http.POST

data class ClassifyRequest(val body: String)
data class ClassifyResponse(val category: String, val confidence: Double, val matchedKeywords: List<String>)

data class ReminderRequest(val body: String, val receivedAtEpochMillis: Long, val timezoneOffsetMinutes: Int)
data class ReminderResponse(
    val isReminder: Boolean,
    val title: String?,
    val detail: String?,
    val dueAtEpochMillis: Long?,
)

/** The server/ project's two Layer 3 endpoints — see server/README.md for the contract. */
interface CloudApi {
    @POST("v1/classify")
    suspend fun classify(@Body request: ClassifyRequest): ClassifyResponse

    @POST("v1/reminders/extract")
    suspend fun extractReminder(@Body request: ReminderRequest): ReminderResponse
}
