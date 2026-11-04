package com.simohypers.limusic.ui.component.selection

import com.simohypers.limusic.ui.theme.rounded
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simohypers.limusic.domain.data.model.browse.album.Track
import com.simohypers.limusic.expect.ui.PlatformBackHandler
import com.simohypers.limusic.ui.component.SaveTracksSheet
import com.simohypers.limusic.ui.icon.Close
import com.simohypers.limusic.ui.icon.Delete
import com.simohypers.limusic.ui.icon.Download
import com.simohypers.limusic.ui.icon.FavoriteBorder
import com.simohypers.limusic.ui.icon.PlaylistAdd
import com.simohypers.limusic.ui.icon.QueueMusic
import com.simohypers.limusic.ui.icon.Repeat
import com.simohypers.limusic.ui.icon.LimusicIcons
import com.simohypers.limusic.ui.icon.SkipNext
import com.simohypers.limusic.ui.theme.typo
import com.simohypers.limusic.viewModel.SharedViewModel
import com.simohypers.limusic.viewModel.SongSelectionViewModel
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import limusic.composeapp.generated.resources.Res
import limusic.composeapp.generated.resources.add_to_a_playlist
import limusic.composeapp.generated.resources.add_to_queue
import limusic.composeapp.generated.resources.cancel
import limusic.composeapp.generated.resources.clear
import limusic.composeapp.generated.resources.download
import limusic.composeapp.generated.resources.exit_selection
import limusic.composeapp.generated.resources.like
import limusic.composeapp.generated.resources.loading
import limusic.composeapp.generated.resources.n_selected
import limusic.composeapp.generated.resources.play_next
import limusic.composeapp.generated.resources.play_only_these_on_repeat
import limusic.composeapp.generated.resources.remove
import limusic.composeapp.generated.resources.remove_from_this_playlist
import limusic.composeapp.generated.resources.remove_n_from_playlist
import limusic.composeapp.generated.resources.select_all_count
import limusic.composeapp.generated.resources.selection_hidden
import limusic.composeapp.generated.resources.selection_hint
import limusic.composeapp.generated.resources.selection_pending

// Desktop's track selection (ui/src/lib/selection.ts, TrackSelectionBar.svelte). One list owns it,
// and a pick is a row of that list rather than a song, so the two copies of a song in a playlist
// are picked apart, and a pick outlives a sort or a search that hides it.

/** A row's key: its playlist entry when it has one, else its videoId and which copy of it this is. */
fun rowKey(
    videoId: String,
    setVideoId: String? = null,
    copy: Int = 1,
) = setVideoId ?: "$videoId#$copy"

/** Each row with its [rowKey]. */
fun keyedRows(tracks: List<Track>): List<Pair<String, Track>> {
    val copies = HashMap<String, Int>()
    return tracks.map { track ->
        val copy = (copies[track.videoId] ?: 0) + 1
        copies[track.videoId] = copy
        rowKey(track.videoId, track.setVideoId, copy) to track
    }
}

@Stable
class TrackSelection {
    /** Off until a long press or "Select tracks": rows only pick while it is on. */
    var active by mutableStateOf(false)
        private set

    // The track rides along: a re-sort can take a picked row away until its page comes back.
    private val picked = mutableStateMapOf<String, Track>()

    /** Select all was asked for; it lands once every page is in. */
    var selectingAll by mutableStateOf(false)
        private set

    val count: Int get() = picked.size

    fun has(key: String) = key in picked

    fun enter() {
        active = true
    }

    /** A long press: select mode, with that row picked. */
    fun start(
        key: String,
        track: Track,
    ) {
        active = true
        picked[key] = track
    }

    fun toggle(
        key: String,
        track: Track,
    ) {
        if (picked.remove(key) == null) picked[key] = track
    }

    fun selectAll() {
        selectingAll = true
    }

    fun clear() {
        picked.clear()
        // Clearing is the newer instruction: a Select all still waiting on pages is dropped.
        selectingAll = false
    }

    fun exit() {
        active = false
        clear()
    }

    /** The picks in list order, not tap order: that is the order they queue in. */
    fun tracks(rows: List<Pair<String, Track>>): List<Track> = rows.mapNotNull { (key, track) -> track.takeIf { key in picked } }

    fun missing(rows: List<Pair<String, Track>>): Int {
        val loaded = rows.mapTo(HashSet()) { it.first }
        return picked.keys.count { it !in loaded }
    }

    internal fun pickAll(rows: List<Pair<String, Track>>) {
        rows.forEach { (key, track) -> picked[key] = track }
        selectingAll = false
    }

    /** Only with every page in: until then a missing pick may just be on a page still to come. */
    internal fun retain(rows: List<Pair<String, Track>>) {
        val loaded = rows.mapTo(HashSet()) { it.first }
        picked.keys.retainAll(loaded)
    }
}

@Composable
fun rememberTrackSelection(): TrackSelection = remember { TrackSelection() }

/**
 * Floats above the mini player while [selection] is active, like desktop's bar: what is picked,
 * Select all, Clear and leave on top, the actions under it. Fills the screen it is put on to sit
 * at its bottom, [bottomPadding] up (the tabs and mini player, when the screen is drawn under them).
 *
 * [rows] is the whole list in its order on screen and [shown] what the search leaves of it.
 * [complete] says every page is in; until then [total] is the list's own length and [loadRest]
 * fetches the rest. [from] names the album or playlist for the queue. [onRemove] only where the
 * list is yours to edit.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackSelectionBar(
    selection: TrackSelection,
    rows: List<Pair<String, Track>>,
    bottomPadding: Dp,
    shown: List<Pair<String, Track>> = rows,
    complete: Boolean = true,
    total: Int = rows.size,
    loadRest: () -> Unit = {},
    from: String? = null,
    onRemove: ((tracks: List<Track>, onDone: () -> Unit) -> Unit)? = null,
    // A phone playlist is not offered as a place to add its own songs.
    exceptLocalId: Long? = null,
) {
    val sharedViewModel: SharedViewModel = koinInject()
    val selectionViewModel: SongSelectionViewModel = koinViewModel()
    var saveTracks by remember { mutableStateOf<List<Track>?>(null) }
    var confirmRemove by remember { mutableStateOf(false) }
    var removing by remember { mutableStateOf(false) }

    val missing = selection.missing(rows)
    val pending = if (complete) 0 else missing
    val hidden = selection.count - missing - shown.count { selection.has(it.first) }
    val allSelected = complete && shown.isNotEmpty() && shown.all { selection.has(it.first) }
    val blocked = removing || selection.selectingAll || pending > 0

    PlatformBackHandler(enabled = selection.active) { selection.exit() }
    // One walk for both reasons, so two never page the list at once.
    val wantRest = !complete && (selection.selectingAll || missing > 0)
    LaunchedEffect(wantRest) { if (wantRest) loadRest() }
    LaunchedEffect(selection.selectingAll, complete, shown) {
        if (selection.selectingAll && complete) selection.pickAll(shown)
    }
    LaunchedEffect(complete, rows) { if (complete) selection.retain(rows) }
    // A different set of rows drops a half-made confirmation.
    LaunchedEffect(selection.count) { confirmRemove = false }

    // No pointer input of its own, so touches outside the bar reach the screen under it.
    Box(Modifier.fillMaxSize().padding(bottom = bottomPadding + 8.dp), contentAlignment = Alignment.BottomCenter) {
    AnimatedVisibility(
        visible = selection.active,
        enter = fadeIn(tween(180)) + slideInVertically(spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)) { it / 2 },
        exit = fadeOut(tween(150)) + slideOutVertically(tween(180)) { it / 2 },
    ) {
        val shape = rounded(16.dp)
        val ink = MaterialTheme.colorScheme.onSurface
        val muted = MaterialTheme.colorScheme.onSurfaceVariant
        Column(
            Modifier
                .padding(horizontal = 8.dp)
                .fillMaxWidth()
                .shadow(8.dp, shape)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                // A tap between the buttons would otherwise land on the row under the bar.
                .pointerInput(Unit) { detectTapGestures {} }
                .padding(horizontal = 4.dp),
        ) {
            Row(Modifier.height(44.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = selection::exit, modifier = Modifier.size(36.dp)) {
                    Icon(LimusicIcons.Close, stringResource(Res.string.exit_selection), tint = ink, modifier = Modifier.size(20.dp))
                }
                Row(Modifier.weight(1f).padding(start = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    // The count rolls: up as picks are added, down as they go. Each side keeps its
                    // own number, so the one leaving never reads 0.
                    AnimatedContent(
                        targetState = selection.count,
                        transitionSpec = {
                            val up = targetState > initialState
                            (slideInVertically { if (up) it / 2 else -it / 2 } + fadeIn()) togetherWith
                                (slideOutVertically { if (up) -it / 2 else it / 2 } + fadeOut()) using SizeTransform(clip = false)
                        },
                        label = "selectionCount",
                    ) { count ->
                        if (count > 0) {
                            Text(stringResource(Res.string.n_selected, count), style = typo().titleSmall, color = ink, maxLines = 1)
                        } else {
                            Text(stringResource(Res.string.selection_hint), style = typo().bodySmall, color = muted, maxLines = 1)
                        }
                    }
                    AnimatedContent(targetState = hidden, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "selectionHidden") { n ->
                        if (n > 0) {
                            Text(" · " + stringResource(Res.string.selection_hidden, n), style = typo().bodySmall, color = muted, maxLines = 1)
                        }
                    }
                }
                AnimatedVisibility(selection.count > 0, enter = fadeIn() + expandHorizontally(), exit = fadeOut() + shrinkHorizontally()) {
                    BarButton(stringResource(Res.string.clear), onClick = selection::clear)
                }
                AnimatedVisibility(!allSelected, enter = fadeIn() + expandHorizontally(), exit = fadeOut() + shrinkHorizontally()) {
                    val selectAllCount = if (complete) shown.size else total
                    BarButton(
                        if (selection.selectingAll) {
                            stringResource(Res.string.loading)
                        } else {
                            stringResource(Res.string.select_all_count, selectAllCount)
                        },
                        onClick = selection::selectAll,
                        enabled = selectAllCount > 0 && !selection.selectingAll,
                        container = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    )
                }
                Spacer(Modifier.width(6.dp))
            }
            AnimatedVisibility(
                selection.count > 0,
                enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(),
            ) {
                Column {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                    AnimatedContent(
                        targetState = confirmRemove && onRemove != null,
                        transitionSpec = { fadeIn(tween(200, delayMillis = 60)) togetherWith fadeOut(tween(120)) },
                        label = "selectionActions",
                    ) { confirming ->
                        if (confirming) {
                            // Its own row, not a wider button among the icons: a bulk removal cannot be undone.
                            Row(Modifier.height(48.dp).padding(start = 12.dp, end = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    pluralStringResource(Res.plurals.remove_n_from_playlist, selection.count, selection.count),
                                    style = typo().bodyMedium,
                                    color = ink,
                                    maxLines = 2,
                                    modifier = Modifier.weight(1f),
                                )
                                BarButton(stringResource(Res.string.cancel), onClick = { confirmRemove = false })
                                BarButton(
                                    stringResource(Res.string.remove),
                                    onClick = {
                                        confirmRemove = false
                                        removing = true
                                        onRemove?.invoke(selection.tracks(rows)) {
                                            removing = false
                                            selection.clear()
                                        }
                                    },
                                    enabled = !blocked,
                                    container = MaterialTheme.colorScheme.error,
                                    content = MaterialTheme.colorScheme.onError,
                                )
                            }
                        } else {
                            val tint by animateColorAsState(if (blocked) ink.copy(alpha = 0.38f) else ink, label = "actionTint")
                            Row(
                                Modifier.fillMaxWidth().height(48.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                val action: @Composable (ImageVector, String, (List<Track>) -> Unit) -> Unit = { icon, label, onClick ->
                                    // A small dip while pressed, so a tap on an icon is felt before its toast.
                                    val press = remember { MutableInteractionSource() }
                                    val pressed by press.collectIsPressedAsState()
                                    val scale by animateFloatAsState(if (pressed) 0.82f else 1f, label = "actionPress")
                                    // A hold names the icon over it and runs nothing: the tooltip eats the release.
                                    TooltipBox(
                                        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                                        tooltip = { PlainTooltip { Text(label) } },
                                        state = rememberTooltipState(),
                                    ) {
                                        IconButton(onClick = { onClick(selection.tracks(rows)) }, enabled = !blocked, interactionSource = press) {
                                            Icon(
                                                icon,
                                                label,
                                                tint = tint,
                                                modifier =
                                                    Modifier.size(22.dp).graphicsLayer {
                                                        scaleX = scale
                                                        scaleY = scale
                                                    },
                                            )
                                        }
                                    }
                                }
                                // Done with these rows once one of these runs: the bar closing is what says it
                                // worked. Add to playlist and Remove keep it, as they ask something first.
                                val thenExit: ((List<Track>) -> Unit) -> (List<Track>) -> Unit = { run -> { run(it); selection.exit() } }
                                action(LimusicIcons.SkipNext, stringResource(Res.string.play_next), thenExit { sharedViewModel.playListNext(it, from) })
                                action(LimusicIcons.QueueMusic, stringResource(Res.string.add_to_queue), thenExit { sharedViewModel.addListToQueue(it, from) })
                                action(LimusicIcons.PlaylistAdd, stringResource(Res.string.add_to_a_playlist)) { saveTracks = it }
                                if (onRemove != null) {
                                    action(LimusicIcons.Delete, stringResource(Res.string.remove_from_this_playlist)) { confirmRemove = true }
                                }
                                action(LimusicIcons.Repeat, stringResource(Res.string.play_only_these_on_repeat), thenExit { sharedViewModel.playOnRepeat(it, from) })
                                action(LimusicIcons.Download, stringResource(Res.string.download), thenExit { selectionViewModel.downloadTracks(it) })
                                action(LimusicIcons.FavoriteBorder, stringResource(Res.string.like), thenExit { selectionViewModel.likeTracks(it) })
                            }
                        }
                    }
                }
            }
            AnimatedContent(targetState = pending, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "selectionPending") { n ->
                if (n > 0) {
                    Text(
                        pluralStringResource(Res.plurals.selection_pending, n, n),
                        style = typo().bodySmall,
                        color = muted,
                        modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 10.dp),
                    )
                }
            }
        }
    }
    }
    saveTracks?.let { SaveTracksSheet(it, onDismiss = { saveTracks = null }, exceptLocalId = exceptLocalId) }
}

/** The bar's text buttons: smaller than Material's, in the app's own type, on an optional pill. */
@Composable
private fun BarButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    container: Color = Color.Transparent,
    content: Color = MaterialTheme.colorScheme.primary,
) {
    Box(
        Modifier
            .height(30.dp)
            .clip(CircleShape)
            .background(container)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = typo().titleSmall.copy(fontSize = 12.sp),
            color = if (enabled) content else content.copy(alpha = 0.38f),
            maxLines = 1,
        )
    }
}
