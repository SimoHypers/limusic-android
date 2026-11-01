package com.simohypers.limusic.ui.theme

import androidx.compose.ui.graphics.Color

// ===== Brand =====

/**
 * Seed for the generated schemes (a custom colour the user has not picked yet). Limusic's crimson,
 * the desktop app's `--primary` in its dark palette.
 */
val seed = Color(0xFFA50036)

// ===== Limusic palette =====
// Converted from the desktop app's default theme tokens (ui/src/routes/layout.css, oklch with the
// neutral hue 326). The neutrals carry a faint magenta tint; the accent is a crimson.

internal object LimusicDark {
    val background = Color(0xFF0C090C)
    val foreground = Color(0xFFFAFAFA)
    val card = Color(0xFF1D161D)
    val primary = Color(0xFFFF2056) // the desktop's sidebar-primary: bright enough for text and icons
    val onPrimary = Color(0xFFFFFFFF)
    val primaryContainer = Color(0xFFA50036) // the desktop's --primary, used as a fill
    val onPrimaryContainer = Color(0xFFFFF1F2)
    val secondary = Color(0xFF292629)
    val muted = Color(0xFF2B212B)
    val mutedForeground = Color(0xFFA89EA8)
    val ring = Color(0xFF7A697A)
    val border = Color(0x1AFFFFFF)
    val destructive = Color(0xFFFF6467)

    // Surface ramp at the card's hue, darkest to lightest.
    val surfaceLowest = Color(0xFF080408)
    val surfaceLow = Color(0xFF120C12)
    val surface = Color(0xFF161116)
    val surfaceHigh = Color(0xFF1C171C)
    val surfaceHighest = Color(0xFF262126)
}

internal object LimusicLight {
    val background = Color(0xFFFFFFFF)
    val foreground = Color(0xFF0C090C)
    val primary = Color(0xFFC70036)
    val onPrimary = Color(0xFFFFF1F2)
    val primaryContainer = Color(0xFFFFE1E6)
    val onPrimaryContainer = Color(0xFFA50036)
    val secondary = Color(0xFFF4F4F4)
    val onSecondary = Color(0xFF1A171A)
    val muted = Color(0xFFF3F1F3)
    val mutedForeground = Color(0xFF7A697A)
    val border = Color(0xFFE7E4E7)
    val ring = Color(0xFFA89EA8)
    val destructive = Color(0xFFE7000B)
}

// ===== Preset palettes =====
// Desktop's preset themes (layout.css), oklch converted to sRGB, translucent borders flattened onto
// the background. One mode each, as its shadcn tokens; [Palette.scheme] maps them onto Material roles.

internal class Palette(
    val background: Long,
    val foreground: Long,
    val card: Long,
    val primary: Long,
    val onPrimary: Long,
    val secondary: Long,
    val onSecondary: Long,
    val muted: Long,
    val mutedForeground: Long,
    val border: Long,
    val ring: Long,
    val destructive: Long,
)

internal val CatppuccinLight =
    Palette(0xFFEFF1F5, 0xFF4C4F69, 0xFFFFFFFF, 0xFF8839EF, 0xFFFFFFFF, 0xFFCCD0DA, 0xFF4C4F69, 0xFFDCE0E8, 0xFF6C6F85, 0xFFBCC0CC, 0xFF8839EF, 0xFFD20F39)
internal val CatppuccinDark =
    Palette(0xFF181825, 0xFFCDD6F4, 0xFF1E1E2E, 0xFFCBA6F7, 0xFF1E1E2E, 0xFF585B70, 0xFFCDD6F4, 0xFF292C3C, 0xFFA6ADC8, 0xFF313244, 0xFFCBA6F7, 0xFFF38BA8)
internal val CaffeineLight =
    Palette(0xFFF9F9F9, 0xFF202020, 0xFFFCFCFC, 0xFF644A40, 0xFFFFFFFF, 0xFFFFDFB5, 0xFF582D1D, 0xFFEFEFEF, 0xFF646464, 0xFFD8D8D8, 0xFF644A40, 0xFFE54D2E)
internal val CaffeineDark =
    Palette(0xFF111111, 0xFFEEEEEE, 0xFF191919, 0xFFFFE0C2, 0xFF081A1B, 0xFF393028, 0xFFFFE0C2, 0xFF222222, 0xFFB4B4B4, 0xFF201E18, 0xFFFFE0C2, 0xFFE54D2E)
internal val NeonLight =
    Palette(0xFFF8F9FA, 0xFF0C0C1D, 0xFFFFFFFF, 0xFFFF00C8, 0xFFFFFFFF, 0xFFF0F0FF, 0xFF0C0C1D, 0xFFF0F0FF, 0xFF0C0C1D, 0xFFDFE6E9, 0xFFFF00C8, 0xFFFF3D00)
internal val NeonDark =
    Palette(0xFF0C0C1D, 0xFFECEFF4, 0xFF1E1E3F, 0xFFFF00C8, 0xFFFFFFFF, 0xFF1E1E3F, 0xFFECEFF4, 0xFF151530, 0xFF8085A6, 0xFF2E2E5E, 0xFFFF00C8, 0xFFFF3D00)

// ponytail: desktop's AMOLED cards are black and set apart by borders. Phone cards have no border,
// so the card takes AMOLED's secondary grey (#171717) instead; the page stays #000000.
internal val AmoledDark =
    Palette(0xFF000000, 0xFFFAFAFA, 0xFF171717, 0xFFFAFAFA, 0xFF0A0A0A, 0xFF171717, 0xFFFAFAFA, 0xFF0F0F0F, 0xFFA1A1A1, 0xFF242424, 0xFF737373, 0xFFFF6467)

/** Desktop's eight accent swatches, mid-tones that read as a fill under white text in both modes. */
val AccentSwatches = listOf("E11D48", "EA580C", "D97706", "16A34A", "0D9488", "2563EB", "7C3AED", "DB2777")

/** Material You's seed where there is no wallpaper colour (below Android 12): desktop's purple. */
internal val MaterialYouSeed = Color(0xFF6750A4)

// ===== Semantic colors (not derivable from the color scheme) =====

/** Liked/favorite state (heart buttons, favorite tiles). */
val favoriteColor = Color(0xFFFF2056)

/** Currently playing lyric line: plain white, as on the desktop app. */
val lyricActiveColor = Color(0xFFFFFFFF)

val shimmerBackground = Color(0x7E383737)
val shimmerLine = Color(0xFF4D4848)

// Light-theme counterparts of the shimmer tokens.
val shimmerBackgroundLight = Color(0x7EDCD8D8)
val shimmerLineLight = Color(0xFFCFC8C8)

val overlay = Color(0x32242424)
val blackMoreOverlay = Color(0x8f242424)

// ===== Desktop shell =====
// Spotify-style layering for the desktop window: the window itself takes the extreme, panels
// step one shade back towards the middle so they read as raised. The light panel is not here —
// it comes from colorScheme.surfaceContainer, which already sits at the right distance.

val desktopWindowDark = Color(0xFF000000)
val desktopWindowLight = Color(0xFFFFFFFF)
val desktopPanelDark = Color(0xFF121212)

// ===== Desktop window controls =====
// The macOS traffic lights. Fixed by convention rather than by theme — users read these by
// colour, so they stay the same in light and dark.

val windowCloseButton = Color(0xFFFF605C)
val windowCloseButtonHover = Color(0xFFE54942)
val windowMinimiseButton = Color(0xFFFFBD44)
val windowMinimiseButtonHover = Color(0xFFE5A93D)
val windowMaximiseButton = Color(0xFF00CA4E)
val windowMaximiseButtonHover = Color(0xFF00B344)

// ===== Legacy — do not add new usages =====

/**
 * Old M3 primary (lavender). Kept only for the SettingScreen storage bar,
 * which stays untouched by owner's decision.
 */
@Deprecated("Legacy storage bar color only — use MaterialTheme.colorScheme.primary in new code")
val md_theme_dark_primary = Color(0xFFB2C5FF)
