package com.hmx.harness.ui.liquidglass

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.platform.LocalDensity
import com.styropyr0.prismal.PrismalGlass
import com.styropyr0.prismal.depth.PrismalDepthShadow
import com.styropyr0.prismal.drawPrismalGlass
import com.styropyr0.prismal.effects.applyPrismalGlassEffects
import com.styropyr0.prismal.shapes.PrismalCapsule
import com.styropyr0.prismal.shapes.PrismalRoundedRectangle
import com.styropyr0.prismal.sources.PrismalGlassLayer
import com.styropyr0.prismal.specular.PrismalSpecular
import com.styropyr0.prismal.specular.PrismalSpecularStyle
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** The app's sampled backdrop, provided once at the theme root (PrismalAGSL). */
val LocalHmxGlassBackdrop = staticCompositionLocalOf<PrismalGlassLayer?> { null }

/**
 * Lightweight, always-on glass material.
 *
 * A translucent solid color is not glass: this draws a layered vertical tint
 * gradient, a top specular sweep, a refracted light pool, a luminous gradient
 * edge and a soft shadow. It never samples or blurs the backdrop, so it costs
 * no offscreen texture and is safe on mid-range devices. When the existing
 * backdrop engine is enabled it stacks on top of the real blur/lens material.
 */
@Composable
fun Modifier.liquidGlass(
    level: LiquidGlassLevel = LiquidGlassLevel.STANDARD,
    shape: Shape,
    accent: Color = Color.Unspecified,
    elevation: Dp? = null,
    cornerRadius: Dp = 20.dp,
    opacity: GlassOpacity = GlassOpacity.AUTO,
): Modifier = composed {
    val backdrop = LocalHmxGlassBackdrop.current
    val density = LocalDensity.current
    val tokens = LocalGlassTokens.current
    val profile = LocalGlassProfile.current
    val depth = depthOf(level, tokens)
    val tint = accent.takeOrElse { tokens.surfaceTint }
    val lift = elevation ?: depth.elevation.dp

    // REAL LIQUID GLASS — PrismalAGSL samples the recorded backdrop, applies a
    // backdrop blur, an edge lens (optical refraction + chromatic dispersion)
    // and specular/depth layers. It degrades automatically on older APIs.
    if (backdrop != null && PrismalGlass.isSupported() && profile.useBackdropShader) {
        val glassShape: () -> androidx.compose.ui.graphics.Shape =
            if (shape == CircleShape) ({ PrismalCapsule() })
            else ({ PrismalRoundedRectangle(cornerRadius) })
        val blurPx = (when (level) {
            LiquidGlassLevel.LIGHT -> 5.dp
            LiquidGlassLevel.STANDARD -> 10.dp
            LiquidGlassLevel.DEEP -> 13.dp
            LiquidGlassLevel.FOCUS -> 9.dp
        }) * profile.blurScale
        val lensHeight = (when (level) {
            LiquidGlassLevel.LIGHT -> 7.dp
            LiquidGlassLevel.STANDARD -> 12.dp
            LiquidGlassLevel.DEEP -> 15.dp
            LiquidGlassLevel.FOCUS -> 17.dp
        }) * profile.lensScale
        val lensAmount = (when (level) {
            LiquidGlassLevel.LIGHT -> 12.dp
            LiquidGlassLevel.STANDARD -> 22.dp
            LiquidGlassLevel.DEEP -> 27.dp
            LiquidGlassLevel.FOCUS -> 30.dp
        }) * profile.lensScale
        val dispersion = if (profile.useRefraction) (if (level == LiquidGlassLevel.FOCUS) 0.24f else 0.14f) else 0f
        val surfaceAlpha = opacityFor(opacity, level, tokens)
        // Low-intensity neutral specular: a faint slate rim in dark mode and a
        // soft neutral sheen in light mode. Never a bright white outline.
        val specularAlpha = (if (tokens.dark) .20f else .34f) * profile.specularScale
        return@composed this.drawPrismalGlass(
            backdrop = backdrop,
            shape = glassShape,
            effects = {
                applyPrismalGlassEffects(
                    density = density,
                    adaptiveLuminance = false,
                    luminance = .5f,
                    blurRadiusPx = with(density) { blurPx.toPx() },
                    refractionHeightPx = with(density) { lensHeight.toPx() },
                    refractionAmountPx = with(density) { lensAmount.toPx() },
                    chromaticAberration = dispersion,
                    brightness = 0f,
                    saturation = 1.12f,
                )
            },
            specular = {
                PrismalSpecular(
                    width = .6.dp,
                    alpha = specularAlpha,
                    style = PrismalSpecularStyle.Default(
                        color = tokens.edge.copy(alpha = .55f), falloff = 1.5f),
                )
            },
            depthShadow = {
                PrismalDepthShadow(
                    radius = 16.dp * profile.shadowScale,
                    color = tokens.shadow.copy(alpha = tokens.shadowAlpha * .28f),
                )
            },
            onDrawSurface = {
                drawRect(tint.copy(alpha = surfaceAlpha))
            },
        )
    }

    // FALLBACK — lightweight layered gradient material. Same tint, edges and
    // purpose-driven alpha as the shader path; only the GPU work is removed.
    val bodyAlpha = opacityFor(opacity, level, tokens)
    this
        .shadow(lift, shape, clip = false,
            ambientColor = tokens.shadow.copy(alpha = tokens.shadowAlpha * .55f),
            spotColor = tokens.shadow.copy(alpha = tokens.shadowAlpha * .35f))
        .drawBehind {
            val w = size.width
            val h = size.height
            if (w <= 0f || h <= 0f) return@drawBehind

            // 1. Body: neutral translucent tint, slightly darker at the base.
            drawRoundShape(shape, Brush.verticalGradient(
                0f to tint.copy(alpha = bodyAlpha * 1.05f),
                .55f to tint.copy(alpha = bodyAlpha),
                1f to tokens.surfaceStrong.copy(alpha = bodyAlpha * .82f)))

            // 2. Specular light sweeping the top edge — faint, not a white cap.
            drawRoundShape(shape, Brush.verticalGradient(
                0f to tokens.specular.copy(alpha = depth.highlightAlpha),
                .16f to Color.Transparent))

            // 3. A barely visible neutral refraction pool.
            drawRoundShape(shape, Brush.radialGradient(
                listOf(tokens.specular.copy(alpha = .045f), Color.Transparent),
                center = Offset(w * .82f, h * .12f), radius = maxOf(w, h) * .9f))

            // 4. Hairline edge: neutral, low alpha, never a glowing white border.
            drawRoundShape(shape, Brush.linearGradient(
                0f to tokens.edge.copy(alpha = depth.borderAlpha),
                .5f to tokens.edge.copy(alpha = depth.borderAlpha * .55f),
                1f to tokens.edge.copy(alpha = depth.borderAlpha * .8f)),
                stroke = Stroke(width = if (profile.innerHairline) .8f else .6f))

            // 5. Inner hairline: a second, tighter reflection on capable GPUs only.
            if (profile.innerHairline) {
                drawRoundShape(shape, SolidColor(tokens.specular.copy(alpha = depth.highlightAlpha * .22f)),
                    stroke = Stroke(width = .5f), inset = 1.5f)
            }
        }
}

private fun DrawScope.drawRoundShape(
    shape: Shape,
    brush: Brush,
    stroke: Stroke? = null,
    inset: Float = 0f,
) {
    val rect = Rect(inset, inset, size.width - inset, size.height - inset)
    if (rect.width <= 0f || rect.height <= 0f) return
    val outline = shape.createOutline(rect.size, layoutDirection, this)
    drawOutline(outline, brush, 1f, style = stroke ?: Fill)
}
