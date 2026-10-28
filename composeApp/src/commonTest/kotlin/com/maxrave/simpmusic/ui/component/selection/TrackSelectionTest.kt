package com.maxrave.simpmusic.ui.component.selection

import com.maxrave.domain.data.model.browse.album.Track
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// The cases from desktop's ui/src/lib/selection.check.ts that the phone's simpler model keeps.
class TrackSelectionTest {
    private fun song(
        id: String,
        entry: String? = null,
    ) = Track(
        album = null,
        artists = null,
        duration = null,
        durationSeconds = null,
        isAvailable = true,
        isExplicit = false,
        likeStatus = null,
        thumbnails = null,
        title = id,
        videoId = id,
        videoType = null,
        category = null,
        feedbackTokens = null,
        resultType = null,
        setVideoId = entry,
    )

    @Test
    fun duplicatesPickApartAndQueueInListOrder() {
        val rows = keyedRows(listOf(song("same", "first"), song("other"), song("same", "second"), song("twin"), song("twin")))
        assertEquals(5, rows.map { it.first }.toSet().size)
        val s = TrackSelection()
        s.start(rows[2].first, rows[2].second)
        s.toggle(rows[0].first, rows[0].second)
        s.toggle(rows[4].first, rows[4].second)
        assertEquals(listOf("first", "second", null), s.tracks(rows).map { it.setVideoId })
        s.toggle(rows[0].first, rows[0].second)
        assertEquals(2, s.count)
    }

    @Test
    fun aResortKeepsPicksUntilTheLastPage() {
        val all = keyedRows(listOf(song("a", "A"), song("b", "B"), song("c", "C")))
        val s = TrackSelection()
        s.start(all[0].first, all[0].second)
        s.toggle(all[2].first, all[2].second)
        // Page one of the new order holds neither pick: both wait, nothing is dropped.
        val pageOne = keyedRows(listOf(song("b", "B")))
        assertEquals(2, s.missing(pageOne))
        val resorted = keyedRows(listOf(song("b", "B"), song("c", "C"), song("a", "A")))
        assertEquals(listOf("c", "a"), s.tracks(resorted).map { it.videoId })
        // A complete list without a pick prunes only that pick.
        s.retain(keyedRows(listOf(song("b", "B"), song("a", "A"))))
        assertEquals(1, s.count)
        assertTrue(s.has(all[0].first))
    }

    @Test
    fun selectAllWaitsAndClearWins() {
        val rows = keyedRows(listOf(song("a"), song("b")))
        val s = TrackSelection()
        s.enter()
        s.selectAll()
        s.clear()
        assertEquals(false, s.selectingAll)
        s.selectAll()
        s.pickAll(rows)
        assertEquals(2, s.count)
        s.exit()
        assertEquals(0, s.count)
        assertEquals(false, s.active)
    }
}
