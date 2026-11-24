package com.hmx.harness.ui.liquidglass

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Cinematic, static background layer. No continuous animation: cheap on low-end
 * GPUs. This is background-only so Prismal can record it as the glass backdrop;
 * app content is drawn as a sibling above it.
 *
 * When [base] is supplied the layer reproduces the exact backdrop the kyant
 * engine paints (theme surface plus two accent glows), so glass sampled by
 * Prismal matches what the user actually sees instead of an unrelated gradient.
 */
@Composable
fun LiquidGlassAmbientBackground(
    modifier: Modifier = Modifier,
    base: Color? = null,
    accent: Color? = null,
    secondary: Color? = null,
) {
    val tokens = LocalGlassTokens.current
    val resolvedBase = base
    val resolvedAccent = accent ?: MaterialTheme.colorScheme.primary
    val resolvedSecondary = secondary ?: MaterialTheme.colorScheme.tertiary
    Box(modifier.fillMaxSize().drawBehind {
        val w = size.width
        val h = size.height
        if (resolvedBase != null) {
            drawRect(resolvedBase)
            drawRect(Brush.radialGradient(
                listOf(resolvedAccent.copy(alpha = .14f), Color.Transparent),
                center = Offset(w * .12f, h * .12f), radius = size.maxDimension * .7f))
            drawRect(Brush.radialGradient(
                listOf(resolvedSecondary.copy(alpha = .10f), Color.Transparent),
                center = Offset(w * .95f, h * .7f), radius = size.maxDimension * .6f))
            return@drawBehind
        }
        drawRect(Brush.verticalGradient(listOf(tokens.backgroundTop, tokens.backgroundBottom)))
        drawRect(Brush.radialGradient(
            listOf(resolvedAccent.copy(alpha = if (tokens.dark) .13f else .09f), Color.Transparent),
            center = Offset(w * .1f, h * .1f), radius = maxOf(w, h) * .75f))
        drawRect(Brush.radialGradient(
            listOf(resolvedSecondary.copy(alpha = if (tokens.dark) .09f else .07f), Color.Transparent),
            center = Offset(w * .95f, h * .74f), radius = maxOf(w, h) * .65f))
    })
}

/** Base glass container. Presentation only — no state, no callbacks. */
@Composable
fun LiquidGlassSurface(
    modifier: Modifier = Modifier,
    level: LiquidGlassLevel = LiquidGlassLevel.STANDARD,
    shape: Shape = RoundedCornerShape(20.dp),
    tonalAccent: Color = Color.Unspecified,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    opacity: GlassOpacity = GlassOpacity.PANEL,
    content: @Composable () -> Unit,
) {
    Box(modifier.liquidGlass(level, shape, tonalAccent, opacity = opacity).clip(shape)) {
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
            Box(Modifier.padding(contentPadding).fillMaxWidth(), contentAlignment = Alignment.TopStart) { content() }
        }
    }
}

/** Tappable glass tile/card. [onClick] is the existing screen callback, unchanged. */
@Composable
fun LiquidGlassCard(
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    level: LiquidGlassLevel = LiquidGlassLevel.STANDARD,
    shape: Shape = RoundedCornerShape(22.dp),
    accent: Color = Color.Unspecified,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    opacity: GlassOpacity = GlassOpacity.CARD,
    content: @Composable () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val clickable = if (onClick != null && enabled) Modifier.clickable(
        interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick) else Modifier
    Box(modifier
        .liquidGlass(if (enabled) level else LiquidGlassLevel.LIGHT, shape, accent, opacity = opacity)
        .clip(shape)
        .then(clickable)) {
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
            Box(Modifier.padding(contentPadding).fillMaxWidth()) { content() }
        }
    }
}

/** Primary action. [onClick] and [enabled] are passed straight through. */
@Composable
fun LiquidGlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accent: Color = LiquidGlassPalette.ElectricViolet,
    minHeight: Dp = 48.dp,
    content: @Composable RowScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier
            .heightIn(min = minHeight)
            .liquidGlass(if (enabled) LiquidGlassLevel.FOCUS else LiquidGlassLevel.LIGHT,
                RoundedCornerShape(18.dp), accent)
            .clip(RoundedCornerShape(18.dp))
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, enabled = enabled, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        content = {
            CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
                ProvideTextStyle(MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)) {
                    content()
                }
            }
        },
    )
}

/** Circular icon control with an accessible label. */
@Composable
fun LiquidGlassIconButton(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    level: LiquidGlassLevel = LiquidGlassLevel.STANDARD,
    content: @Composable () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier.size(44.dp)
            .liquidGlass(level, CircleShape)
            .clip(CircleShape)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button,
                enabled = enabled, onClickLabel = contentDescription, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) { content() }
    }
}

/** Selectable chip: visual selection state only; the caller owns the boolean. */
@Composable
fun LiquidGlassChip(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(50)
    Box(
        modifier.heightIn(min = 36.dp)
            .liquidGlass(if (selected) LiquidGlassLevel.FOCUS else LiquidGlassLevel.LIGHT, shape,
                if (selected) LiquidGlassPalette.SoftPurple else Color.Unspecified)
            .clip(shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides
            if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant) {
            ProvideTextStyle(MaterialTheme.typography.labelLarge) { content() }
        }
    }
}

/** Glass top toolbar. All slots are existing screen content/callbacks. */
@Composable
fun LiquidGlassToolbar(
    modifier: Modifier = Modifier,
    navigation: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier.fillMaxWidth()
            .liquidGlass(LiquidGlassLevel.DEEP, RoundedCornerShape(24.dp), opacity = GlassOpacity.CAPSULE)
            .clip(RoundedCornerShape(24.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (navigation != null) Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) { navigation() }
        Row(Modifier.weight(1f).padding(horizontal = 8.dp)) { title?.invoke() }
        Row(verticalAlignment = Alignment.CenterVertically, content = actions)
    }
}

/** Dialog/panel surface without Material's opaque default container. */
@Composable
fun LiquidGlassDialogSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(28.dp),
    content: @Composable () -> Unit,
) {
    Box(modifier.liquidGlass(LiquidGlassLevel.DEEP, shape, opacity = GlassOpacity.DIALOG).clip(shape)) {
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) { content() }
    }
}

/** Section heading with restrained letterspacing and readable contrast. */
@Composable
fun LiquidGlassSectionHeader(
    text: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        trailing?.invoke()
    }
}

/** Small status pill used by provider/model/agent state indicators. */
@Composable
fun LiquidGlassStatusPill(
    text: String,
    modifier: Modifier = Modifier,
    active: Boolean = false,
) {
    Box(modifier.liquidGlass(if (active) LiquidGlassLevel.FOCUS else LiquidGlassLevel.LIGHT, RoundedCornerShape(50),
        if (active) LiquidGlassPalette.ElectricViolet else Color.Unspecified)
        .clip(RoundedCornerShape(50))
        .padding(horizontal = 10.dp, vertical = 4.dp)) {
        Text(text, style = MaterialTheme.typography.labelSmall,
            color = if (active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Stacked glass panel used by lists of settings/providers on wide layouts. */
@Composable
fun LiquidGlassGroup(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier.fillMaxWidth().liquidGlass(LiquidGlassLevel.LIGHT, RoundedCornerShape(24.dp))
        .clip(RoundedCornerShape(24.dp)).padding(6.dp), content = content)
}

/*
 * Shared names for the one material system above. These are thin, presentation
 * only aliases so screens speak the same vocabulary; they add no new renderer.
 */

@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    level: LiquidGlassLevel = LiquidGlassLevel.STANDARD,
    shape: Shape = RoundedCornerShape(20.dp),
    opacity: GlassOpacity = GlassOpacity.PANEL,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable () -> Unit,
) = LiquidGlassSurface(modifier, level, shape, Color.Unspecified, contentPadding, opacity, content)

@Composable
fun GlassCard(
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    level: LiquidGlassLevel = LiquidGlassLevel.STANDARD,
    shape: Shape = RoundedCornerShape(22.dp),
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable () -> Unit,
) = LiquidGlassCard(onClick, modifier, level, shape, Color.Unspecified, enabled, contentPadding, GlassOpacity.CARD, content)

@Composable
fun GlassToolbar(
    modifier: Modifier = Modifier,
    navigation: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) = LiquidGlassToolbar(modifier, navigation, title, actions)

@Composable
fun GlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) = LiquidGlassButton(onClick, modifier, enabled, LiquidGlassPalette.ElectricViolet, 48.dp, content)

@Composable
fun GlassIconButton(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) = LiquidGlassIconButton(onClick, contentDescription, modifier, enabled, LiquidGlassLevel.STANDARD, content)

@Composable
fun GlassChip(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) = LiquidGlassChip(selected, onClick, modifier, content)

@Composable
fun GlassNavigationDrawer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) = LiquidGlassSurface(modifier, LiquidGlassLevel.DEEP, RoundedCornerShape(32.dp), Color.Unspecified,
    PaddingValues(0.dp), GlassOpacity.DRAWER, content)

@Composable
fun GlassPopupMenu(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(28.dp),
    content: @Composable () -> Unit,
) = LiquidGlassSurface(modifier, LiquidGlassLevel.DEEP, shape, Color.Unspecified,
    PaddingValues(6.dp), GlassOpacity.MENU, content)

@Composable
fun GlassDialog(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(28.dp),
    content: @Composable () -> Unit,
) = LiquidGlassDialogSurface(modifier, shape, content)

@Composable
fun GlassInput(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    contentPadding: PaddingValues = PaddingValues(12.dp),
    content: @Composable () -> Unit,
) = LiquidGlassSurface(modifier, LiquidGlassLevel.DEEP, shape, Color.Unspecified,
    contentPadding, GlassOpacity.COMPOSER, content)

@Composable
fun GlassBottomBar(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) = Row(modifier.fillMaxWidth().liquidGlass(LiquidGlassLevel.DEEP, RoundedCornerShape(24.dp),
    opacity = GlassOpacity.CAPSULE).clip(RoundedCornerShape(24.dp)).padding(horizontal = 8.dp, vertical = 6.dp),
    verticalAlignment = Alignment.CenterVertically, content = content)
