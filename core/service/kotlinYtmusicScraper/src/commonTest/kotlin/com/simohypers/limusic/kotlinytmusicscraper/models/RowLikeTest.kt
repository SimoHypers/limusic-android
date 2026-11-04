package com.simohypers.limusic.kotlinytmusicscraper.models

import com.simohypers.limusic.kotlinytmusicscraper.models.response.LikeStatus
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** A playlist row's like button states the account's like; a row without one states nothing. */
class RowLikeTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }

    private fun menu(buttons: String) =
        json.decodeFromString<Menu>("""{"menuRenderer": {"items": [], "topLevelButtons": [$buttons]}}""")

    private fun like(
        status: String,
        allowed: Boolean = true,
    ) = """{"likeButtonRenderer": {"likeStatus": "$status", "likesAllowed": $allowed}}"""

    @Test
    fun readsTheRowsOwnLike() {
        assertEquals(LikeStatus.LIKE, menu(like("LIKE")).likeStatus())
        assertEquals(LikeStatus.INDIFFERENT, menu(like("INDIFFERENT")).likeStatus())
        assertEquals(LikeStatus.DISLIKE, menu(like("DISLIKE")).likeStatus())
    }

    @Test
    fun aRowThatStatesNothingIsNotUnliked() {
        assertNull((null as Menu?).likeStatus())
        assertNull(menu("").likeStatus())
        assertNull(menu(like("INDIFFERENT", allowed = false)).likeStatus())
    }
}
