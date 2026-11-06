package com.phuzle.labs.messages.domain.search

import com.phuzle.labs.messages.domain.model.Category

enum class SearchSort(val label: String) {
    /** Strong matches first, newest first within each band. */
    BestMatch("Best match"),
    Newest("Newest"),
    Oldest("Oldest"),
}

enum class SearchRange(val label: String, private val days: Int?) {
    Any("Any time", null),
    Week("Past week", 7),
    Month("Past month", 30),
    Year("Past year", 365);

    /** Inclusive lower bound on a message timestamp, or 0 for no bound. */
    fun fromMillis(now: Long): Long = days?.let { now - it * DAY_MILLIS } ?: 0L
}

enum class SearchDirection(val label: String, val sqlValue: Int) {
    Any("All", -1),
    Received("Received", 0),
    Sent("Sent", 1),
}

data class SearchFilters(
    val category: Category = Category.All,
    val range: SearchRange = SearchRange.Any,
    val direction: SearchDirection = SearchDirection.Any,
    val sort: SearchSort = SearchSort.BestMatch,
) {
    /** How many non-default choices are applied — drives the badge on the Filters button. */
    val activeCount: Int
        get() = listOf(category != Category.All, range != SearchRange.Any, direction != SearchDirection.Any, sort != SearchSort.BestMatch).count { it }
}

data class Ranked<T>(val item: T, val hit: SearchMatcher.Hit)

object SearchRanking {
    /**
     * Keeps the [items] that match [query] and orders them. [SearchSort.BestMatch] splits results
     * into a strong band (whole word / word start) and a weak band (mid-word) and sorts each band
     * newest-first — so a strong match from last week beats a weak one from yesterday, but among
     * equally good matches a recent one is never buried under one from years ago.
     */
    fun <T> rank(
        query: SearchMatcher.Query,
        items: List<T>,
        sort: SearchSort,
        allowFuzzy: Boolean = false,
        text: (T) -> String,
        time: (T) -> Long,
    ): List<Ranked<T>> {
        val matched = ArrayList<Ranked<T>>()
        for (item in items) {
            SearchMatcher.match(query, text(item), allowFuzzy)?.let { matched += Ranked(item, it) }
        }
        val comparator: Comparator<Ranked<T>> = when (sort) {
            SearchSort.BestMatch -> compareByDescending<Ranked<T>> { it.hit.tier.strong }
                .thenByDescending { time(it.item) }
                .thenByDescending { it.hit.tier.weight }
            SearchSort.Newest -> compareByDescending { time(it.item) }
            SearchSort.Oldest -> compareBy { time(it.item) }
        }
        return matched.sortedWith(comparator)
    }

    /** Contacts always rank by how well the *name* matched, then recency. */
    fun <T> rankContacts(
        query: SearchMatcher.Query,
        items: List<T>,
        name: (T) -> String,
        time: (T) -> Long,
    ): List<Ranked<T>> {
        val matched = items.mapNotNull { item -> SearchMatcher.match(query, name(item), allowFuzzy = true)?.let { Ranked(item, it) } }
        return matched.sortedWith(compareByDescending<Ranked<T>> { it.hit.tier.weight }.thenByDescending { time(it.item) })
    }
}

private const val DAY_MILLIS = 24L * 60 * 60 * 1000
