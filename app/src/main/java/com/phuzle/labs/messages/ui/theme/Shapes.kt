package com.phuzle.labs.messages.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** The design system's two corner radii — no pill shapes, no elevation anywhere. */
val ShapeSmall = RoundedCornerShape(8.dp)
val ShapeMedium = RoundedCornerShape(12.dp)

val MessagesShapes = Shapes(
    extraSmall = ShapeSmall,
    small = ShapeSmall,
    medium = ShapeMedium,
    large = ShapeMedium,
    extraLarge = ShapeMedium,
)
