package com.phuzle.labs.messages

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import com.phuzle.labs.messages.core.cloud.CloudClassifierClient
import com.phuzle.labs.messages.core.contacts.ContactLookup
import com.phuzle.labs.messages.core.notifications.MessageNotifier
import com.phuzle.labs.messages.core.push.UpdateChecker
import com.phuzle.labs.messages.core.sms.SmsHistoryImporter
import com.phuzle.labs.messages.core.sms.SmsSender
import com.phuzle.labs.messages.data.backup.DriveBackupMerger
import com.phuzle.labs.messages.data.backup.GoogleDriveBackupManager
import com.phuzle.labs.messages.data.backup.LocalBackupManager
import com.phuzle.labs.messages.data.db.AppDatabase
import com.phuzle.labs.messages.data.prefs.SettingsRepository
import com.phuzle.labs.messages.data.repository.DraftRepository
import com.phuzle.labs.messages.data.repository.PassbookRepository
import com.phuzle.labs.messages.data.repository.ThreadRepository
import com.phuzle.labs.messages.domain.categorization.CategoryClassifier
import com.phuzle.labs.messages.domain.categorization.RegexRules

/**
 * Hand-rolled composition root. The app is small enough that a DI framework would be pure
 * ceremony — everything here is a cheap, stateless-ish singleton built once in
 * [MessagesApplication] and reused by the ViewModel, receivers, and workers alike.
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val database: AppDatabase by lazy { AppDatabase.getInstance(appContext) }
    val threadRepository: ThreadRepository by lazy {
        ThreadRepository(database.threadDao(), database.messageDao(), database.blockedNumberDao())
    }
    val passbookRepository: PassbookRepository by lazy { PassbookRepository(database.passbookDao()) }
    val draftRepository: DraftRepository by lazy { DraftRepository(database.draftDao()) }
    val settingsRepository: SettingsRepository by lazy { SettingsRepository(appContext) }
    val backupManager: LocalBackupManager by lazy { LocalBackupManager(appContext) }
    val driveBackupManager: GoogleDriveBackupManager by lazy { GoogleDriveBackupManager(appContext) }
    val driveBackupMerger: DriveBackupMerger by lazy { DriveBackupMerger(appContext, database) }
    val contactLookup: ContactLookup by lazy { ContactLookup(appContext) }
    val regexRules: RegexRules by lazy { RegexRules.loadFrom(appContext) }
    val classifier: CategoryClassifier by lazy {
        CategoryClassifier(regexRules) { number -> contactLookup.isKnownContact(number) }
    }
    val smsSender: SmsSender by lazy { SmsSender(appContext) }
    val messageNotifier: MessageNotifier by lazy { MessageNotifier(appContext, settingsRepository) }
    val updateChecker: UpdateChecker by lazy { UpdateChecker() }
    val smsHistoryImporter: SmsHistoryImporter by lazy {
        SmsHistoryImporter(appContext, database.threadDao(), database.messageDao(), contactLookup, classifier)
    }
    val cloudClassifierClient: CloudClassifierClient by lazy { CloudClassifierClient() }

    fun copyToClipboard(label: String, text: String) {
        val clipboard = appContext.getSystemService(android.content.ClipboardManager::class.java)
        clipboard?.setPrimaryClip(android.content.ClipData.newPlainText(label, text))
    }

    fun isDefaultSmsApp(): Boolean = com.phuzle.labs.messages.core.sms.DefaultSmsAppHelper.isDefaultSmsApp(appContext)

    /** Gate for the Drive "Wi-Fi only" setting — checked fresh before every Drive network call,
     * not just used to render a static label. */
    fun isOnWifi(): Boolean {
        val cm = appContext.getSystemService(android.net.ConnectivityManager::class.java) ?: return false
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI)
    }

    /**
     * Anonymous sign-in gives every install a stable Firebase Auth UID, so the console's
     * Authentication > Users tab becomes a real "who's connected" list (creation + last-active
     * timestamps) with zero login UI. It's pseudonymous by design — swap in Google Sign-In later
     * if named/identified users are ever needed.
     */
    fun ensureAnonymousSignIn() {
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            auth.signInAnonymously()
                .addOnFailureListener { e -> Log.w("AppContainer", "Anonymous sign-in failed", e) }
        }
    }

    /** Lets us broadcast alerts (update available, maintenance) from the Firebase console with no backend. */
    fun subscribeToAnnouncements() {
        FirebaseMessaging.getInstance().subscribeToTopic("announcements")
            .addOnFailureListener { e -> Log.w("AppContainer", "Topic subscription failed", e) }
    }
}

val Context.appContainer: AppContainer
    get() = (applicationContext as MessagesApplication).container
