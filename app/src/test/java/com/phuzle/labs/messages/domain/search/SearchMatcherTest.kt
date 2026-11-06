package com.phuzle.labs.messages.domain.search

import com.phuzle.labs.messages.domain.search.SearchMatcher.Tier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchMatcherTest {
    private fun q(raw: String) = SearchMatcher.parse(raw)!!
    private fun hit(query: String, text: String, fuzzy: Boolean = false) = SearchMatcher.match(q(query), text, fuzzy)

    // --- the reported bug -----------------------------------------------------------------

    @Test fun scatteredLettersAreNotAMatch() {
        // "collective" is a subsequence of this text but nowhere near a real match.
        assertNull(hit("collective", "Au Credit Card festive offers are here, get 10% cashback on every spend"))
    }

    @Test fun realWordMatches() {
        assertNotNull(hit("collective", "The collective agreement was signed"))
    }

    // --- tiers ----------------------------------------------------------------------------

    @Test fun exactTextIsStrongest() = assertEquals(Tier.Exact, hit("jordan reyes", "Jordan Reyes")!!.tier)

    @Test fun wholeWord() = assertEquals(Tier.WholeWord, hit("otp", "Your OTP is 123456")!!.tier)

    @Test fun wordPrefix() = assertEquals(Tier.WordPrefix, hit("collect", "collective offers")!!.tier)

    @Test fun midWordIsWeak() {
        val h = hit("tive", "collective offers")!!
        assertEquals(Tier.PhraseInside, h.tier)
        assertFalse(h.tier.strong)
    }

    @Test fun tokensInAnyOrderAtWordStarts() = assertEquals(Tier.TokensAtWord, hit("card credit", "Your Credit Card statement")!!.tier)

    @Test fun tokensAnywhereIsWeak() {
        val h = hit("edit ard", "Your Credit Card statement")!!
        assertEquals(Tier.TokensAnywhere, h.tier)
        assertFalse(h.tier.strong)
    }

    @Test fun everyTokenMustBePresent() = assertNull(hit("credit loan", "Your Credit Card statement"))

    @Test fun tierOrderingIsStrongToWeak() {
        val order = listOf(Tier.Exact, Tier.WholeWord, Tier.WordPrefix, Tier.TokensAtWord, Tier.PhraseInside, Tier.TokensAnywhere, Tier.Fuzzy)
        assertEquals(order.sortedByDescending { it.weight }, order)
        assertEquals(listOf(true, true, true, true, false, false, false), order.map { it.strong })
    }

    // --- case, symbols, unicode -----------------------------------------------------------

    @Test fun caseInsensitive() = assertNotNull(hit("HDFC", "hdfc bank alert"))

    @Test fun regexCharactersAreLiteral() {
        assertNotNull(hit("a.b", "ref a.b done"))
        assertNull(hit("a.b", "ref axb done"))
        assertNotNull(hit("(x)", "value (x) here"))
        assertNotNull(hit("c++", "learn c++ today"))
    }

    @Test fun nonAsciiCase() = assertNotNull(hit("émile", "Message from ÉMILE today"))

    @Test fun blankQueryParsesToNull() {
        assertNull(SearchMatcher.parse(""))
        assertNull(SearchMatcher.parse("   "))
    }

    @Test fun tokensAreDeduplicatedAndCapped() {
        assertEquals(listOf("a"), SearchMatcher.parse("a a A".lowercase())!!.tokens)
        assertEquals(SearchMatcher.MAX_TOKENS, SearchMatcher.parse("a b c d e f")!!.tokens.size)
    }

    // --- contact-name fuzzy fallback -------------------------------------------------------

    @Test fun fuzzyOnlyWhenAllowed() {
        assertNull(hit("gdrv", "Google Drive"))
        assertEquals(Tier.Fuzzy, hit("gdrv", "Google Drive", fuzzy = true)!!.tier)
    }

    @Test fun fuzzyIgnoresVeryShortQueries() = assertNull(hit("gd", "Google Drive", fuzzy = true))

    // --- highlighting ---------------------------------------------------------------------

    @Test fun highlightsEveryOccurrence() {
        val h = hit("ab", "ab xx ab")!!
        assertEquals(setOf(0, 1, 6, 7), h.indices)
    }

    // --- snippets -------------------------------------------------------------------------

    @Test fun shortTextIsUntouched() {
        val text = "Your OTP is 123456"
        val h = hit("otp", text)!!
        val s = SearchMatcher.snippet(text, h)
        assertEquals(text, s.text)
        assertEquals(h.indices, s.indices)
    }

    @Test fun longTextIsCenteredOnTheMatch() {
        val text = "x".repeat(200) + " the needle is here " + "y".repeat(200)
        val h = hit("needle", text)!!
        val s = SearchMatcher.snippet(text, h, maxLength = 60)
        assertTrue(s.text.startsWith("…"))
        assertTrue(s.text.endsWith("…"))
        val marked = s.indices.sorted().joinToString("") { s.text[it].toString() }
        assertEquals("needle", marked)
    }

    @Test fun matchNearTheStartHasNoLeadingEllipsis() {
        val text = "needle " + "z".repeat(300)
        val s = SearchMatcher.snippet(text, hit("needle", text)!!, maxLength = 40)
        assertFalse(s.text.startsWith("…"))
        assertTrue(s.text.endsWith("…"))
        assertEquals("needle", s.indices.sorted().joinToString("") { s.text[it].toString() })
    }

    @Test fun newlinesAreFlattenedWithoutShiftingIndices() {
        val text = "first line\nsecond needle line"
        val h = hit("needle", text)!!
        val s = SearchMatcher.snippet(text, h)
        assertFalse(s.text.contains('\n'))
        assertEquals("needle", s.indices.sorted().joinToString("") { s.text[it].toString() })
    }

    // --- SQL pre-filter patterns ------------------------------------------------------------

    @Test fun likePatternsPadUnusedSlots() {
        assertEquals(listOf("%otp%", "%", "%", "%"), SearchMatcher.likePatterns(q("otp")))
        assertEquals(listOf("%a%", "%b%", "%", "%"), SearchMatcher.likePatterns(q("a b")))
    }

    @Test fun likeWildcardsAreEscaped() {
        assertEquals("100\\% sure", SearchMatcher.escapeLike("100% sure"))
        assertEquals("a\\_b", SearchMatcher.escapeLike("a_b"))
        assertEquals("a\\\\b", SearchMatcher.escapeLike("a\\b"))
    }
}
