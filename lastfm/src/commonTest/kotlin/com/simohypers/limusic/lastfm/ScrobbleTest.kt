package com.simohypers.limusic.lastfm

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Desktop's lastfm.rs tests, against the list-of-artists track the phone has. */
class ScrobbleTest {
    private fun track(
        title: String,
        artists: List<String>,
        isVideo: Boolean = false,
        album: String? = null,
        id: String = "T1Vkc5w79-M",
    ) = ScrobbleTrack(id, title, artists, album, isVideo)

    private fun crosses(
        pos: Double,
        duration: Double,
        cfg: ScrobbleConfig = ScrobbleConfig(),
    ) = scrobbleAt(duration, cfg)?.let { pos >= it } == true

    @Test
    fun thresholdFollowsLastfmsRuleByDefault() {
        assertFalse(crosses(89.0, 180.0))
        assertTrue(crosses(90.0, 180.0))
        assertFalse(crosses(239.0, 1200.0))
        assertTrue(crosses(240.0, 1200.0))
        assertFalse(crosses(29.0, 20.0), "under 30s never scrobbles")
        assertFalse(crosses(120.0, 0.0), "unknown length: only the 4 minutes apply")
        assertTrue(crosses(240.0, 0.0))
    }

    @Test
    fun thresholdFollowsTheUsersNumbers() {
        val noCap = ScrobbleConfig(percent = 90, minutes = 0)
        assertFalse(crosses(1000.0, 1200.0, noCap))
        assertTrue(crosses(1080.0, 1200.0, noCap))
        assertNull(scrobbleAt(0.0, noCap))
        assertEquals(120.0, scrobbleAt(200.0, ScrobbleConfig(percent = 100, minutes = 2)))
        assertEquals(2.0, scrobbleAt(200.0, ScrobbleConfig(percent = 0, minutes = 0)), "out of range clamps")
    }

    @Test
    fun theDefaultChangesNothing() {
        val r = resolve(track("Song - Live", listOf("A", "B"), isVideo = true, album = "Album"), ScrobbleConfig())
        assertEquals(Triple("A & B", "Song - Live", "Album"), Triple(r.artist, r.title, r.album))
        assertTrue(r.skip == null && r.edit == null && !r.split && r.rules.isEmpty())
        assertTrue(ScrobbleConfig.parse("{oops").enabled)
    }

    @Test
    fun bylinesReadLikeYouTubes() {
        assertEquals("A, B & C", track("x", listOf("A", "B", "C")).byline)
        assertEquals("Solo", track("x", listOf("Solo")).byline)
    }

    @Test
    fun primaryArtistKeepsAJointActUnlessStrict() {
        assertEquals("A", primaryArtist(listOf("A", "B", "C"), false))
        assertEquals("Future & Metro Boomin", primaryArtist(listOf("Future", "Metro Boomin"), false))
        assertEquals("Future", primaryArtist(listOf("Future", "Metro Boomin"), true))
        // One credit that YouTube wrote as text: desktop's string cut.
        assertEquals("Artist A", primaryArtist(listOf("Artist A, Artist B"), false))
        assertEquals("Simon", primaryArtist(listOf("Simon&Garfunkel"), true))
        assertEquals("Simon&Garfunkel", primaryArtist(listOf("Simon&Garfunkel"), false))
        assertEquals("& Juliet", primaryArtist(listOf("& Juliet"), true))
        assertEquals("Delara", primaryArtist(listOf("Delara"), true))
    }

    @Test
    fun videoTitlesSplitWhenAsked() {
        val cfg = ScrobbleConfig(splitVideoTitles = true)
        val r = resolve(track("Ruelle - Fire Meets Fate - Shadowhunters 3x10 Music (Mid Season Finale)", listOf("alexias788"), isVideo = true), cfg)
        assertTrue(r.split)
        assertEquals("Ruelle", r.artist)
        assertEquals("Fire Meets Fate - Shadowhunters 3x10 Music (Mid Season Finale)", r.title)
        val song = resolve(track("Song - Remastered 2011", listOf("Band")), cfg)
        assertTrue(!song.split && song.artist == "Band", "songs are never split")
        assertEquals("Ruelle" to "War of Hearts", splitTitle("Ruelle – War of Hearts"))
        assertNull(splitTitle(" - Intro"))
        assertNull(splitTitle("Re-Run"))
    }

    @Test
    fun rulesReplaceInOrderAndIgnoreCase() {
        val cfg =
            ScrobbleConfig(
                rules =
                    listOf(
                        ScrobbleRule(field = "title", find = """\s*[(\[](?:official\s+)?(?:music\s+)?video[)\]]"""),
                        ScrobbleRule(field = "artist", find = "VEVO$"),
                        ScrobbleRule(field = "album", find = "x", replace = "y"),
                    ),
            )
        val r = resolve(track("Higher Ground [OFFICIAL Music Video]", listOf("NovaSkyVEVO"), isVideo = true), cfg)
        assertEquals("Higher Ground" to "NovaSky", r.title to r.artist)
        assertEquals(listOf(0, 1), r.rules)
        val sensitive = ScrobbleConfig(rules = listOf(ScrobbleRule(field = "artist", find = "(?-i)vevo$")))
        assertTrue(resolve(track("x", listOf("NovaSkyVEVO")), sensitive).rules.isEmpty(), "(?-i) is case-sensitive again")
    }

    @Test
    fun namedGroupsMoveTextBetweenFields() {
        val cfg = ScrobbleConfig(rules = listOf(ScrobbleRule(field = "title", find = "^(?<artist>.+?) - (?<title>.+)$", replace = "ignored")))
        val r = resolve(track("Ruelle - Fire Meets Fate", listOf("alexias788")), cfg)
        assertEquals("Ruelle" to "Fire Meets Fate", r.artist to r.title)
        val none = resolve(track("Fire Meets Fate", listOf("Ruelle")), cfg)
        assertTrue(none.rules.isEmpty() && none.title == "Fire Meets Fate")
    }

    @Test
    fun aBrokenRuleIsReportedAndSkipped() {
        val cfg = ScrobbleConfig(rules = listOf(ScrobbleRule(find = "(unclosed"), ScrobbleRule(find = "Song", replace = "Track")))
        val r = resolve(track("Song", listOf("A")), cfg)
        assertEquals(1, r.errors.size)
        assertEquals(0, r.errors[0].first)
        assertEquals("Track", r.title)
        val wipe = ScrobbleConfig(rules = listOf(ScrobbleRule(field = "artist", find = ".*")))
        assertEquals("incomplete", resolve(track("Song", listOf("A")), wipe).skip)
    }

    @Test
    fun anEditIsFinalAndCanSkip() {
        val edit = ScrobbleEdit(key = "T1Vkc5w79-M", artist = "Ruelle", title = "Fire Meets Fate")
        val cfg = ScrobbleConfig(edits = listOf(edit), rules = listOf(ScrobbleRule(field = "artist", find = "Ruelle", replace = "Nope")), splitVideoTitles = true)
        val t = track("whatever", listOf("alexias788"), isVideo = true, album = "Shadowhunters")
        val r = resolve(t, cfg)
        assertEquals(0, r.edit)
        assertEquals(Triple("Ruelle", "Fire Meets Fate", "Shadowhunters"), Triple(r.artist, r.title, r.album))
        assertTrue(!r.split && r.rules.isEmpty())
        assertNull(resolve(track("x", listOf("y"), id = "dQw4w9WgXcQ"), cfg).edit)
        assertEquals("edit", resolve(t, ScrobbleConfig(edits = listOf(edit.copy(skip = true)))).skip)
    }

    @Test
    fun keylessEditsMatchOnMetadata() {
        val cfg = ScrobbleConfig(edits = listOf(ScrobbleEdit(fromArtist = "white noise co", skip = true)))
        assertEquals("edit", resolve(track("Rain 10h", listOf("White Noise Co")), cfg).skip)
        assertNull(resolve(track("Rain 10h", listOf("Someone")), cfg).skip)
        val empty = ScrobbleConfig(edits = listOf(ScrobbleEdit(skip = true)))
        assertNull(resolve(track("a", listOf("b")), empty).skip, "an edit with nothing to match matches nothing")
    }

    @Test
    fun theConfigRoundTripsWithDesktopsFieldNames() {
        val cfg = ScrobbleConfig(percent = 70, splitVideoTitles = true, edits = listOf(ScrobbleEdit(fromArtist = "x")))
        assertTrue("\"split_video_titles\":true" in cfg.toJson())
        assertTrue("\"from_artist\":\"x\"" in cfg.toJson())
        assertEquals(cfg, ScrobbleConfig.parse(cfg.toJson()))
    }

    /** The owner's case: official music videos scrobbled with "(Official Music Video)" in the title. */
    @Test
    fun thePresetsCleanWhatYouTubeAdds() {
        val cfg = ScrobbleConfig(rules = SCROBBLE_PRESETS.map { it.rule })
        fun title(t: String) = resolve(track(t, listOf("A")), cfg).title
        assertEquals("Blinding Lights", title("Blinding Lights (Official Music Video)"))
        assertEquals("Blinding Lights", title("Blinding Lights [Official Video]"))
        assertEquals("Blinding Lights", title("Blinding Lights (Lyrics)"))
        assertEquals("Blinding Lights", title("Blinding Lights (Official Audio)"))
        assertEquals("Blinding Lights", title("Blinding Lights (Official Visualizer)"))
        assertEquals("Here Comes the Sun", title("Here Comes the Sun - Remastered 2009"))
        assertEquals("HUMBLE.", title("HUMBLE. (Explicit)"))
        assertEquals("Blinding Lights - Chromatics Remix", title("Blinding Lights - Chromatics Remix"), "a real dash stays")
        assertEquals("The Weeknd", resolve(track("x", listOf("The Weeknd - Topic")), cfg).artist)
        assertEquals("TheWeeknd", resolve(track("x", listOf("TheWeekndVEVO")), cfg).artist)
        assertTrue(SCROBBLE_PRESETS.all { patternError(it.rule.find) == null })
        assertEquals(null, patternError(""))
        assertTrue(patternError("(unclosed") != null)
    }
}
