package com.phuzle.labs.messages.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** One UI-leaning corner radii: bigger, softer rounding on cards, full pill shapes on buttons/chips. */
val ShapeSmall = RoundedCornerShape(14.dp)
val ShapeMedium = RoundedCornerShape(20.dp)
val ShapeLarge = RoundedCornerShape(28.dp)

/** True stadium/pill shape (rounds to a half-circle regardless of height) for buttons and chips. */
val ShapePill = RoundedCornerShape(50)

val MessagesShapes = Shapes(
    extraSmall = ShapeSmall,
    small = ShapeSmall,
    medium = ShapeMedium,
    large = ShapeLarge,
    extraLarge = ShapeLarge,
)
