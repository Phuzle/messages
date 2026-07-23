package com.phuzle.labs.messages

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.phuzle.labs.messages.core.notifications.NotificationChannels
import com.phuzle.labs.messages.work.AutoBackupWorker
import com.phuzle.labs.messages.work.OtpEvictionWorker
import com.phuzle.labs.messages.work.RecycleBinPurgeWorker
import com.phuzle.labs.messages.work.ScheduledSendWorker
import java.util.concurrent.TimeUnit

class MessagesApplication : Application() {

    val container: AppContainer by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        NotificationChannels.registerAll(this)
        schedulePeriodicWork()
        container.ensureAnonymousSignIn()
        container.subscribeToAnnouncements()
    }

    private fun schedulePeriodicWork() {
        val workManager = WorkManager.getInstance(this)

        workManager.enqueueUniquePeriodicWork(
            RecycleBinPurgeWorker.UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<RecycleBinPurgeWorker>(1, TimeUnit.DAYS).build(),
        )
        workManager.enqueueUniquePeriodicWork(
            OtpEvictionWorker.UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<OtpEvictionWorker>(15, TimeUnit.MINUTES).build(),
        )
        workManager.enqueueUniquePeriodicWork(
            ScheduledSendWorker.UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<ScheduledSendWorker>(15, TimeUnit.MINUTES).build(),
        )
        workManager.enqueueUniquePeriodicWork(
            AutoBackupWorker.UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<AutoBackupWorker>(6, TimeUnit.HOURS).build(),
        )
    }
}
