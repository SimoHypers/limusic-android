package com.simohypers.limusic.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.toRoute
import com.simohypers.limusic.common.LibraryChipType
import com.simohypers.limusic.ui.navigation.destination.home.SettingsDestination
import com.simohypers.limusic.ui.navigation.destination.library.LibraryDynamicPlaylistDestination
import com.simohypers.limusic.ui.navigation.destination.list.PlaylistDestination
import com.simohypers.limusic.ui.theme.typo
import org.jetbrains.compose.resources.stringResource
import kotlin.reflect.KClass

val BottomNavScreen.libraryChip: LibraryChipType?
    get() = when (this) {
        BottomNavScreen.Playlists -> LibraryChipType.LOCAL_PLAYLIST
        BottomNavScreen.Albums -> LibraryChipType.ALBUMS
        BottomNavScreen.Artists -> LibraryChipType.ARTISTS
        BottomNavScreen.Songs -> LibraryChipType.SONGS
        BottomNavScreen.Downloads -> LibraryChipType.DOWNLOADED_PLAYLIST
        BottomNavScreen.Podcasts -> LibraryChipType.FAVORITE_PODCAST
        else -> null
    }

fun NavBackStackEntry.matchesTab(
    tab: BottomNavScreen,
    isYouTubeLoggedIn: Boolean = false,
): Boolean {
    val dest = destination
    return when {
        tab == BottomNavScreen.LikedSongs -> {
            if (isYouTubeLoggedIn) {
                dest.hasRoute(PlaylistDestination::class) && try {
                    toRoute<PlaylistDestination>().playlistId == "LM"
                } catch (_: Exception) {
                    false
                }
            } else {
                dest.hasRoute(LibraryDynamicPlaylistDestination::class)
            }
        }
        dest.hasRoute(LibraryDynamicPlaylistDestination::class) ->
            tab.destination is LibraryDynamicPlaylistDestination
        else ->
            dest.hasRoute(tab.destination::class)
    }
}

/** Position of [entry]'s tab in [tabs], or -1 when it is not a tab. */
fun limusicTabIndex(
    entry: NavBackStackEntry,
    tabs: List<BottomNavScreen> = BottomNavScreen.allTabs,
    isYouTubeLoggedIn: Boolean = false,
): Int =
    tabs.indexOfFirst { tab -> entry.matchesTab(tab, isYouTubeLoggedIn) }

/** Position of [destination] in the tab bar, or -1 when it is not a tab. */
fun limusicTabIndex(
    destination: NavDestination,
    tabs: List<BottomNavScreen> = BottomNavScreen.allTabs,
    isYouTubeLoggedIn: Boolean = false,
): Int =
    tabs.indexOfFirst { tab ->
        if (tab == BottomNavScreen.LikedSongs) {
            if (isYouTubeLoggedIn) {
                destination.hasRoute(PlaylistDestination::class)
            } else {
                destination.hasRoute(LibraryDynamicPlaylistDestination::class)
            }
        } else if (tab.destination is LibraryDynamicPlaylistDestination) {
            destination.hasRoute(LibraryDynamicPlaylistDestination::class)
        } else {
            destination.hasRoute(tab.destination::class)
        }
    }

/**
 * Opens a tab the way tapping it does. A tab opened with a plain navigate (an app link) sits on
 * Home's stack, and the next Home tap then saves and restores it straight back.
 */
fun NavController.navigateToTab(destination: Any) =
    navigate(destination) {
        popUpTo(graph.startDestinationId) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }

/**
 * The bottom tabs, the user's [tabs] in their order. The tab you are in is the last tab on the back
 * stack, so it stays lit on the album or artist pages opened from it. Tapping it there goes back to
 * the tab's root; tapping it on the root calls [onReselect] (scroll to top or refresh). [compact] is
 * the "Compact navigation bar" setting: icons only, centred in a 56dp bar. Off, the bar keeps
 * Material's own 80dp and labels.
 */
@Composable
fun LimusicNavigationBar(
    navController: NavController,
    tabs: List<BottomNavScreen>,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    currentLibraryChip: LibraryChipType = LibraryChipType.YOUR_LIBRARY,
    isYouTubeLoggedIn: Boolean = false,
    onLibraryTabRequest: (LibraryChipType?, Boolean) -> Unit = { _, _ -> },
    onReselect: (KClass<*>) -> Unit = {},
) {
    val backStack by navController.currentBackStack.collectAsState()
    val tabEntry = backStack.indexOfLast { entry ->
        tabs.any { tab -> entry.matchesTab(tab, isYouTubeLoggedIn) }
    }
    val stackTab = backStack.getOrNull(tabEntry)?.let { entry ->
        limusicTabIndex(entry, tabs, isYouTubeLoggedIn)
    } ?: -1
    // Settings and the pages opened from it belong to no tab, so none is lit there (unless Settings is a bottom tab)
    val inSettings = backStack.indexOfLast { it.destination.hasRoute(SettingsDestination::class) } > tabEntry
    val isSettingsTab = tabs.any { it.destination is SettingsDestination }
    val currentTab = if (inSettings && !isSettingsTab) -1 else stackTab
    val onTabRoot = backStack.lastOrNull()?.let { entry ->
        tabs.getOrNull(currentTab)?.let { tab -> entry.matchesTab(tab, isYouTubeLoggedIn) }
    } == true
    Column(modifier) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        NavigationBar(
            // NavigationBar pads itself by the system inset, so its height has to include it.
            modifier =
                if (compact) {
                    Modifier.height(CompactBarHeight + NavigationBarDefaults.windowInsets.asPaddingValues().calculateBottomPadding())
                } else {
                    Modifier
                },
            containerColor = MaterialTheme.colorScheme.background,
            tonalElevation = 0.dp,
        ) {
            tabs.forEachIndexed { index, tab ->
                val selected = index == currentTab
                NavigationBarItem(
                    selected = selected,
                    onClick = {
                        val targetDestination = if (tab == BottomNavScreen.LikedSongs && isYouTubeLoggedIn) {
                            PlaylistDestination("LM", isYourYouTubePlaylist = true)
                        } else {
                            tab.destination
                        }

                        val libraryChip = tab.libraryChip
                        if (libraryChip != null) {
                            onLibraryTabRequest(libraryChip, true)
                        } else {
                            onLibraryTabRequest(null, false)
                        }

                        val targetOnStack = backStack.any { entry ->
                            if (tab == BottomNavScreen.LikedSongs && isYouTubeLoggedIn) {
                                entry.destination.hasRoute(PlaylistDestination::class) && try {
                                    entry.toRoute<PlaylistDestination>().playlistId == "LM"
                                } catch (e: Exception) {
                                    false
                                }
                            } else if (tab.destination is LibraryDynamicPlaylistDestination) {
                                entry.destination.hasRoute(LibraryDynamicPlaylistDestination::class)
                            } else {
                                entry.destination.hasRoute(tab.destination::class)
                            }
                        }

                        if (selected && onTabRoot) {
                            onReselect(targetDestination::class)
                        } else if (index == stackTab) {
                            // Restoring this tab's saved stack would bring the same page back, so pop to its root.
                            navController.popBackStack(targetDestination, inclusive = false)
                        } else if (targetOnStack) {
                            // Target tab is already in the back stack under a sub-screen, pop back to it.
                            navController.popBackStack(targetDestination, inclusive = false)
                        } else {
                            navController.navigateToTab(targetDestination)
                        }
                    },
                    // Without its label the icon names the tab, for screen readers.
                    icon = { Icon(tab.icon, contentDescription = if (compact) stringResource(tab.title) else null) },
                    label =
                        if (compact) {
                            null
                        } else {
                            { Text(stringResource(tab.title), style = typo().bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                        },
                    colors =
                        NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.onSurface,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                )
            }
        }
    }
}

// Each item holds itself at M3's 80dp unless the bar caps it. With no label, 56dp centres the
// icon and its 32dp indicator with 12dp either side.
private val CompactBarHeight = 56.dp

/**
 * The top bar Home and Library share: the title on the left, the tab's own [actions] on the right. Draws below the status bar and nothing behind itself, so the screen decides the ground.
 */
@Composable
fun LimusicTabTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(64.dp)
                .padding(start = 16.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = typo().titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Text(text = subtitle, style = typo().bodySmall, maxLines = 1)
            }
        }
        actions()
    }
}
