package com.maxrave.kotlinytmusicscraper.pages

import com.maxrave.kotlinytmusicscraper.models.MusicResponsiveListItemRenderer
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class PlayCountTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }

    private fun row(third: String) =
        json.decodeFromString<MusicResponsiveListItemRenderer>(
            """{"flexColumns":[
              {"musicResponsiveListItemFlexColumnRenderer":{"text":{"runs":[{"text":"Song"}]}}},
              {"musicResponsiveListItemFlexColumnRenderer":{"text":{"runs":[{"text":"Artist"}]}}},
              {"musicResponsiveListItemFlexColumnRenderer":{"text":{"runs":[$third]}}}
            ]}""",
        )

    @Test
    fun onlyAnUnlinkedNumberIsAPlayCount() {
        assertEquals("53M plays", row("""{"text":"53M plays"}""").playCount())
        assertEquals("1,234 plays", row("""{"text":"1,234 plays"}""").playCount())
        assertEquals(null, row("""{"text":"The Album"}""").playCount())
        assertEquals(null, row("""{"text":"3:02"}""").playCount())
        // An album whose name starts with a digit links to the album; it is not a count.
        assertEquals(null, row("""{"text":"1989","navigationEndpoint":{"browseEndpoint":{"browseId":"MPREb_x"}}}""").playCount())
    }
}
