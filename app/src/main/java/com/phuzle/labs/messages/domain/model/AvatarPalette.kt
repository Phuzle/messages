package com.phuzle.labs.messages.domain.model

import kotlin.math.absoluteValue

/** The muted avatar colors the prototype ships with, picked deterministically per sender. */
object AvatarPalette {
    private val colors = listOf(
        0xFF5B6B8CL, 0xFF0F172AL, 0xFF334155L, 0xFF7C6A9CL, 0xFF64748BL, 0xFF8C7A6BL,
    )

    fun forSeed(seed: String): Long = colors[seed.hashCode().absoluteValue % colors.size]
}
