package com.simohypers.limusic.ui.theme

import androidx.compose.ui.graphics.Color
import com.simohypers.limusic.domain.manager.DataStoreManager
import com.simohypers.limusic.ui.screen.home.hex
import com.simohypers.limusic.ui.screen.home.toHsv
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AppearanceTest {
    @Test
    fun oldColourSettingsCarryOver() {
        val custom = Appearance.parse(null, DataStoreManager.THEME_COLOR_CUSTOM, "FF9B72CF")
        assertEquals("9B72CF", custom.accent)
        assertEquals(ThemePreset.DEFAULT, custom.theme)
        assertEquals(Appearance.WALLPAPER, Appearance.parse(null, DataStoreManager.THEME_COLOR_WALLPAPER, "FFA50036").accent)
        assertNull(Appearance.parse(null, DataStoreManager.THEME_COLOR_DEFAULT, "FFA50036").accent)
    }

    @Test
    fun storedPicksWinAndSurviveUnknownValues() {
        val picked = Appearance(theme = ThemePreset.NEON, accent = "2563EB", radius = 12f, fontFiles = listOf("/f/a.ttf"))
        assertEquals(picked, Appearance.parse(Appearance.encode(picked), DataStoreManager.THEME_COLOR_CUSTOM, "FF000000"))
        // A preset this build doesn't know falls back to Default instead of dropping everything.
        val future = Appearance.parse("""{"theme":"TOKYO_NIGHT","accent":"16A34A"}""", DataStoreManager.THEME_COLOR_DEFAULT, "")
        assertEquals(ThemePreset.DEFAULT, future.theme)
        assertEquals("16A34A", future.accent)
    }

    @Test
    fun resetKeepsThemeAndLoadedFonts() {
        val picked = Appearance(ThemePreset.CATPPUCCIN, "E11D48", 200f, 3f, "OUTFIT", null, listOf("/f/a.ttf"))
        assertEquals(Appearance(ThemePreset.CATPPUCCIN, fontFiles = listOf("/f/a.ttf")), picked.reset())
        assertEquals(true, picked.reset().isPlain)
    }

    @Test
    fun pickerColoursRoundTrip() {
        for (hex in AccentSwatches + listOf("000000", "FFFFFF", "FF0000", "808080")) {
            val (h, s, v) = parseThemeColorHex(hex)!!.toHsv()
            assertEquals(hex, Color.hsv(h, s, v).hex(), "round trip of $hex")
        }
    }
}
