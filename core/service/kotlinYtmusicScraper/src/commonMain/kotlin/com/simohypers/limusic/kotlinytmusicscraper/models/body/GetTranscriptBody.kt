package com.simohypers.limusic.kotlinytmusicscraper.models.body

import com.simohypers.limusic.kotlinytmusicscraper.models.Context
import kotlinx.serialization.Serializable

@Serializable
data class GetTranscriptBody(
    val context: Context,
    val params: String,
)