package com.simohypers.limusic.kotlinytmusicscraper.models.body

import com.simohypers.limusic.kotlinytmusicscraper.models.Context
import kotlinx.serialization.Serializable

@Serializable
data class GetSearchSuggestionsBody(
    val context: Context,
    val input: String,
)