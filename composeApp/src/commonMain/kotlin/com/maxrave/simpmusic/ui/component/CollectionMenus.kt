package com.maxrave.simpmusic.ui.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maxrave.domain.data.model.browse.album.Track
import com.maxrave.domain.data.model.browse.playlist.PlaylistState
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.simpmusic.expect.ui.persistPickedImage
import com.maxrave.simpmusic.viewModel.AlbumUIState
import com.maxrave.simpmusic.viewModel.AlbumViewModel
import com.maxrave.simpmusic.viewModel.LocalPlaylistState
import com.maxrave.simpmusic.viewModel.LocalPlaylistUIEvent
import com.maxrave.simpmusic.viewModel.LocalPlaylistViewModel
import com.maxrave.simpmusic.viewModel.PlaylistUIEvent
import com.maxrave.simpmusic.viewModel.PlaylistViewModel
import com.maxrave.simpmusic.viewModel.SharedViewModel
import com.maxrave.simpmusic.viewModel.SongSelectionViewModel
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.cancel
import simpmusic.composeapp.generated.resources.delete_playlist_desc
import simpmusic.composeapp.generated.resources.delete_playlist_desc_local
import simpmusic.composeapp.generated.resources.move
import simpmusic.composeapp.generated.resources.move_playlist_desc
import simpmusic.composeapp.generated.resources.move_to_youtube_music

// The ⋮ menus of a playlist, an album and a local playlist, with everything their rows open, so a
// page and a Home shortcut show the same menu. Each one calls onClose once its sheet and whatever
// the sheet opened are all gone. Callers wrap them in key(request) so a new ⋮ tap always gets a
// fresh menu, even while an earlier one still waits on a fetch.

/** The Save to playlist picker: a local playlist (never [exceptLocalId]) or one of the account's. */
@Composable
fun SaveTracksSheet(
    tracks: List<Track>,
    onDismiss: () -> Unit,
    exceptLocalId: Long? = null,
) {
    val selectionViewModel: SongSelectionViewModel = koinViewModel()
    val localPlaylists by selectionViewModel.listLocalPlaylist.collectAsStateWithLifecycle()
    val youTubePlaylists by selectionViewModel.youTubePlaylists.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { selectionViewModel.loadYouTubePlaylists() }
    AddToPlaylistModalBottomSheet(
        isBottomSheetVisible = true,
        listLocalPlaylist = localPlaylists.filter { it.id != exceptLocalId },
        listYouTubePlaylist = youTubePlaylists,
        onDismiss = onDismiss,
        onClick = { playlist -> selectionViewModel.addTracksToPlaylist(playlist.id, tracks) },
        onYTPlaylistClick = { playlist -> selectionViewModel.addTracksToYouTubePlaylist(playlist.browseId, tracks) },
    )
}

/**
 * [placeholder] stands in until [viewModel] has the playlist, so the menu opens at once; the rows
 * that need the playlist wait for it, as desktop's card menu does.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistMenu(
    viewModel: PlaylistViewModel,
    placeholder: PlaylistState,
    isYourYouTubePlaylist: Boolean,
    onClose: () -> Unit,
    onDeleted: () -> Unit,
    onRemoveShortcut: (() -> Unit)? = null,
    onSelectTracks: (() -> Unit)? = null,
) {
    val sharedViewModel: SharedViewModel = koinInject()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val loaded = uiState.data
    val data = loaded ?: placeholder
    var menu by remember { mutableStateOf(true) }
    // Save to playlist waits for every page; the picker opens once they are in.
    var fetching by remember { mutableStateOf(false) }
    var saveTracks by remember { mutableStateOf<List<Track>?>(null) }
    var editing by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    if (!menu && !fetching && saveTracks == null && !editing && !deleting) LaunchedEffect(Unit) { onClose() }

    // Liked Music (LM) is a playlist YouTube fills from ratings: no edit, no delete.
    val own = (data.owned || isYourYouTubePlaylist) && !data.isRadio && data.id.removePrefix("VL") != "LM"
    if (menu) {
        PlaylistBottomSheet(
            onDismiss = { menu = false },
            shareLink = "https://music.youtube.com/playlist?list=${data.id.removePrefix("VL")}",
            onPlayNext = { viewModel.getFullTracks { sharedViewModel.playListNext(it, data.title) } },
            onAddToQueue = { viewModel.getFullTracks { sharedViewModel.addListToQueue(it, data.title) } },
            onStartRadio = if (data.isRadio) null else ({ viewModel.afterLoad { viewModel.onUIEvent(PlaylistUIEvent.StartRadio) } }),
            onSaveToPlaylist = {
                fetching = true
                viewModel.getFullTracks {
                    saveTracks = it
                    fetching = false
                }
            },
            onEditPlaylist = if (own) ({ editing = true }) else null,
            onDeletePlaylist = if (own) ({ deleting = true }) else null,
            onRemoveShortcut = onRemoveShortcut,
            onPlay = { viewModel.afterLoad { viewModel.onUIEvent(PlaylistUIEvent.PlayAll) } },
            onShufflePlay = { viewModel.afterLoad { viewModel.onUIEvent(PlaylistUIEvent.Shuffle) } },
            onSelectTracks = onSelectTracks,
        )
    }
    // Opens once the playlist is in: the sheet starts from its real description and privacy.
    if (editing && loaded != null) {
        EditPlaylistBottomSheet(
            title = loaded.title,
            description = loaded.description.orEmpty(),
            privacy = loaded.privacy ?: "PRIVATE",
            thumbnail = loaded.thumbnail,
            onDismiss = { editing = false },
            onSave = { title, description, privacy, cover, onResult ->
                viewModel.editPlaylist(title, description, privacy, cover) { ok ->
                    onResult(ok)
                    if (ok) {
                        editing = false
                        sharedViewModel.youTubePlaylistsChanged()
                    }
                }
            },
        )
    }
    if (deleting) {
        DeletePlaylistDialog(
            message = stringResource(Res.string.delete_playlist_desc, data.title),
            onDismiss = { deleting = false },
            onConfirm = {
                deleting = false
                viewModel.afterLoad {
                    viewModel.deletePlaylist {
                        sharedViewModel.youTubePlaylistsChanged()
                        onDeleted()
                    }
                }
            },
        )
    }
    saveTracks?.let { SaveTracksSheet(it, onDismiss = { saveTracks = null }) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumMenu(
    viewModel: AlbumViewModel,
    uiState: AlbumUIState,
    browseId: String,
    onClose: () -> Unit,
    onRemoveShortcut: (() -> Unit)? = null,
    onSelectTracks: (() -> Unit)? = null,
) {
    val sharedViewModel: SharedViewModel = koinInject()
    var menu by remember { mutableStateOf(true) }
    var fetching by remember { mutableStateOf(false) }
    var saveTracks by remember { mutableStateOf<List<Track>?>(null) }
    if (!menu && !fetching && saveTracks == null) LaunchedEffect(Unit) { onClose() }

    if (menu) {
        PlaylistBottomSheet(
            onDismiss = { menu = false },
            shareLink =
                if (uiState.audioPlaylistId.isNotEmpty()) {
                    "https://music.youtube.com/playlist?list=${uiState.audioPlaylistId}"
                } else {
                    "https://music.youtube.com/browse/$browseId"
                },
            onPlayNext = { viewModel.afterLoad { sharedViewModel.playListNext(it.listTrack, it.title) } },
            onAddToQueue = { viewModel.afterLoad { sharedViewModel.addListToQueue(it.listTrack, it.title) } },
            onStartRadio = { viewModel.afterLoad { viewModel.startRadio() } },
            onSaveToPlaylist = {
                fetching = true
                viewModel.afterLoad(onDone = { fetching = false }) { saveTracks = it.listTrack }
            },
            onRemoveShortcut = onRemoveShortcut,
            onPlay = { viewModel.afterLoad { album -> album.listTrack.firstOrNull()?.let(viewModel::playTrack) } },
            onShufflePlay = { viewModel.afterLoad { viewModel.shuffle() } },
            onSelectTracks = onSelectTracks,
        )
    }
    saveTracks?.let { SaveTracksSheet(it, onDismiss = { saveTracks = null }) }
}

@Composable
fun LocalPlaylistMenu(
    viewModel: LocalPlaylistViewModel,
    uiState: LocalPlaylistState,
    onClose: () -> Unit,
    onDeleted: () -> Unit,
    onRemoveShortcut: (() -> Unit)? = null,
    // The playlist is on YouTube now and gone from here; by default that is a delete.
    onMoved: (youTubeId: String) -> Unit = { onDeleted() },
) {
    val sharedViewModel: SharedViewModel = koinInject()
    val coroutineScope = rememberCoroutineScope()
    val signedIn by sharedViewModel.getYouTubeLoggedIn().collectAsStateWithLifecycle(initialValue = null)
    var menu by remember { mutableStateOf(true) }
    var fetching by remember { mutableStateOf(false) }
    var saveTracks by remember { mutableStateOf<List<Track>?>(null) }
    var editing by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    var moving by remember { mutableStateOf(false) }
    if (!menu && !fetching && saveTracks == null && !editing && !deleting && !moving) {
        LaunchedEffect(Unit) { onClose() }
    }

    if (menu) {
        LocalPlaylistBottomSheet(
            onDismiss = { menu = false },
            onPlayNext = { viewModel.withFullTracks { sharedViewModel.playListNext(it, uiState.title) } },
            onAddToQueue = { viewModel.addAllToQueue() },
            onSaveToPlaylist = {
                fetching = true
                viewModel.withFullTracks(onDone = { fetching = false }) { saveTracks = it }
            },
            onEditPlaylist = { editing = true },
            // Signed in only, and not for a playlist synced back when Sync kept both copies.
            onMoveToYouTube = { moving = true }.takeIf { signedIn == DataStoreManager.TRUE && uiState.ytPlaylistId == null },
            onDelete = { deleting = true },
            onRemoveShortcut = onRemoveShortcut,
            onPlay = { viewModel.onUIEvent(LocalPlaylistUIEvent.PlayClick) },
            onShufflePlay = { viewModel.onUIEvent(LocalPlaylistUIEvent.ShuffleClick) },
        )
    }
    if (editing) {
        EditPlaylistBottomSheet(
            title = uiState.title,
            description = null,
            privacy = null,
            thumbnail = uiState.thumbnail,
            onDismiss = { editing = false },
            onSave = { title, _, _, cover, onResult ->
                title?.let { viewModel.updatePlaylistTitle(it, uiState.id) }
                coroutineScope.launch {
                    // Named after the content: Coil caches by url, so a fixed name would keep
                    // showing the old cover, and the same picture re-picked reuses its file.
                    cover
                        ?.let { persistPickedImage(it, "cover_${it.contentHashCode().toUInt()}.jpg") }
                        ?.let { viewModel.updatePlaylistThumbnail(it, uiState.id) }
                    onResult(true)
                    editing = false
                }
            },
        )
    }
    if (deleting) {
        DeletePlaylistDialog(
            message = stringResource(Res.string.delete_playlist_desc_local, uiState.title),
            onDismiss = { deleting = false },
            onConfirm = {
                deleting = false
                viewModel.deletePlaylist(uiState.id)
                onDeleted()
            },
        )
    }
    saveTracks?.let { SaveTracksSheet(it, onDismiss = { saveTracks = null }, exceptLocalId = uiState.id) }
    if (moving) {
        MoveDialog(stringResource(Res.string.move_playlist_desc, uiState.title), onDismiss = { moving = false }) {
            viewModel.moveToYouTube(uiState.id) { youTubeId ->
                sharedViewModel.playlistCreatedOnYouTube(uiState.title, youTubeId)
                onMoved(youTubeId)
            }
        }
    }
}

@Composable
private fun MoveDialog(
    text: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val colors = rememberSurfaceDarkColors()
    AlertDialog(
        containerColor = colors.container,
        titleContentColor = colors.content,
        textContentColor = colors.content,
        title = { Text(text = stringResource(Res.string.move_to_youtube_music)) },
        text = { Text(text = text) },
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onConfirm()
                onDismiss()
            }) {
                Text(text = stringResource(Res.string.move))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(Res.string.cancel))
            }
        },
    )
}
