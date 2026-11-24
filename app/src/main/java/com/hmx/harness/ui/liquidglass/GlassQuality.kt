package com.hmx.harness.ui.liquidglass

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Rendering capability tier. Visual identity is identical on every tier; only
 * the number and cost of GPU layers changes.
 */
@Immutable
enum class GlassTier { LOW, MID, HIGH }

/** Short, smooth and interruptible motion; reduced further on low tier. */
@Immutable
data class GlassMotion(
    val shortMs: Int,
    val mediumMs: Int,
    val scale: Float,
)

@Immutable
data class GlassRenderProfile(
    val blurScale: Float,
    val lensScale: Float,
    val specularScale: Float,
    val shadowScale: Float,
    val useBackdropShader: Boolean,
    val useRefraction: Boolean,
    val innerHairline: Boolean,
    val motion: GlassMotion,
)

internal fun profileFor(tier: GlassTier): GlassRenderProfile = when (tier) {
    GlassTier.LOW -> GlassRenderProfile(
        blurScale = 0f, lensScale = 0f, specularScale = .55f, shadowScale = .5f,
        useBackdropShader = false, useRefraction = false, innerHairline = false,
        motion = GlassMotion(shortMs = 90, mediumMs = 160, scale = .85f))
    GlassTier.MID -> GlassRenderProfile(
        blurScale = .72f, lensScale = .65f, specularScale = .8f, shadowScale = .8f,
        useBackdropShader = true, useRefraction = true, innerHairline = false,
        motion = GlassMotion(shortMs = 130, mediumMs = 220, scale = .95f))
    GlassTier.HIGH -> GlassRenderProfile(
        blurScale = 1f, lensScale = 1f, specularScale = 1f, shadowScale = 1f,
        useBackdropShader = true, useRefraction = true, innerHairline = true,
        motion = GlassMotion(shortMs = 160, mediumMs = 280, scale = 1f))
}

/**
 * Detect once at theme creation. Uses platform signals rather than model names:
 * low-RAM flag, heap budget, core count, GL version and Android version.
 * Unknown values fall back to MID so the app still looks like the same design.
 */
internal fun detectGlassTier(context: Context): GlassTier {
    val memoryMb = runCatching { Runtime.getRuntime().maxMemory() / (1024L * 1024L) }.getOrDefault(0L)
    val cores = runCatching { Runtime.getRuntime().availableProcessors() }.getOrDefault(2)
    val lowRam = runCatching {
        @Suppress("DEPRECATION")
        (context.getSystemService(Context.ACTIVITY_SERVICE) as? android.app.ActivityManager)?.isLowRamDevice
    }.getOrDefault(null)
    val gles = runCatching {
        @Suppress("DEPRECATION")
        (context.getSystemService(Context.ACTIVITY_SERVICE) as? android.app.ActivityManager)
            ?.deviceConfigurationInfo?.reqGlEsVersion ?: 0
    }.getOrDefault(0)
    val vulkan = runCatching {
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_LEVEL)
    }.getOrDefault(false)

    if (lowRam == true || memoryMb in 1..255 || cores <= 4) return GlassTier.LOW
    if (memoryMb >= 512 && cores >= 8 && (gles >= 0x30002 || vulkan) && Build.VERSION.SDK_INT >= 33) {
        return GlassTier.HIGH
    }
    return GlassTier.MID
}

val LocalGlassTier = staticCompositionLocalOf { GlassTier.MID }
val LocalGlassProfile = staticCompositionLocalOf { profileFor(GlassTier.MID) }

/** Round a dp value once so low-end canvases draw fewer distinct sizes. */
internal fun Float.scaledDp(scale: Float): Dp = ((this * scale).roundToInt()).toFloat().dp
