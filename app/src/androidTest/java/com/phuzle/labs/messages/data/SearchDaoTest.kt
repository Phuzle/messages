package com.phuzle.labs.messages.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phuzle.labs.messages.core.sms.SmsProviderSync
import com.phuzle.labs.messages.data.db.AppDatabase
import com.phuzle.labs.messages.data.db.entity.MessageEntity
import com.phuzle.labs.messages.data.db.entity.ThreadEntity
import com.phuzle.labs.messages.data.repository.ThreadRepository
import com.phuzle.labs.messages.domain.model.Category
import com.phuzle.labs.messages.domain.search.SearchMatcher
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** The SQL half of search: the token pre-filter, its filters, and what it must never return. */
@RunWith(AndroidJUnit4::class)
class SearchDaoTest {
    private lateinit var db: AppDatabase
    private lateinit var repo: ThreadRepository
    private val day = 24L * 60 * 60 * 1000
    private val now = 1_800_000_000_000L

    @Before fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        repo = ThreadRepository(db.threadDao(), db.messageDao(), db.blockedNumberDao(), SmsProviderSync(context))
    }

    @After fun tearDown() = db.close()

    private suspend fun thread(
        id: String, name: String = id, category: Category = Category.Others,
        archived: Boolean = false, isPrivate: Boolean = false, deletedAt: Long? = null,
    ) = db.threadDao().upsert(
        ThreadEntity(id, sender = id, displayName = name, category = category.name, isBusiness = true, avatarColor = 0xFF336699, lastMessagePreview = "", lastMessageTime = now, unread = false, archived = archived, isPrivate = isPrivate, deletedAt = deletedAt),
    )

    private suspend fun message(thread: String, body: String, ts: Long = now, outgoing: Boolean = false, sent: Boolean = true, deletedAt: Long? = null) =
        db.messageDao().insert(MessageEntity(threadId = thread, body = body, timestamp = ts, outgoing = outgoing, sent = sent, deletedAt = deletedAt))

    private fun patterns(q: String) = SearchMatcher.likePatterns(SearchMatcher.parse(q)!!)

    private suspend fun search(q: String, from: Long = 0, direction: Int = -1, category: String = "") =
        repo.searchMessages(patterns(q), from, direction, category, 1000).map { it.body }

    @Test fun everyTokenMustAppear() = runBlocking {
        thread("t1")
        message("t1", "your credit card statement")
        message("t1", "your debit card")
        assertEquals(listOf("your credit card statement"), search("credit card"))
    }

    @Test fun eachMatchingMessageIsItsOwnRow() = runBlocking {
        thread("t1")
        repeat(5) { message("t1", "otp number $it", ts = now - it * day) }
        assertEquals(5, search("otp").size)
    }

    @Test fun newestFirst() = runBlocking {
        thread("t1")
        message("t1", "otp old", ts = now - 100 * day)
        message("t1", "otp new", ts = now)
        assertEquals(listOf("otp new", "otp old"), search("otp"))
    }

    @Test fun wildcardCharactersAreLiteral() = runBlocking {
        thread("t1")
        message("t1", "save 100% today")
        message("t1", "save 1000 today")
        message("t1", "a_b")
        message("t1", "axb")
        assertEquals(listOf("save 100% today"), search("100%"))
        assertEquals(listOf("a_b"), search("a_b"))
    }

    @Test fun caseInsensitiveForAscii() = runBlocking {
        thread("t1")
        message("t1", "HDFC Bank alert")
        assertEquals(1, search("hdfc").size)
    }

    @Test fun archivedPrivateAndDeletedThreadsStayOut() = runBlocking {
        thread("inbox"); thread("arch", archived = true); thread("priv", isPrivate = true); thread("del", deletedAt = now)
        listOf("inbox", "arch", "priv", "del").forEach { message(it, "otp for $it") }
        assertEquals(listOf("otp for inbox"), search("otp"))
    }

    @Test fun deletedAndUnsentScheduledMessagesStayOut() = runBlocking {
        thread("t1")
        message("t1", "otp live")
        message("t1", "otp deleted", deletedAt = now)
        message("t1", "otp scheduled", sent = false)
        assertEquals(listOf("otp live"), search("otp"))
    }

    @Test fun dateDirectionAndCategoryFilters() = runBlocking {
        thread("o", category = Category.Otp); thread("p", category = Category.Personal)
        message("o", "code a", ts = now - 40 * day)
        message("o", "code b", ts = now - 2 * day)
        message("p", "code c", ts = now - 1 * day, outgoing = true)
        assertEquals(setOf("code b", "code c"), search("code", from = now - 7 * day).toSet())
        assertEquals(listOf("code c"), search("code", direction = 1))
        assertEquals(setOf("code a", "code b"), search("code", category = "Otp").toSet())
    }

    @Test fun threadSearchCoversWholeHistory() = runBlocking {
        thread("t1")
        repeat(200) { message("t1", "filler $it", ts = now - it * 1000) }
        message("t1", "the needle", ts = now - 400 * day)
        val hits = repo.searchInThread("t1", patterns("needle"), 100)
        assertEquals(listOf("the needle"), hits.map { it.body })
    }

    @Test fun betweenLoadsAContiguousStretch() = runBlocking {
        thread("t1")
        (1..10).forEach { message("t1", "m$it", ts = now + it * 1000L) }
        val got = repo.messagesBetween("t1", now + 3000L, now + 8000L).map { it.body }
        assertEquals(listOf("m3", "m4", "m5", "m6", "m7"), got)
    }

    // --- list summary stays on the latest message ----------------------------------------

    @Test fun olderMessageArrivingLateDoesNotReplaceThePreview() = runBlocking {
        repo.recordIncomingMessage("AX-BANK", "AX-BANK", true, Category.Transactions, "newest message", now)
        repo.recordIncomingMessage("AX-BANK", "AX-BANK", true, Category.Transactions, "delayed older message", now - 3 * day)
        val t = db.threadDao().findBySender("AX-BANK")!!
        assertEquals("newest message", t.lastMessagePreview)
        assertEquals(now, t.lastMessageTime)
        assertTrue(db.messageDao().allForThread(t.id).size == 2)
    }

    @Test fun newerMessageUpdatesThePreview() = runBlocking {
        repo.recordIncomingMessage("AX-BANK", "AX-BANK", true, Category.Transactions, "first", now - day)
        repo.recordIncomingMessage("AX-BANK", "AX-BANK", true, Category.Transactions, "second", now)
        assertEquals("second", db.threadDao().findBySender("AX-BANK")!!.lastMessagePreview)
    }
}
