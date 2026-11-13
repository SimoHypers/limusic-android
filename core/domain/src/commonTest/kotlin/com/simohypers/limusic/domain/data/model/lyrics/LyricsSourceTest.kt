package com.simohypers.limusic.domain.data.model.lyrics

import kotlin.test.Test
import kotlin.test.assertEquals

class LyricsSourceTest {
    private fun ids(setting: String?) = LyricsSource.encode(LyricsSource.order(setting))

    @Test
    fun orderDropsUnknownIdsAndAddsTheRest() {
        assertEquals("limusic,boidu,unison,lyricsplus,lrclib,youtube,netease,qq,kugou", ids(null))
        // SimpMusic's id, from an order saved before it went, is dropped like any unknown one.
        // Limusic, not named yet, goes first; the rest that aren't named go last.
        assertEquals("limusic,lrclib,-boidu,unison,lyricsplus,youtube,netease,qq,kugou", ids("lrclib, -boidu,simpmusic,lrclib"))
        // Named, it stays where the user put it, on or off.
        assertEquals("lrclib,limusic,boidu,unison,lyricsplus,youtube,netease,qq,kugou", ids("lrclib,limusic,boidu"))
        assertEquals("boidu,-limusic,unison,lyricsplus,lrclib,youtube,netease,qq,kugou", ids("boidu,-limusic"))
    }

    @Test
    fun everySourceOffStaysOffWhenAReleaseAddsOne() {
        assertEquals(
            "-limusic,-boidu,-lyricsplus,-lrclib,-youtube,-netease,-qq,-kugou,-unison",
            ids("-boidu,-lyricsplus,-lrclib,-youtube,-simpmusic,-netease,-qq,-kugou"),
        )
    }
}
