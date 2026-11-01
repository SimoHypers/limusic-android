package com.simohypers.limusic.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.text.TextStyle
import androidx.compose.material3.LocalTextStyle
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
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import com.materialkolor.hct.Hct
import com.materialkolor.PaletteStyle
import com.materialkolor.rememberDynamicColorScheme
import com.simohypers.limusic.domain.manager.DataStoreManager
import com.simohypers.limusic.expect.ui.SystemBarAppearanceEffect
import com.simohypers.limusic.expect.ui.platformDynamicColorScheme
import kotlin.math.sqrt

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
 * The dark scheme to use for immersive screens while the app itself is on the light theme.
 * Provided by [AppTheme], consumed by [ForceDarkContent]; null only outside of [AppTheme].
 */
val LocalForcedDarkColorScheme = staticCompositionLocalOf<ColorScheme?> { null }

/**
 * True while "Adapt colors to artwork" has a cover colour, so the app's own schemes (including the
 * forced dark one the players get) already come from the playing cover.
 */
val LocalArtworkColors = staticCompositionLocalOf { false }

/** The app's own scheme and whether it is dark, so [AppThemeContent] can undo [ForceDarkContent]. */
private val LocalAppScheme = staticCompositionLocalOf<Pair<ColorScheme, Boolean>?> { null }

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
internal val LimusicShapes
    get() =
        Shapes(
            extraSmall = rounded(4.dp),
            small = rounded(7.dp),
            medium = rounded(10.dp),
            large = rounded(14.dp),
            extraLarge = rounded(20.dp),
        )

/**
 * The Roundness setting as a multiple of the corners the app was drawn with (desktop's 0.45rem).
 * Written by [AppTheme] and read through [rounded], so every shape built in composition follows it.
 * A shape kept in a top-level val would be built once at scale 1, hence the getters on those.
 */
object Corners {
    var scale by mutableFloatStateOf(1f)
        internal set
}

/** A rounded shape drawn at [radius] for the default roundness, scaled by the setting. */
fun rounded(radius: Dp): RoundedCornerShape = RoundedCornerShape(radius * Corners.scale)

fun rounded(
    topStart: Dp = 0.dp,
    topEnd: Dp = 0.dp,
    bottomEnd: Dp = 0.dp,
    bottomStart: Dp = 0.dp,
): RoundedCornerShape {
    val k = Corners.scale
    return RoundedCornerShape(topStart * k, topEnd * k, bottomEnd * k, bottomStart * k)
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AppTheme(
    themeMode: String = DataStoreManager.THEME_MODE_DARK,
    appearance: Appearance = Appearance(),
    // "Adapt colors to artwork": the playing cover's colour, null while the switch is off, nothing
    // plays or the cover has no colour. Layered over the theme, which comes back untouched.
    artworkColor: Color? = null,
    content:
        @Composable()
        () -> Unit,
) {
    val isDark = appearance.theme.alwaysDark || isDarkTheme(themeMode)
    val colorScheme = rememberThemeScheme(appearance, isDark, artworkColor)
    // Immersive screens stay dark even at light theme (see [ForceDarkContent]).
    val forcedDarkScheme = if (isDark) colorScheme else rememberThemeScheme(appearance, true, artworkColor)
    SystemBarAppearanceEffect(isDark)
    val cornerScale = appearance.cornerScale
    SideEffect { Corners.scale = cornerScale }
    val fonts = AppFonts(rememberFontFamily(appearance.bodyFont), rememberFontFamily(appearance.headingFont))
    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        shapes = LimusicShapes,
        content = {
            CompositionLocalProvider(
                LocalTextStyle provides defaultTextStyle(),
                LocalRippleConfiguration provides SoftRippleConfiguration,
                LocalContentColor provides colorScheme.onSurfaceVariant,
                LocalAppColors provides if (isDark) DarkAppColors else LightAppColors,
                LocalIsDarkTheme provides isDark,
                LocalForcedDarkColorScheme provides forcedDarkScheme,
                LocalAppFonts provides fonts,
                LocalAppScheme provides (colorScheme to isDark),
                LocalArtworkColors provides (artworkColor != null),
                content = content,
            )
        },
        typography = typo(colorScheme, fonts = fonts),
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
                LocalTextStyle provides defaultTextStyle(),
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

/**
 * The reverse of [ForceDarkContent]: puts the app's own theme back for a subtree, so a menu opened
 * from an immersive screen still follows light or dark like the rest of the app.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppThemeContent(content: @Composable () -> Unit) {
    val (scheme, isDark) = LocalAppScheme.current ?: return content()
    MaterialExpressiveTheme(
        colorScheme = scheme,
        shapes = LimusicShapes,
        content = {
            CompositionLocalProvider(
                LocalTextStyle provides defaultTextStyle(),
                LocalForceDarkText provides false,
                LocalIsDarkTheme provides isDark,
                LocalContentColor provides scheme.onSurfaceVariant,
                LocalAppColors provides if (isDark) DarkAppColors else LightAppColors,
                content = content,
            )
        },
        typography = typo(scheme, forceDark = false),
    )
}

/**
 * MaterialTheme's default text style, minus [typo]'s baked-in grey. Material hands every Text
 * `typography.bodyLarge` as its base, and a button label (labelLarge, no colour of its own) merged
 * onto that kept the grey instead of the button's content colour: grey "Save" on an accent pill.
 * With the colour unspecified, a plain Text falls back to LocalContentColor, which each theme below
 * sets to the same onSurfaceVariant, and buttons get onPrimary / primary as Material intends.
 */
@Composable
private fun defaultTextStyle(): TextStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.Unspecified)

/**
 * The scheme for [appearance] in one mode: the preset, then the accent (the user's, or the
 * wallpaper's), then the playing cover over both while "Adapt colors to artwork" has one. Material
 * You is generated whole instead, from the cover, the accent or the wallpaper, in that order.
 */
@Composable
internal fun rememberThemeScheme(
    appearance: Appearance,
    isDark: Boolean,
    artwork: Color? = null,
): ColorScheme {
    val theme = appearance.theme
    val wallpaper =
        if (theme == ThemePreset.MATERIAL_YOU || appearance.accent == Appearance.WALLPAPER) platformDynamicColorScheme(isDark) else null
    if (theme == ThemePreset.MATERIAL_YOU) {
        val seed = artwork ?: appearance.accentColor
        if (seed == null && wallpaper != null) return wallpaper
        return rememberDynamicColorScheme(seedColor = seed ?: MaterialYouSeed, isDark = isDark, style = PaletteStyle.TonalSpot)
    }
    val accent = artwork?.let { artworkAccent(it, isDark) } ?: appearance.accentColor?.let { userAccent(it, isDark) }
    val wallpaperAccent = wallpaper?.primary
    val wallpaperOn = wallpaper?.onPrimary
    return remember(theme, appearance.hue, isDark, accent, wallpaperAccent, artwork) {
        var scheme =
            when (theme) {
                ThemePreset.DEFAULT ->
                    (if (isDark) LimusicDarkColorScheme else LimusicLightColorScheme)
                        .let { s -> appearance.hue?.let { s.withNeutralHue(it.toDouble()) } ?: s }
                else -> (if (isDark) theme.dark else theme.light)!!.scheme(isDark)
            }
        if (accent != null) {
            scheme = scheme.withAccent(accent, onAccent(accent), isDark)
        } else if (wallpaperAccent != null && wallpaperOn != null) {
            scheme = scheme.withAccent(wallpaperAccent, wallpaperOn, isDark)
        }
        if (artwork != null) scheme = scheme.withArtworkFills(artwork, isDark)
        scheme
    }
}

/**
 * A palette preset as a Material scheme, mapped the way [LimusicDarkColorScheme] maps the desktop's
 * default tokens. The container ramp climbs from the page to the palette's card; a palette whose card
 * sits on the wrong side of its page (white cards on an off-white page, in light mode) climbs to its
 * muted grey instead, since Material containers step towards the reader.
 */
internal fun Palette.scheme(isDark: Boolean): ColorScheme {
    val bg = Color(background)
    val fg = Color(foreground)
    val cardColor = Color(card)
    val raised = if (card != background && (cardColor.luminance() > bg.luminance()) == isDark) cardColor else Color(muted)
    val base = if (isDark) darkColorScheme() else lightColorScheme()
    return base
        .copy(
            secondary = Color(mutedForeground),
            onSecondary = bg,
            secondaryContainer = Color(secondary),
            onSecondaryContainer = Color(onSecondary),
            background = bg,
            onBackground = fg,
            surface = bg,
            onSurface = fg,
            surfaceVariant = Color(muted),
            onSurfaceVariant = Color(mutedForeground),
            inverseSurface = fg,
            inverseOnSurface = bg,
            error = Color(destructive),
            onError = bg,
            outline = Color(ring),
            outlineVariant = Color(border),
            scrim = Color.Black,
            surfaceBright = lerp(raised, fg, 0.1f),
            surfaceDim = bg,
            surfaceContainerLowest = bg,
            surfaceContainerLow = lerp(bg, raised, 0.5f),
            surfaceContainer = lerp(bg, raised, 0.8f),
            surfaceContainerHigh = raised,
            surfaceContainerHighest = lerp(raised, fg, 0.1f),
        ).withAccent(Color(primary), Color(onPrimary), isDark)
}

/** [primary] as the accent, its containers at Material's container tones of the same hue. */
private fun ColorScheme.withAccent(
    primary: Color,
    onPrimary: Color,
    isDark: Boolean,
): ColorScheme {
    val hct = Hct.fromInt(primary.toArgb())

    fun at(
        maxChroma: Double,
        tone: Double,
    ) = Color(Hct.from(hct.hue, minOf(hct.chroma, maxChroma), tone).toInt())
    val container = at(48.0, if (isDark) 30.0 else 90.0)
    val onContainer = at(16.0, if (isDark) 90.0 else 10.0)
    return copy(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = container,
        onPrimaryContainer = onContainer,
        inversePrimary = primary,
        surfaceTint = primary,
        tertiary = primary,
        onTertiary = onPrimary,
        tertiaryContainer = container,
        onTertiaryContainer = onContainer,
    )
}

/**
 * A picked accent, kept as picked unless it would vanish as text: lifted to tone 50 on a dark page,
 * held to 60 on a light one. Desktop's swatches all sit inside that band and don't move.
 */
private fun userAccent(
    color: Color,
    isDark: Boolean,
): Color {
    val hct = Hct.fromInt(color.toArgb())
    val tone = if (isDark) maxOf(hct.tone, 50.0) else minOf(hct.tone, 60.0)
    return if (tone == hct.tone) color else Color(Hct.from(hct.hue, hct.chroma, tone).toInt())
}

/** A cover's colour pulled to the tone the mode needs, as desktop's artcolor.ts `toAccent` does. */
private fun artworkAccent(
    color: Color,
    isDark: Boolean,
): Color {
    val hct = Hct.fromInt(color.toArgb())
    return Color(Hct.from(hct.hue, maxOf(hct.chroma, 30.0), if (isDark) 70.0 else 40.0).toInt())
}

/** Text on an accent fill: desktop's two neutrals, picked by perceived brightness (color.ts `isLight`). */
fun onAccent(color: Color): Color {
    val hsp = sqrt(0.299f * color.red * color.red + 0.587f * color.green * color.green + 0.114f * color.blue * color.blue)
    return if (hsp > 0.6f) Color(0xFF171717) else Color(0xFFFAFAFA)
}

/** The Default theme's greys turned to [hue] at their own tone and chroma (desktop's `--hue`). */
private fun ColorScheme.withNeutralHue(hue: Double): ColorScheme {
    fun Color.h(): Color {
        val hct = Hct.fromInt(toArgb())
        return Color(Hct.from(hue, hct.chroma, hct.tone).toInt())
    }
    return copy(
        secondary = secondary.h(),
        onSecondary = onSecondary.h(),
        secondaryContainer = secondaryContainer.h(),
        onSecondaryContainer = onSecondaryContainer.h(),
        background = background.h(),
        onBackground = onBackground.h(),
        surface = surface.h(),
        onSurface = onSurface.h(),
        surfaceVariant = surfaceVariant.h(),
        onSurfaceVariant = onSurfaceVariant.h(),
        inverseSurface = inverseSurface.h(),
        inverseOnSurface = inverseOnSurface.h(),
        outline = outline.h(),
        outlineVariant = outlineVariant.h(),
        surfaceBright = surfaceBright.h(),
        surfaceDim = surfaceDim.h(),
        surfaceContainerLowest = surfaceContainerLowest.h(),
        surfaceContainerLow = surfaceContainerLow.h(),
        surfaceContainer = surfaceContainer.h(),
        surfaceContainerHigh = surfaceContainerHigh.h(),
        surfaceContainerHighest = surfaceContainerHighest.h(),
    )
}

/** Where the tint slider starts on the Default theme: the hue its greys already have. */
val DefaultNeutralHue: Float = Hct.fromInt(LimusicDark.card.toArgb()).hue.toFloat()

/**
 * Desktop's artwork tint: the Limusic fills (background, surfaces, containers) turned to the cover's
 * hue at their own tone. Text and outlines stay as they are, as on desktop, where tinting them too
 * made the page hard to read.
 */
private fun ColorScheme.withArtworkFills(
    artwork: Color,
    isDark: Boolean,
): ColorScheme {
    val hue = Hct.fromInt(artwork.toArgb()).hue
    // ponytail: one chroma for every fill, roughly desktop's oklch 0.03-0.04. Turn these down
    // together if a saturated cover is too much; per-role values only if one surface reads wrong.
    val chroma = if (isDark) 10.0 else 6.0

    // Light tones stop at 98: pure white has no room for a hue (desktop's light page is 0.985 too).
    fun Color.tint(): Color {
        val tone = Hct.fromInt(toArgb()).tone
        return Color(Hct.from(hue, chroma, if (isDark) tone else minOf(tone, 98.0)).toInt())
    }
    return copy(
        background = background.tint(),
        surface = surface.tint(),
        surfaceVariant = surfaceVariant.tint(),
        surfaceBright = surfaceBright.tint(),
        surfaceDim = surfaceDim.tint(),
        surfaceContainerLowest = surfaceContainerLowest.tint(),
        surfaceContainerLow = surfaceContainerLow.tint(),
        surfaceContainer = surfaceContainer.tint(),
        surfaceContainerHigh = surfaceContainerHigh.tint(),
        surfaceContainerHighest = surfaceContainerHighest.tint(),
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
