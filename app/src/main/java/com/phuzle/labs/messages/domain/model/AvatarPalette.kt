package com.phuzle.labs.messages.domain.model

import kotlin.math.absoluteValue

/** Deterministic per-sender avatar colors — enough distinct hues that two senders in the same
 * inbox rarely read as "the same color", not just shades of the same blue-gray. All dark/saturated
 * enough to keep the white category icon (see AvatarBubble) legible on top. */
object AvatarPalette {
    private val colors = listOf(
        0xFF2563EBL, // blue
        0xFF0F766EL, // teal
        0xFF15803DL, // green
        0xFFB45309L, // amber
        0xFFC2410CL, // burnt orange
        0xFFBE123CL, // rose
        0xFFA21CAFL, // magenta
        0xFF7C3AEDL, // violet
        0xFF4F46E5L, // indigo
        0xFF334155L, // slate
        0xFF0369A1L, // sky blue
        0xFF854D0EL, // brown/gold
    )

    fun forSeed(seed: String): Long = colors[seed.hashCode().absoluteValue % colors.size]
}
