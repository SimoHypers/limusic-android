package com.maxrave.simpmusic.ui.screen.home

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.maxrave.common.CHART_SUPPORTED_COUNTRY
import com.maxrave.simpmusic.ui.component.CenterLoadingBox
import com.maxrave.simpmusic.ui.component.DropdownButton
import com.maxrave.simpmusic.ui.component.EndOfPage
import com.maxrave.simpmusic.ui.component.HomeItem
import com.maxrave.simpmusic.ui.component.HomeShimmer
import com.maxrave.simpmusic.ui.component.LimusicTabTopBar
import com.maxrave.simpmusic.ui.navigation.destination.home.ExploreDestination
import com.maxrave.simpmusic.viewModel.ExploreViewModel
import com.maxrave.simpmusic.viewModel.SharedViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.explore

/** The Explore tab: new releases, moods & genres and the charts, which used to trail the Home feed. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    innerPadding: PaddingValues,
    navController: NavController,
    viewModel: ExploreViewModel = koinViewModel(),
    sharedViewModel: SharedViewModel = koinInject(),
) {
    val newRelease by viewModel.newRelease.collectAsStateWithLifecycle()
    val mood by viewModel.mood.collectAsStateWithLifecycle()
    val chart by viewModel.chart.collectAsStateWithLifecycle()
    val chartRegion by viewModel.chartRegion.collectAsStateWithLifecycle()
    val loadingChart by viewModel.loadingChart.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val reloadDestination by sharedViewModel.reloadDestination.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    // Tapping the Explore tab again: back to the top, or a refresh when already there.
    LaunchedEffect(reloadDestination) {
        if (reloadDestination == ExploreDestination::class) {
            if (listState.firstVisibleItemIndex > 0) listState.animateScrollToItem(0) else viewModel.refresh()
            sharedViewModel.reloadDestinationDone()
        }
    }

    Column(Modifier.fillMaxSize()) {
        LimusicTabTopBar(title = stringResource(Res.string.explore), navController = navController)
        PullToRefreshBox(
            isRefreshing = loading && newRelease.isNotEmpty(),
            onRefresh = viewModel::refresh,
            modifier = Modifier.fillMaxSize(),
        ) {
            if (loading && newRelease.isEmpty() && mood == null) {
                HomeShimmer()
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(bottom = innerPadding.calculateBottomPadding()),
                ) {
                    items(newRelease, key = { it.hashCode() }) { item ->
                        Box(Modifier.padding(horizontal = 15.dp)) {
                            HomeItem(navController = navController, data = item)
                        }
                    }
                    mood?.let { moods ->
                        item(key = "moods") {
                            Box(Modifier.padding(horizontal = 15.dp)) {
                                MoodMomentAndGenre(mood = moods, navController = navController)
                            }
                        }
                    }
                    item(key = "chart") {
                        Column(Modifier.padding(horizontal = 15.dp, vertical = 10.dp)) {
                            ChartTitle()
                            Spacer(Modifier.height(5.dp))
                            chartRegion?.let { region ->
                                DropdownButton(
                                    items = CHART_SUPPORTED_COUNTRY.itemsData.toList(),
                                    defaultSelected =
                                        CHART_SUPPORTED_COUNTRY.itemsData.getOrNull(CHART_SUPPORTED_COUNTRY.items.indexOf(region))
                                            ?: CHART_SUPPORTED_COUNTRY.itemsData[1],
                                ) { selected ->
                                    viewModel.exploreChart(
                                        CHART_SUPPORTED_COUNTRY.items[CHART_SUPPORTED_COUNTRY.itemsData.indexOf(selected)],
                                    )
                                }
                            }
                            Spacer(Modifier.height(5.dp))
                            Crossfade(targetState = loadingChart, label = "Chart") { isLoading ->
                                if (isLoading) {
                                    CenterLoadingBox(Modifier.fillMaxWidth().height(400.dp))
                                } else {
                                    chart?.let { ChartData(chart = it, navController = navController) }
                                }
                            }
                        }
                    }
                    item(key = "end") { EndOfPage() }
                }
            }
        }
    }
}
