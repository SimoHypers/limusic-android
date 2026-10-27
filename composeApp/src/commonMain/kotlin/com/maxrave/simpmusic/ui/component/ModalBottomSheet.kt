package com.maxrave.simpmusic.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.maxrave.domain.data.entities.DownloadState
import com.maxrave.domain.data.entities.LocalPlaylistEntity
import com.maxrave.domain.data.entities.SongEntity
import com.maxrave.domain.data.model.download.DownloadProgress
import com.maxrave.domain.data.model.searchResult.playlists.PlaylistsResult
import com.maxrave.domain.data.model.searchResult.songs.Artist
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.domain.mediaservice.handler.MediaPlayerHandler
import com.maxrave.domain.utils.FilterState
import com.maxrave.domain.utils.connectArtists
import com.maxrave.domain.utils.toListName
import com.maxrave.simpmusic.expect.copyToClipboard
import com.maxrave.simpmusic.expect.shareUrl
import com.maxrave.simpmusic.extension.displayNameRes
import com.maxrave.simpmusic.extension.greyScale
import com.maxrave.simpmusic.ui.icon.AccessAlarm
import com.maxrave.simpmusic.ui.icon.Add
import com.maxrave.simpmusic.ui.icon.AddCircleOutline
import com.maxrave.simpmusic.ui.icon.Album
import com.maxrave.simpmusic.ui.icon.CheckCircle
import com.maxrave.simpmusic.ui.icon.Close
import com.maxrave.simpmusic.ui.icon.ContentCopy
import com.maxrave.simpmusic.ui.icon.Delete
import com.maxrave.simpmusic.ui.icon.Done
import com.maxrave.simpmusic.ui.icon.DownloadForOffline
import com.maxrave.simpmusic.ui.icon.DownloadForOfflineOutlined
import com.maxrave.simpmusic.ui.icon.Downloading
import com.maxrave.simpmusic.ui.icon.Edit
import com.maxrave.simpmusic.ui.icon.FavoriteBorder
import com.maxrave.simpmusic.ui.icon.GridView
import com.maxrave.simpmusic.ui.icon.Info
import com.maxrave.simpmusic.ui.icon.KeyboardArrowDown
import com.maxrave.simpmusic.ui.icon.KeyboardDoubleArrowDown
import com.maxrave.simpmusic.ui.icon.KeyboardDoubleArrowUp
import com.maxrave.simpmusic.ui.icon.PeopleAlt
import com.maxrave.simpmusic.ui.icon.PlayArrow
import com.maxrave.simpmusic.ui.icon.PlayCircle
import com.maxrave.simpmusic.ui.icon.PlaylistAdd
import com.maxrave.simpmusic.ui.icon.QueueMusic
import com.maxrave.simpmusic.ui.icon.Remove
import com.maxrave.simpmusic.ui.icon.Search
import com.maxrave.simpmusic.ui.icon.Sensors
import com.maxrave.simpmusic.ui.icon.Share
import com.maxrave.simpmusic.ui.icon.Shuffle
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.icon.SkipNext
import com.maxrave.simpmusic.ui.icon.Speed
import com.maxrave.simpmusic.ui.icon.Sync
import com.maxrave.simpmusic.ui.icon.Tune
import com.maxrave.simpmusic.ui.navigation.destination.list.AlbumDestination
import com.maxrave.simpmusic.ui.navigation.destination.list.ArtistDestination
import com.maxrave.simpmusic.ui.theme.AppThemeContent
import com.maxrave.simpmusic.ui.theme.seed
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.NowPlayingBottomSheetUIEvent
import com.maxrave.simpmusic.viewModel.NowPlayingBottomSheetViewModel
import com.maxrave.simpmusic.viewModel.SharedViewModel
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import multiplatform.network.cmptoast.ToastGravity
import multiplatform.network.cmptoast.showToast
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.liked_songs_count
import simpmusic.composeapp.generated.resources.add_to_a_playlist
import simpmusic.composeapp.generated.resources.add_to_queue
import simpmusic.composeapp.generated.resources.album
import simpmusic.composeapp.generated.resources.artists
import simpmusic.composeapp.generated.resources.baseline_favorite_24
import simpmusic.composeapp.generated.resources.bitrate
import simpmusic.composeapp.generated.resources.bpm
import simpmusic.composeapp.generated.resources.can_not_be_empty
import simpmusic.composeapp.generated.resources.cancel
import simpmusic.composeapp.generated.resources.codec
import simpmusic.composeapp.generated.resources.copied_to_clipboard
import simpmusic.composeapp.generated.resources.delete
import simpmusic.composeapp.generated.resources.delete_from_queue
import simpmusic.composeapp.generated.resources.delete_playlist
import simpmusic.composeapp.generated.resources.delete_song_from_playlist
import simpmusic.composeapp.generated.resources.description
import simpmusic.composeapp.generated.resources.download
import simpmusic.composeapp.generated.resources.download_speed
import simpmusic.composeapp.generated.resources.download_this_song_video_file_to_your_device
import simpmusic.composeapp.generated.resources.downloaded
import simpmusic.composeapp.generated.resources.downloading
import simpmusic.composeapp.generated.resources.downloading_audio
import simpmusic.composeapp.generated.resources.downloading_video
import simpmusic.composeapp.generated.resources.edit_playlist
import simpmusic.composeapp.generated.resources.error_occurred
import simpmusic.composeapp.generated.resources.extract_source
import simpmusic.composeapp.generated.resources.itag
import simpmusic.composeapp.generated.resources.key
import simpmusic.composeapp.generated.resources.like
import simpmusic.composeapp.generated.resources.liked
import simpmusic.composeapp.generated.resources.list_all_cookies_of_this_page
import simpmusic.composeapp.generated.resources.merging_audio_and_video
import simpmusic.composeapp.generated.resources.mime_type
import simpmusic.composeapp.generated.resources.move_down
import simpmusic.composeapp.generated.resources.move_to_play_next
import simpmusic.composeapp.generated.resources.move_up
import simpmusic.composeapp.generated.resources.no_album
import simpmusic.composeapp.generated.resources.no_description
import simpmusic.composeapp.generated.resources.new_playlist
import simpmusic.composeapp.generated.resources.no_results_found
import simpmusic.composeapp.generated.resources.now_playing_upper
import simpmusic.composeapp.generated.resources.ok
import simpmusic.composeapp.generated.resources.pitch
import simpmusic.composeapp.generated.resources.play
import simpmusic.composeapp.generated.resources.play_next
import simpmusic.composeapp.generated.resources.playback_speed
import simpmusic.composeapp.generated.resources.playback_speed_pitch
import simpmusic.composeapp.generated.resources.playback_speed_pitch_disabled
import simpmusic.composeapp.generated.resources.plays
import simpmusic.composeapp.generated.resources.processing
import simpmusic.composeapp.generated.resources.queue
import simpmusic.composeapp.generated.resources.radio
import simpmusic.composeapp.generated.resources.remove_shortcut
import simpmusic.composeapp.generated.resources.save_to_playlist
import simpmusic.composeapp.generated.resources.scale
import simpmusic.composeapp.generated.resources.search
import simpmusic.composeapp.generated.resources.set
import simpmusic.composeapp.generated.resources.share
import simpmusic.composeapp.generated.resources.shuffle_play
import simpmusic.composeapp.generated.resources.sleep_minutes
import simpmusic.composeapp.generated.resources.sleep_timer
import simpmusic.composeapp.generated.resources.sleep_timer_end_of_song
import simpmusic.composeapp.generated.resources.sleep_timer_off
import simpmusic.composeapp.generated.resources.sleep_timer_set_error
import simpmusic.composeapp.generated.resources.sleep_timer_warning
import simpmusic.composeapp.generated.resources.song_info
import simpmusic.composeapp.generated.resources.sort_by
import simpmusic.composeapp.generated.resources.start_radio
import simpmusic.composeapp.generated.resources.title
import simpmusic.composeapp.generated.resources.to_download_folder
import simpmusic.composeapp.generated.resources.unknown
import simpmusic.composeapp.generated.resources.warning
import simpmusic.composeapp.generated.resources.yes
import simpmusic.composeapp.generated.resources.your_playlists
import simpmusic.composeapp.generated.resources.your_youtube_cookie
import simpmusic.composeapp.generated.resources.your_youtube_playlists
import simpmusic.composeapp.generated.resources.youtube_url
import com.maxrave.domain.utils.withoutYouTubeTwins
import com.maxrave.simpmusic.ui.icon.Cloud
import simpmusic.composeapp.generated.resources.move_to_youtube_music

// ─────────────────────────────────────────────────────────────────────────────
// Sentinel value used by SleepTimerBottomSheet to signal "end of current song"
// Handle this in NowPlayingBottomSheetViewModel / SharedViewModel:
//   if (minutes == END_OF_SONG_SENTINEL) → stop after current track finishes
// ─────────────────────────────────────────────────────────────────────────────
const val END_OF_SONG_SENTINEL = Int.MAX_VALUE

@ExperimentalMaterial3Api
@Composable
fun InfoPlayerBottomSheet(
    onDismiss: () -> Unit,
    sharedViewModel: SharedViewModel = koinInject(),
) {
    AppThemeContent {
        val coroutineScope = rememberCoroutineScope()
        val localDensity = LocalDensity.current
        val windowInsets = WindowInsets.systemBars
        val scrollState = rememberScrollState()
        val sheetState =
            rememberModalBottomSheetState(
                skipPartiallyExpanded = true,
            )

        val screenDataState by sharedViewModel.nowPlayingScreenData.collectAsStateWithLifecycle()
        val songEntity by sharedViewModel.nowPlayingState.map { it?.songEntity }.collectAsState(null)
        val format by sharedViewModel.format.collectAsState(null)
        val extractSource by sharedViewModel.extractSource.collectAsState()
        val downloadProgress by sharedViewModel.downloadFileProgress.collectAsStateWithLifecycle()

        ModalBottomSheet(
            onDismissRequest = {
                onDismiss()
            },
            containerColor = rememberSurfaceDarkColors().container,
            contentColor = Color.Transparent,
            dragHandle = {},
            scrimColor = Color.Black.copy(alpha = .5f),
            sheetState = sheetState,
            modifier = Modifier.fillMaxHeight(),
            contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
            shape = RectangleShape,
        ) {
            // This dialog MUST stay inside the sheet's content lambda. A Dialog is its own window
            // (Android ComponentDialog) / scene layer (skiko), so nothing in the layout tree orders
            // it — the layer attached LAST wins, and DisposableEffects attach in composition order.
            // Written as a sibling BEFORE ModalBottomSheet it lost to the sheet whenever both entered
            // composition in the same pass: reopening the sheet mid-download, or an Android config
            // change (downloadProgress lives in the ViewModel, so it survives this composable leaving).
            // Nested here, the sheet's layer necessarily exists first, so the dialog is always on top.
            if (downloadProgress != DownloadProgress.INIT) {
                BasicAlertDialog(
                    onDismissRequest = { },
                    modifier = Modifier.wrapContentSize(),
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        color = rememberSurfaceDarkColors().container,
                        tonalElevation = AlertDialogDefaults.TonalElevation,
                        shadowElevation = 1.dp,
                    ) {
                        Column(
                            Modifier.padding(
                                horizontal = 20.dp,
                                vertical = 20.dp,
                            ),
                        ) {
                            Text(
                                stringResource(Res.string.downloading),
                                style = typo().headlineMedium,
                            )
                            Row(Modifier.padding(top = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                                if (!downloadProgress.isDone && !downloadProgress.isError) {
                                    CircularProgressIndicator()
                                    Spacer(Modifier.size(15.dp))
                                }
                                Crossfade(downloadProgress) {
                                    if (it.isMerging) {
                                        Text(
                                            text = stringResource(Res.string.merging_audio_and_video),
                                            modifier = Modifier.padding(vertical = 5.dp),
                                            style = typo().bodyMedium,
                                        )
                                    } else if (it.isError) {
                                        Column {
                                            Text(
                                                text = stringResource(Res.string.error_occurred),
                                                modifier = Modifier.padding(vertical = 5.dp),
                                                style = typo().bodyMedium,
                                            )
                                            Text(
                                                text = downloadProgress.errorMessage,
                                                modifier = Modifier.padding(bottom = 5.dp),
                                                maxLines = 2,
                                                style = typo().bodyMedium,
                                            )
                                        }
                                    } else if (it.isDone) {
                                        Text(
                                            text =
                                                stringResource(Res.string.downloaded) +
                                                    stringResource(Res.string.to_download_folder)
                                                        .replace("\"", ""),
                                            modifier = Modifier.padding(vertical = 5.dp),
                                            style = typo().bodyMedium,
                                        )
                                    } else {
                                        Column {
                                            if (it.audioDownloadProgress != 0f) {
                                                Text(
                                                    text =
                                                        stringResource(
                                                            Res.string.downloading_audio,
                                                            (downloadProgress.audioDownloadProgress * 100).toString() + "%",
                                                        ),
                                                    modifier = Modifier.padding(vertical = 5.dp),
                                                    style = typo().bodyMedium,
                                                )
                                            }
                                            if (it.videoDownloadProgress != 0f) {
                                                Text(
                                                    text =
                                                        stringResource(
                                                            Res.string.downloading_video,
                                                            (downloadProgress.videoDownloadProgress * 100).toString() + "%",
                                                        ),
                                                    modifier = Modifier.padding(vertical = 5.dp),
                                                    style = typo().bodyMedium,
                                                )
                                            }
                                            if (downloadProgress.downloadSpeed != 0) {
                                                Text(
                                                    text =
                                                        stringResource(
                                                            Res.string.download_speed,
                                                            downloadProgress.downloadSpeed.toString() + " kb/s",
                                                        ),
                                                    modifier = Modifier.padding(vertical = 5.dp),
                                                    style = typo().bodyMedium,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            Crossfade(downloadProgress) {
                                if (it.isError) {
                                    Column {
                                        Spacer(Modifier.height(10.dp))
                                        OutlinedButton(onClick = {
                                            sharedViewModel.downloadFileDone()
                                        }) { Text(stringResource(Res.string.ok)) }
                                    }
                                }
                                if (it.isDone) {
                                    Column {
                                        Spacer(Modifier.height(10.dp))
                                        OutlinedButton(onClick = {
                                            sharedViewModel.downloadFileDone()
                                        }) { Text(stringResource(Res.string.ok)) }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Card(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(),
                shape = RectangleShape,
                colors = CardDefaults.cardColors().copy(containerColor = rememberSurfaceDarkColors().container),
            ) {
                Column(
                    modifier =
                        Modifier
                            .verticalScroll(scrollState)
                            .padding(
                                top =
                                    with(localDensity) {
                                        windowInsets.getTop(localDensity).toDp()
                                    },
                            ),
                ) {
                    TopAppBar(
                        windowInsets = WindowInsets(0, 0, 0, 0),
                        colors =
                            TopAppBarDefaults.topAppBarColors().copy(
                                containerColor = Color.Transparent,
                            ),
                        title = {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Text(
                                    text = stringResource(Res.string.now_playing_upper),
                                    style = typo().bodyMedium,
                                    color = rememberSurfaceDarkColors().content,
                                )
                                Text(
                                    text = screenDataState.nowPlayingTitle,
                                    style = typo().labelMedium,
                                    color = rememberSurfaceDarkColors().content,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .wrapContentHeight(align = Alignment.CenterVertically)
                                            .basicMarquee(
                                                iterations = Int.MAX_VALUE,
                                                animationMode = MarqueeAnimationMode.Immediately,
                                            ).focusable(),
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = {
                                coroutineScope.launch {
                                    sheetState.hide()
                                    onDismiss()
                                }
                            }) {
                                Icon(
                                    imageVector = SimpIcons.KeyboardArrowDown,
                                    contentDescription = "",
                                    tint = rememberSurfaceDarkColors().content,
                                )
                            }
                        },
                        actions = {
                            Box(
                                modifier = Modifier.size(48.dp),
                            )
                        },
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = stringResource(Res.string.title),
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                        textAlign = TextAlign.Center,
                        style = typo().labelMedium,
                        color = rememberSurfaceDarkColors().content,
                    )
                    Text(
                        text = screenDataState.nowPlayingTitle,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .wrapContentHeight(align = Alignment.CenterVertically)
                                .padding(horizontal = 10.dp)
                                .basicMarquee(
                                    iterations = Int.MAX_VALUE,
                                    animationMode = MarqueeAnimationMode.Immediately,
                                ).focusable()
                                .padding(horizontal = 10.dp),
                        style = typo().bodyMedium,
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = stringResource(Res.string.artists),
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                        textAlign = TextAlign.Center,
                        style = typo().labelMedium,
                        color = rememberSurfaceDarkColors().content,
                    )
                    Text(
                        text = screenDataState.artistName,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .wrapContentHeight(align = Alignment.CenterVertically)
                                .basicMarquee(
                                    iterations = Int.MAX_VALUE,
                                    animationMode = MarqueeAnimationMode.Immediately,
                                ).focusable()
                                .padding(horizontal = 10.dp),
                        style = typo().bodyMedium,
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = stringResource(Res.string.album),
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                        textAlign = TextAlign.Center,
                        style = typo().labelMedium,
                        color = rememberSurfaceDarkColors().content,
                    )
                    Text(
                        text = songEntity?.albumName ?: stringResource(Res.string.unknown),
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .wrapContentHeight(align = Alignment.CenterVertically)
                                .basicMarquee(
                                    iterations = Int.MAX_VALUE,
                                    animationMode = MarqueeAnimationMode.Immediately,
                                ).focusable()
                                .padding(horizontal = 10.dp),
                        style = typo().bodyMedium,
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = stringResource(Res.string.itag),
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                        textAlign = TextAlign.Center,
                        style = typo().labelMedium,
                        color = rememberSurfaceDarkColors().content,
                    )
                    Text(
                        text = format?.itag?.toString() ?: stringResource(Res.string.unknown),
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .wrapContentHeight(align = Alignment.CenterVertically)
                                .basicMarquee(
                                    iterations = Int.MAX_VALUE,
                                    animationMode = MarqueeAnimationMode.Immediately,
                                ).focusable()
                                .padding(horizontal = 10.dp),
                        style = typo().bodyMedium,
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = stringResource(Res.string.mime_type),
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                        textAlign = TextAlign.Center,
                        style = typo().labelMedium,
                        color = rememberSurfaceDarkColors().content,
                    )
                    Text(
                        text = format?.mimeType ?: stringResource(Res.string.unknown),
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .wrapContentHeight(align = Alignment.CenterVertically)
                                .basicMarquee(
                                    iterations = Int.MAX_VALUE,
                                    animationMode = MarqueeAnimationMode.Immediately,
                                ).focusable()
                                .padding(horizontal = 10.dp),
                        style = typo().bodyMedium,
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = stringResource(Res.string.codec),
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                        textAlign = TextAlign.Center,
                        style = typo().labelMedium,
                        color = rememberSurfaceDarkColors().content,
                    )
                    Text(
                        text = format?.codecs ?: stringResource(Res.string.unknown),
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .wrapContentHeight(align = Alignment.CenterVertically)
                                .basicMarquee(
                                    iterations = Int.MAX_VALUE,
                                    animationMode = MarqueeAnimationMode.Immediately,
                                ).focusable()
                                .padding(horizontal = 10.dp),
                        style = typo().bodyMedium,
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = stringResource(Res.string.bitrate),
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                        textAlign = TextAlign.Center,
                        style = typo().labelMedium,
                        color = rememberSurfaceDarkColors().content,
                    )
                    Text(
                        text = format?.bitrate?.toString() ?: stringResource(Res.string.unknown),
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .wrapContentHeight(align = Alignment.CenterVertically)
                                .basicMarquee(
                                    iterations = Int.MAX_VALUE,
                                    animationMode = MarqueeAnimationMode.Immediately,
                                ).focusable()
                                .padding(horizontal = 10.dp),
                        style = typo().bodyMedium,
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = stringResource(Res.string.bpm),
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                        textAlign = TextAlign.Center,
                        style = typo().labelMedium,
                        color = rememberSurfaceDarkColors().content,
                    )
                    Text(
                        text = format?.bpm?.toString() ?: stringResource(Res.string.unknown),
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .wrapContentHeight(align = Alignment.CenterVertically)
                                .basicMarquee(
                                    iterations = Int.MAX_VALUE,
                                    animationMode = MarqueeAnimationMode.Immediately,
                                ).focusable()
                                .padding(horizontal = 10.dp),
                        style = typo().bodyMedium,
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = stringResource(Res.string.key),
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                        textAlign = TextAlign.Center,
                        style = typo().labelMedium,
                        color = rememberSurfaceDarkColors().content,
                    )
                    Text(
                        text = format?.musicKey ?: stringResource(Res.string.unknown),
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .wrapContentHeight(align = Alignment.CenterVertically)
                                .basicMarquee(
                                    iterations = Int.MAX_VALUE,
                                    animationMode = MarqueeAnimationMode.Immediately,
                                ).focusable()
                                .padding(horizontal = 10.dp),
                        style = typo().bodyMedium,
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = stringResource(Res.string.scale),
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                        textAlign = TextAlign.Center,
                        style = typo().labelMedium,
                        color = rememberSurfaceDarkColors().content,
                    )
                    Text(
                        text = format?.keyScale ?: stringResource(Res.string.unknown),
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .wrapContentHeight(align = Alignment.CenterVertically)
                                .basicMarquee(
                                    iterations = Int.MAX_VALUE,
                                    animationMode = MarqueeAnimationMode.Immediately,
                                ).focusable()
                                .padding(horizontal = 10.dp),
                        style = typo().bodyMedium,
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                    )

                    Text(
                        text = stringResource(Res.string.extract_source),
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                        textAlign = TextAlign.Center,
                        style = typo().labelMedium,
                        color = rememberSurfaceDarkColors().content,
                    )
                    Text(
                        text = extractSource ?: stringResource(Res.string.unknown),
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .wrapContentHeight(align = Alignment.CenterVertically)
                                .basicMarquee(
                                    iterations = Int.MAX_VALUE,
                                    animationMode = MarqueeAnimationMode.Immediately,
                                ).focusable()
                                .padding(horizontal = 10.dp),
                        style = typo().bodyMedium,
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                    )

                    Text(
                        text = stringResource(Res.string.plays),
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                        textAlign = TextAlign.Center,
                        style = typo().labelMedium,
                        color = rememberSurfaceDarkColors().content,
                    )
                    Text(
                        text = screenDataState.songInfoData?.viewCount?.toString() ?: stringResource(Res.string.unknown),
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .wrapContentHeight(align = Alignment.CenterVertically)
                                .basicMarquee(
                                    iterations = Int.MAX_VALUE,
                                    animationMode = MarqueeAnimationMode.Immediately,
                                ).focusable()
                                .padding(horizontal = 10.dp),
                        style = typo().bodyMedium,
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = stringResource(Res.string.description),
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                        textAlign = TextAlign.Center,
                        style = typo().labelMedium,
                        color = rememberSurfaceDarkColors().content,
                    )
                    Text(
                        text = screenDataState.songInfoData?.description ?: stringResource(Res.string.no_description),
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .wrapContentHeight(align = Alignment.CenterVertically)
                                .padding(horizontal = 10.dp),
                        style = typo().bodyMedium,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = stringResource(Res.string.youtube_url),
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                        textAlign = TextAlign.Center,
                        style = typo().labelMedium,
                        color = rememberSurfaceDarkColors().content,
                    )
                    Text(
                        text =
                            buildAnnotatedString {
                                withLink(
                                    LinkAnnotation.Url(
                                        "https://music.youtube.com/watch?v=${songEntity?.videoId}",
                                        TextLinkStyles(style = SpanStyle(textDecoration = TextDecoration.Underline)),
                                    ),
                                ) {
                                    append("https://music.youtube.com/watch?v=${songEntity?.videoId}")
                                }
                            },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .wrapContentHeight(align = Alignment.CenterVertically)
                                .basicMarquee(
                                    iterations = Int.MAX_VALUE,
                                    animationMode = MarqueeAnimationMode.Immediately,
                                ).focusable(),
                        style = typo().bodyMedium,
                        textAlign = TextAlign.Center,
                    )
                    OutlinedButton(
                        enabled = screenDataState.bitmap != null,
                        onClick = {
                            sharedViewModel.downloadFile(
                                bitmap = screenDataState.bitmap ?: return@OutlinedButton,
                            )
                        },
                        modifier =
                            Modifier
                                .wrapContentSize()
                                .align(Alignment.CenterHorizontally)
                                .padding(vertical = 10.dp),
                    ) {
                        Text(text = stringResource(Res.string.download_this_song_video_file_to_your_device))
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    EndOfModalBottomSheet()
                }
            }
        }
    }
}

/** A queue row a song sheet was opened from: its position, and the track that sat there then. */
data class QueuePosition(
    val index: Int,
    val videoId: String,
)

/**
 * The rows only a queue track has: move it up, down or to play next, or take it out of the queue.
 *
 * They act by POSITION, and a radio trims its played history off the front while the sheet can be
 * open, which moves every row. If the track is no longer at its position they do nothing rather
 * than move or delete whatever slid into its place.
 */
@Composable
private fun QueueItemActions(
    position: QueuePosition,
    onDone: () -> Unit,
    musicServiceHandler: MediaPlayerHandler = koinInject<MediaPlayerHandler>(),
) {
    val coroutineScope = rememberCoroutineScope()
    val index = position.index
    val queueSize =
        musicServiceHandler.queueData.value
            ?.data
            ?.listTracks
            ?.size ?: 0
    val current = musicServiceHandler.currentOrderIndex()
    val act: (suspend () -> Unit) -> Unit = { action ->
        val stillThere =
            musicServiceHandler.queueData.value
                ?.data
                ?.listTracks
                ?.getOrNull(index)
                ?.videoId == position.videoId
        if (stillThere) coroutineScope.launch { action() }
        onDone()
    }
    if (index > 0) {
        ActionButton(icon = SimpIcons.KeyboardDoubleArrowUp, text = Res.string.move_up) {
            act { musicServiceHandler.moveInQueue(index, index - 1) }
        }
    }
    if (index < queueSize - 1) {
        ActionButton(icon = SimpIcons.KeyboardDoubleArrowDown, text = Res.string.move_down) {
            act { musicServiceHandler.moveInQueue(index, index + 1) }
        }
    }
    // Hidden on the playing track and on the one already next: there is nowhere to move them.
    if (current >= 0 && index != current && index != current + 1) {
        ActionButton(icon = SimpIcons.SkipNext, text = Res.string.move_to_play_next) {
            act { musicServiceHandler.moveItemToPlayNext(index) }
        }
    }
    ActionButton(icon = SimpIcons.Delete, text = Res.string.delete_from_queue) {
        act { musicServiceHandler.removeMediaItem(index) }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NowPlayingBottomSheet(
    onDismiss: () -> Unit,
    navController: NavController,
    song: SongEntity?,
    viewModel: NowPlayingBottomSheetViewModel = koinViewModel(),
    setSleepTimerEnable: Boolean = false,
    onNavigateToOtherScreen: () -> Unit = {},
    onDelete: (() -> Unit)? = null,
    onLibraryDelete: (() -> Unit)? = null,
    /** Set when opened from a queue row: adds that row's move and delete actions at the top. */
    queuePosition: QueuePosition? = null,
    dataStoreManager: DataStoreManager = koinInject<DataStoreManager>(),
    /** Set from the full player: adds a "Song info" row. */
    onShowInfo: (() -> Unit)? = null,
) {
    // The menu follows the app theme even when opened from the player or an album page.
    AppThemeContent {
        NowPlayingBottomSheetContent(
            onDismiss, navController, song, viewModel, setSleepTimerEnable, onNavigateToOtherScreen,
            onDelete, onLibraryDelete, queuePosition, dataStoreManager, onShowInfo,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun NowPlayingBottomSheetContent(
    onDismiss: () -> Unit,
    navController: NavController,
    song: SongEntity?,
    viewModel: NowPlayingBottomSheetViewModel,
    setSleepTimerEnable: Boolean,
    onNavigateToOtherScreen: () -> Unit,
    onDelete: (() -> Unit)?,
    onLibraryDelete: (() -> Unit)?,
    queuePosition: QueuePosition?,
    dataStoreManager: DataStoreManager,
    onShowInfo: (() -> Unit)?,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    val modelBottomSheetState =
        rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
        )
    val hideModalBottomSheet: () -> Unit =
        {
            coroutineScope.launch {
                modelBottomSheetState.hide()
                onDismiss()
            }
        }

    var addToAPlaylist by remember { mutableStateOf(false) }
    var artist by remember { mutableStateOf(false) }
    var sleepTimer by remember { mutableStateOf(false) }
    var sleepTimerWarning by remember { mutableStateOf(false) }
    var isBottomSheetVisible by rememberSaveable { mutableStateOf(false) }
    var changePlaybackSpeedPitch by remember { mutableStateOf(false) }
    val crossfadeEnabled by dataStoreManager.crossfadeEnabled.collectAsState(DataStoreManager.FALSE)

    LaunchedEffect(uiState) {
        if (uiState.songUIState.videoId.isNotEmpty() && !isBottomSheetVisible) {
            isBottomSheetVisible = true
        }
    }

    LaunchedEffect(key1 = song) {
        viewModel.setSongEntity(song)
    }

    if (changePlaybackSpeedPitch) {
        val playbackSpeed by dataStoreManager.playbackSpeed.collectAsState(1f)
        val pitch by dataStoreManager.pitch.collectAsState(0)
        PlaybackSpeedPitchBottomSheet(
            onDismiss = { changePlaybackSpeedPitch = false },
            playbackSpeed = playbackSpeed,
            pitch = pitch,
        ) { speed, p ->
            viewModel.onUIEvent(
                NowPlayingBottomSheetUIEvent.ChangePlaybackSpeedPitch(
                    speed = speed,
                    pitch = p,
                ),
            )
        }
    }

    if (addToAPlaylist) {
        AddToPlaylistModalBottomSheet(
            isBottomSheetVisible = true,
            listLocalPlaylist = uiState.listLocalPlaylist,
            listYouTubePlaylist = uiState.listYouTubePlaylist,
            onDismiss = { addToAPlaylist = false },
            onClick = {
                viewModel.onUIEvent(NowPlayingBottomSheetUIEvent.AddToPlaylist(it.id))
            },
            onYTPlaylistClick = {
                viewModel.onUIEvent(NowPlayingBottomSheetUIEvent.AddToYouTubePlaylist(it.browseId))
            },
            videoId = uiState.songUIState.videoId,
        )
    }
    if (artist) {
        ArtistModalBottomSheet(
            isBottomSheetVisible = true,
            artists = uiState.songUIState.listArtists,
            navController = navController,
            onNavigateToOtherScreen = onNavigateToOtherScreen,
            onDismiss = { artist = false },
        )
    }

    if (sleepTimer) {
        SleepTimerBottomSheet(onDismiss = { sleepTimer = false }) { minutes: Int ->
            if (setSleepTimerEnable) {
                viewModel.onUIEvent(
                    NowPlayingBottomSheetUIEvent.SetSleepTimer(
                        cancel = false,
                        minutes = minutes,
                    ),
                )
            }
        }
    }

    if (sleepTimerWarning) {
        AlertDialog(
            containerColor = rememberSurfaceDarkColors().container,
            onDismissRequest = { sleepTimerWarning = false },
            confirmButton = {
                TextButton(onClick = {
                    sleepTimerWarning = false
                    viewModel.onUIEvent(
                        NowPlayingBottomSheetUIEvent.SetSleepTimer(
                            cancel = true,
                        ),
                    )
                }) {
                    Text(text = stringResource(Res.string.yes), style = typo().labelSmall)
                }
            },
            dismissButton = {
                TextButton(onClick = { sleepTimerWarning = false }) {
                    Text(text = stringResource(Res.string.cancel), style = typo().labelSmall)
                }
            },
            title = {
                Text(text = stringResource(Res.string.warning), style = typo().labelSmall)
            },
            text = {
                Text(text = stringResource(Res.string.sleep_timer_warning), style = typo().bodyMedium)
            },
        )
    }

    if (isBottomSheetVisible) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = modelBottomSheetState,
            containerColor = Color.Transparent,
            contentColor = Color.Transparent,
            dragHandle = null,
            scrimColor = Color.Black.copy(alpha = .5f),
            contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
        ) {
            Card(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                // Limusic's sheet corner, on the card colour its desktop menus use.
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                colors = CardDefaults.cardColors().copy(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                ) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier =
                            Modifier
                                .width(32.dp)
                                .height(4.dp),
                        colors =
                            CardDefaults.cardColors().copy(
                                containerColor = rememberSurfaceDarkColors().handle,
                            ),
                        shape = RoundedCornerShape(50),
                    ) {}
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val thumb = uiState.songUIState.thumbnails
                        AsyncImage(
                            model =
                                ImageRequest
                                    .Builder(LocalPlatformContext.current)
                                    .data(thumb)
                                    .diskCachePolicy(CachePolicy.ENABLED)
                                    .diskCacheKey(thumb)
                                    .crossfade(550)
                                    .build(),
                            placeholder = rememberHolderPainter(),
                            error = rememberHolderPainter(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier =
                                Modifier
                                    .align(Alignment.CenterVertically)
                                    .size(48.dp)
                                    .clip(MaterialTheme.shapes.small),
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            Text(
                                text = uiState.songUIState.title,
                                style = typo().labelMedium,
                                // typo() bakes a colour into the style, computed from the app's own
                                // scheme — on this always-dark sheet that reads as washed out next
                                // to the ActionButton rows below, which take their colour from here.
                                color = rememberSurfaceDarkColors().content,
                                maxLines = 1,
                                modifier =
                                    Modifier
                                        .wrapContentHeight(Alignment.CenterVertically)
                                        .basicMarquee(animationMode = MarqueeAnimationMode.Immediately)
                                        .focusable(),
                            )
                            Text(
                                text =
                                    uiState.songUIState.listArtists
                                        .toListName()
                                        .connectArtists(),
                                style = typo().bodySmall,
                                color = rememberSurfaceDarkColors().subtitle,
                                maxLines = 1,
                                modifier =
                                    Modifier
                                        .wrapContentHeight(Alignment.CenterVertically)
                                        .basicMarquee(animationMode = MarqueeAnimationMode.Immediately)
                                        .focusable(),
                            )
                        }
                    }
                    HorizontalDivider(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    if (queuePosition != null) {
                        QueueItemActions(position = queuePosition, onDone = hideModalBottomSheet)
                    }
                    Crossfade(targetState = onDelete != null) {
                        if (it) {
                            ActionButton(
                                icon = SimpIcons.Delete,
                                text = Res.string.delete_song_from_playlist,
                            ) {
                                hideModalBottomSheet()
                                onDelete?.invoke()
                            }
                        }
                    }
                    Crossfade(targetState = onLibraryDelete != null) {
                        if (it) {
                            ActionButton(
                                icon = SimpIcons.Delete,
                                text = Res.string.delete,
                            ) {
                                hideModalBottomSheet()
                                onLibraryDelete?.invoke()
                            }
                        }
                    }
                    CheckBoxActionButton(
                        defaultChecked = uiState.songUIState.liked,
                        isHeartIcon = true,
                        onChangeListener = {
                            viewModel.onUIEvent(NowPlayingBottomSheetUIEvent.ToggleLike)
                        },
                    )
                    ActionButton(
                        icon =
                            when (uiState.songUIState.downloadState) {
                                DownloadState.STATE_NOT_DOWNLOADED -> SimpIcons.DownloadForOfflineOutlined
                                DownloadState.STATE_DOWNLOADING -> SimpIcons.Downloading
                                DownloadState.STATE_DOWNLOADED -> SimpIcons.DownloadForOffline
                                DownloadState.STATE_PREPARING -> SimpIcons.Downloading
                                else -> SimpIcons.DownloadForOfflineOutlined
                            },
                        // The old baseline_downloaded.xml carried #FF00A0CB baked in; the shared
                        // symbol is neutral, so the "done" state has to say the colour out loud.
                        iconColor =
                            if (uiState.songUIState.downloadState == DownloadState.STATE_DOWNLOADED) {
                                Color(0xFF00A0CB)
                            } else {
                                Color.Unspecified
                            },
                        text =
                            when (uiState.songUIState.downloadState) {
                                DownloadState.STATE_NOT_DOWNLOADED -> Res.string.download
                                DownloadState.STATE_DOWNLOADING -> Res.string.downloading
                                DownloadState.STATE_DOWNLOADED -> Res.string.downloaded
                                DownloadState.STATE_PREPARING -> Res.string.downloading
                                else -> Res.string.download
                            },
                    ) {
                        viewModel.onUIEvent(NowPlayingBottomSheetUIEvent.Download)
                    }
                    ActionButton(
                        icon = SimpIcons.PlaylistAdd,
                        text = Res.string.add_to_a_playlist,
                    ) {
                        viewModel.resetPlaylists()
                        addToAPlaylist = true
                    }
                    // A queue row is already in the queue: its own rows above move it instead.
                    if (queuePosition == null) {
                        ActionButton(
                            icon = SimpIcons.PlayCircle,
                            text = Res.string.play_next,
                        ) {
                            viewModel.onUIEvent(NowPlayingBottomSheetUIEvent.PlayNext)
                        }
                        ActionButton(icon = SimpIcons.QueueMusic, text = Res.string.add_to_queue) {
                            viewModel.onUIEvent(NowPlayingBottomSheetUIEvent.AddToQueue)
                        }
                    }
                    ActionButton(
                        icon = SimpIcons.PeopleAlt,
                        text = Res.string.artists,
                    ) {
                        artist = true
                    }
                    ActionButton(
                        icon = SimpIcons.Album,
                        // Three states, not two. A track can carry an album ID with no title: the
                        // row it was parsed from links an album but never spells its name out.
                        // That case still navigates, so it must not read "No album" — but the name
                        // is genuinely unknown, so fall back to the generic label rather than
                        // showing a blank row. The parser deliberately leaves the name empty
                        // instead of inventing one, because a made-up title would travel out to
                        // MediaSession and into external scrobblers.
                        text =
                            when {
                                uiState.songUIState.album == null -> Res.string.no_album
                                uiState.songUIState.album
                                    ?.name
                                    .isNullOrBlank() -> Res.string.album
                                else -> null
                            },
                        textString =
                            uiState.songUIState.album
                                ?.name
                                ?.takeIf { it.isNotBlank() },
                        enable = uiState.songUIState.album != null,
                    ) {
                        uiState.songUIState.album?.id?.let { id ->
                            onNavigateToOtherScreen()
                            navController.navigate(AlbumDestination(browseId = id))
                        }
                    }
                    ActionButton(
                        icon = SimpIcons.Sensors,
                        text = Res.string.start_radio,
                    ) {
                        viewModel.onUIEvent(
                            NowPlayingBottomSheetUIEvent.StartRadio(
                                videoId = uiState.songUIState.videoId,
                                name = "\"${uiState.songUIState.title}\" ${runBlocking { getString(Res.string.radio) }}",
                            ),
                        )
                        hideModalBottomSheet()
                    }
                    Crossfade(targetState = setSleepTimerEnable) {
                        val sleepTimerState = uiState.sleepTimer
                        if (it) {
                            // timeRemaining > 0 → countdown mode, -1 → end-of-song mode, 0 → off
                            val isEndOfSong = sleepTimerState.timeRemaining == -1
                            val isRunning = sleepTimerState.timeRemaining > 0 || isEndOfSong
                            Crossfade(targetState = isRunning) { running ->
                                if (running) {
                                    ActionButton(
                                        icon = SimpIcons.AccessAlarm,
                                        textString =
                                            if (isEndOfSong) {
                                                stringResource(Res.string.sleep_timer_end_of_song)
                                            } else {
                                                stringResource(Res.string.sleep_timer, sleepTimerState.timeRemaining.toString())
                                            },
                                        text = null,
                                        textColor = seed,
                                        iconColor = seed,
                                    ) {
                                        sleepTimerWarning = true
                                    }
                                } else {
                                    ActionButton(
                                        icon = SimpIcons.AccessAlarm,
                                        text = Res.string.sleep_timer_off,
                                    ) {
                                        sleepTimer = true
                                    }
                                }
                            }
                        }
                    }
                    Crossfade(targetState = setSleepTimerEnable) {
                        if (it) {
                            ActionButton(
                                icon = SimpIcons.Speed,
                                text =
                                    if (crossfadeEnabled != DataStoreManager.TRUE) {
                                        Res.string.playback_speed_pitch
                                    } else {
                                        Res.string.playback_speed_pitch_disabled
                                    },
                                enable = crossfadeEnabled != DataStoreManager.TRUE,
                            ) {
                                changePlaybackSpeedPitch = true
                            }
                        }
                    }
                    ActionButton(
                        icon = SimpIcons.Share,
                        text = Res.string.share,
                    ) {
                        viewModel.onUIEvent(NowPlayingBottomSheetUIEvent.Share)
                    }
                    if (onShowInfo != null) {
                        ActionButton(icon = SimpIcons.Info, text = Res.string.song_info) {
                            hideModalBottomSheet()
                            onShowInfo()
                        }
                    }
                    EndOfModalBottomSheet()
                }
            }
        }
    }
}

/**
 * A collection menu's first row, as on desktop: Play, in the accent, with Shuffle play as a round
 * button at its far end. Laid out like [ActionButton] so the rows below line up with it.
 */
@Composable
fun PlayActionRow(
    onPlay: () -> Unit,
    onShufflePlay: () -> Unit,
) {
    val accent = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onPlay).padding(start = 20.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            imageVector = SimpIcons.PlayArrow,
            contentDescription = null,
            modifier = Modifier.wrapContentSize(Alignment.Center).padding(12.dp),
            colorFilter = ColorFilter.tint(accent),
        )
        Text(
            text = stringResource(Res.string.play),
            style = typo().labelSmall,
            color = accent,
            modifier = Modifier.padding(start = 10.dp).weight(1f),
        )
        Box(
            modifier = Modifier.size(40.dp).clip(CircleShape).background(accent.copy(alpha = 0.14f)).clickable(onClick = onShufflePlay),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                imageVector = SimpIcons.Shuffle,
                contentDescription = stringResource(Res.string.shuffle_play),
                modifier = Modifier.size(20.dp),
                colorFilter = ColorFilter.tint(accent),
            )
        }
    }
}

@Composable
fun ActionButton(
    icon: ImageVector,
    text: StringResource?,
    textString: String? = null,
    textColor: Color? = null,
    iconColor: Color = Color.Unspecified,
    enable: Boolean = true,
    onClick: () -> Unit,
) {
    val c = rememberSurfaceDarkColors()
    val resolvedIconColor = if (iconColor == Color.Unspecified) c.content else iconColor
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .wrapContentHeight(Alignment.CenterVertically)
                .then(
                    if (enable) Modifier.clickable { onClick.invoke() } else Modifier.greyScale(),
                ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 20.dp),
        ) {
            Image(
                imageVector = icon,
                contentDescription = if (text != null) stringResource(text) else textString ?: "",
                modifier =
                    Modifier
                        .wrapContentSize(Alignment.Center)
                        .padding(12.dp),
                colorFilter =
                    if (enable) {
                        ColorFilter.tint(resolvedIconColor)
                    } else {
                        ColorFilter.tint(c.disabled)
                    },
            )
            Text(
                text = if (text != null) stringResource(text) else textString ?: "",
                style = typo().labelSmall,
                color = if (enable) textColor ?: c.content else c.disabled,
                modifier =
                    Modifier
                        .padding(start = 10.dp)
                        .wrapContentHeight(Alignment.CenterVertically),
            )
        }
    }
}

@Composable
fun CheckBoxActionButton(
    defaultChecked: Boolean,
    isHeartIcon: Boolean,
    onChangeListener: (checked: Boolean) -> Unit,
) {
    var stateChecked by remember { mutableStateOf(defaultChecked) }
    Box(
        modifier =
            Modifier
                .wrapContentSize(align = Alignment.Center)
                .clickable {
                    stateChecked = !stateChecked
                    onChangeListener(stateChecked)
                },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                Modifier
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth(),
        ) {
            Box(Modifier.padding(10.dp)) {
                if (isHeartIcon) {
                    HeartCheckBox(checked = stateChecked, size = 30)
                } else {
                    Crossfade(stateChecked) {
                        if (it) {
                            Icon(SimpIcons.CheckCircle, "")
                        } else {
                            Icon(SimpIcons.AddCircleOutline, "")
                        }
                    }
                }
            }
            Text(
                text =
                    if (stateChecked) {
                        stringResource(Res.string.liked)
                    } else {
                        stringResource(Res.string.like)
                    },
                style = typo().labelSmall,
                // Matches [ActionButton], which this sits directly above in every sheet that uses
                // both — without it the label alone falls back to the colour typo() carries.
                color = rememberSurfaceDarkColors().content,
                modifier =
                    Modifier
                        .padding(start = 10.dp)
                        .wrapContentHeight(Alignment.CenterVertically),
            )
        }
    }
}

@Composable
fun HeartCheckBox(
    size: Int = 24,
    checked: Boolean,
    tint: Color = rememberSurfaceDarkColors().content,
    onStateChange: (() -> Unit)? = null,
) {
    val burstState = rememberHeartBurstState()
    Box(
        modifier =
            Modifier
                .size(size.dp)
                // Before .clip: the burst draws outside the button bounds and the circle clip
                // would trim it to the heart's own circle.
                .heartBurst(burstState)
                .clip(CircleShape)
                .clickable {
                    // Judged at TAP time: tapping an unchecked heart is a like. Firing from the
                    // tap — not from watching `checked` — is what keeps a track change onto an
                    // already-liked song from celebrating a like nobody gave.
                    if (!checked) burstState.fire()
                    onStateChange?.invoke()
                },
    ) {
        Crossfade(targetState = checked, modifier = Modifier.fillMaxSize()) {
            if (it) {
                Image(
                    painter = painterResource(Res.drawable.baseline_favorite_24),
                    contentDescription = "Favorite checked",
                    modifier = Modifier.fillMaxSize().padding(4.dp),
                )
            } else {
                Image(
                    imageVector = SimpIcons.FavoriteBorder,
                    contentDescription = "Favorite unchecked",
                    modifier = Modifier.fillMaxSize().padding(4.dp),
                    colorFilter = ColorFilter.tint(tint),
                )
            }
        }
    }
}

@ExperimentalMaterial3Api
@Composable
fun PlaybackSpeedPitchBottomSheet(
    onDismiss: () -> Unit,
    playbackSpeed: Float,
    pitch: Int,
    onSet: (playbackSpeed: Float, pitch: Int) -> Unit,
) {
    AppThemeContent {
        val modelBottomSheetState =
            rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = modelBottomSheetState,
            containerColor = Color.Transparent,
            contentColor = Color.Transparent,
            dragHandle = null,
            scrimColor = Color.Black.copy(alpha = .5f),
            contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
        ) {
            Card(
                modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                colors = CardDefaults.cardColors().copy(containerColor = rememberSurfaceDarkColors().container),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Card(
                        modifier = Modifier.width(40.dp).height(4.dp),
                        colors = CardDefaults.cardColors().copy(containerColor = rememberSurfaceDarkColors().handle),
                        shape = RoundedCornerShape(50),
                    ) {}
                    Spacer(modifier = Modifier.height(16.dp))
                    // Playback Speed row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Image(
                            imageVector = SimpIcons.Speed,
                            contentDescription = stringResource(Res.string.playback_speed),
                            modifier = Modifier.size(24.dp),
                            colorFilter = ColorFilter.tint(rememberSurfaceDarkColors().subtitle),
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(
                            onClick = {
                                val newSpeed = (kotlin.math.floor((playbackSpeed - 0.1f) * 10f) / 10f).coerceIn(0.2f, 2f)
                                onSet(
                                    newSpeed,
                                    pitch,
                                )
                            },
                        ) {
                            Icon(
                                SimpIcons.Remove,
                                contentDescription = "Decrease speed",
                                tint = rememberSurfaceDarkColors().subtitle,
                            )
                        }
                        Text(
                            text = "x${String.format("%.1f", playbackSpeed)}",
                            style = typo().titleMedium,
                            color = rememberSurfaceDarkColors().subtitle,
                            modifier = Modifier.widthIn(min = 60.dp),
                            textAlign = TextAlign.Center,
                        )
                        IconButton(
                            onClick = {
                                val newSpeed = (kotlin.math.floor((playbackSpeed + 0.1f) * 10f) / 10f).coerceIn(0.2f, 2f)
                                onSet(
                                    newSpeed,
                                    pitch,
                                )
                            },
                        ) {
                            Icon(
                                SimpIcons.Add,
                                contentDescription = "Increase speed",
                                tint = rememberSurfaceDarkColors().subtitle,
                            )
                        }
                    }
                    // Shown on every platform. It used to be hidden on Desktop because LibVLC had no
                    // independent pitch control, but that backend is long gone — mpv shifts pitch with
                    // its rubberband filter. The control is still locked out while crossfade is on,
                    // handled by the caller: crossfade owns mpv's filter chain and the two would fight
                    // over it.
                    run {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                SimpIcons.Tune,
                                contentDescription = stringResource(Res.string.pitch),
                                modifier = Modifier.size(24.dp),
                                tint = rememberSurfaceDarkColors().subtitle,
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            IconButton(
                                onClick = {
                                    val newPitch = (pitch - 1).coerceIn(-12, 12)
                                    onSet(playbackSpeed, newPitch)
                                },
                            ) {
                                Icon(
                                    SimpIcons.Remove,
                                    contentDescription = "Decrease pitch",
                                    tint = rememberSurfaceDarkColors().subtitle,
                                )
                            }
                            Text(
                                text = "$pitch",
                                style = typo().titleMedium,
                                color = rememberSurfaceDarkColors().subtitle,
                                modifier = Modifier.widthIn(min = 60.dp),
                                textAlign = TextAlign.Center,
                            )
                            IconButton(
                                onClick = {
                                    val newPitch = (pitch + 1).coerceIn(-12, 12)
                                    onSet(playbackSpeed, newPitch)
                                },
                            ) {
                                Icon(
                                    SimpIcons.Add,
                                    contentDescription = "Increase pitch",
                                    tint = rememberSurfaceDarkColors().subtitle,
                                )
                            }
                        }
                    }
                    EndOfModalBottomSheet()
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// REDESIGNED SleepTimerBottomSheet
// Quick presets (5, 10, 15, 30, 45 min, 1 hour) + End of Song + Custom input
// Passes END_OF_SONG_SENTINEL (Int.MAX_VALUE) for "End of Song" option.
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepTimerBottomSheet(
    onDismiss: () -> Unit,
    /** Highlights and the Set button; the full player passes its cover colour. */
    accent: Color = seed,
    onSetTimer: (minutes: Int) -> Unit,
) {
    AppThemeContent {
        val coroutineScope = rememberCoroutineScope()
        val modelBottomSheetState =
            rememberModalBottomSheetState(skipPartiallyExpanded = true)

        // -1 = nothing selected, Int.MAX_VALUE = End of Song, else = minutes
        var selectedPreset by rememberSaveable { mutableIntStateOf(-1) }
        var showCustomInput by rememberSaveable { mutableStateOf(false) }
        var customMinutes by rememberSaveable { mutableStateOf("") }

        data class Preset(
            val label: String,
            val minutes: Int,
        )

        val presets =
            listOf(
                Preset("5 min", 5),
                Preset("10 min", 10),
                Preset("15 min", 15),
                Preset("30 min", 30),
                Preset("45 min", 45),
                Preset("1 hour", 60),
            )

        // Whether the Set button should be enabled
        val isSetEnabled =
            when {
                selectedPreset == END_OF_SONG_SENTINEL -> true
                selectedPreset > 0 && !showCustomInput -> true
                showCustomInput && (customMinutes.toIntOrNull() ?: 0) > 0 -> true
                else -> false
            }

        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = modelBottomSheetState,
            containerColor = Color.Transparent,
            contentColor = Color.Transparent,
            dragHandle = null,
            scrimColor = Color.Black.copy(alpha = .5f),
            contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
        ) {
            Card(
                modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                colors = CardDefaults.cardColors().copy(containerColor = rememberSurfaceDarkColors().container),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 16.dp),
                ) {
                    Spacer(modifier = Modifier.height(8.dp))

                    // Drag handle
                    Card(
                        modifier = Modifier.width(40.dp).height(4.dp),
                        colors = CardDefaults.cardColors().copy(containerColor = rememberSurfaceDarkColors().disabled),
                        shape = RoundedCornerShape(50),
                    ) {}

                    Spacer(modifier = Modifier.height(16.dp))

                    // Title row with alarm icon
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Image(
                            imageVector = SimpIcons.AccessAlarm,
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(accent),
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(Res.string.sleep_timer_off),
                            style = typo().titleMedium,
                            color = rememberSurfaceDarkColors().content,
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // ── Preset grid: 3 columns × 2 rows ──────────────────────
                    val presetRows = presets.chunked(3)
                    presetRows.forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            row.forEach { preset ->
                                val isSelected = selectedPreset == preset.minutes && !showCustomInput
                                OutlinedButton(
                                    onClick = {
                                        selectedPreset = preset.minutes
                                        showCustomInput = false
                                        customMinutes = ""
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors =
                                        ButtonDefaults.outlinedButtonColors(
                                            containerColor = if (isSelected) accent.copy(alpha = 0.15f) else Color.Transparent,
                                        ),
                                    border =
                                        BorderStroke(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            color = if (isSelected) accent else rememberSurfaceDarkColors().disabled,
                                        ),
                                    contentPadding = PaddingValues(vertical = 10.dp, horizontal = 4.dp),
                                ) {
                                    Text(
                                        text = preset.label,
                                        style = typo().bodySmall,
                                        color = if (isSelected) accent else rememberSurfaceDarkColors().subtitle,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // ── End of Song + Custom row ─────────────────────────────
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        // End of Song
                        val isEndSelected = selectedPreset == END_OF_SONG_SENTINEL && !showCustomInput
                        OutlinedButton(
                            onClick = {
                                selectedPreset = END_OF_SONG_SENTINEL
                                showCustomInput = false
                                customMinutes = ""
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors =
                                ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isEndSelected) accent.copy(alpha = 0.15f) else Color.Transparent,
                                ),
                            border =
                                BorderStroke(
                                    width = if (isEndSelected) 1.5.dp else 1.dp,
                                    color = if (isEndSelected) accent else rememberSurfaceDarkColors().disabled,
                                ),
                            contentPadding = PaddingValues(vertical = 10.dp, horizontal = 4.dp),
                        ) {
                            Text(
                                text = "End of song",
                                style = typo().bodySmall,
                                color = if (isEndSelected) accent else rememberSurfaceDarkColors().subtitle,
                                fontWeight = if (isEndSelected) FontWeight.SemiBold else FontWeight.Normal,
                            )
                        }

                        // Custom
                        val isCustomSelected = showCustomInput
                        OutlinedButton(
                            onClick = {
                                showCustomInput = true
                                selectedPreset = -1
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors =
                                ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isCustomSelected) accent.copy(alpha = 0.15f) else Color.Transparent,
                                ),
                            border =
                                BorderStroke(
                                    width = if (isCustomSelected) 1.5.dp else 1.dp,
                                    color = if (isCustomSelected) accent else rememberSurfaceDarkColors().disabled,
                                ),
                            contentPadding = PaddingValues(vertical = 10.dp, horizontal = 4.dp),
                        ) {
                            Text(
                                text = "Custom",
                                style = typo().bodySmall,
                                color = if (isCustomSelected) accent else rememberSurfaceDarkColors().subtitle,
                                fontWeight = if (isCustomSelected) FontWeight.SemiBold else FontWeight.Normal,
                            )
                        }
                    }

                    // ── Custom input (animated expand) ───────────────────────
                    AnimatedVisibility(visible = showCustomInput) {
                        Column {
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = customMinutes,
                                onValueChange = { value ->
                                    if (value.all { it.isDigit() } && value.length <= 4) {
                                        customMinutes = value
                                    }
                                },
                                label = {
                                    Text(
                                        text = stringResource(Res.string.sleep_minutes),
                                        style = typo().bodySmall,
                                    )
                                },
                                suffix = {
                                    Text(
                                        text = "min",
                                        style = typo().bodySmall,
                                        color = rememberSurfaceDarkColors().disabled,
                                    )
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // ── Set button ───────────────────────────────────────────
                    Button(
                        onClick = {
                            when {
                                selectedPreset == END_OF_SONG_SENTINEL -> {
                                    onSetTimer(END_OF_SONG_SENTINEL)
                                    coroutineScope.launch {
                                        modelBottomSheetState.hide()
                                        onDismiss()
                                    }
                                }
                                showCustomInput -> {
                                    val mins = customMinutes.toIntOrNull() ?: 0
                                    if (mins > 0) {
                                        onSetTimer(mins)
                                        coroutineScope.launch {
                                            modelBottomSheetState.hide()
                                            onDismiss()
                                        }
                                    } else {
                                        showToast(
                                            runBlocking { getString(Res.string.sleep_timer_set_error) },
                                            ToastGravity.Bottom,
                                        )
                                    }
                                }
                                selectedPreset > 0 -> {
                                    onSetTimer(selectedPreset)
                                    coroutineScope.launch {
                                        modelBottomSheetState.hide()
                                        onDismiss()
                                    }
                                }
                                else -> {
                                    showToast(
                                        runBlocking { getString(Res.string.sleep_timer_set_error) },
                                        ToastGravity.Bottom,
                                    )
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = accent,
                                disabledContainerColor = accent.copy(alpha = 0.3f),
                            ),
                        enabled = isSetEnabled,
                    ) {
                        Text(
                            text = stringResource(Res.string.set),
                            style = typo().labelSmall,
                            // Plain black or white, whichever reads on the accent: a pale cover colour
                            // needs black, the app's red and darker covers need white.
                            color = if (accent.luminance() > 0.4f) Color.Black else Color.White,
                            modifier = Modifier.padding(vertical = 4.dp),
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    EndOfModalBottomSheet()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToPlaylistModalBottomSheet(
    isBottomSheetVisible: Boolean,
    listLocalPlaylist: List<LocalPlaylistEntity>,
    listYouTubePlaylist: List<PlaylistsResult>,
    videoId: String? = null,
    onClick: (LocalPlaylistEntity) -> Unit,
    // Null where the songs can only go into phone playlists: no YouTube tab, and New playlist makes one on the phone.
    onYTPlaylistClick: ((PlaylistsResult) -> Unit)? = null,
    onDismiss: () -> Unit,
) {
    AppThemeContent {
        val coroutineScope = rememberCoroutineScope()
        val modelBottomSheetState =
            rememberModalBottomSheetState(skipPartiallyExpanded = false)
        val hideModalBottomSheet: () -> Unit =
            {
                coroutineScope.launch {
                    modelBottomSheetState.hide()
                    onDismiss()
                }
            }
        val sharedViewModel: SharedViewModel = koinInject()
        val signedIn = sharedViewModel.getYouTubeLoggedIn().collectAsStateWithLifecycle(DataStoreManager.FALSE).value == DataStoreManager.TRUE
        val youTube = signedIn && onYTPlaylistClick != null
        val createdPlaylists by sharedViewModel.createdPlaylists.collectAsStateWithLifecycle()
        // One just made on YouTube shows until YouTube's own list catches up, as in Library.
        val youTubePlaylists =
            remember(listYouTubePlaylist, createdPlaylists) {
                val listed = listYouTubePlaylist.mapTo(HashSet()) { it.browseId.removePrefix("VL") }
                createdPlaylists.filter { it.browseId.removePrefix("VL") !in listed } + listYouTubePlaylist
            }
        var newPlaylist by remember { mutableStateOf(false) }
        if (newPlaylist) {
            val lastPickYouTube by sharedViewModel.newPlaylistOnYouTube.collectAsStateWithLifecycle()
            NewPlaylistSheet(
                signedIn = signedIn,
                onYouTube = lastPickYouTube,
                canPickYouTube = youTube,
                onDismiss = { newPlaylist = false },
                onCreate = { title, toYouTube, onResult ->
                    // Like desktop: the songs go straight into the new playlist, then both sheets close.
                    sharedViewModel.createPlaylist(title, toYouTube) { created ->
                        onResult(created != null)
                        when (created) {
                            is PlaylistsResult -> onYTPlaylistClick?.invoke(created)
                            is LocalPlaylistEntity -> onClick(created)
                        }
                        if (created != null) {
                            newPlaylist = false
                            hideModalBottomSheet()
                        }
                    }
                },
            )
        }
        if (isBottomSheetVisible) {
            ModalBottomSheet(
                onDismissRequest = onDismiss,
                sheetState = modelBottomSheetState,
                containerColor = Color.Transparent,
                contentColor = Color.Transparent,
                dragHandle = null,
                scrimColor = Color.Black.copy(alpha = .5f),
                contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
            ) {
                Card(
                    modifier =
                        Modifier
                            .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
                            .fillMaxWidth()
                            .wrapContentHeight(),
                    shape = BottomSheetDefaults.ExpandedShape,
                    colors = CardDefaults.cardColors().copy(containerColor = rememberSurfaceDarkColors().container),
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier =
                            Modifier
                                .padding(horizontal = 10.dp)
                                // The sheet is handed no insets, so nothing else lifts the list above the keyboard.
                                // Navigation bars are left out: EndOfModalBottomSheet already pads for them.
                                .windowInsetsPadding(WindowInsets.ime.exclude(WindowInsets.navigationBars)),
                    ) {
                        Spacer(modifier = Modifier.height(5.dp))
                        Card(
                            modifier = Modifier.width(60.dp).height(4.dp),
                            colors = CardDefaults.cardColors().copy(containerColor = rememberSurfaceDarkColors().handle),
                            shape = RoundedCornerShape(50),
                        ) {}
                        Spacer(modifier = Modifier.height(5.dp))

                        var query by remember { mutableStateOf("") }
                        var isSearchFocused by remember { mutableStateOf(false) }
                        BasicTextField(
                            value = query,
                            onValueChange = { query = it },
                            singleLine = true,
                            textStyle = typo().labelSmall.copy(color = rememberSurfaceDarkColors().content),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(start = 12.dp, top = 8.dp, end = 12.dp, bottom = 4.dp)
                                    .height(40.dp)
                                    .clip(CircleShape)
                                    .background(rememberSurfaceDarkColors().content.copy(alpha = 0.08f))
                                    .onFocusChanged { focusState ->
                                        isSearchFocused = focusState.isFocused
                                        // A half-open sheet would leave the results under the keyboard.
                                        if (focusState.isFocused) coroutineScope.launch { modelBottomSheetState.expand() }
                                    },
                            decorationBox = { innerTextField ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Icon(
                                        imageVector = SimpIcons.Search,
                                        contentDescription = null,
                                        tint = rememberSurfaceDarkColors().subtitle,
                                        modifier = Modifier.size(20.dp),
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(modifier = Modifier.weight(1f)) {
                                        if (query.isEmpty()) {
                                            Text(
                                                text = stringResource(Res.string.search),
                                                style = typo().labelSmall,
                                                color = rememberSurfaceDarkColors().subtitle,
                                            )
                                        }
                                        innerTextField()
                                    }
                                    if (query.isEmpty()) {
                                        Spacer(modifier = Modifier.width(12.dp))
                                    } else {
                                        IconButton(onClick = { query = "" }) {
                                            Icon(
                                                imageVector = SimpIcons.Close,
                                                contentDescription = null,
                                                tint = rememberSurfaceDarkColors().subtitle,
                                                modifier = Modifier.size(20.dp),
                                            )
                                        }
                                    }
                                }
                            },
                        )

                        // The account's playlists come first and open first; the phone's are the second tab.
                        var onYouTubeTab by remember(youTube) { mutableStateOf(youTube) }
                        if (youTube) {
                            Row(
                                modifier =
                                    Modifier
                                        .horizontalScroll(rememberScrollState())
                                        .padding(horizontal = 12.dp)
                                        .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Chip(
                                    isAnimated = false,
                                    isSelected = onYouTubeTab,
                                    text = stringResource(Res.string.your_youtube_playlists),
                                    onClick = { onYouTubeTab = true },
                                )
                                Chip(
                                    isAnimated = false,
                                    isSelected = !onYouTubeTab,
                                    text = stringResource(Res.string.your_playlists),
                                    onClick = { onYouTubeTab = false },
                                )
                            }
                        }

                        PlaylistPickRow(
                            title = stringResource(Res.string.new_playlist),
                            subtitle = null,
                            onClick = { newPlaylist = true },
                        ) {
                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxSize()
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(SimpIcons.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }

                        val keyword = query.trim()
                        // A playlist synced before Sync became a move shows once, as the YouTube one.
                        val shownLocalPlaylist =
                            remember(listLocalPlaylist, youTubePlaylists, keyword) {
                                listLocalPlaylist.withoutYouTubeTwins(youTubePlaylists).filter { it.title.contains(keyword, ignoreCase = true) }
                            }
                        val shownYouTubePlaylist =
                            remember(youTubePlaylists, keyword) { youTubePlaylists.filter { it.title.contains(keyword, ignoreCase = true) } }
                        // While searching, the list takes the rest of the sheet. Otherwise the sheet resizes with every
                        // keystroke and drags the field along with it.
                        val listModifier = if (isSearchFocused || query.isNotEmpty()) Modifier.weight(1f) else Modifier
                        if (if (onYouTubeTab) shownYouTubePlaylist.isEmpty() else shownLocalPlaylist.isEmpty()) {
                            // With no playlists at all there is nothing to say: New playlist above is the way on.
                            Box(listModifier) {
                                if (keyword.isNotEmpty()) {
                                    Text(
                                        text = stringResource(Res.string.no_results_found),
                                        style = typo().labelSmall,
                                        modifier = Modifier.padding(20.dp),
                                        color = rememberSurfaceDarkColors().disabled,
                                    )
                                }
                            }
                        } else {
                            Crossfade(onYouTubeTab, modifier = listModifier) { youTubeTab ->
                                if (youTubeTab) {
                                    LazyColumn {
                                        items(shownYouTubePlaylist) { playlist ->
                                            PlaylistPickRow(
                                                title = playlist.title,
                                                subtitle = listOf(playlist.author, playlist.itemCount).filter { it.isNotBlank() }.joinToString(" • "),
                                                onClick = {
                                                    onYTPlaylistClick?.invoke(playlist)
                                                    hideModalBottomSheet()
                                                },
                                            ) { PlaylistPickCover(playlist.thumbnails.lastOrNull()?.url) }
                                        }
                                    }
                                } else {
                                    LazyColumn {
                                        items(shownLocalPlaylist) { playlist ->
                                            val count = playlist.tracks?.size ?: 0
                                            PlaylistPickRow(
                                                title = playlist.title,
                                                subtitle = pluralStringResource(Res.plurals.liked_songs_count, count, count),
                                                added = playlist.tracks?.contains(videoId) == true,
                                                onClick = {
                                                    onClick(playlist)
                                                    hideModalBottomSheet()
                                                },
                                            ) { PlaylistPickCover(playlist.thumbnail) }
                                        }
                                    }
                                }
                            }
                        }
                        EndOfModalBottomSheet()
                    }
                }
            }
        }
    }
}

/** A row of the Add to playlist picker: a 48dp cover, the title, and a line under it. [added] greys it out with a tick. */
@Composable
private fun PlaylistPickRow(
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    added: Boolean = false,
    cover: @Composable () -> Unit,
) {
    val colors = rememberSurfaceDarkColors()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable(enabled = !added, onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Box(Modifier.size(48.dp).clip(RoundedCornerShape(6.dp))) { cover() }
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = typo().titleSmall,
                color = if (added) colors.disabled else colors.content,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!subtitle.isNullOrEmpty()) {
                Text(
                    text = subtitle,
                    style = typo().bodySmall,
                    color = if (added) colors.disabled else colors.subtitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (added) Icon(SimpIcons.Done, contentDescription = null, tint = colors.subtitle)
    }
}

@Composable
private fun PlaylistPickCover(url: String?) {
    AsyncImage(
        model =
            ImageRequest
                .Builder(LocalPlatformContext.current)
                .data(url)
                .diskCachePolicy(CachePolicy.ENABLED)
                .diskCacheKey(url)
                .crossfade(true)
                .build(),
        placeholder = rememberHolderPainter(),
        error = rememberHolderPainter(),
        fallback = rememberHolderPainter(),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize(),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistModalBottomSheet(
    isBottomSheetVisible: Boolean,
    artists: List<Artist>,
    navController: NavController,
    onNavigateToOtherScreen: () -> Unit,
    onDismiss: () -> Unit,
) {
    AppThemeContent {
        val coroutineScope = rememberCoroutineScope()
        val modelBottomSheetState =
            rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val hideModalBottomSheet: () -> Unit =
            {
                coroutineScope.launch {
                    modelBottomSheetState.hide()
                    onDismiss()
                }
            }
        if (isBottomSheetVisible) {
            ModalBottomSheet(
                onDismissRequest = onDismiss,
                sheetState = modelBottomSheetState,
                containerColor = Color.Transparent,
                contentColor = Color.Transparent,
                dragHandle = null,
                scrimColor = Color.Black.copy(alpha = .5f),
                contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                    colors = CardDefaults.cardColors().copy(containerColor = rememberSurfaceDarkColors().container),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Spacer(modifier = Modifier.height(5.dp))
                        Card(
                            modifier = Modifier.width(60.dp).height(4.dp),
                            colors = CardDefaults.cardColors().copy(containerColor = rememberSurfaceDarkColors().handle),
                            shape = RoundedCornerShape(50),
                        ) {}
                        Spacer(modifier = Modifier.height(5.dp))
                        LazyColumn {
                            items(artists) { artist ->
                                Box(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                val id = artist.id
                                                if (!id.isNullOrBlank()) {
                                                    onNavigateToOtherScreen()
                                                    navController.navigate(ArtistDestination(id))
                                                }
                                            },
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(20.dp).align(Alignment.CenterStart),
                                    ) {
                                        Image(
                                            imageVector = SimpIcons.PeopleAlt,
                                            contentDescription = "",
                                            colorFilter = ColorFilter.tint(rememberSurfaceDarkColors().content),
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(text = artist.name, style = typo().labelSmall)
                                    }
                                }
                            }
                            item { EndOfModalBottomSheet() }
                        }
                    }
                }
            }
        }
    }
}

@Composable
@ExperimentalMaterial3Api
/**
 * The ⋮ menu of a YouTube playlist or an album, in Limusic desktop's order. A null action hides
 * its row: a radio has no radio of its own, only your own playlists can be edited, and only a
 * Home shortcut's menu can take it off Home.
 */
fun PlaylistBottomSheet(
    onDismiss: () -> Unit,
    shareLink: String,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onStartRadio: (() -> Unit)?,
    onSaveToPlaylist: () -> Unit,
    onEditPlaylist: (() -> Unit)? = null,
    onDeletePlaylist: (() -> Unit)? = null,
    onRemoveShortcut: (() -> Unit)? = null,
    onPlay: () -> Unit,
    onShufflePlay: () -> Unit,
) {
    AppThemeContent {
        val coroutineScope = rememberCoroutineScope()
        val modelBottomSheetState =
            rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val hideModalBottomSheet: () -> Unit =
            {
                coroutineScope.launch {
                    modelBottomSheetState.hide()
                    onDismiss()
                }
            }
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = modelBottomSheetState,
            containerColor = Color.Transparent,
            contentColor = Color.Transparent,
            dragHandle = null,
            scrimColor = Color.Black.copy(alpha = .5f),
            contentWindowInsets = { WindowInsets(0) },
        ) {
            Card(
                modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                colors = CardDefaults.cardColors().copy(containerColor = rememberSurfaceDarkColors().container),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(modifier = Modifier.height(5.dp))
                    Card(
                        modifier = Modifier.width(60.dp).height(4.dp),
                        colors = CardDefaults.cardColors().copy(containerColor = rememberSurfaceDarkColors().handle),
                        shape = RoundedCornerShape(50),
                    ) {}
                    Spacer(modifier = Modifier.height(5.dp))
                    val run: (() -> Unit) -> Unit = { action ->
                        action()
                        hideModalBottomSheet()
                    }
                    PlayActionRow(onPlay = { run(onPlay) }, onShufflePlay = { run(onShufflePlay) })
                    ActionButton(icon = SimpIcons.PlayCircle, text = Res.string.play_next) { run(onPlayNext) }
                    ActionButton(icon = SimpIcons.QueueMusic, text = Res.string.add_to_queue) { run(onAddToQueue) }
                    if (onStartRadio != null) {
                        ActionButton(icon = SimpIcons.Sensors, text = Res.string.start_radio) { run(onStartRadio) }
                    }
                    ActionButton(icon = SimpIcons.PlaylistAdd, text = Res.string.save_to_playlist) { run(onSaveToPlaylist) }
                    val shareTitle = stringResource(Res.string.share)
                    ActionButton(icon = SimpIcons.Share, text = Res.string.share) {
                        shareUrl(shareTitle, shareLink)
                    }
                    if (onRemoveShortcut != null) {
                        ActionButton(icon = SimpIcons.GridView, text = Res.string.remove_shortcut) { run(onRemoveShortcut) }
                    }
                    if (onEditPlaylist != null) {
                        ActionButton(icon = SimpIcons.Edit, text = Res.string.edit_playlist) { run(onEditPlaylist) }
                    }
                    if (onDeletePlaylist != null) {
                        val danger = MaterialTheme.colorScheme.error
                        ActionButton(
                            icon = SimpIcons.Delete,
                            text = Res.string.delete_playlist,
                            textColor = danger,
                            iconColor = danger,
                        ) { run(onDeletePlaylist) }
                    }
                    EndOfModalBottomSheet()
                }
            }
        }
    }
}

/**
 * The ⋮ menu of a playlist on this device: [PlaylistBottomSheet]'s rows minus radio and share
 * (there is no YouTube playlist behind it), plus Move to YouTube Music when [onMoveToYouTube] is
 * given (signed in).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalPlaylistBottomSheet(
    onDismiss: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onSaveToPlaylist: () -> Unit,
    onEditPlaylist: () -> Unit,
    onMoveToYouTube: (() -> Unit)?,
    onDelete: () -> Unit,
    onRemoveShortcut: (() -> Unit)? = null,
    onPlay: () -> Unit,
    onShufflePlay: () -> Unit,
) {
    AppThemeContent {
        val coroutineScope = rememberCoroutineScope()
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val run: (() -> Unit) -> Unit = { action ->
            action()
            coroutineScope.launch {
                sheetState.hide()
                onDismiss()
            }
        }
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            containerColor = Color.Transparent,
            contentColor = Color.Transparent,
            dragHandle = null,
            scrimColor = Color.Black.copy(alpha = .5f),
            contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
        ) {
            Card(
                modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                colors = CardDefaults.cardColors().copy(containerColor = rememberSurfaceDarkColors().container),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(modifier = Modifier.height(5.dp))
                    Card(
                        modifier = Modifier.width(60.dp).height(4.dp),
                        colors = CardDefaults.cardColors().copy(containerColor = rememberSurfaceDarkColors().handle),
                        shape = RoundedCornerShape(50),
                    ) {}
                    Spacer(modifier = Modifier.height(5.dp))
                    PlayActionRow(onPlay = { run(onPlay) }, onShufflePlay = { run(onShufflePlay) })
                    ActionButton(icon = SimpIcons.PlayCircle, text = Res.string.play_next) { run(onPlayNext) }
                    ActionButton(icon = SimpIcons.QueueMusic, text = Res.string.add_to_queue) { run(onAddToQueue) }
                    ActionButton(icon = SimpIcons.PlaylistAdd, text = Res.string.save_to_playlist) { run(onSaveToPlaylist) }
                    if (onRemoveShortcut != null) {
                        ActionButton(icon = SimpIcons.GridView, text = Res.string.remove_shortcut) { run(onRemoveShortcut) }
                    }
                    ActionButton(icon = SimpIcons.Edit, text = Res.string.edit_playlist) { run(onEditPlaylist) }
                    if (onMoveToYouTube != null) {
                        ActionButton(icon = SimpIcons.Cloud, text = Res.string.move_to_youtube_music) { run(onMoveToYouTube) }
                    }
                    val danger = MaterialTheme.colorScheme.error
                    ActionButton(
                        icon = SimpIcons.Delete,
                        text = Res.string.delete_playlist,
                        textColor = danger,
                        iconColor = danger,
                    ) { run(onDelete) }
                    EndOfModalBottomSheet()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SortPlaylistBottomSheet(
    selectedState: FilterState,
    onDismiss: () -> Unit,
    onSortChanged: (FilterState) -> Unit,
) {
    AppThemeContent {
        val modelBottomSheetState =
            rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val filterOptions =
            remember {
                listOf(
                    FilterState.CustomOrder,
                    FilterState.NewerFirst,
                    FilterState.OlderFirst,
                    FilterState.Title,
                )
            }

        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = modelBottomSheetState,
            containerColor = Color.Transparent,
            contentColor = Color.Transparent,
            dragHandle = null,
            scrimColor = Color.Black.copy(alpha = .5f),
            contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
        ) {
            Card(
                modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                colors = CardDefaults.cardColors().copy(containerColor = rememberSurfaceDarkColors().container),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(modifier = Modifier.height(5.dp))
                    Card(
                        modifier = Modifier.width(60.dp).height(4.dp),
                        colors = CardDefaults.cardColors().copy(containerColor = rememberSurfaceDarkColors().handle),
                        shape = RoundedCornerShape(50),
                    ) {}
                    Text(
                        stringResource(Res.string.sort_by),
                        style = typo().labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier =
                            Modifier
                                .padding(start = 16.dp, top = 16.dp, bottom = 24.dp)
                                .align(Alignment.Start),
                    )
                    LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp)) {
                        items(filterOptions, key = { it.hashCode() }) { filterOption ->
                            val isSelected = filterOption == selectedState
                            Row(
                                Modifier
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        onSortChanged(filterOption)
                                        onDismiss()
                                    }.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = stringResource(filterOption.displayNameRes()),
                                    style = typo().labelMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isSelected) seed else rememberSurfaceDarkColors().content,
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                if (isSelected) {
                                    Image(
                                        imageVector = SimpIcons.Done,
                                        contentDescription = "Selected",
                                        colorFilter = ColorFilter.tint(seed),
                                        modifier = Modifier.size(32.dp),
                                    )
                                } else {
                                    Spacer(modifier = Modifier.size(32.dp))
                                }
                            }
                        }
                    }
                    EndOfModalBottomSheet()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevLogInBottomSheet(
    onDismiss: () -> Unit,
    type: DevLogInType,
    onDone: (String) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val modelBottomSheetState =
        rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var value by rememberSaveable { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = modelBottomSheetState,
        containerColor = Color.Transparent,
        contentColor = Color.Transparent,
        dragHandle = null,
        scrimColor = Color.Black.copy(alpha = .5f),
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().wrapContentHeight(),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            colors = CardDefaults.cardColors().copy(containerColor = rememberSurfaceDarkColors().container),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.height(5.dp))
                Card(
                    modifier = Modifier.width(60.dp).height(4.dp),
                    colors = CardDefaults.cardColors().copy(containerColor = rememberSurfaceDarkColors().handle),
                    shape = RoundedCornerShape(50),
                ) {}
                Spacer(modifier = Modifier.height(10.dp))
                Text(text = runBlocking { type.getTitle() }, style = typo().labelSmall)
                Spacer(modifier = Modifier.height(5.dp))
                OutlinedTextField(
                    value = value,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    onValueChange = { value = it },
                    maxLines = 1,
                )
                Spacer(modifier = Modifier.height(5.dp))
                TextButton(
                    onClick = {
                        if (value.isNotEmpty() && value.isNotBlank()) {
                            showToast(runBlocking { getString(Res.string.processing) }, ToastGravity.Bottom)
                            onDismiss()
                            onDone(value)
                        } else {
                            showToast(runBlocking { getString(Res.string.can_not_be_empty) }, ToastGravity.Bottom)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                ) {
                    Text(text = stringResource(Res.string.set), style = typo().labelSmall)
                }
                Spacer(modifier = Modifier.height(5.dp))
                EndOfModalBottomSheet()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevCookieLogInBottomSheet(
    onDismiss: () -> Unit,
    type: DevLogInType,
    cookies: List<Pair<String, String?>>,
) {
    val clipboardManager = LocalClipboard.current
    val coroutineScope = rememberCoroutineScope()
    val modelBottomSheetState =
        rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = modelBottomSheetState,
        containerColor = Color.Transparent,
        contentColor = Color.Transparent,
        dragHandle = null,
        scrimColor = Color.Black.copy(alpha = .5f),
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().wrapContentHeight(),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            colors = CardDefaults.cardColors().copy(containerColor = rememberSurfaceDarkColors().container),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.height(5.dp))
                Card(
                    modifier = Modifier.width(60.dp).height(4.dp),
                    colors = CardDefaults.cardColors().copy(containerColor = rememberSurfaceDarkColors().handle),
                    shape = RoundedCornerShape(50),
                ) {}
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = stringResource(Res.string.list_all_cookies_of_this_page),
                    style = typo().labelSmall,
                )
                cookies.forEach { cookie ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(12.dp),
                    ) {
                        Text(text = cookie.first, style = typo().bodyMedium, modifier = Modifier.weight(1f))
                        SelectionContainer(modifier = Modifier.weight(2f)) {
                            Text(text = cookie.second ?: "", style = typo().bodyMedium)
                        }
                        val copied = stringResource(Res.string.copied_to_clipboard)
                        IconButton(onClick = {
                            copyToClipboard(cookie.first, cookie.second ?: "")
                            showToast(copied, ToastGravity.Bottom)
                        }) {
                            Icon(imageVector = SimpIcons.ContentCopy, contentDescription = "Copy")
                        }
                    }
                }
                EndOfModalBottomSheet()
            }
        }
    }
}

@Composable
fun EndOfModalBottomSheet() {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(
                    WindowInsets.navigationBars
                        .asPaddingValues()
                        .calculateBottomPadding()
                        .value
                        .toInt()
                        .dp + 8.dp,
                ),
    ) {}
}

sealed class DevLogInType {

    data object YouTube : DevLogInType()

    suspend fun getTitle(): String =
        when (this) {
            is YouTube -> getString(Res.string.your_youtube_cookie)
        }
}