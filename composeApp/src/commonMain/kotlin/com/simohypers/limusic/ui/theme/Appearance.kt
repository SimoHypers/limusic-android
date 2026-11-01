package com.simohypers.limusic.ui.theme

import androidx.compose.ui.graphics.Color
import com.simohypers.limusic.domain.manager.DataStoreManager
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * The theme presets, desktop Limusic's palettes (ui/src/routes/layout.css). Default is the app's own
 * hand-tuned scheme ([LimusicDarkColorScheme]); Material You is generated (wallpaper or a seed); the
 * rest are the desktop's token sets, light and dark.
 */
enum class ThemePreset(
    val label: String,
    internal val light: Palette? = null,
    internal val dark: Palette? = null,
    // Stays black in light mode too, as on desktop: a black theme that lights the panel back up is
    // pointless.
    val alwaysDark: Boolean = false,
    // Each preset brings its own type, as on desktop.
    val body: AppFont = AppFont.OXANIUM,
    val heading: AppFont = AppFont.IBM_PLEX_SANS,
    // And its own corner radius, desktop's --radius in dp (1rem = 16).
    val radius: Float = DEFAULT_RADIUS,
) {
    DEFAULT("Default"),
    CATPPUCCIN("Catppuccin", CatppuccinLight, CatppuccinDark, body = AppFont.MONTSERRAT, heading = AppFont.MONTSERRAT, radius = 5.6f),
    CAFFEINE("Caffeine", CaffeineLight, CaffeineDark, body = AppFont.SYSTEM, heading = AppFont.SYSTEM, radius = 8f),
    NEON("Neon", NeonLight, NeonDark, body = AppFont.OUTFIT, heading = AppFont.OUTFIT, radius = 8f),
    AMOLED("AMOLED", AmoledDark, AmoledDark, alwaysDark = true),
    MATERIAL_YOU("Material You", body = AppFont.GOOGLE_SANS, heading = AppFont.GOOGLE_SANS, radius = 12f),
}

/** Desktop's default --radius, 0.45rem: the radius the app's own corners were drawn at. */
const val DEFAULT_RADIUS = 7.2f

/**
 * What the user picked on the Appearance page: a preset plus overrides on top of it. Null means
 * untouched, so the preset shows through, the way desktop's custom layer works (theme.svelte.ts).
 * Stored as one JSON string, like Edit Home's layout.
 */
@Serializable
data class Appearance(
    val theme: ThemePreset = ThemePreset.DEFAULT,
    // "RRGGBB", [WALLPAPER], or null for the theme's own accent.
    val accent: String? = null,
    // Hue (HCT, 0-360) of the Default theme's greys.
    val hue: Float? = null,
    // Corner radius in dp, desktop's Roundness; null for the theme's own.
    val radius: Float? = null,
    // An [AppFont] name or a loaded file's path; null for the theme's own.
    val fontBody: String? = null,
    val fontHeading: String? = null,
    // Font files the user loaded. A small library both font rows pick from, not an override, so
    // Reset leaves it alone.
    val fontFiles: List<String> = emptyList(),
) {
    val accentColor: Color? get() = accent?.takeIf { it != WALLPAPER }?.let(::parseThemeColorHex)

    /** Nothing overridden, so Reset has nothing to do. */
    val isPlain: Boolean get() = accent == null && hue == null && radius == null && fontBody == null && fontHeading == null

    fun reset() = Appearance(theme = theme, fontFiles = fontFiles)

    val bodyFont: String get() = fontBody ?: theme.body.name
    val headingFont: String get() = fontHeading ?: theme.heading.name

    /** How much rounder than the app's own corners this is ([Corners.scale]). */
    val cornerScale: Float get() = (radius ?: theme.radius) / DEFAULT_RADIUS

    companion object {
        const val KEY = "appearance"
        const val WALLPAPER = "wallpaper"

        private val json = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        }

        /**
         * The stored JSON, or the pre-presets settings carried over: a custom colour stays the
         * accent, Wallpaper stays the wallpaper accent, on the Default theme.
         */
        fun parse(
            raw: String?,
            legacySource: String,
            legacyHex: String,
        ): Appearance =
            raw?.let { runCatching { json.decodeFromString<Appearance>(it) }.getOrNull() }
                ?: Appearance(
                    accent =
                        when (legacySource) {
                            DataStoreManager.THEME_COLOR_CUSTOM -> legacyHex.takeLast(6)
                            DataStoreManager.THEME_COLOR_WALLPAPER -> WALLPAPER
                            else -> null
                        },
                )

        fun encode(appearance: Appearance): String = json.encodeToString(appearance)
    }
}
