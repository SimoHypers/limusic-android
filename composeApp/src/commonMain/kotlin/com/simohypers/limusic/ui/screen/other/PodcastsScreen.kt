package com.simohypers.limusic.ui.screen.other

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.simohypers.limusic.domain.data.model.browse.album.Track
import com.simohypers.limusic.domain.utils.toSongEntity
import com.simohypers.limusic.domain.utils.toTrack
import com.simohypers.limusic.extension.barBlurStyle
import com.simohypers.limusic.ui.component.CenterLoadingBox
import com.simohypers.limusic.ui.component.CollectionAuthorLine
import com.simohypers.limusic.ui.component.CollectionHero
import com.simohypers.limusic.ui.component.CollectionTitle
import com.simohypers.limusic.ui.component.DescriptionView
import com.simohypers.limusic.ui.component.EndOfPage
import com.simohypers.limusic.ui.component.HeartCheckBox
import com.simohypers.limusic.ui.component.NowPlayingBottomSheet
import com.simohypers.limusic.ui.component.PodcastEpisodeFullWidthItem
import com.simohypers.limusic.ui.component.RippleIconButton
import com.simohypers.limusic.ui.component.pillSurface
import com.simohypers.limusic.ui.icon.ArrowBackIosNew
import com.simohypers.limusic.ui.icon.LimusicIcons
import com.simohypers.limusic.ui.icon.Share
import com.simohypers.limusic.ui.navigation.destination.list.ArtistDestination
import com.simohypers.limusic.ui.theme.rounded
import com.simohypers.limusic.ui.theme.typo
import com.simohypers.limusic.viewModel.PodcastUIEvent
import com.simohypers.limusic.viewModel.PodcastUIState
import com.simohypers.limusic.viewModel.PodcastViewModel
import com.simohypers.limusic.viewModel.SharedViewModel
import com.simohypers.limusic.viewModel.UIEvent
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.blur.hazeBlur
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import limusic.composeapp.generated.resources.Res
import limusic.composeapp.generated.resources.episodes_count
import limusic.composeapp.generated.resources.podcast
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/**
 * A podcast, laid out like an album page: the cover fading into the page with the title block over
 * its foot, one Play (newest episode down), save and share in the pill, then the episodes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PodcastScreen(
    viewModel: PodcastViewModel = koinViewModel(),
    sharedViewModel: SharedViewModel = koinInject(),
    podcastId: String,
    navController: NavController,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isFavorite by viewModel.isFavorite.collectAsStateWithLifecycle()
    val playingVideoId by viewModel.nowPlayingVideoId.collectAsStateWithLifecycle()
    val queueData by sharedViewModel.getQueueDataState().collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current

    val lazyState = rememberLazyListState()
    val pastHero by remember { derivedStateOf { lazyState.firstVisibleItemIndex > 0 } }
    val hazeState = rememberHazeState()
    val background = MaterialTheme.colorScheme.background
    var menuTrack by remember { mutableStateOf<Track?>(null) }

    LaunchedEffect(podcastId) {
        if ((uiState as? PodcastUIState.Success)?.id != podcastId) viewModel.getPodcastBrowse(podcastId)
    }

    menuTrack?.let {
        NowPlayingBottomSheet(onDismiss = { menuTrack = null }, navController = navController, song = it.toSongEntity())
    }

    Crossfade(targetState = uiState) { state ->
        when (state) {
            is PodcastUIState.Success -> {
                val data = state.data
                val id = state.id
                val isThisPlaying = playingVideoId.isNotEmpty() && queueData?.data?.playlistId == id
                LazyColumn(
                    modifier = Modifier.fillMaxSize().background(background).hazeSource(hazeState),
                    state = lazyState,
                ) {
                    item(contentType = "header") {
                        Column {
                            CollectionHero(
                                thumbnail = data.thumbnail.lastOrNull()?.url,
                                onBack = { navController.navigateUp() },
                                actions = {
                                    Row(
                                        modifier = Modifier.height(48.dp).pillSurface(rounded(24.dp)).padding(horizontal = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                                            HeartCheckBox(
                                                size = 28,
                                                checked = isFavorite,
                                                tint = MaterialTheme.colorScheme.onSurface,
                                            ) { viewModel.onUIEvent(PodcastUIEvent.ToggleFavorite(id, !isFavorite)) }
                                        }
                                        IconButton(onClick = { viewModel.onUIEvent(PodcastUIEvent.Share(id)) }, modifier = Modifier.size(40.dp)) {
                                            Icon(LimusicIcons.Share, null, tint = MaterialTheme.colorScheme.onSurface)
                                        }
                                    }
                                },
                            ) {
                                CollectionTitle(
                                    kicker = stringResource(Res.string.podcast),
                                    title = data.title,
                                    playing = isThisPlaying,
                                    onPlay =
                                        if (data.listEpisode.isEmpty()) {
                                            null
                                        } else {
                                            {
                                                if (isThisPlaying) {
                                                    sharedViewModel.onUIEvent(UIEvent.PlayPause)
                                                } else {
                                                    viewModel.onUIEvent(PodcastUIEvent.PlayAll(id))
                                                }
                                            }
                                        },
                                    onShuffle = null,
                                )
                                CollectionAuthorLine(
                                    name = data.author.name,
                                    thumbnail = data.authorThumbnail,
                                    meta = listOf(pluralStringResource(Res.plurals.episodes_count, data.listEpisode.size, data.listEpisode.size)),
                                    onClick = data.author.id?.takeIf { it.isNotEmpty() }?.let { channelId -> { navController.navigate(ArtistDestination(channelId)) } },
                                )
                            }
                            data.description?.takeIf { it.isNotBlank() }?.let { text ->
                                DescriptionView(
                                    text = text,
                                    limitLine = 2,
                                    onTimeClicked = {},
                                    onURLClicked = { url -> uriHandler.openUri(url) },
                                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                    itemsIndexed(data.listEpisode, key = { index, episode -> episode.videoId + "item_$index" }) { _, episode ->
                        PodcastEpisodeFullWidthItem(
                            episode = episode,
                            playing = episode.videoId == playingVideoId,
                            onClick = { viewModel.onUIEvent(PodcastUIEvent.EpisodeClick(episode.videoId, id)) },
                            onMoreClickListener = { menuTrack = episode.toTrack() },
                        )
                    }
                    item { EndOfPage() }
                }
                // The album page's bar: the title takes over once the hero has scrolled away.
                AnimatedVisibility(
                    visible = pastHero,
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut() + slideOutVertically(),
                ) {
                    TopAppBar(
                        title = {
                            Text(
                                text = data.title,
                                style = typo().titleMedium,
                                maxLines = 1,
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .wrapContentHeight(align = Alignment.CenterVertically)
                                        .basicMarquee(iterations = Int.MAX_VALUE, animationMode = MarqueeAnimationMode.Immediately)
                                        .focusable(),
                            )
                        },
                        navigationIcon = {
                            Box(Modifier.padding(horizontal = 5.dp)) {
                                RippleIconButton(LimusicIcons.ArrowBackIosNew, Modifier.size(32.dp), true, MaterialTheme.colorScheme.onSurface) {
                                    navController.navigateUp()
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                        modifier = Modifier.hazeBlur(HazeInput.Sources(hazeState), barBlurStyle(background, 0.55f)),
                    )
                }
            }

            is PodcastUIState.Loading -> {
                CenterLoadingBox(modifier = Modifier.fillMaxSize())
            }

            is PodcastUIState.Error -> {
                LaunchedEffect(state) {
                    viewModel.makeToast("Error: ${state.message}")
                    navController.navigateUp()
                }
            }
        }
    }
}
