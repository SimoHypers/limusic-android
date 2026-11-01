package com.simohypers.limusic.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.unit.sp
import com.simohypers.limusic.expect.ui.fileFontFamily
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.FontResource
import limusic.composeapp.generated.resources.Res
import limusic.composeapp.generated.resources.dm_sans
import limusic.composeapp.generated.resources.google_sans
import limusic.composeapp.generated.resources.ibm_plex_sans
import limusic.composeapp.generated.resources.montserrat
import limusic.composeapp.generated.resources.outfit
import limusic.composeapp.generated.resources.oxanium

/**
 * The bundled faces (all OFL, licences in files/fonts), desktop's font list. The variable ones are
 * subset to Latin, Greek, Cyrillic and Vietnamese; other scripts fall back to the system font.
 */
enum class AppFont(
    val label: String,
    internal val res: FontResource?,
) {
    OXANIUM("Oxanium", Res.font.oxanium),
    IBM_PLEX_SANS("IBM Plex Sans", Res.font.ibm_plex_sans),
    MONTSERRAT("Montserrat", Res.font.montserrat),
    OUTFIT("Outfit", Res.font.outfit),
    DM_SANS("DM Sans", Res.font.dm_sans),
    GOOGLE_SANS("Google Sans", Res.font.google_sans),
    SYSTEM("System", null),
}

private val FontWeights = listOf(FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold)

/**
 * A font as the Appearance settings store it: an [AppFont] name, or the path of a file the user
 * loaded. Each weight is an instance of the file's `wght` axis; a static file just ignores it. A file
 * that has gone missing gives the system font.
 */
@OptIn(ExperimentalTextApi::class)
@Composable
fun rememberFontFamily(id: String): FontFamily {
    val bundled = AppFont.entries.firstOrNull { it.name == id } ?: return remember(id) { fileFontFamily(id) ?: FontFamily.Default }
    val res = bundled.res ?: return FontFamily.Default
    return FontFamily(
        FontWeights.map { weight ->
            Font(res, weight, FontStyle.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)))
        },
    )
}

/** The name a loaded font file goes by: its file name without the extension, as on desktop. */
fun fontFileLabel(path: String): String = path.substringAfterLast('/').substringBeforeLast('.')

/** Both faces, built once by [AppTheme] so [typo] doesn't rebuild seven Font entries per call. */
@Immutable
data class AppFonts(
    val body: FontFamily,
    val heading: FontFamily,
)

val LocalAppFonts = staticCompositionLocalOf<AppFonts?> { null }

/**
 * When true, [typo] keeps the original always-light text colors (pure white titles, #A8A8A8 body)
 * regardless of theme. Immersive screens drawn over dark artwork provide `true` so their text stays
 * readable at light theme. Everything else leaves it false and gets theme-aware colors.
 */
val LocalForceDarkText = staticCompositionLocalOf { false }

@Composable
fun typo(
    colorScheme: ColorScheme = MaterialTheme.colorScheme,
    forceDark: Boolean = LocalForceDarkText.current,
    // Outside AppTheme (previews) nothing provides the pair, so build one here.
    fonts: AppFonts = LocalAppFonts.current ?: AppFonts(rememberFontFamily(AppFont.OXANIUM.name), rememberFontFamily(AppFont.IBM_PLEX_SANS.name)),
): Typography {
    // Titles were pure white, everything else muted gray (#A8A8A8) in the old dark-only palette.
    // forceDark keeps that; otherwise both come from the scheme (theme-aware light/dark).
    val titleColor = if (forceDark) Color.White else colorScheme.onBackground
    val bodyColor = if (forceDark) Color(0xFFA8A8A8) else colorScheme.onSurfaceVariant

    return remember(fonts, titleColor, bodyColor) { buildTypography(fonts, titleColor, bodyColor) }
}

private fun buildTypography(
    fonts: AppFonts,
    titleColor: Color,
    bodyColor: Color,
): Typography {
    val fontFamily = fonts.body
    val headingFamily = fonts.heading
    val typo =
        Typography(
            /***
             * This typo().is use for the title of the Playlist, Artist, Song, Album, etc. in Home, Mood, Genre, Playlist, etc.
             */
            titleSmall =
                TextStyle(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = fontFamily,
                    color = titleColor,
                ),
            titleMedium =
                TextStyle(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = fontFamily,
                    color = titleColor,
                ),
            titleLarge =
                TextStyle(
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = headingFamily,
                    color = titleColor,
                ),
            bodySmall =
                TextStyle(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal,
                    fontFamily = fontFamily,
                    color = bodyColor,
                ),
            bodyMedium =
                TextStyle(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    fontFamily = fontFamily,
                    color = bodyColor,
                ),
            bodyLarge =
                TextStyle(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Normal,
                    fontFamily = fontFamily,
                    color = bodyColor,
                ),
            displayLarge =
                TextStyle(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Normal,
                    fontFamily = fontFamily,
                    color = bodyColor,
                ),
            headlineMedium =
                TextStyle(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = headingFamily,
                    color = bodyColor,
                ),
            headlineLarge =
                TextStyle(
                    fontSize = 23.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = headingFamily,
                    color = bodyColor,
                ),
            labelMedium =
                TextStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = fontFamily,
                    color = bodyColor,
                ),
            labelSmall =
                TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = fontFamily,
                    color = bodyColor,
                ),
            // ...
        )
    return typo
}
