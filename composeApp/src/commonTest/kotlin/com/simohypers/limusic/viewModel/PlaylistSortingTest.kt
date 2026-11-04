package com.simohypers.limusic.viewModel

import com.simohypers.limusic.domain.data.entities.PairSongLocalPlaylist
import com.simohypers.limusic.domain.data.entities.SongEntity
import com.simohypers.limusic.domain.data.model.browse.album.Track
import com.simohypers.limusic.domain.data.model.searchResult.songs.Album
import com.simohypers.limusic.domain.data.model.searchResult.songs.Artist
import com.simohypers.limusic.domain.utils.PlaylistSort
import com.simohypers.limusic.domain.utils.PlaylistSort.ALBUM
import com.simohypers.limusic.domain.utils.PlaylistSort.ARTIST
import com.simohypers.limusic.domain.utils.PlaylistSort.DEFAULT
import com.simohypers.limusic.domain.utils.PlaylistSort.NEWEST
import com.simohypers.limusic.domain.utils.PlaylistSort.OLDEST
import com.simohypers.limusic.domain.utils.PlaylistSort.PLAYS
import com.simohypers.limusic.domain.utils.PlaylistSort.TITLE
import com.simohypers.limusic.domain.utils.PlaylistSort.TOP
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.datetime.LocalDateTime

// Desktop's ui/src/lib/sort.check.ts, case for case.
class PlaylistSortingTest {
    private fun song(
        id: String,
        title: String,
        artist: String,
        album: String? = null,
    ) = Track(
        album = album?.let { Album(id = "", name = it) },
        artists = listOf(Artist(id = null, name = artist)),
        duration = null,
        durationSeconds = null,
        isAvailable = true,
        isExplicit = false,
        likeStatus = null,
        thumbnails = null,
        title = title,
        videoId = id,
        videoType = null,
        category = null,
        feedbackTokens = null,
        resultType = null,
    )

    // Deliberately messy: mixed case, a numeric run, a missing album, two tracks that tie on artist.
    private val items =
        listOf(
            song("a", "Zebra", "beta", "Second"),
            song("b", "track 10", "Alpha", "First"),
            song("c", "track 9", "Alpha"),
            song("d", "apple", "Gamma", "First"),
        )
    private val tied = listOf(song("x", "Same", "Alpha"), song("y", "Same", "Alpha"))
    private val counts = mapOf("b" to 4L, "d" to 1L)

    private fun ids(list: List<Track>) = list.joinToString("") { it.videoId }

    private fun sorted(
        sort: PlaylistSort,
        newestFirst: Boolean = false,
        desc: Boolean = false,
        plays: Map<String, Long> = counts,
        list: List<Track> = items,
    ) = ids(sortTracks(list, sort, newestFirst, desc, plays))

    @Test
    fun defaultIsANoOpAndNothingIsLostOrDuplicated() {
        assertSame(items, sortTracks(items, DEFAULT, false))
        for (sort in PlaylistSort.entries) {
            for (desc in listOf(false, true)) {
                assertEquals("abcd", sorted(sort, desc = desc).toList().sorted().joinToString(""), "$sort desc=$desc")
            }
        }
        assertEquals("abcd", ids(items), "the input is still in playlist order")
    }

    @Test
    fun addOrder() {
        assertEquals("abcd", sorted(OLDEST))
        assertEquals("dcba", sorted(NEWEST))
        assertEquals("abcd", sorted(NEWEST, newestFirst = true), "Liked Music")
        assertEquals("dcba", sorted(OLDEST, newestFirst = true))
    }

    @Test
    fun keys() {
        assertEquals("dcba", sorted(TITLE), "apple, track 9, track 10, Zebra")
        assertEquals("cbad", sorted(ARTIST), "artist groups, title orders inside a group")
        assertEquals("xy", sorted(ARTIST, list = tied), "a total tie keeps playlist order")
        assertEquals("bdac", sorted(ALBUM), "First, Second, then no album; playlist order inside one")
    }

    @Test
    fun descendingReversesTheKeyOnly() {
        assertEquals("dcba", sorted(DEFAULT, desc = true))
        assertEquals("abcd", sorted(NEWEST, desc = true))
        assertEquals("abcd", sorted(OLDEST, newestFirst = true, desc = true))
        assertEquals("abcd", sorted(TITLE, desc = true))
        assertEquals("dabc", sorted(ARTIST, desc = true), "the tiebreak flips too")
        assertEquals("abdc", sorted(ALBUM, desc = true), "albums reverse, not tracks; albumless still last")
        assertEquals("xy", sorted(ARTIST, desc = true, list = tied))
    }

    @Test
    fun mostPlayed() {
        assertEquals("bdac", sorted(PLAYS))
        assertEquals("acdb", sorted(PLAYS, desc = true), "least played first")
        assertEquals("abcd", sorted(PLAYS, plays = emptyMap()))
        assertEquals("bdac", sorted(PLAYS, plays = counts + ("zzz" to 99L)), "stray ids ignored")
    }

    @Test
    fun topVotedHasNoLocalForm() {
        assertSame(items, sortTracks(items, TOP, false))
        assertEquals("dcba", sorted(TOP, desc = true))
    }

    @Test
    fun whatYouTubeStoresAndReproduces() {
        assertNull(persistedSort(PLAYS, false))
        assertEquals(NEWEST, persistedSort(NEWEST, false))
        assertEquals(OLDEST, persistedSort(NEWEST, true))
        assertEquals(NEWEST, persistedSort(OLDEST, true))
        for (s in listOf(DEFAULT, TITLE, ARTIST, ALBUM)) {
            assertEquals(s, persistedSort(s, false))
            assertEquals(s, persistedSort(s, true), "$s descending stores the ascending sort")
            assertTrue(storedExactly(s, false))
            assertFalse(storedExactly(s, true), "$s descending has to be remembered on the phone")
        }
        assertFalse(storedExactly(PLAYS, false))
        assertTrue(storedExactly(NEWEST, true) && storedExactly(OLDEST, true))
        assertEquals(DEFAULT, fetchSort(PLAYS))
        for (s in PlaylistSort.entries - PLAYS) assertEquals(s, fetchSort(s))
    }

    // Phone playlists: rows know when they were added, and a drag has put the newest song first.
    private fun row(
        rowId: Int,
        id: String,
        title: String,
        day: Int,
        plays: Long = 0,
    ) = SongEntity(
        videoId = id,
        title = title,
        totalPlayTime = plays,
        duration = "",
        durationSeconds = 0,
        isAvailable = true,
        isExplicit = false,
        likeStatus = "",
        videoType = "",
        category = null,
        resultType = null,
    ) to PairSongLocalPlaylist(id = rowId, playlistId = 1, songId = id, position = rowId, inPlaylist = LocalDateTime(2026, 1, day, 0, 0))

    private val stored = listOf(row(0, "c", "Cherry", 3, plays = 2), row(1, "a", "Apple", 1), row(2, "b", "Banana", 2, plays = 5), row(3, "a", "Apple", 4))

    private fun phone(
        sort: PlaylistSort,
        desc: Boolean = false,
    ) = sortPhonePlaylist(stored, ListSort(sort, desc)).joinToString("") { it.second.id.toString() }

    @Test
    fun phonePlaylists() {
        assertSame(stored, sortPhonePlaylist(stored, ListSort()), "Custom order is the stored order")
        assertEquals("3210", phone(DEFAULT, desc = true))
        assertEquals("3021", phone(NEWEST), "by add date, not by position")
        assertEquals("1203", phone(OLDEST))
        assertEquals("1320", phone(TITLE), "the same song twice keeps both rows")
        assertEquals("2013", phone(PLAYS))
    }

    @Test
    fun storedSortRoundTrips() {
        for (sort in PLAYLIST_SORTS) {
            for (desc in listOf(false, true)) assertEquals(ListSort(sort, desc), ListSort.parse(ListSort(sort, desc).stored()))
        }
        assertEquals("", ListSort().stored())
        assertEquals(ListSort(), ListSort.parse(null))
        assertEquals(ListSort(), ListSort.parse("older_first"), "the old global setting's values mean nothing")
        assertEquals(ListSort(), ListSort.parse("ALBUM", DOWNLOAD_SORTS), "a sort the list does not offer")
    }
}
