package com.simohypers.limusic.viewModel

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HomeShelvesTest {
    @Test
    fun sameKindInEveryLanguage() {
        assertEquals("quick_picks", homeShelfKind("Quick picks", null))
        assertEquals("quick_picks", homeShelfKind("Schnellauswahl", null))
        assertEquals("quick_picks", homeShelfKind("  quick PICKS ", null))
        assertNull(homeShelfKind("Indila", null))
    }

    @Test
    fun moreButtonBeatsTitle() {
        assertEquals("mixed_for_you", homeShelfKind("Whatever YouTube calls it", "FEmusic_mixed_for_you"))
        assertEquals("quick_picks", homeShelfKind("Quick picks", "FEmusic_something_else"))
    }

    @Test
    fun arrangeKeepsUnrankedInArrivalOrderAfterRanked() {
        val layout = HomeLayout(order = listOf("c", "a"))
        assertEquals(listOf("c", "a", "b", "d"), layout.arrange(listOf("a", "b", "c", "d")) { it })
        assertEquals(listOf("a", "b"), HomeLayout().arrange(listOf("a", "b")) { it })
    }

    @Test
    fun layoutSurvivesARoundTripAndBadInput() {
        val layout = HomeLayout(order = listOf("x"), hidden = setOf("y"), chips = false, cards = HomeLayout.CardSize.LARGE)
        assertEquals(layout, HomeLayout.parse(HomeLayout.encode(layout)))
        assertEquals(HomeLayout(), HomeLayout.parse("not json"))
        assertEquals(HomeLayout(), HomeLayout.parse(null))
    }
}
