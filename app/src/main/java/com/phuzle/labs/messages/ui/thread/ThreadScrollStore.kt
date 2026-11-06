package com.phuzle.labs.messages.ui.thread

import androidx.compose.foundation.lazy.LazyListState

/**
 * Keeps each open conversation's scroll position alive while Contact info (or anything else that
 * hides the thread without leaving it) is on top. Cleared by AppRoot once the user actually leaves
 * the thread, so the next time a conversation opens it lands on the latest message as usual.
 */
class ThreadScrollStore {
    private val states = HashMap<String, LazyListState>()

    fun has(threadId: String): Boolean = states.containsKey(threadId)
    fun stateFor(threadId: String): LazyListState = states.getOrPut(threadId) { LazyListState() }
    fun clear() = states.clear()
}
