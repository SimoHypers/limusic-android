package com.simohypers.limusic.ui.component

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import com.simohypers.limusic.ui.icon.Home
import com.simohypers.limusic.ui.icon.LibraryMusic
import com.simohypers.limusic.ui.icon.Search
import com.simohypers.limusic.ui.icon.Sensors
import com.simohypers.limusic.ui.icon.LimusicIcons
import com.simohypers.limusic.ui.navigation.destination.home.HomeDestination
import com.simohypers.limusic.ui.navigation.destination.library.LibraryDestination
import com.simohypers.limusic.ui.navigation.destination.library.MixForYouDestination
import com.simohypers.limusic.ui.navigation.destination.search.SearchDestination
import org.jetbrains.compose.resources.StringResource
import limusic.composeapp.generated.resources.Res
import limusic.composeapp.generated.resources.home
import limusic.composeapp.generated.resources.library
import limusic.composeapp.generated.resources.mix
import limusic.composeapp.generated.resources.search

sealed class BottomNavScreen(
    val ordinal: Int,
    val destination: Any,
    val title: StringResource,
    val icon: @Composable () -> Unit,
) {
    data object Home : BottomNavScreen(
        ordinal = 0,
        destination = HomeDestination,
        title = Res.string.home,
        icon = {
            Icon(
                LimusicIcons.Home,
                contentDescription = null,
            )
        },
    )

    data object Search : BottomNavScreen(
        ordinal = 1,
        destination = SearchDestination,
        title = Res.string.search,
        icon = {
            Icon(
                LimusicIcons.Search,
                contentDescription = null,
            )
        },
    )

    data object Library : BottomNavScreen(
        ordinal = 2,
        destination = LibraryDestination,
        title = Res.string.library,
        icon = {
            Icon(
                imageVector = LimusicIcons.LibraryMusic,
                contentDescription = null,
            )
        },
    )

    // Only shown while signed in to YouTube — an anonymous session gets no mixes.
    // Labelled "Mix", not "Mix for you": the full title is the widest label in the bar and forces
    // every tab to be that wide. The screen itself still uses the full title.
    data object MixForYou : BottomNavScreen(
        ordinal = 4,
        destination = MixForYouDestination,
        title = Res.string.mix,
        icon = {
            Icon(
                imageVector = LimusicIcons.Sensors,
                contentDescription = null,
            )
        },
    )
}