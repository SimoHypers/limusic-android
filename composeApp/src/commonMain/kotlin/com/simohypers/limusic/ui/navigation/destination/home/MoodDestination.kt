package com.simohypers.limusic.ui.navigation.destination.home

import kotlinx.serialization.Serializable

@Serializable
data class MoodDestination(
    val params: String,
    // What the Search tile looked like, so the page opens in its colours. Other callers leave them
    // out and the page falls back to the accent and its first cover.
    val color: Long? = null,
    val artwork: String? = null,
    // The Search section it came from ("Moods & moments", "Genres"), shown over the name.
    val kicker: String? = null,
)