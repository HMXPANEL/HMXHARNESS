package com.hmx.harness.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Corner radii for the Liquid Glass design system.
 *
 * Glass panels read as soft, floating surfaces: larger, consistent radii with
 * a calm progression. Small controls stay compact enough for dense settings
 * rows; true pills (composer, chips) are still created inline with CircleShape.
 */
val HarnessShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)
