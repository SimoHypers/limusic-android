package com.maxrave.simpmusic.ui.navigation.destination.home

import kotlinx.serialization.Serializable

/** One page of Settings; [section] is a `SettingsSection` name. */
@Serializable
data class SettingsSectionDestination(
    val section: String,
)
