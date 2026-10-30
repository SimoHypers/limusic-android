package com.simohypers.limusic.domain.data.player

/**
 * Generic player error wrapper
 */
data class PlayerError(
    val errorCode: Int,
    val errorCodeName: String,
    val message: String?,
    /** YouTube's reason when its player refused the video ([PlaybackRefusals]); null otherwise. */
    val refusal: String? = null,
)