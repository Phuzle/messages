package com.phuzle.labs.messages

import android.content.Context
import com.phuzle.labs.messages.core.contacts.ContactLookup
import com.phuzle.labs.messages.core.notifications.MessageNotifier
import com.phuzle.labs.messages.core.sms.SmsSender
import com.phuzle.labs.messages.data.backup.LocalBackupManager
import com.phuzle.labs.messages.data.db.AppDatabase
import com.phuzle.labs.messages.data.prefs.SettingsRepository
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
    val settingsRepository: SettingsRepository by lazy { SettingsRepository(appContext) }
    val backupManager: LocalBackupManager by lazy { LocalBackupManager(appContext) }
    val contactLookup: ContactLookup by lazy { ContactLookup(appContext) }
    val regexRules: RegexRules by lazy { RegexRules.loadFrom(appContext) }
    val classifier: CategoryClassifier by lazy {
        CategoryClassifier(regexRules) { number -> contactLookup.isKnownContact(number) }
    }
    val smsSender: SmsSender by lazy { SmsSender(appContext) }
    val messageNotifier: MessageNotifier by lazy { MessageNotifier(appContext, settingsRepository) }

    fun copyToClipboard(label: String, text: String) {
        val clipboard = appContext.getSystemService(android.content.ClipboardManager::class.java)
        clipboard?.setPrimaryClip(android.content.ClipData.newPlainText(label, text))
    }

    fun isDefaultSmsApp(): Boolean = com.phuzle.labs.messages.core.sms.DefaultSmsAppHelper.isDefaultSmsApp(appContext)
}

val Context.appContainer: AppContainer
    get() = (applicationContext as MessagesApplication).container
