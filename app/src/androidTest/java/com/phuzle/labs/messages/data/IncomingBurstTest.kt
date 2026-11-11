package com.phuzle.labs.messages.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phuzle.labs.messages.core.sms.SmsProviderSync
import com.phuzle.labs.messages.data.db.AppDatabase
import com.phuzle.labs.messages.data.repository.ThreadRepository
import com.phuzle.labs.messages.domain.model.Category
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Several SMS_DELIVER broadcasts run at once; they must land in one conversation, intact. */
@RunWith(AndroidJUnit4::class)
class IncomingBurstTest {
    private lateinit var db: AppDatabase
    private lateinit var repo: ThreadRepository

    @Before fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        repo = ThreadRepository(db.threadDao(), db.messageDao(), db.blockedNumberDao(), SmsProviderSync(context))
    }

    @After fun tearDown() = db.close()

    private suspend fun receive(sender: String, body: String, ts: Long) = repo.recordIncomingMessage(
        sender = sender, displayName = sender, isBusiness = true, category = Category.Personal, body = body, timestampMillis = ts,
    )

    @Test fun burstFromANewSenderMakesOneThreadWithEveryMessage() = runBlocking {
        val results = (1..8).map { i -> async(Dispatchers.IO) { receive("+919876543210", "message $i", 1_000L + i) } }.awaitAll()

        assertEquals("every arrival sees the same thread", 1, results.map { it.first.id }.distinct().size)
        val threadId = results.first().first.id
        assertEquals(8, db.messageDao().observeForThread(threadId).first().size)
        val thread = db.threadDao().findBySender("+919876543210")!!
        assertEquals("message 8", thread.lastMessagePreview)
        assertEquals(1_008L, thread.lastMessageTime)
        assertTrue(thread.unread)
    }

    @Test fun burstIntoAnExistingThreadKeepsEveryMessage() = runBlocking {
        receive("HDFCBK", "first", 10L)
        (1..6).map { i -> async(Dispatchers.IO) { receive("HDFCBK", "burst $i", 100L + i) } }.awaitAll()
        val thread = db.threadDao().findBySender("HDFCBK")!!
        assertEquals(7, db.messageDao().observeForThread(thread.id).first().size)
        assertEquals("burst 6", thread.lastMessagePreview)
    }
}
