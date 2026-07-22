package com.phuzle.labs.messages.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.phuzle.labs.messages.appContainer
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/** Storage settings: "24-hour OTP eviction — permanently purge OTP codes after 24h", opt-in. */
class OtpEvictionWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val container = applicationContext.appContainer
        val enabled = container.settingsRepository.settingsFlow.first().otpEvictionEnabled
        if (enabled) {
            val cutoff = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(24)
            container.threadRepository.purgeOtpMessagesBefore(cutoff)
        }
        return Result.success()
    }

    companion object {
        const val UNIQUE_WORK_NAME = "otp_eviction"
    }
}
