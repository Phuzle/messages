package com.phuzle.labs.messages.domain.search

import com.phuzle.labs.messages.domain.model.Category
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchRankingTest {
    private data class Msg(val id: Int, val body: String, val time: Long)

    private val day = 24L * 60 * 60 * 1000
    private val now = 1_000_000L * day
    private fun q(raw: String) = SearchMatcher.parse(raw)!!

    private fun rank(query: String, items: List<Msg>, sort: SearchSort = SearchSort.BestMatch) =
        SearchRanking.rank(q(query), items, sort, text = { it.body }, time = { it.time }).map { it.item.id }

    @Test fun strongMatchesComeBeforeWeakOnes() {
        val weakRecent = Msg(1, "collectively we agree", now)               // word prefix: strong
        val midWord = Msg(2, "the recollection of events", now - 1 * day)    // mid-word: weak
        val strongOld = Msg(3, "a collect call", now - 700 * day)            // word prefix: strong
        // Strong band newest-first, then the weak one — even though the weak one is newer than strongOld.
        assertEquals(listOf(1, 3, 2), rank("collect", listOf(midWord, strongOld, weakRecent)))
    }

    @Test fun recentStrongMatchBeatsTwoYearOldStrongMatch() {
        val old = Msg(1, "Your OTP is 111111", now - 730 * day)
        val recent = Msg(2, "OTP 222222 for login", now - 1 * day)
        assertEquals(listOf(2, 1), rank("otp", listOf(old, recent)))
    }

    @Test fun scatteredLetterJunkIsExcluded() {
        val junk = Msg(1, "Au Credit Card festive offers are live now", now)
        val real = Msg(2, "Our collective bargaining update", now - 400 * day)
        assertEquals(listOf(2), rank("collective", listOf(junk, real)))
    }

    @Test fun newestAndOldestSortIgnoreStrength() {
        val strongOld = Msg(1, "collect now", now - 500 * day)
        val weakNew = Msg(2, "recollect", now - 1 * day)
        val items = listOf(strongOld, weakNew)
        assertEquals(listOf(2, 1), rank("collect", items, SearchSort.Newest))
        assertEquals(listOf(1, 2), rank("collect", items, SearchSort.Oldest))
    }

    @Test fun equalTimestampsAreStable() {
        val items = listOf(Msg(1, "otp a", now), Msg(2, "otp b", now))
        assertEquals(listOf(1, 2), rank("otp", items))
    }

    @Test fun noMatchesGivesEmpty() = assertTrue(rank("zzz", listOf(Msg(1, "hello", now))).isEmpty())

    @Test fun contactsRankByNameQualityThenRecency() {
        data class C(val name: String, val time: Long)
        val items = listOf(C("Jordan Reyes", now - day), C("Jo", now), C("Jordan", now - 5 * day), C("Mr. Jordanson", now))
        val ranked = SearchRanking.rankContacts(q("jordan"), items, { it.name }, { it.time }).map { it.item.name }
        assertEquals(listOf("Jordan", "Jordan Reyes", "Mr. Jordanson"), ranked)
    }

    @Test fun rangeBounds() {
        assertEquals(0L, SearchRange.Any.fromMillis(now))
        assertEquals(now - 7 * day, SearchRange.Week.fromMillis(now))
        assertEquals(now - 365 * day, SearchRange.Year.fromMillis(now))
    }

    @Test fun activeFilterCount() {
        assertEquals(0, SearchFilters().activeCount)
        assertEquals(2, SearchFilters(category = Category.Otp, sort = SearchSort.Newest).activeCount)
        assertEquals(4, SearchFilters(Category.Otp, SearchRange.Week, SearchDirection.Sent, SearchSort.Oldest).activeCount)
    }
}
