package com.simohypers.limusic.kotlinytmusicscraper.cipher

import io.ktor.client.HttpClient
import kotlin.test.Test
import kotlin.test.assertNotNull

class PlayerConfigSourcesTest {
    /** A table the store's whitelist turns away is never fetched, and nothing logs why. */
    @Test
    fun everyTablePassesTheStoresGate() {
        val store = RemotePlayerConfigStore(HttpClient(), InMemoryPlayerConfigRepository(""))
        for (url in FaradayCipherEngine.PLAYER_CONFIG_URLS) assertNotNull(store.validatedSourceUrlOrNull(url), url)
    }
}
