package com.simohypers.limusic.domain.data.model.browse.artist

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SameArtistNameTest {
    @Test
    fun matchesTheSameNameOnly() {
        assertTrue(sameArtistName("Taylor Swift", "Taylor Swift"))
        assertTrue(sameArtistName("AC/DC", "AC-DC"))
        assertTrue(sameArtistName("Hall & Oates", "hall & oates"))
        assertTrue(sameArtistName("Beyoncé", "BEYONCÉ"))
        assertFalse(sameArtistName("The Diary Of A CEO", "Oumou Sangaré"))
        assertFalse(sameArtistName("Drake", null))
        assertFalse(sameArtistName("!!!", "!!!"))
    }
}
