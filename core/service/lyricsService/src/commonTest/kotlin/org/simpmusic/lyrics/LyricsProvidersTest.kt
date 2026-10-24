package org.simpmusic.lyrics

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LyricsProvidersTest {
    @Test
    fun lrcRepeatsMultiStampLinesAndSkipsTags() {
        val lines = parseLrc("[ar:Someone]\n[00:12.5][01:02.345]Chorus line\n[00:05.00][Intro]\n")
        assertEquals(listOf(5_000L, 12_500L, 62_345L), lines.map { it.timeMs })
        assertEquals("[Intro]", lines[0].text)
        assertEquals("Chorus line", lines[1].text)
    }

    @Test
    fun elrcSplitsWordsAtInlineStamps() {
        val line = parseElrc("[00:10.50]<00:10.50>Hello <00:11.20>world").single()
        assertEquals("Hello world", line.text)
        assertEquals(listOf(10_500L, 11_200L), line.words!!.map { it.startMs })
    }

    @Test
    fun ttmlKeepsSpacesBetweenSpansWithTheWordBefore() {
        val xml =
            """<tt><body><div><p begin="1.5" end="3.0"><span begin="1.5" end="2">Hel</span><span begin="2" end="2.4">lo</span> <span begin="2.4" end="3">world</span></p></div></body></tt>"""
        val line = parseTtml(xml).single()
        assertEquals("Hello world", line.text)
        assertEquals(1_500L to 3_000L, line.timeMs to line.endMs)
        // Syllables join into words: "Hel" + "lo " is one word starting at 1.5s.
        assertEquals("<00:01.500>Hello <00:02.400>world", line.words!!.toRichSync())
    }

    @Test
    fun ttmlTimes() {
        assertEquals(233_713L, parseTtmlTime("3:53.713"))
        assertEquals(3_723_500L, parseTtmlTime("1:02:03.5"))
        assertEquals(450L, parseTtmlTime("450ms"))
        assertEquals(12_500L, parseTtmlTime("12.5s"))
    }

    @Test
    fun durationMatchPicksTheClosestCutInsideTheWindow() {
        val hits = listOf(263.0, 231.0, 234.0, null)
        assertEquals(234.0, hits.bestByDuration(233) { it })
        assertNull(listOf(263.0).bestByDuration(233) { it })
        assertEquals(263.0, listOf(263.0).bestByDuration(null) { it })
    }

    @Test
    fun searchTitleDropsAsides() {
        assertEquals(
            "Bad Apple／ Lizz Robinett",
            searchTitle("Bad Apple／ Lizz Robinett (English Cover) | Lyrics/Lyric Video [English]"),
        )
    }

    @Test
    fun plainAndRichSyncTypes() {
        assertEquals("UNSYNCED", Found(listOf(LyricLine(null, text = "a"))).toLyrics()!!.lyrics!!.syncType)
        assertEquals("RICH_SYNCED", Found(parseElrc("[00:01.00]<00:01.00>a <00:02.00>b")).toLyrics()!!.lyrics!!.syncType)
        assertNull(Found(emptyList()).toLyrics())
    }

    @Test
    fun oneCueEverywhereIsPlain() {
        assertEquals("UNSYNCED", Found(parseLrc("[00:00.00]a\n[00:00.00]b")).toLyrics()!!.lyrics!!.syncType)
        assertEquals("LINE_SYNCED", Found(parseLrc("[00:00.00]a\n[00:03.00]b")).toLyrics()!!.lyrics!!.syncType)
    }
}
