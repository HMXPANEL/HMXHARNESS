package com.hmx.harness.ui.liquidglass

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Theme-aware material tokens for the Liquid Glass system.
 *
 * The glass is neutral and background-aware: dark mode uses charcoal/slate
 * tint rather than white, light mode uses a soft neutral tint rather than a
 * bright white overlay. Edge lighting and specular sweeps are deliberately
 * low intensity so the material reads as transparent glass instead of a
 * milky card with a glowing outline.
 */
@Immutable
data class GlassTokens(
    val dark: Boolean,
    val backgroundTop: Color,
    val backgroundBottom: Color,
    val surfaceTint: Color,
    val surfaceStrong: Color,
    val edge: Color,
    val specular: Color,
    val shadow: Color,
    /** Body tint alpha for neutral cards and list rows. */
    val cardFillAlpha: Float,
    /** Toolbars and floating capsules: stronger so moving text never bleeds. */
    val capsuleFillAlpha: Float,
    /** Navigation drawer: mostly settled, still faintly translucent. */
    val drawerFillAlpha: Float,
    /** Popup menus: strongly separated from the app behind them. */
    val menuFillAlpha: Float,
    /** Dialogs and bottom sheets: the strongest material in the system. */
    val dialogFillAlpha: Float,
    /** Input composer: calm base for typed text. */
    val composerFillAlpha: Float,
    /** Border treatment for all levels (kept hairline and tinted). */
    val edgeAlpha: Float,
    val specularAlpha: Float,
    val shadowAlpha: Float,
    /** Dim applied to an intentional workspace background image. */
    val canvasDimAlpha: Float,
    val canvasBlurDp: Float,
)

internal fun glassTokensFor(dark: Boolean): GlassTokens = if (dark) {
    GlassTokens(
        dark = true,
        backgroundTop = Color(0xFF101216),
        backgroundBottom = Color(0xFF08090C),
        surfaceTint = Color(0xFF1B1E25),
        surfaceStrong = Color(0xFF15181E),
        edge = Color(0xFFB9C2D0),
        specular = Color(0xFFE9EDF4),
        shadow = Color(0xFF000000),
        cardFillAlpha = .46f,
        capsuleFillAlpha = .66f,
        drawerFillAlpha = .88f,
        menuFillAlpha = .86f,
        dialogFillAlpha = .90f,
        composerFillAlpha = .70f,
        edgeAlpha = .14f,
        specularAlpha = .10f,
        shadowAlpha = .55f,
        canvasDimAlpha = .62f,
        canvasBlurDp = 14f,
    )
} else {
    GlassTokens(
        dark = false,
        backgroundTop = Color(0xFFF7F8FA),
        backgroundBottom = Color(0xFFECEEF2),
        surfaceTint = Color(0xFFFFFFFF),
        surfaceStrong = Color(0xFFF6F7F9),
        edge = Color(0xFF66707F),
        specular = Color(0xFFFFFFFF),
        shadow = Color(0xFF1B1F27),
        cardFillAlpha = .50f,
        capsuleFillAlpha = .72f,
        drawerFillAlpha = .90f,
        menuFillAlpha = .88f,
        dialogFillAlpha = .92f,
        composerFillAlpha = .74f,
        edgeAlpha = .16f,
        specularAlpha = .24f,
        shadowAlpha = .14f,
        canvasDimAlpha = .42f,
        canvasBlurDp = 12f,
    )
}

val LocalGlassTokens = staticCompositionLocalOf { glassTokensFor(dark = true) }
