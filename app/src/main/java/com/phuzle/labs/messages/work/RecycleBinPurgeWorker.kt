package com.phuzle.labs.messages.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.phuzle.labs.messages.appContainer
import java.util.concurrent.TimeUnit

/** Storage settings: "Deleted threads are purged automatically after 30 days." */
class RecycleBinPurgeWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30)
        applicationContext.appContainer.threadRepository.purgeDeletedBefore(cutoff)
        return Result.success()
    }

    companion object {
        const val UNIQUE_WORK_NAME = "recycle_bin_purge"
    }
}
