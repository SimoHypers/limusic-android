package com.simohypers.limusic.ui.screen.other

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.simohypers.limusic.logger.Logger
import com.simohypers.limusic.extension.ultraThinBarStyle
import com.simohypers.limusic.ui.component.CenterLoadingBox
import com.simohypers.limusic.ui.component.EndOfPage
import com.simohypers.limusic.ui.component.HomeItemContentPlaylist
import com.simohypers.limusic.ui.component.RippleIconButton
import com.simohypers.limusic.ui.icon.ArrowBackIosNew
import com.simohypers.limusic.ui.icon.LimusicIcons
import com.simohypers.limusic.ui.navigation.destination.list.AlbumDestination
import com.simohypers.limusic.ui.navigation.destination.list.MoreAlbumsDestination
import com.simohypers.limusic.ui.theme.typo
import com.simohypers.limusic.viewModel.MoreAlbumsUIState
import com.simohypers.limusic.viewModel.MoreAlbumsViewModel
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.blur.hazeBlur
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreAlbumsScreen(
    innerPadding: PaddingValues,
    navController: NavController,
    id: String? = null,
    type: String? = null,
    viewModel: MoreAlbumsViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val hazeState = rememberHazeState()

    LaunchedEffect(id, type) {
        Logger.d("MoreAlbumsScreen", "id: $id, type: $type")
        if (id != null) {
            if (type != null) {
                when (type) {
                    MoreAlbumsDestination.ALBUM_TYPE -> {
                        viewModel.getAlbumMore(id)
                    }

                    MoreAlbumsDestination.SINGLE_TYPE -> {
                        viewModel.getSingleMore(id)
                    }
                }
            }
        }
    }

    Crossfade(targetState = uiState) { state ->
        when (state) {
            is MoreAlbumsUIState.Success -> {
                val data = state.albumItems
                LazyVerticalGrid(
                    // The cards fill their cells and bring their own 10dp inset, so 6dp here puts
                    // the covers 16dp from the edges and 20dp apart, like the pages around it.
                    columns = GridCells.Adaptive(minSize = 150.dp),
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .hazeSource(state = hazeState),
                    contentPadding = PaddingValues(horizontal = 6.dp),
                ) {
                    item(
                        span = { GridItemSpan(maxLineSpan) },
                    ) {
                        Spacer(
                            Modifier
                                .size(
                                    innerPadding.calculateTopPadding() + 64.dp,
                                ),
                        )
                    }
                    items(data) { album ->
                        val data = album
                        HomeItemContentPlaylist(
                            data = data,
                            fillMaxWidth = true,
                            onClick = {
                                navController.navigate(
                                    AlbumDestination(
                                        browseId = album.browseId,
                                    ),
                                )
                            },
                        )
                    }
                    item(
                        span = { GridItemSpan(maxLineSpan) },
                    ) {
                        EndOfPage()
                    }
                }
                TopAppBar(
                    modifier =
                        Modifier
                            .hazeBlur(HazeInput.Sources(hazeState), ultraThinBarStyle()),
                    title = {
                        Text(
                            text = state.title,
                            style = typo().titleMedium,
                            maxLines = 1,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .wrapContentHeight(
                                        align = Alignment.CenterVertically,
                                    ).basicMarquee(
                                        iterations = Int.MAX_VALUE,
                                        animationMode = MarqueeAnimationMode.Immediately,
                                    ).focusable(),
                        )
                    },
                    navigationIcon = {
                        Box(Modifier.padding(horizontal = 5.dp)) {
                            RippleIconButton(
                                LimusicIcons.ArrowBackIosNew,
                                Modifier
                                    .size(32.dp),
                                true,
                                tint = MaterialTheme.colorScheme.onSurface,
                            ) {
                                navController.navigateUp()
                            }
                        }
                    },
                    colors =
                        TopAppBarDefaults.topAppBarColors(
                            Color.Transparent,
                            Color.Unspecified,
                            Color.Unspecified,
                            Color.Unspecified,
                            Color.Unspecified,
                        ),
                )
            }

            is MoreAlbumsUIState.Error -> {
                viewModel.makeToast(state.message)
            }

            MoreAlbumsUIState.Loading -> {
                CenterLoadingBox(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(15.dp),
                )
            }
        }
    }
}