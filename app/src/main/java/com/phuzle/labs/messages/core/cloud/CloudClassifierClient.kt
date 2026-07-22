package com.phuzle.labs.messages.core.cloud

import android.util.Log
import com.phuzle.labs.messages.domain.model.Category
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.TimeZone
import java.util.concurrent.TimeUnit

data class ReminderResult(val title: String, val detail: String, val dueAtEpochMillis: Long)

/**
 * Layer 3 of the PRD's pipeline: called only for messages Layer 1 landed on Unknown for, and only
 * when the user has opted in (Settings > Chats > "Use cloud fallback for unclear messages" — off
 * by default). Every call redacts the body with [PiiScrubber] first. Failures (server down, no
 * network, bad response) are swallowed to null — this is a nice-to-have enhancement, never a
 * blocker for the real SMS pipeline.
 */
class CloudClassifierClient {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(4, TimeUnit.SECONDS)
        .build()

    private fun api(baseUrl: String): CloudApi {
        val normalized = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        return Retrofit.Builder()
            .baseUrl(normalized)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CloudApi::class.java)
    }

    private val minConfidence = 0.4

    suspend fun classify(baseUrl: String, body: String): Category? = runCatching {
        val response = api(baseUrl).classify(ClassifyRequest(PiiScrubber.scrub(body)))
        if (response.confidence < minConfidence) return@runCatching null
        response.category.toCategory()
    }.onFailure { Log.w("CloudClassifierClient", "classify failed", it) }.getOrNull()

    suspend fun extractReminder(baseUrl: String, body: String, receivedAtEpochMillis: Long): ReminderResult? = runCatching {
        val offsetMinutes = TimeZone.getDefault().getOffset(receivedAtEpochMillis) / 60_000
        val response = api(baseUrl).extractReminder(
            ReminderRequest(PiiScrubber.scrub(body), receivedAtEpochMillis, offsetMinutes),
        )
        val dueAt = response.dueAtEpochMillis
        if (!response.isReminder || dueAt == null) return@runCatching null
        ReminderResult(
            title = response.title?.takeIf { it.isNotBlank() } ?: "Reminder",
            detail = response.detail?.takeIf { it.isNotBlank() } ?: body,
            dueAtEpochMillis = dueAt,
        )
    }.onFailure { Log.w("CloudClassifierClient", "reminder extraction failed", it) }.getOrNull()

    private fun String.toCategory(): Category? = when (this) {
        "PERSONAL" -> Category.Personal
        "TRANSACTIONS" -> Category.Transactions
        "OTP" -> Category.Otp
        "PROMOTIONS" -> Category.Promotions
        "OTHERS" -> Category.Others
        else -> null
    }
}
