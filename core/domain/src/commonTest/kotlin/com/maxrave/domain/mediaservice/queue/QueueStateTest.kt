package com.maxrave.domain.mediaservice.queue

import com.maxrave.common.LOCAL_PLAYLIST_ID
import com.maxrave.domain.data.model.browse.album.Track
import com.maxrave.domain.mediaservice.handler.RepeatState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Desktop's queue tests (src-tauri/src/state.rs), carried over with the code they check. */
class QueueStateTest {
    private fun song(id: String) =
        Track(
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
        )

    private fun queued(id: String) = song(id).copy(queued = true)

    private fun added(id: String) = song(id).copy(queuedEnd = true)

    private fun auto(id: String) = song(id).copy(autoplay = true)

    private fun ids(items: List<Track>) = items.map { it.videoId }

    private fun queue(
        vararg items: Track,
        current: Int = 0,
        radio: Boolean = false,
    ) = QueueState().apply {
        this.items = items.toMutableList()
        this.current = current
        this.radio = radio
    }

    @Test
    fun trimPlayedKeepsTheHistoryBounded() {
        val items = (0 until 300).map { song("s$it") }.toMutableList()
        val short = items.take(150).toMutableList()
        assertEquals(0, trimPlayed(short, 150, KEEP_PLAYED))
        assertEquals(150, short.size)

        val playing = items[250].videoId
        val dropped = trimPlayed(items, 250, KEEP_PLAYED)
        assertEquals(50, dropped)
        assertEquals(250, items.size)
        assertEquals(playing, items[250 - dropped].videoId)

        // An endless radio: append, advance, trim. Bounded, not rising.
        val radio = mutableListOf(song("seed"))
        var current = 0
        repeat(30) { hop ->
            (0 until 20).mapTo(radio) { song("h$hop-$it") }
            current += 20
            val now = radio[current].videoId
            current -= trimPlayed(radio, current, KEEP_PLAYED)
            assertEquals(now, radio[current].videoId)
            assertTrue(radio.size <= KEEP_PLAYED + 21)
        }
    }

    @Test
    fun droppingUpcomingKeepsHistoryAndTheShuffleSnapshotInStep() {
        val q = queue(auto("played"), song("now"), auto("r1"), song("a1"), auto("r2"), current = 1)
        q.shuffleOrig = mutableListOf(auto("played"), song("now"), song("a1"), auto("r1"), auto("r2"))
        assertTrue(q.retainUpcoming { !it.autoplay })
        assertEquals(listOf("played", "now", "a1"), ids(q.items))
        assertEquals(listOf("played", "now", "a1"), ids(q.shuffleOrig!!))
        assertEquals(1, q.current)
        assertFalse(q.retainUpcoming { !it.autoplay })
    }

    @Test
    fun playNextStacksFifoAfterCurrent() {
        assertEquals(1, playNextIndex(listOf(song("a"), song("b"), song("c")), 0))
        assertEquals(2, playNextIndex(listOf(song("a"), queued("s1"), song("b")), 0))
        assertEquals(1, playNextIndex(listOf(song("a")), 0))
        assertEquals(0, playNextIndex(emptyList(), 0))
    }

    @Test
    fun radioReplacesTheTailButKeepsHistoryAndManualAdds() {
        val q = queue(song("done"), song("now"), queued("mine"), song("old1"), song("old2"), current = 1)
        val page = listOf(song("now"), song("r1"), song("done"), song("mine"), song("old1"))
        q.spliceRadio(page, "RDAMVMnow", "Now Radio")
        assertEquals(listOf("done", "now", "mine", "r1", "old1"), ids(q.items))
        assertEquals(1, q.current)
        assertEquals("RDAMVMnow", q.radioSeed)
        assertEquals("Now Radio", q.sourceName)
        assertTrue(q.radio)
        assertTrue(q.items.none { it.autoplay })
    }

    @Test
    fun radioStartedUnderShuffleStaysShuffledAndRestorable() {
        val q = queue(song("now"))
        q.shuffleOrig = mutableListOf(song("now"))
        q.spliceRadio((0 until 50).map { song("r$it") }, "RDAMVMnow", null)
        assertEquals("now", q.items[0].videoId)
        assertEquals(51, q.shuffleOrig!!.size)
        assertEquals(ids(q.shuffleOrig!!).sorted(), ids(q.items).sorted())
    }

    @Test
    fun nextIndexWrapsOnlyOnRepeatAll() {
        assertNull(nextIndex(3, 2, RepeatState.None))
        assertEquals(0, nextIndex(3, 2, RepeatState.All))
        assertNull(nextIndex(3, 2, RepeatState.One))
        assertEquals(1, nextIndex(3, 0, RepeatState.None))
        assertNull(nextIndex(0, 0, RepeatState.All))
        assertEquals(0, nextIndex(1, 0, RepeatState.All))
    }

    @Test
    fun appendedPageMixesIntoTheTailButLeavesTheNextTrack() {
        val positions = mutableListOf<Int>()
        repeat(20) {
            val q = queue(song("a"), song("b"), song("c"))
            q.shuffleOrig = q.items.toMutableList()
            q.appendPage((0 until 100).map { song("p$it") }, playing = true, whole = false)
            assertEquals(103, q.items.size)
            assertEquals("a", q.items[0].videoId)
            assertEquals("b", q.items[1].videoId)
            assertEquals(103, q.shuffleOrig!!.size)
            assertEquals(listOf("a", "b", "c"), ids(q.shuffleOrig!!).take(3))
            assertEquals(ids(q.shuffleOrig!!).sorted(), ids(q.items).sorted())
            positions += q.items.indexOfFirst { it.videoId == "c" }
        }
        assertTrue(positions.any { it != 2 }, "page was appended, not mixed in")
    }

    @Test
    fun appendedPageKeepsOrderWhenShuffleIsOff() {
        val q = queue(song("a"), song("b"))
        q.appendPage(listOf(song("c"), song("d")), playing = true, whole = false)
        assertEquals(listOf("a", "b", "c", "d"), ids(q.items))
        assertNull(q.shuffleOrig)
    }

    @Test
    fun appendedPageJoinsTheAddToQueueBlockItBelongsTo() {
        fun from(
            id: String,
            pl: String,
        ) = song(id).copy(queuedEnd = true, queuedFrom = pl)
        val q = queue(song("a"), song("b"), from("x1", "X"), from("y1", "Y"))
        q.shuffleOrig = q.items.toMutableList()
        q.appendPage(listOf(from("x2", "X"), from("x3", "X")), playing = false, whole = false)
        assertEquals(listOf("a", "b", "x1", "x2", "x3", "y1"), ids(q.items))
        assertEquals(6, q.shuffleOrig!!.size)
    }

    @Test
    fun droppingADuplicateBeforeTheCurrentTrackMovesTheCurrentIndex() {
        val q = queue(song("dup"), song("a"), song("b"), song("dup"), current = 1)
        assertTrue(q.dropDuplicates(setOf("dup")))
        assertEquals(listOf("a", "b"), ids(q.items))
        assertEquals(0, q.current)
        assertEquals(1, playNextIndex(q.items, q.current))

        val playing = queue(song("a"), song("b"))
        assertFalse(playing.dropDuplicates(setOf("a")))
        assertEquals(2, playing.items.size)
    }

    @Test
    fun addToQueueGoesToTheTailAheadOfAutoplay() {
        assertEquals(3, queue(song("a"), song("b"), song("c")).enqueueAt())
        assertEquals(4, queue(song("a"), queued("mine"), song("b"), added("x1"), auto("r1")).enqueueAt())
        assertEquals(2, queue(song("r1"), added("x1"), song("r2"), song("r3"), radio = true).enqueueAt())
        assertEquals(1, queue(song("a")).enqueueAt())
    }

    @Test
    fun thePlayingPlaylistsNextPageGoesInFrontOfTheAdds() {
        val q = queue(song("a1"), queued("p"), song("a2"), added("x1"), auto("r1"))
        q.appendPage(listOf(song("a3"), song("a4")), playing = true, whole = false)
        assertEquals(listOf("a1", "p", "a2", "a3", "a4", "x1", "r1"), ids(q.items))

        val onlyAdd = queue(song("a1"), added("x1"))
        onlyAdd.appendPage(listOf(song("a2")), playing = true, whole = false)
        assertEquals(listOf("a1", "a2", "x1"), ids(onlyAdd.items))
    }

    @Test
    fun mixesAreNotWalked() {
        assertTrue(isMix("VLRDCLAK5uy_abc"))
        assertTrue(isMix("RDAMVMxyz"))
        assertFalse(isMix("VLPL123"))
        assertFalse(isMix("VLOLAK5uy_abc"))
        assertFalse(isMix(null))
    }

    @Test
    fun unshuffleRestoresOrderAndCurrent() {
        val orig = listOf(song("a"), song("b"), song("c"), song("d"))
        unshuffled(orig, setOf("c"), "c", 9).let { (items, idx) ->
            assertEquals(listOf("a", "b", "c", "d"), ids(items))
            assertEquals(2, idx)
        }
        unshuffled(orig, setOf("a", "b", "c", "d"), "c", 9).let { (items, idx) ->
            assertEquals(listOf("a", "b", "c"), ids(items))
            assertEquals(2, idx)
        }
        unshuffled(orig, setOf("a", "c"), "a", 9).let { (items, idx) ->
            assertEquals(listOf("a", "b", "d"), ids(items))
            assertEquals(0, idx)
        }
        assertEquals(3, unshuffled(orig, emptySet(), "zz", 9).second)

        val withNext = listOf(song("a"), song("b"), song("c"), queued("mine1"), queued("mine2"))
        assertEquals(listOf("a", "mine1", "mine2", "b", "c"), ids(unshuffled(withNext, setOf("a"), "a", 9).first))

        val withAdds = listOf(song("a"), song("b"), added("tail"), song("c"), queued("mine"))
        assertEquals(listOf("a", "mine", "b", "c", "tail"), ids(unshuffled(withAdds, setOf("a"), "a", 9).first))
    }

    @Test
    fun shufflePreservesPrefixAndMultiset() {
        val all = (0 until 10).map { "t$it" }
        val items = all.map(::song).toMutableList()
        shuffleUpcoming(items, 2, false)
        assertEquals(all.take(3), ids(items).take(3))
        assertEquals(all.sorted(), ids(items).sorted())
    }

    @Test
    fun newQueueShufflePlaysClickedTrackFirst() {
        val all = (0 until 10).map { "t$it" }
        val items = all.map(::song).toMutableList()
        assertEquals(0, shuffleNewQueue(items, 7))
        assertEquals("t7", items[0].videoId)
        assertEquals(all.sorted(), ids(items).sorted())
        assertEquals(0, shuffleNewQueue(mutableListOf(), 3))
        val one = mutableListOf(song("only"))
        assertEquals(0, shuffleNewQueue(one, 5))
        assertEquals("only", one[0].videoId)
    }

    @Test
    fun manualAddsSurviveContextSwitch() {
        val items = listOf(queued("q0"), song("b"), queued("q1"), song("c"), queued("q2"))
        assertEquals(listOf("q1", "q2"), ids(upcomingQueued(items, 1)))
        assertTrue(upcomingQueued(listOf(song("a")), 0).isEmpty())
    }

    @Test
    fun shuffleLeavesTheManualQueueBlockInPlace() {
        val items = (listOf(song("now"), queued("q1"), queued("q2")) + (0 until 8).map { song("t$it") }).toMutableList()
        shuffleUpcoming(items, 0, false)
        assertEquals("q1", items[1].videoId)
        assertEquals("q2", items[2].videoId)
        assertEquals(11, items.size)
    }

    @Test
    fun shuffleRandomizesAPlayNextAlbumButNotLooseAdds() {
        var moved = false
        repeat(20) {
            val items =
                (
                    listOf(song("now"), queued("q1"), queued("q2")) +
                        (0 until 12).map { song("a$it").copy(queued = true, queuedFrom = "Album") } +
                        (0 until 4).map { song("t$it") }
                ).toMutableList()
            shuffleUpcoming(items, 0, false)
            assertEquals("q1", items[1].videoId)
            assertEquals("q2", items[2].videoId)
            val block = ids(items.subList(3, 15))
            assertTrue(block.all { it.startsWith("a") })
            moved = moved || block != (0 until 12).map { "a$it" }
            assertEquals(12, block.toSet().size)
        }
        assertTrue(moved, "the album block was never shuffled")
    }

    @Test
    fun shuffleKeepsAutoplayAfterQueueTracks() {
        val items = (listOf(song("now")) + (0 until 4).map { song("p$it") } + (0 until 4).map { auto("a$it") }).toMutableList()
        shuffleUpcoming(items, 0, false)
        assertEquals(listOf(false, false, false, false, true, true, true, true), items.drop(1).map { it.autoplay })
    }

    @Test
    fun shuffleKeepsTailAddsBehindThePlaylistInTheirOrder() {
        val items =
            (
                listOf(song("now")) + (0 until 6).map { song("p$it") } + (0 until 4).map { added("x$it") } +
                    (0 until 2).map { auto("a$it") }
            ).toMutableList()
        shuffleUpcoming(items, 0, false)
        assertEquals((0 until 6).map { "p$it" }, ids(items.subList(1, 7)).sorted())
        assertEquals(listOf("x0", "x1", "x2", "x3"), ids(items.subList(7, 11)))
        assertTrue(items.drop(11).all { it.autoplay })
    }

    @Test
    fun shufflingTheWholeQueueMixesTheAddsIn() {
        val items =
            (
                listOf(song("now"), queued("next")) + (0 until 20).map { song("p$it") } +
                    (0 until 20).map { added("x$it") } + (0 until 2).map { auto("a$it") }
            ).toMutableList()
        val before = ids(items).toSet()
        shuffleUpcoming(items, 0, true)
        assertEquals("next", items[1].videoId)
        assertTrue(items.drop(42).all { it.autoplay })
        assertEquals(before, ids(items).toSet())
        assertTrue(items.subList(2, 22).any { it.queuedEnd }, "adds are mixed into the playlist")
    }

    @Test
    fun radioSeedFromSource() {
        assertEquals("RDCLAK5uy_x", radioSeedFor("VLRDCLAK5uy_x"))
        assertEquals("RDAMPLPL123", radioSeedFor("VLPL123"))
        assertEquals("RDAMPLOLAK5uy_x", radioSeedFor("OLAK5uy_x"))
        assertNull(radioSeedFor(null))
        assertNull(radioSeedFor("${LOCAL_PLAYLIST_ID}7"))
    }

    @Test
    fun autoplayMergeDedupesAndCaps() {
        val items = mutableListOf(song("a"), song("b"))
        val existing = ids(items).toMutableSet()
        val fresh = listOf(song("a"), song("c"), song("c"), song("d"), song("e"))
        assertEquals(2, mergeRadio(items, fresh, existing.toMutableSet(), 2))
        assertEquals(listOf("a", "b", "c", "d"), ids(items))
        assertTrue(items[2].autoplay && items[3].autoplay)
        assertFalse(items[0].autoplay || items[1].autoplay)
        assertEquals(0, mergeRadio(items.toMutableList(), listOf(song("a")), existing, 20))
    }

    @Test
    fun radioPutsUnheardSongsFirst() {
        val items = listOf("a", "b", "c", "d", "e").map(::song).toMutableList()
        freshen(items, mapOf("a" to 300L, "c" to 100L))
        assertEquals(listOf("b", "d", "e", "c", "a"), ids(items))
    }

    @Test
    fun aReplacedQueueComesBackWhereItWasLeft() {
        val q = queue(song("a"), song("b"), song("c"), current = 1)
        q.shuffleOrig = mutableListOf(song("c"), song("b"), song("a"))
        q.sourceName = "Deep cuts"
        q.sourceId = "PL1"
        q.radioSeed = "RDAMPLPL1"

        q.keepContext(42_500)
        q.items = mutableListOf(song("clicked"))
        q.current = 0
        assertNull(q.shuffleOrig)

        val prev = assertNotNull(q.prev)
        q.prev = null
        assertEquals("b" to 42_500L, q.restoreContext(prev))
        assertEquals(listOf("a", "b", "c"), ids(q.items))
        assertEquals(1, q.current)
        assertEquals("PL1", q.sourceId)
        assertEquals("RDAMPLPL1", q.radioSeed)
        assertNotNull(q.shuffleOrig)
    }

    @Test
    fun theBackLineLastsAsLongAsTheRestoreDoes() {
        val q = queue(song("a"), song("b"), current = 1)
        assertNull(q.prevTrackTitle())
        q.keepContext(12_000)
        q.items = mutableListOf(song("clicked"))
        q.current = 0
        assertEquals("b", q.prevTrackTitle())
        q.items.add(song("radio"))
        q.seekTo(1)
        assertNull(q.prevTrackTitle())
        q.seekTo(0)
        assertNull(q.prevTrackTitle())
    }

    @Test
    fun nothingIsKeptFromAnEmptyQueue() {
        val q = QueueState()
        q.keepContext(0)
        assertNull(q.prev)
    }

    // The Android-side entry points, checked against the desktop rules they bundle.

    @Test
    fun aClickedSongCarriesTheManualAddsAndKeepsTheOldQueue() {
        val q = queue(song("a"), song("b"), queued("mine"), song("c"), added("later"), current = 1)
        q.startSong(song("clicked"), positionMs = 5_000, radio = true, sticky = false, gen = 7)
        assertEquals(listOf("clicked", "mine", "later"), ids(q.items))
        assertEquals("clicked Radio", q.sourceName)
        assertEquals(7L, q.hydrating)
        assertEquals("b", q.prevTrackTitle())

        q.absorbSongRadio(listOf(song("clicked"), song("r1"), song("mine"), song("r2")), whole = false)
        assertEquals(listOf("clicked", "mine", "later", "r1", "r2"), ids(q.items))
    }

    @Test
    fun aPlaylistPutsPlayNextAfterTheClickedTrackAndAddsAtTheEnd() {
        val q = queue(song("x"), queued("next"), added("tail"))
        q.startTracks(listOf(song("p0"), song("p1"), song("p2")), 1, "VLPL1", "List", shuffle = false, sticky = false, whole = false, positionMs = 0)
        assertEquals(listOf("p0", "p1", "next", "p2", "tail"), ids(q.items))
        assertEquals(1, q.current)
        assertEquals("RDAMPLPL1", q.radioSeed)
    }

    @Test
    fun aPlayNextOfARowAlreadyQueuedMovesIt() {
        val q = queue(song("now"), queued("a"), queued("b"), song("p1"))
        q.insertQueued(listOf(song("b")), next = true, from = null, dedupe = false, whole = false)
        assertEquals(listOf("now", "a", "b", "p1"), ids(q.items))
        q.insertQueued(listOf(song("z")), next = false, from = null, dedupe = false, whole = false)
        assertEquals(listOf("now", "a", "b", "p1", "z"), ids(q.items))
        assertTrue(q.items.last().queuedEnd)
    }

    @Test
    fun turningShuffleOffDropsWhatItAlreadyPlayed() {
        val q = queue(*(0 until 6).map { song("t$it") }.toTypedArray())
        q.toggleShuffle(whole = false)
        assertTrue(q.shuffled)
        q.current = 3
        val heard = ids(q.items.subList(0, 4)).toSet()
        val playing = q.items[3].videoId
        q.toggleShuffle(whole = false)
        assertFalse(q.shuffled)
        assertEquals(playing, q.items[q.current].videoId)
        assertTrue(q.items.drop(q.current + 1).none { it.videoId in heard })
    }

    @Test
    fun autoplayAppendsMarkedTracksAndMoveMakesThemYours() {
        val q = queue(song("a"), song("b"))
        assertEquals(2, q.absorbAutoplay(listOf(song("b"), song("r1"), song("r2"))))
        assertEquals(listOf("a", "b", "r1", "r2"), ids(q.items))
        assertTrue(q.move(3, 1))
        assertEquals(listOf("a", "r2", "b", "r1"), ids(q.items))
        assertFalse(q.items[1].autoplay)
        assertFalse(q.move(0, 2), "the playing track stays")
    }

    // Desktop's queue panel checks (ui/src/lib/queue.check.ts).

    @Test
    fun theSheetSplitsOnceWhereAutoplayBegins() {
        var v = queueView(listOf(song("a1"), song("now"), queued("p1"), song("n1").copy(queuedEnd = true, queuedFrom = "X"), song("a2"), auto("r1"), auto("r2")), 1)
        assertEquals(5, v.autoplayFrom)
        assertEquals(2, v.queued)
        assertEquals(3, v.leftCount)

        v = queueView(listOf(song("a1"), auto("r1"), auto("now"), auto("r3")), 2)
        assertEquals(3, v.autoplayFrom, "played autoplay rows are ordinary rows")

        v = queueView(listOf(auto("now"), queued("p1"), auto("r2")), 0)
        assertEquals(2, v.autoplayFrom, "a manual add is not autoplay")

        v = queueView(listOf(song("now"), auto("r1"), song("a1"), auto("r2")), 0)
        assertEquals(1, v.autoplayFrom, "once autoplay starts the rest goes with it")

        assertEquals(0, queueView(listOf(queued("p0"), song("now")), 1).queued, "played adds are not upcoming")

        v = queueView(emptyList(), 0)
        assertEquals(0, v.autoplayFrom)
        assertEquals(0, v.leftCount)
    }

    @Test
    fun whatIsLeftCountsOnlyWhatFollowsBeforeAutoplay() {
        assertEquals(225, clockSecs("3:45"))
        assertEquals(3723, clockSecs("1:02:03"))
        assertEquals(0, clockSecs(null))
        assertEquals(0, clockSecs("Cast of EPIC: The Musical"))
        fun timed(
            id: String,
            d: String,
        ) = song(id).copy(duration = d)
        val v = queueView(listOf(timed("a", "9:00"), timed("now", "3:00"), timed("b", "2:30"), timed("c", "1:30"), timed("r", "4:00").copy(autoplay = true)), 1)
        assertEquals(2, v.leftCount)
        assertEquals(240, v.leftSecs)
        assertEquals(0, queueView(listOf(timed("now", "3:00")), 0).leftCount)
    }
}
