package com.maxrave.kotlinytmusicscraper.parser

import com.maxrave.domain.utils.PlaylistSort
import com.maxrave.domain.utils.PlaylistSort.ALBUM
import com.maxrave.domain.utils.PlaylistSort.ARTIST
import com.maxrave.domain.utils.PlaylistSort.DEFAULT
import com.maxrave.domain.utils.PlaylistSort.NEWEST
import com.maxrave.domain.utils.PlaylistSort.OLDEST
import com.maxrave.domain.utils.PlaylistSort.TITLE
import com.maxrave.domain.utils.PlaylistSort.TOP
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

// Desktop's tests for the same table (crates/innertube models/browse.rs).
class PlaylistSortTest {
    // The exact params YouTube's own menu hands out, so a typo in the table cannot pass.
    @Test
    fun paramsMatchTheMenuYouTubeSends() {
        assertEquals("2ggA", DEFAULT.params(false))
        assertEquals("2ggA", DEFAULT.params(true)) // no reversed manual order exists server-side
        assertEquals("2ggECAIQAw==", NEWEST.params(false))
        assertEquals("2ggECAEQAw==", OLDEST.params(false))
        assertEquals("2ggECAIQBA==", TOP.params(false))
        assertEquals("2ggECAEQBQ==", TITLE.params(false))
        assertEquals("2ggECAEQBg==", ARTIST.params(false))
        assertEquals("2ggECAEQBw==", ALBUM.params(false))
        // Date is already directional, so reversing one date sort lands on the other.
        assertEquals(OLDEST.params(false), NEWEST.params(true))
        assertEquals(NEWEST.params(false), OLDEST.params(true))
        for (s in listOf(TITLE, ARTIST, ALBUM, TOP)) assertNotEquals(s.params(false), s.params(true), "$s")
        assertNull(PlaylistSort.PLAYS.params(false))
    }

    // 4/5/6 answer HTTP 500 and wedge the playlist, so the dynamic preference must stay in 1..3.
    @Test
    fun editActionsStayInsideTheEnumsYouTubeAccepts() {
        for (s in PlaylistSort.entries) {
            val a = s.editAction() ?: continue
            a.playlistDynamicSortPreference?.let { assertTrue(it in 1..3, "$s would 500 with pref $it") }
                ?: assertTrue(a.playlistVideoOrder in listOf(0, 1, 2, 6), "$s sends video order ${a.playlistVideoOrder}")
        }
    }

    private fun shelf(menu: String) =
        Json.parseToJsonElement(
            """{"musicSideAlignedItemRenderer":{"startItems":[{"sortFilterSubMenuRenderer":$menu}]}}""",
        )

    @Test
    fun readsTheSelectedSortOffAnOwnedPlaylistsMenu() {
        val menu =
            shelf(
                """{"subMenuItems":[
                {"title":"Manual ordering","selected":false,"serviceEndpoint":{"playlistEditEndpoint":{"playlistId":"PLx",
                  "actions":[{"action":"ACTION_SET_PLAYLIST_VIDEO_ORDER","playlistVideoOrder":0}]}}},
                {"title":"Artist","selected":true,"serviceEndpoint":{"playlistEditEndpoint":{"playlistId":"PLx",
                  "actions":[{"action":"ACTION_SET_PLAYLIST_DYNAMIC_SORT_PREFERENCE","playlistDynamicSortPreference":2}]}}}
                ]}""",
            ).playlistSortMenu()!!
        assertEquals(ARTIST, menu.selected)
        assertTrue(menu.editable)
    }

    // Liked Music and other people's playlists: view-only, and the params come back percent-encoded.
    @Test
    fun readsTheSelectedSortOffAViewOnlyMenu() {
        val menu =
            shelf(
                """{"subMenuItems":[
                {"title":"Default ordering","selected":false,"serviceEndpoint":{"browseEndpoint":{"browseId":"VLLM","params":"2ggA"}}},
                {"title":"Title","selected":true,"serviceEndpoint":{"browseEndpoint":{"browseId":"VLLM","params":"2ggECAEQBQ%3D%3D"}}}
                ]}""",
            ).playlistSortMenu()!!
        assertEquals(TITLE, menu.selected)
        assertFalse(menu.editable)
    }

    @Test
    fun noMenuOnAListYouTubeWillNotReorder() {
        assertNull(Json.parseToJsonElement("""{"musicSideAlignedItemRenderer":{"startItems":[]}}""").playlistSortMenu())
    }
}
