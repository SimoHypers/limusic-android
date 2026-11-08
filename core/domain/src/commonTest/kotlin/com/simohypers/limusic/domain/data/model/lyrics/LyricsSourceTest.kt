package com.simohypers.limusic.domain.data.model.lyrics

import kotlin.test.Test
import kotlin.test.assertEquals

class LyricsSourceTest {
    private fun ids(setting: String?) = LyricsSource.encode(LyricsSource.order(setting))

    @Test
    fun orderDropsUnknownIdsAndAddsTheRest() {
        assertEquals("boidu,unison,lyricsplus,lrclib,youtube,netease,qq,kugou", ids(null))
        // SimpMusic's id, from an order saved before it went, is dropped like any unknown one.
        assertEquals("lrclib,-boidu,unison,lyricsplus,youtube,netease,qq,kugou", ids("lrclib, -boidu,simpmusic,lrclib"))
    }

    @Test
    fun everySourceOffStaysOffWhenAReleaseAddsOne() {
        assertEquals(
            "-boidu,-lyricsplus,-lrclib,-youtube,-netease,-qq,-kugou,-unison",
            ids("-boidu,-lyricsplus,-lrclib,-youtube,-simpmusic,-netease,-qq,-kugou"),
        )
    }
}
