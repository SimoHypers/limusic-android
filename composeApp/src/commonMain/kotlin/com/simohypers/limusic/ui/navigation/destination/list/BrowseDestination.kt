package com.simohypers.limusic.ui.navigation.destination.list

import kotlinx.serialization.Serializable

@Serializable
data class BrowseDestination(
    val browseId: String,
    val params: String? = null,
    /** The home section's own title, shown when the page names none. */
    val title: String? = null,
    /** The section's small line over its title, shown over the name. */
    val kicker: String? = null,
)
