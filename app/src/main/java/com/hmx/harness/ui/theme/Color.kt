package com.hmx.harness.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Google-blue–seeded Material 3 palettes.
 *
 * These are the fallback schemes used when wallpaper dynamic color is unavailable or
 * disabled (pre-Android 12 devices, or dynamicColor off). The seed is the Google Blue
 * (#0B57D0) used across Google's Material 3 apps.
 *
 * The tertiary ramp is deliberately a neutral slate instead of the default purple,
 * the UI does not use tertiary for branding, and a purple accent leaking through
 * chips/badges is exactly what made the old design feel noisy.
 */

internal val LightColors = lightColorScheme(
    primary = Color(0xFF0B57D0),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD4E3FF),
    onPrimaryContainer = Color(0xFF001A41),
    secondary = Color(0xFF555F71),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD9E2F9),
    onSecondaryContainer = Color(0xFF121C2B),
    tertiary = Color(0xFF4A5560),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFD9DFE6),
    onTertiaryContainer = Color(0xFF141B22),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF9F9FF),
    onBackground = Color(0xFF1A1B20),
    surface = Color(0xFFFAFAFC),
    onSurface = Color(0xFF191C20),
    surfaceVariant = Color(0xFFE0E2EC),
    onSurfaceVariant = Color(0xFF44474E),
    outline = Color(0xFF75777F),
    outlineVariant = Color(0xFFC4C6D0),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF3F4FA),
    surfaceContainer = Color(0xFFEDEEF4),
    surfaceContainerHigh = Color(0xFFE7E9EF),
    surfaceContainerHighest = Color(0xFFE2E2E9),
    inverseSurface = Color(0xFF2E3036),
    inverseOnSurface = Color(0xFFF1F0F7),
    inversePrimary = Color(0xFFA8C7FA),
    scrim = Color(0xFF000000),
)

/**
 * HMXHARNESS Liquid Glass dark scheme.
 *
 * Deep midnight / violet surfaces carry a small alpha so the cinematic ambient
 * background shows through on every screen. Foreground roles stay near-opaque
 * and high-contrast: glass must never cost readability.
 * This replaces the previous Google-blue seeded dark scheme as the default;
 * dynamic color, custom palettes and AMOLED mode keep their own behavior.
 */
internal val DarkColors = darkColorScheme(
    primary = Color(0xFF9575FF),
    onPrimary = Color(0xFF120A24),
    primaryContainer = Color(0xB8332459),
    onPrimaryContainer = Color(0xFFEEE7FF),
    secondary = Color(0xFF8FD6FF),
    onSecondary = Color(0xFF07172A),
    secondaryContainer = Color(0xB8243554),
    onSecondaryContainer = Color(0xFFE3F4FF),
    tertiary = Color(0xFFC4A8FF),
    onTertiary = Color(0xFF1C1033),
    tertiaryContainer = Color(0xB8332852),
    onTertiaryContainer = Color(0xFFF2EAFF),
    error = Color(0xFFFF9A9A),
    onError = Color(0xFF3B0710),
    errorContainer = Color(0xB8672333),
    onErrorContainer = Color(0xFFFFE2E2),
    background = Color(0xFF090713),
    onBackground = Color(0xFFF7F5FF),
    surface = Color(0xCC151027),
    onSurface = Color(0xFFF7F5FF),
    surfaceVariant = Color(0xB82C2447),
    onSurfaceVariant = Color(0xFFC9C2DE),
    outline = Color(0xFF6F6689),
    outlineVariant = Color(0xB8544A6E),
    surfaceContainerLowest = Color(0xE6090713),
    surfaceContainerLow = Color(0xCC1B1530),
    surfaceContainer = Color(0xC4221B3A),
    surfaceContainerHigh = Color(0xC42C2348),
    surfaceContainerHighest = Color(0xC4382C56),
    inverseSurface = Color(0xFFF4F0FF),
    inverseOnSurface = Color(0xFF1A1330),
    inversePrimary = Color(0xFF6D4AFF),
    scrim = Color(0xFF04020C),
)

/**
 * AMOLED surfaces: the dark scheme with every background role at (or near)
 * true black, where OLED pixels are fully off. Accent and content roles come
 * from the base scheme untouched, so this works over both the static palette
 * and a wallpaper-derived dynamic one.
 */
internal fun ColorScheme.amoledSurfaces(): ColorScheme = copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceVariant = Color(0xFF1B1B1F),
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF0A0A0C),
    surfaceContainer = Color(0xFF101014),
    surfaceContainerHigh = Color(0xFF16161A),
    surfaceContainerHighest = Color(0xFF1D1D22),
    outlineVariant = Color(0xFF2C2C31),
)

/**
 * The only hardcoded hues in the app: status colors.
 *
 * M3 has no success/warning roles, and status must read the same regardless of the
 * (dynamic, wallpaper-derived) accent, a green "done" and a red "failed" are
 * universal. Everything else in the UI uses theme roles only.
 */
class StatusColors(val success: Color, val warning: Color)

internal val StatusLight = StatusColors(
    success = Color(0xFF157A3E),
    warning = Color(0xFF9A6A00),
)

internal val StatusDark = StatusColors(
    success = Color(0xFF83D69F),
    warning = Color(0xFFF0C97A),
)

val LocalStatusColors = staticCompositionLocalOf { StatusLight }
