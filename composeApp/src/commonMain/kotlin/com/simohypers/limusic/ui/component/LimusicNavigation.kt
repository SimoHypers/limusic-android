package com.simohypers.limusic.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import com.simohypers.limusic.ui.icon.Home
import com.simohypers.limusic.ui.icon.LibraryMusic
import com.simohypers.limusic.ui.icon.Search
import com.simohypers.limusic.ui.icon.LimusicIcons
import com.simohypers.limusic.ui.navigation.destination.home.HomeDestination
import com.simohypers.limusic.ui.navigation.destination.library.LibraryDestination
import com.simohypers.limusic.ui.navigation.destination.search.SearchDestination
import com.simohypers.limusic.ui.theme.typo
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import limusic.composeapp.generated.resources.Res
import limusic.composeapp.generated.resources.home
import limusic.composeapp.generated.resources.library
import limusic.composeapp.generated.resources.search
import kotlin.reflect.KClass

/** One bottom tab: where it goes, what it is called, how it looks. */
private class LimusicTab(
    val destination: Any,
    val label: StringResource,
    val icon: ImageVector,
)

// The three tabs: Home, Search, Library.
private val limusicTabs =
    listOf(
        LimusicTab(HomeDestination, Res.string.home, LimusicIcons.Home),
        LimusicTab(SearchDestination, Res.string.search, LimusicIcons.Search),
        LimusicTab(LibraryDestination, Res.string.library, LimusicIcons.LibraryMusic),
    )

/** Position of [destination] in the tab bar, or -1 when it is not a tab. */
fun limusicTabIndex(destination: NavDestination): Int = limusicTabs.indexOfFirst { destination.hasRoute(it.destination::class) }

/**
 * The bottom tabs. The tab you are in is the last tab on the back stack, so it stays lit on the
 * album or artist pages opened from it. Tapping it there goes back to the tab's root; tapping it on
 * the root calls [onReselect] (scroll to top or refresh).
 */
@Composable
fun LimusicNavigationBar(
    navController: NavController,
    modifier: Modifier = Modifier,
    onReselect: (KClass<*>) -> Unit = {},
) {
    val backStack by navController.currentBackStack.collectAsState()
    val currentTab = backStack.lastOrNull { limusicTabIndex(it.destination) >= 0 }?.destination?.let(::limusicTabIndex) ?: -1
    val onTabRoot = backStack.lastOrNull()?.destination?.let(::limusicTabIndex) == currentTab
    Column(modifier) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.background,
            tonalElevation = 0.dp,
        ) {
            limusicTabs.forEachIndexed { index, tab ->
                val selected = index == currentTab
                NavigationBarItem(
                    selected = selected,
                    onClick = {
                        if (selected && onTabRoot) {
                            onReselect(tab.destination::class)
                        } else if (selected) {
                            // Restoring this tab's saved stack would bring the same page back, so pop to its root.
                            navController.popBackStack(tab.destination, inclusive = false)
                        } else {
                            navController.navigate(tab.destination) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    icon = { Icon(tab.icon, contentDescription = null) },
                    label = { Text(stringResource(tab.label), style = typo().bodySmall) },
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
