package com.simohypers.limusic.kotlinytmusicscraper.parser

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class LibraryParserTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }

    private val thumb = """{"musicThumbnailRenderer":{"thumbnail":{"thumbnails":[{"url":"u","width":226,"height":226}]}}}"""

    // Shaped like FEmusic_liked_albums: cards in a grid, the next page as nextContinuationData.
    @Test
    fun cardsAndTheOldTokenWhereverTheySit() {
        val page =
            parseLibraryPage(
                json.parseToJsonElement(
                    """{"contents":{"singleColumnBrowseResultsRenderer":{"tabs":[{"tabRenderer":{"content":{"sectionListRenderer":{"contents":[
                    {"itemSectionRenderer":{"contents":[{"chipCloudRenderer":{}}]}},
                    {"gridRenderer":{"items":[
                      {"musicTwoRowItemRenderer":{"title":{"runs":[{"text":"folklore"}]},"thumbnailRenderer":$thumb,
                        "navigationEndpoint":{"browseEndpoint":{"browseId":"MPREb_a"}}}},
                      {"musicTwoRowItemRenderer":{"title":{"runs":[{"text":"evermore"}]},"thumbnailRenderer":$thumb,
                        "navigationEndpoint":{"browseEndpoint":{"browseId":"MPREb_b"}}}}
                    ],"continuations":[{"nextContinuationData":{"continuation":"OLD"}}]}}
                    ]}}}}]}}}""",
                ),
                json,
            )
        assertEquals(listOf("folklore", "evermore"), page.cards.map { it.title.runs?.first()?.text })
        assertEquals(0, page.rows.size)
        assertEquals("OLD", page.continuation)
    }

    // Shaped like a shelf continuation: rows, then a continuationItemRenderer carrying the token.
    @Test
    fun rowsAndTheNewToken() {
        val page =
            parseLibraryPage(
                json.parseToJsonElement(
                    """{"onResponseReceivedActions":[{"appendContinuationItemsAction":{"continuationItems":[
                      {"musicResponsiveListItemRenderer":{"flexColumns":[{"musicResponsiveListItemFlexColumnRenderer":{"text":{"runs":[{"text":"Taylor Swift"}]}}}],
                        "thumbnail":$thumb,"navigationEndpoint":{"browseEndpoint":{"browseId":"UCa"}}}},
                      {"continuationItemRenderer":{"continuationEndpoint":{"continuationCommand":{"token":"NEW"}}}}
                    ]}}]}""",
                ),
                json,
            )
        assertEquals(1, page.rows.size)
        assertEquals("NEW", page.continuation)
    }

    @Test
    fun lastPageHasNoToken() {
        assertEquals(null, parseLibraryPage(json.parseToJsonElement("""{"contents":{}}"""), json).continuation)
    }
}
