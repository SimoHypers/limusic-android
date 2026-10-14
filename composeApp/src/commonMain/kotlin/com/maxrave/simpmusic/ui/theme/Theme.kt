package com.maxrave.simpmusic.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.unit.dp
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.RippleConfiguration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.materialkolor.PaletteStyle
import com.materialkolor.rememberDynamicColorScheme
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.simpmusic.expect.ui.SystemBarAppearanceEffect
import com.maxrave.simpmusic.expect.ui.platformDynamicColorScheme

/**
 * Semantic colors that sit outside the Material 3 ColorScheme.
 * Read them via [LocalAppColors] inside composables.
 */
@Immutable
data class AppColors(
    val favorite: Color,
    val lyricActive: Color,
    val shimmerBackground: Color,
    val shimmerLine: Color,
    val overlay: Color,
    val overlayHeavy: Color,
)

private val DarkAppColors =
    AppColors(
        favorite = favoriteColor,
        lyricActive = lyricActiveColor,
        shimmerBackground = shimmerBackground,
        shimmerLine = shimmerLine,
        overlay = overlay,
        overlayHeavy = blackMoreOverlay,
    )

// Overlays stay dark in both themes: they cover artwork, where content is always light.
private val LightAppColors =
    DarkAppColors.copy(
        shimmerBackground = shimmerBackgroundLight,
        shimmerLine = shimmerLineLight,
    )

val LocalAppColors = staticCompositionLocalOf { DarkAppColors }

/** True in dark theme. Provided by [AppTheme] so platform glass (blur/tint) can adapt reliably. */
val LocalIsDarkTheme = staticCompositionLocalOf { true }

/**
 * Whether liquid-glass surfaces may actually draw glass.
 *
 * Provided by [AppTheme] from the user's setting; the default is true so previews and anything
 * composed outside [AppTheme] keep today's look. The setting row is Android-only, so Desktop always
 * provides true — its capsule player and detail-screen buttons are glass by design, with no switch.
 */
val LocalLiquidGlassEnabled = staticCompositionLocalOf { true }

/**
 * The dark scheme to use for immersive screens while the app itself is on the light theme.
 * Provided by [AppTheme], consumed by [ForceDarkContent]; null only outside of [AppTheme].
 */
val LocalForcedDarkColorScheme = staticCompositionLocalOf<ColorScheme?> { null }

/** Parses "RRGGBB" or "AARRGGBB" (optionally "#"-prefixed) into a [Color]; null if malformed. */
fun parseThemeColorHex(hex: String): Color? {
    val clean = hex.trim().removePrefix("#")
    val argb =
        when (clean.length) {
            6 -> "FF$clean"
            8 -> clean
            else -> return null
        }
    return argb.toLongOrNull(16)?.let { Color(it) }
}

/** Limusic's dark palette as a Material 3 scheme: tinted near-black neutrals and a crimson accent. */
internal val LimusicDarkColorScheme: ColorScheme =
    darkColorScheme(
        primary = LimusicDark.primary,
        onPrimary = LimusicDark.onPrimary,
        primaryContainer = LimusicDark.primaryContainer,
        onPrimaryContainer = LimusicDark.onPrimaryContainer,
        inversePrimary = LimusicLight.primary,
        secondary = LimusicDark.mutedForeground,
        onSecondary = LimusicDark.background,
        secondaryContainer = LimusicDark.secondary,
        onSecondaryContainer = LimusicDark.foreground,
        tertiary = LimusicDark.primary,
        onTertiary = LimusicDark.onPrimary,
        tertiaryContainer = LimusicDark.primaryContainer,
        onTertiaryContainer = LimusicDark.onPrimaryContainer,
        background = LimusicDark.background,
        onBackground = LimusicDark.foreground,
        surface = LimusicDark.background,
        onSurface = LimusicDark.foreground,
        surfaceVariant = LimusicDark.muted,
        onSurfaceVariant = LimusicDark.mutedForeground,
        surfaceTint = LimusicDark.primary,
        inverseSurface = LimusicDark.foreground,
        inverseOnSurface = LimusicDark.background,
        error = LimusicDark.destructive,
        onError = LimusicDark.background,
        outline = LimusicDark.ring,
        outlineVariant = LimusicDark.secondary,
        scrim = Color.Black,
        surfaceBright = LimusicDark.surfaceHighest,
        surfaceDim = LimusicDark.background,
        surfaceContainerLowest = LimusicDark.surfaceLowest,
        surfaceContainerLow = LimusicDark.surfaceLow,
        surfaceContainer = LimusicDark.surface,
        surfaceContainerHigh = LimusicDark.card,
        surfaceContainerHighest = LimusicDark.surfaceHighest,
    )

/** Limusic's light palette: white, faintly tinted greys and the same crimson. */
internal val LimusicLightColorScheme: ColorScheme =
    lightColorScheme(
        primary = LimusicLight.primary,
        onPrimary = LimusicLight.onPrimary,
        primaryContainer = LimusicLight.primaryContainer,
        onPrimaryContainer = LimusicLight.onPrimaryContainer,
        inversePrimary = LimusicDark.primary,
        secondary = LimusicLight.mutedForeground,
        onSecondary = LimusicLight.background,
        secondaryContainer = LimusicLight.secondary,
        onSecondaryContainer = LimusicLight.onSecondary,
        tertiary = LimusicLight.primary,
        onTertiary = LimusicLight.onPrimary,
        tertiaryContainer = LimusicLight.primaryContainer,
        onTertiaryContainer = LimusicLight.onPrimaryContainer,
        background = LimusicLight.background,
        onBackground = LimusicLight.foreground,
        surface = LimusicLight.background,
        onSurface = LimusicLight.foreground,
        surfaceVariant = LimusicLight.muted,
        onSurfaceVariant = LimusicLight.mutedForeground,
        surfaceTint = LimusicLight.primary,
        inverseSurface = LimusicLight.foreground,
        inverseOnSurface = LimusicLight.background,
        error = LimusicLight.destructive,
        onError = LimusicLight.background,
        outline = LimusicLight.ring,
        outlineVariant = LimusicLight.border,
        scrim = Color.Black,
        surfaceBright = LimusicLight.background,
        surfaceDim = LimusicLight.border,
        surfaceContainerLowest = LimusicLight.background,
        surfaceContainerLow = LimusicLight.muted,
        surfaceContainer = LimusicLight.secondary,
        surfaceContainerHigh = LimusicLight.muted,
        surfaceContainerHighest = LimusicLight.border,
    )

/** Limusic's corner radii: the desktop's 0.45rem for small parts, growing for cards and sheets. */
internal val LimusicShapes =
    Shapes(
        extraSmall = RoundedCornerShape(4.dp),
        small = RoundedCornerShape(7.dp),
        medium = RoundedCornerShape(10.dp),
        large = RoundedCornerShape(14.dp),
        extraLarge = RoundedCornerShape(20.dp),
    )

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AppTheme(
    themeMode: String = DataStoreManager.THEME_MODE_DARK,
    themeColorSource: String = DataStoreManager.THEME_COLOR_DEFAULT,
    customThemeColor: Color? = null,
    liquidGlassEnabled: Boolean = true,
    content:
        @Composable()
        () -> Unit,
) {
    val isDark = isDarkTheme(themeMode)
    val wallpaperScheme =
        if (themeColorSource == DataStoreManager.THEME_COLOR_WALLPAPER) {
            platformDynamicColorScheme(isDark)
        } else {
            null
        }
    val seedColor =
        if (themeColorSource == DataStoreManager.THEME_COLOR_CUSTOM) {
            customThemeColor ?: seed
        } else {
            seed
        }
    // The default is Limusic's own palette. A wallpaper or custom colour generates a scheme from
    // that seed instead, on Limusic's neutrals so the app keeps its ground either way.
    val limusicScheme = if (isDark) LimusicDarkColorScheme else LimusicLightColorScheme
    val colorScheme =
        when {
            wallpaperScheme != null -> wallpaperScheme.withLimusicNeutrals(isDark)
            themeColorSource == DataStoreManager.THEME_COLOR_CUSTOM ->
                rememberDynamicColorScheme(
                    seedColor = seedColor,
                    isDark = isDark,
                    style = PaletteStyle.TonalSpot,
                    modifyColorScheme = { it.withLimusicNeutrals(isDark) },
                )
            else -> limusicScheme
        }
    // Immersive screens stay dark even at light theme (see [ForceDarkContent]).
    val forcedDarkScheme =
        if (isDark) {
            colorScheme
        } else if (themeColorSource == DataStoreManager.THEME_COLOR_DEFAULT) {
            LimusicDarkColorScheme
        } else {
            rememberDynamicColorScheme(
                seedColor = seedColor,
                isDark = true,
                style = PaletteStyle.TonalSpot,
                modifyColorScheme = { it.withLimusicNeutrals(true) },
            )
        }
    SystemBarAppearanceEffect(isDark)
    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        shapes = LimusicShapes,
        content = {
            CompositionLocalProvider(
                LocalRippleConfiguration provides SoftRippleConfiguration,
                LocalContentColor provides colorScheme.onSurfaceVariant,
                LocalAppColors provides if (isDark) DarkAppColors else LightAppColors,
                LocalIsDarkTheme provides isDark,
                LocalForcedDarkColorScheme provides forcedDarkScheme,
                LocalLiquidGlassEnabled provides liquidGlassEnabled,
                content = content,
            )
        },
        typography = typo(colorScheme),
    )
}

/**
 * A quieter ripple than the Material default, applied app-wide.
 *
 * Material 3's own alphas are tuned for light surfaces; on the near-black background this app
 * uses they read as a grey flash rather than a touch response. Each value is roughly 40% of
 * the default — enough to register the touch, not enough to wash the surface.
 *
 * The colour is left unspecified on purpose, so the ripple keeps deriving from LocalContentColor
 * and stays correct inside [ForceDarkContent] subtrees, which run a different scheme.
 *
 * This reaches bare `Modifier.clickable` too, not just Material components: MaterialTheme
 * already provides `material3.ripple()` as LocalIndication, and that is the one indication that
 * reads this configuration. Nothing extra has to be provided for it.
 *
 * The (color, rippleAlpha) constructor is deprecated in favour of one that also takes a `focus`,
 * but that three-argument version is internal to material3, and the two public replacements —
 * (color) and (focus, color) — drop rippleAlpha entirely. Adjusting ripple alpha therefore has
 * no non-deprecated path in this version, so the warning is suppressed rather than designed
 * around. Revisit once material3 opens up the full constructor.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Suppress("DEPRECATION")
private val SoftRippleConfiguration =
    RippleConfiguration(
        // Colour is deliberately left alone. Ripple derives it from LocalContentColor, which is
        // light on the dark theme and dark on the light one — always contrasting with what it
        // covers. Pinning a dark grey here instead made the ripple paint *darker* than the black
        // surface underneath, so a tap read as a sooty smudge rather than a highlight.
        // Alpha is the only lever that softens without changing which way the contrast runs.
        rippleAlpha =
            RippleAlpha(
                draggedAlpha = 0.06f,
                focusedAlpha = 0.04f,
                hoveredAlpha = 0.03f,
                pressedAlpha = 0.04f,
            ),
    )

/**
 * Wraps immersive screens — artist, album, playlist, local playlist, podcast and the players — which
 * always draw over dark artwork and must therefore render dark even on the light theme.
 *
 * [LocalForceDarkText] alone is not enough: it only recolors text through [typo]. Icons, buttons and
 * every other Material default read [LocalContentColor] and [MaterialTheme.colorScheme], which at
 * light theme resolve to dark-on-light and turn grey and unreadable over the artwork. Providing the
 * dark scheme here fixes the whole subtree in one place.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ForceDarkContent(content: @Composable () -> Unit) {
    val darkScheme = LocalForcedDarkColorScheme.current ?: MaterialTheme.colorScheme
    MaterialExpressiveTheme(
        colorScheme = darkScheme,
        shapes = LimusicShapes,
        content = {
            CompositionLocalProvider(
                LocalForceDarkText provides true,
                LocalIsDarkTheme provides true,
                LocalContentColor provides darkScheme.onSurfaceVariant,
                LocalAppColors provides DarkAppColors,
                content = content,
            )
        },
        typography = typo(darkScheme, forceDark = true),
    )
}

/** Keeps a generated scheme's accents but puts it on Limusic's background and surface ramp. */
private fun ColorScheme.withLimusicNeutrals(isDark: Boolean): ColorScheme {
    val base = if (isDark) LimusicDarkColorScheme else LimusicLightColorScheme
    return copy(
        background = base.background,
        onBackground = base.onBackground,
        surface = base.surface,
        onSurface = base.onSurface,
        surfaceVariant = base.surfaceVariant,
        onSurfaceVariant = base.onSurfaceVariant,
        surfaceBright = base.surfaceBright,
        surfaceDim = base.surfaceDim,
        surfaceContainerLowest = base.surfaceContainerLowest,
        surfaceContainerLow = base.surfaceContainerLow,
        surfaceContainer = base.surfaceContainer,
        surfaceContainerHigh = base.surfaceContainerHigh,
        surfaceContainerHighest = base.surfaceContainerHighest,
        outline = base.outline,
        outlineVariant = base.outlineVariant,
        inverseSurface = base.inverseSurface,
        inverseOnSurface = base.inverseOnSurface,
    )
}

/**
 * Resolves the stored theme mode to a plain boolean.
 *
 * Pulled out of [AppTheme] so chrome living *outside* it can ask the same question — the desktop
 * title bar is drawn before AppTheme is entered, so it has no MaterialTheme to read and would
 * otherwise need its own copy of this `when`.
 */
@Composable
fun isDarkTheme(themeMode: String): Boolean =
    when (themeMode) {
        DataStoreManager.THEME_MODE_LIGHT -> false
        DataStoreManager.THEME_MODE_SYSTEM -> isSystemInDarkTheme()
        else -> true
    }
