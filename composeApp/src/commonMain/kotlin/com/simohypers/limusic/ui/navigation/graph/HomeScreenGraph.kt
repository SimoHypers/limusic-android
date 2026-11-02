package com.simohypers.limusic.ui.navigation.graph

import com.simohypers.limusic.ui.screen.home.DevicesSettingsScreen
import com.simohypers.limusic.ui.navigation.destination.home.DevicesSettingsDestination
import androidx.compose.foundation.layout.PaddingValues
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.simohypers.limusic.ui.navigation.destination.home.AppLogDestination
import com.simohypers.limusic.ui.navigation.destination.home.CreditDestination
import com.simohypers.limusic.ui.navigation.destination.home.ListenTogetherDestination
import com.simohypers.limusic.ui.navigation.destination.home.MoodDestination
import com.simohypers.limusic.ui.navigation.destination.home.NotificationDestination
import com.simohypers.limusic.ui.navigation.destination.home.RecentlySongsDestination
import com.simohypers.limusic.ui.navigation.destination.home.SettingsDestination
import com.simohypers.limusic.ui.navigation.destination.home.DiscordSettingsDestination
import com.simohypers.limusic.ui.navigation.destination.home.LyricsSourcesDestination
import com.simohypers.limusic.ui.navigation.destination.home.LastfmSettingsDestination
import com.simohypers.limusic.ui.navigation.destination.home.SettingsSectionDestination
import com.simohypers.limusic.ui.screen.home.ListenTogetherScreen
import com.simohypers.limusic.ui.screen.home.MoodScreen
import com.simohypers.limusic.ui.screen.home.NotificationScreen
import com.simohypers.limusic.ui.screen.home.RecentlySongsScreen
import com.simohypers.limusic.ui.screen.home.SettingScreen
import com.simohypers.limusic.ui.screen.home.DiscordSettingsScreen
import com.simohypers.limusic.ui.screen.home.LyricsSourcesScreen
import com.simohypers.limusic.ui.screen.home.LastfmSettingsScreen
import com.simohypers.limusic.ui.screen.home.SettingsSection
import com.simohypers.limusic.ui.screen.other.AppLogScreen
import com.simohypers.limusic.ui.screen.other.CreditScreen

fun NavGraphBuilder.homeScreenGraph(
    innerPadding: PaddingValues,
    navController: NavController,
) {
    composable<CreditDestination> {
        CreditScreen(
            paddingValues = innerPadding,
            navController = navController,
        )
    }
    composable<AppLogDestination> {
        AppLogScreen(
            paddingValues = innerPadding,
            navController = navController,
        )
    }
    composable<MoodDestination> { entry ->
        val route = entry.toRoute<MoodDestination>()
        MoodScreen(
            navController = navController,
            params = route.params,
            color = route.color,
            artwork = route.artwork,
            kicker = route.kicker,
        )
    }
    composable<ListenTogetherDestination> {
        ListenTogetherScreen(
            navController = navController,
            innerPadding = innerPadding,
        )
    }
    composable<NotificationDestination> {
        NotificationScreen(
            navController = navController,
        )
    }
    composable<RecentlySongsDestination> {
        RecentlySongsScreen(
            navController = navController,
            innerPadding = innerPadding,
        )
    }
    composable<SettingsDestination> {
        SettingScreen(
            navController = navController,
            innerPadding = innerPadding,
        )
    }
    composable<LastfmSettingsDestination> {
        LastfmSettingsScreen(
            navController = navController,
            innerPadding = innerPadding,
        )
    }
    composable<LyricsSourcesDestination> {
        LyricsSourcesScreen(
            navController = navController,
            innerPadding = innerPadding,
        )
    }
    composable<DevicesSettingsDestination> {
        DevicesSettingsScreen(
            navController = navController,
            innerPadding = innerPadding,
        )
    }
    composable<DiscordSettingsDestination> {
        DiscordSettingsScreen(
            navController = navController,
            innerPadding = innerPadding,
        )
    }
    composable<SettingsSectionDestination> { entry ->
        SettingScreen(
            navController = navController,
            innerPadding = innerPadding,
            section = SettingsSection.valueOf(entry.toRoute<SettingsSectionDestination>().section),
        )
    }
}