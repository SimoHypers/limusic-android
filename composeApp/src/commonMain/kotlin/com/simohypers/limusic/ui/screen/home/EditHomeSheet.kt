package com.simohypers.limusic.ui.screen.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.simohypers.limusic.ui.component.EndOfModalBottomSheet
import com.simohypers.limusic.ui.component.SurfaceDarkColors
import com.simohypers.limusic.ui.component.rememberSurfaceDarkColors
import com.simohypers.limusic.ui.icon.DragHandle
import com.simohypers.limusic.ui.icon.GridView
import com.simohypers.limusic.ui.icon.LibraryMusic
import com.simohypers.limusic.ui.icon.LimusicIcons
import com.simohypers.limusic.ui.icon.Tune
import com.simohypers.limusic.ui.theme.AppThemeContent
import com.simohypers.limusic.ui.theme.typo
import com.simohypers.limusic.viewModel.HomeLayout
import com.simohypers.limusic.viewModel.SHORTCUTS_SECTION
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import limusic.composeapp.generated.resources.Res
import limusic.composeapp.generated.resources.artwork_backdrop
import limusic.composeapp.generated.resources.artwork_backdrop_desc
import limusic.composeapp.generated.resources.card_large
import limusic.composeapp.generated.resources.card_medium
import limusic.composeapp.generated.resources.card_size
import limusic.composeapp.generated.resources.card_size_desc
import limusic.composeapp.generated.resources.card_small
import limusic.composeapp.generated.resources.done
import limusic.composeapp.generated.resources.edit_home
import limusic.composeapp.generated.resources.edit_home_desc
import limusic.composeapp.generated.resources.home_kind_albums_for_you
import limusic.composeapp.generated.resources.home_kind_community
import limusic.composeapp.generated.resources.home_kind_daily_discover
import limusic.composeapp.generated.resources.home_kind_featured_playlists
import limusic.composeapp.generated.resources.home_kind_forgotten
import limusic.composeapp.generated.resources.home_kind_fresh_finds
import limusic.composeapp.generated.resources.home_kind_from_library
import limusic.composeapp.generated.resources.home_kind_listen_again
import limusic.composeapp.generated.resources.home_kind_mixed_for_you
import limusic.composeapp.generated.resources.home_kind_music_videos
import limusic.composeapp.generated.resources.home_kind_new_releases
import limusic.composeapp.generated.resources.home_kind_quick_picks
import limusic.composeapp.generated.resources.home_kind_trending
import limusic.composeapp.generated.resources.look
import limusic.composeapp.generated.resources.mood_chips
import limusic.composeapp.generated.resources.mood_chips_desc
import limusic.composeapp.generated.resources.reset
import limusic.composeapp.generated.resources.sections
import limusic.composeapp.generated.resources.sections_hint
import limusic.composeapp.generated.resources.sections_shown
import limusic.composeapp.generated.resources.shortcuts
import limusic.composeapp.generated.resources.turn_all_off
import limusic.composeapp.generated.resources.turn_all_on

/**
 * Edit Home, after desktop's panel: every change lands on Home as it is made, so the page above the
 * half-open sheet is the preview and Done only closes. How Home looks first, then its sections,
 * which move by their handle and switch off in place. [sections] lists every section in Home's
 * current order; [onChange] edits the stored [HomeLayout].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditHomeSheet(
    layout: HomeLayout,
    sections: List<String>,
    onChange: ((HomeLayout) -> HomeLayout) -> Unit,
    onDismiss: () -> Unit,
) {
    AppThemeContent {
        val colors = rememberSurfaceDarkColors()
        // Half open first, over the top of Home, which stays readable above it.
        val sheetState = rememberModalBottomSheetState()
        val scope = rememberCoroutineScope()
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            containerColor = colors.container,
            contentColor = colors.content,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            dragHandle = {
                Box(Modifier.padding(vertical = 10.dp).size(60.dp, 4.dp).clip(RoundedCornerShape(50)).background(colors.handle))
            },
            // Lighter than a menu's: what is behind the sheet is the point.
            scrimColor = Color.Black.copy(alpha = .25f),
            contentWindowInsets = { WindowInsets(0) },
        ) {
            // Fully open, the sheet stops under the status bar instead of sliding beneath it; the
            // body scrolls inside what is left. 24dp is the drag handle above this content.
            val statusBar = with(LocalDensity.current) { WindowInsets.statusBars.getTop(this).toDp() }
            BoxWithConstraints {
            Column(Modifier.heightIn(max = maxHeight - statusBar - 24.dp)) {
            val accent = MaterialTheme.colorScheme.primary
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 16.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(accent.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(LimusicIcons.Tune, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(Res.string.edit_home), style = typo().titleMedium, color = colors.content)
                    Text(stringResource(Res.string.edit_home_desc), style = typo().bodySmall, color = colors.subtitle)
                }
                Spacer(Modifier.width(8.dp))
                // Nothing is pending, so there is no Cancel: Done only closes.
                Button(onClick = { scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() } }) {
                    Text(stringResource(Res.string.done))
                }
            }
            SheetBody(layout, sections, onChange, colors, Modifier.weight(1f, fill = false))
            }
            }
        }
    }
}

@Composable
private fun SheetBody(
    layout: HomeLayout,
    sections: List<String>,
    onChange: ((HomeLayout) -> HomeLayout) -> Unit,
    colors: SurfaceDarkColors,
    modifier: Modifier,
) {
    val scroll = rememberScrollState()
    val density = LocalDensity.current
    val rowPx = with(density) { SectionRowHeight.toPx() }
    val edgePx = with(density) { 72.dp.toPx() }

    // The rows a drag moves. They follow [sections] except mid-drag, so a page of the feed landing
    // under the finger can't reshuffle the list; the order is written once, when the drag ends.
    var rows by remember { mutableStateOf(sections) }
    var dragging by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var pointerY by remember { mutableFloatStateOf(0f) }
    var viewTop by remember { mutableFloatStateOf(0f) }
    var viewBottom by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(sections) { if (dragging == null) rows = sections }

    // Swap once the row is past half of its neighbour: on contact, a pixel of jitter would bounce
    // the list between two orders.
    fun dragBy(dy: Float) {
        val section = dragging ?: return
        dragOffset += dy
        while (true) {
            val i = rows.indexOf(section)
            val by =
                when {
                    dragOffset > rowPx / 2 && i < rows.lastIndex -> 1
                    dragOffset < -rowPx / 2 && i > 0 -> -1
                    else -> break
                }
            rows = rows.toMutableList().apply { add(i + by, removeAt(i)) }
            dragOffset -= by * rowPx
        }
    }

    fun endDrag() {
        if (dragging == null) return
        dragging = null
        dragOffset = 0f
        val order = rows
        onChange { it.copy(order = order) }
    }

    // The list is taller than the sheet: holding a row near an edge scrolls, and the row rides along.
    LaunchedEffect(dragging) {
        if (dragging == null) return@LaunchedEffect
        while (true) {
            withFrameNanos { }
            val speed =
                when {
                    pointerY < viewTop + edgePx -> -(viewTop + edgePx - pointerY) / 6
                    pointerY > viewBottom - edgePx -> (pointerY - (viewBottom - edgePx)) / 6
                    else -> 0f
                }
            if (speed != 0f) {
                val used = scroll.scrollBy(speed)
                if (used != 0f) dragBy(used)
            }
        }
    }

    Column(
        modifier
            .onGloballyPositioned {
                val bounds = it.boundsInRoot()
                viewTop = bounds.top
                viewBottom = bounds.bottom
            }.verticalScroll(scroll)
            .padding(horizontal = 20.dp),
    ) {
        GroupLabel(stringResource(Res.string.look), colors)
        Group(colors) {
            Column(Modifier.padding(16.dp)) {
                Text(stringResource(Res.string.card_size), style = typo().labelSmall, color = colors.content)
                Text(stringResource(Res.string.card_size_desc), style = typo().bodySmall, color = colors.subtitle)
                Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HomeLayout.CardSize.entries.forEach { size ->
                        SizeOption(size, selected = layout.cards == size, colors, Modifier.weight(1f)) {
                            onChange { it.copy(cards = size) }
                        }
                    }
                }
            }
            Divider(colors)
            ToggleRow(stringResource(Res.string.mood_chips), stringResource(Res.string.mood_chips_desc), layout.chips, colors) { on ->
                onChange { it.copy(chips = on) }
            }
            Divider(colors)
            ToggleRow(stringResource(Res.string.artwork_backdrop), stringResource(Res.string.artwork_backdrop_desc), layout.backdrop, colors) { on ->
                onChange { it.copy(backdrop = on) }
            }
        }

        val shown = rows.count { it !in layout.hidden }
        Row(Modifier.fillMaxWidth().padding(top = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) { GroupLabel(stringResource(Res.string.sections), colors) }
            Text(stringResource(Res.string.sections_shown, shown, rows.size), style = typo().bodySmall, color = colors.subtitle)
            // Clear the page in one go, then switch on only the few you want.
            TextButton(onClick = {
                val all = rows
                onChange { if (shown == 0) it.copy(hidden = it.hidden - all.toSet()) else it.copy(hidden = it.hidden + all) }
            }) {
                Text(stringResource(if (shown == 0) Res.string.turn_all_on else Res.string.turn_all_off))
            }
        }
        Group(colors) {
            rows.forEachIndexed { index, section ->
                androidx.compose.runtime.key(section) {
                    val isDragged = dragging == section
                    val on = section !in layout.hidden
                    var handle by remember { mutableStateOf<LayoutCoordinates?>(null) }
                    val divider = colors.disabled.copy(alpha = 0.4f)
                    val last = index == rows.lastIndex
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(SectionRowHeight)
                                .zIndex(if (isDragged) 1f else 0f)
                                .graphicsLayer { translationY = if (isDragged) dragOffset else 0f }
                                .background(if (isDragged) colors.container else Color.Transparent)
                                .drawBehind {
                                    if (!last && !isDragged) {
                                        drawLine(divider, Offset(0f, size.height), Offset(size.width, size.height), 1f)
                                    }
                                }.padding(end = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier
                                .size(48.dp)
                                .onGloballyPositioned { handle = it }
                                .pointerInput(section) {
                                    detectDragGestures(
                                        onDragStart = {
                                            dragging = section
                                            dragOffset = 0f
                                        },
                                        onDrag = { change, amount ->
                                            change.consume()
                                            handle?.let { pointerY = it.localToRoot(change.position).y }
                                            dragBy(amount.y)
                                        },
                                        onDragEnd = { endDrag() },
                                        onDragCancel = { endDrag() },
                                    )
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(LimusicIcons.DragHandle, contentDescription = null, tint = colors.subtitle, modifier = Modifier.size(20.dp))
                        }
                        Icon(
                            if (section == SHORTCUTS_SECTION) LimusicIcons.GridView else LimusicIcons.LibraryMusic,
                            contentDescription = null,
                            tint = if (on) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f) else colors.disabled,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            sectionLabel(section),
                            style = typo().labelSmall,
                            color = if (on) colors.content else colors.subtitle,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        Switch(checked = on, onCheckedChange = { show ->
                            onChange { it.copy(hidden = if (show) it.hidden - section else it.hidden + section) }
                        })
                    }
                }
            }
        }
        Text(
            stringResource(Res.string.sections_hint),
            style = typo().bodySmall,
            color = colors.subtitle,
            modifier = Modifier.padding(top = 8.dp, start = 4.dp),
        )
        // Down here, away from Done: it throws the whole arrangement out.
        TextButton(
            onClick = { onChange { HomeLayout() } },
            enabled = layout != HomeLayout(),
            modifier = Modifier.padding(top = 8.dp),
        ) {
            Text(stringResource(Res.string.reset))
        }
        EndOfModalBottomSheet()
    }
}

private val SectionRowHeight = 52.dp

private val SectionLabels: Map<String, StringResource> =
    mapOf(
        SHORTCUTS_SECTION to Res.string.shortcuts,
        "quick_picks" to Res.string.home_kind_quick_picks,
        "listen_again" to Res.string.home_kind_listen_again,
        "mixed_for_you" to Res.string.home_kind_mixed_for_you,
        "forgotten" to Res.string.home_kind_forgotten,
        "from_library" to Res.string.home_kind_from_library,
        "community" to Res.string.home_kind_community,
        "daily_discover" to Res.string.home_kind_daily_discover,
        "fresh_finds" to Res.string.home_kind_fresh_finds,
        "albums_for_you" to Res.string.home_kind_albums_for_you,
        "trending" to Res.string.home_kind_trending,
        "featured_playlists" to Res.string.home_kind_featured_playlists,
        "new_releases" to Res.string.home_kind_new_releases,
        "music_videos" to Res.string.home_kind_music_videos,
    )

/** Our own label for a kind, so the list reads the same before anything has loaded. */
@Composable
private fun sectionLabel(section: String): String = SectionLabels[section]?.let { stringResource(it) } ?: section.removePrefix("title:")

@Composable
private fun GroupLabel(
    text: String,
    colors: SurfaceDarkColors,
) {
    Text(
        text.uppercase(),
        style = typo().bodySmall,
        color = colors.subtitle,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
    )
}

@Composable
private fun Group(
    colors: SurfaceDarkColors,
    content: @Composable () -> Unit,
) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(colors.content.copy(alpha = 0.05f))) {
        content()
    }
}

@Composable
private fun Divider(colors: SurfaceDarkColors) {
    Box(Modifier.fillMaxWidth().height(1.dp).background(colors.disabled.copy(alpha = 0.4f)))
}

@Composable
private fun ToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    colors: SurfaceDarkColors,
    onChange: (Boolean) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = typo().labelSmall, color = colors.content)
            Text(description, style = typo().bodySmall, color = colors.subtitle)
        }
        Spacer(Modifier.width(16.dp))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

/** A card size, pictured as the same row width holding fewer, bigger cards. */
@Composable
private fun SizeOption(
    size: HomeLayout.CardSize,
    selected: Boolean,
    colors: SurfaceDarkColors,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val accent = MaterialTheme.colorScheme.primary
    val (count, side) =
        when (size) {
            HomeLayout.CardSize.SMALL -> 4 to 11.dp
            HomeLayout.CardSize.MEDIUM -> 3 to 15.dp
            HomeLayout.CardSize.LARGE -> 2 to 24.dp
        }
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier =
            modifier
                .clip(shape)
                .border(1.dp, if (selected) accent else colors.disabled.copy(alpha = 0.5f), shape)
                .background(if (selected) accent.copy(alpha = 0.08f) else Color.Transparent)
                .clickable(onClick = onClick)
                .padding(top = 12.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(Modifier.height(24.dp), horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.Bottom) {
            repeat(count) {
                Box(
                    Modifier
                        .size(side)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (selected) accent.copy(alpha = 0.7f) else colors.subtitle.copy(alpha = 0.3f)),
                )
            }
        }
        Text(
            stringResource(
                when (size) {
                    HomeLayout.CardSize.SMALL -> Res.string.card_small
                    HomeLayout.CardSize.MEDIUM -> Res.string.card_medium
                    HomeLayout.CardSize.LARGE -> Res.string.card_large
                },
            ),
            style = typo().bodySmall,
            color = if (selected) colors.content else colors.subtitle,
        )
    }
}
