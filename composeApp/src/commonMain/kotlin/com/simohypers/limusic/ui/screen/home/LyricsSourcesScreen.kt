package com.simohypers.limusic.ui.screen.home

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.simohypers.limusic.domain.data.model.lyrics.LyricsSource
import com.simohypers.limusic.ui.component.SettingGroup
import com.simohypers.limusic.ui.icon.DragHandle
import com.simohypers.limusic.ui.icon.RestartAlt
import com.simohypers.limusic.ui.icon.LimusicIcons
import com.simohypers.limusic.ui.theme.typo
import com.simohypers.limusic.viewModel.SettingsViewModel
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import limusic.composeapp.generated.resources.Res
import limusic.composeapp.generated.resources.lyrics_about_boidu
import limusic.composeapp.generated.resources.lyrics_about_kugou
import limusic.composeapp.generated.resources.lyrics_about_lrclib
import limusic.composeapp.generated.resources.lyrics_about_lyricsplus
import limusic.composeapp.generated.resources.lyrics_about_netease
import limusic.composeapp.generated.resources.lyrics_about_qq
import limusic.composeapp.generated.resources.lyrics_about_simpmusic
import limusic.composeapp.generated.resources.lyrics_about_youtube
import limusic.composeapp.generated.resources.lyrics_kind_plain
import limusic.composeapp.generated.resources.lyrics_kind_synced
import limusic.composeapp.generated.resources.lyrics_kind_words
import limusic.composeapp.generated.resources.lyrics_sources
import limusic.composeapp.generated.resources.lyrics_sources_hint
import limusic.composeapp.generated.resources.lyrics_sources_none
import limusic.composeapp.generated.resources.lyrics_sources_reset

/** What a set of lyrics is, or the best a source can give: the badge Settings and the picker show. */
enum class LyricsKind(
    val label: StringResource,
) {
    WORDS(Res.string.lyrics_kind_words),
    SYNCED(Res.string.lyrics_kind_synced),
    PLAIN(Res.string.lyrics_kind_plain),
    ;

    companion object {
        fun of(syncType: String?) =
            when (syncType) {
                "RICH_SYNCED" -> WORDS
                "LINE_SYNCED" -> SYNCED
                else -> PLAIN
            }
    }
}

/** The best a source can give, as desktop's Settings badges it. */
private val LyricsSource.bestKind
    get() = if (this == LyricsSource.BOIDU || this == LyricsSource.LYRICSPLUS) LyricsKind.WORDS else LyricsKind.SYNCED

private val LyricsSource.about
    get() =
        when (this) {
            LyricsSource.BOIDU -> Res.string.lyrics_about_boidu
            LyricsSource.LYRICSPLUS -> Res.string.lyrics_about_lyricsplus
            LyricsSource.LRCLIB -> Res.string.lyrics_about_lrclib
            LyricsSource.YOUTUBE -> Res.string.lyrics_about_youtube
            LyricsSource.SIMPMUSIC -> Res.string.lyrics_about_simpmusic
            LyricsSource.NETEASE -> Res.string.lyrics_about_netease
            LyricsSource.QQ -> Res.string.lyrics_about_qq
            LyricsSource.KUGOU -> Res.string.lyrics_about_kugou
        }

/** One badge style per kind, so Settings and the picker read the same. Off greys it out. */
@Composable
fun LyricsKindBadge(
    kind: LyricsKind,
    on: Boolean = true,
) {
    val scheme = MaterialTheme.colorScheme
    val (bg, fg) =
        when {
            !on -> scheme.onSurface.copy(alpha = 0.06f) to scheme.onSurfaceVariant.copy(alpha = 0.6f)
            kind == LyricsKind.WORDS -> scheme.primary.copy(alpha = 0.14f) to scheme.primary
            kind == LyricsKind.SYNCED -> scheme.onSurface.copy(alpha = 0.08f) to scheme.onSurface.copy(alpha = 0.8f)
            else -> scheme.onSurface.copy(alpha = 0.06f) to scheme.onSurfaceVariant
        }
    Text(
        stringResource(kind.label).uppercase(),
        style = typo().labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp),
        color = fg,
        maxLines = 1,
        modifier = Modifier.clip(CircleShape).background(bg).padding(horizontal = 7.dp, vertical = 2.dp),
    )
}

/**
 * Desktop's lyrics provider order (Limusic #23, #326): drag a source up to ask it sooner, switch it
 * off to never ask it. Saved as it changes; a change drops the cached lyrics it would have changed,
 * sparing songs whose source was picked by hand.
 */
@Composable
fun LyricsSourcesScreen(
    innerPadding: PaddingValues,
    navController: NavController,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val saved by viewModel.lyricsProviders.collectAsStateWithLifecycle()
    // The rows a drag moves. They follow [saved] except mid-drag; the order is written on release.
    var rows by remember { mutableStateOf(saved) }
    var dragging by remember { mutableStateOf<LyricsSource?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    // Rows wrap their descriptions differently, so a swap waits for half of the neighbour's own height.
    val heights = remember { mutableStateMapOf<LyricsSource, Int>() }
    LaunchedEffect(saved) { if (dragging == null) rows = saved }

    fun commit(next: List<Pair<LyricsSource, Boolean>>) {
        rows = next
        viewModel.setLyricsProviders(next)
    }

    // ponytail: no edge auto-scroll like Edit Home's; eight rows fit on a phone screen.
    fun dragBy(dy: Float) {
        val source = dragging ?: return
        dragOffset += dy
        while (true) {
            val i = rows.indexOfFirst { it.first == source }
            val by =
                when {
                    i < rows.lastIndex && dragOffset > (heights[rows[i + 1].first] ?: 0) / 2f -> 1
                    i > 0 && dragOffset < -(heights[rows[i - 1].first] ?: 0) / 2f -> -1
                    else -> break
                }
            val passed = heights[rows[i + by].first] ?: 0
            rows = rows.toMutableList().apply { add(i + by, removeAt(i)) }
            dragOffset -= by * passed
        }
    }

    fun endDrag() {
        if (dragging == null) return
        dragging = null
        dragOffset = 0f
        commit(rows)
    }

    val isDefault = rows == LyricsSource.order(null)
    val scheme = MaterialTheme.colorScheme
    Column(
        Modifier
            .fillMaxSize()
            .padding(top = innerPadding.calculateTopPadding())
            .verticalScroll(rememberScrollState()),
    ) {
        SettingTopBar(title = stringResource(Res.string.lyrics_sources), onBack = { navController.navigateUp() }, windowInsets = WindowInsets(0))
        Column(Modifier.padding(horizontal = 16.dp)) {
            Row(Modifier.fillMaxWidth().padding(start = 16.dp, top = 8.dp), verticalAlignment = Alignment.Top) {
                Text(
                    stringResource(Res.string.lyrics_sources_hint),
                    style = typo().bodySmall,
                    modifier = Modifier.weight(1f).padding(top = 10.dp),
                )
                TextButton(onClick = { commit(LyricsSource.order(null)) }, enabled = !isDefault) {
                    Icon(LimusicIcons.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(Res.string.lyrics_sources_reset))
                }
            }
            SettingGroup(
                title = null,
                footer =
                    if (rows.none { it.second }) {
                        { Text(stringResource(Res.string.lyrics_sources_none), style = typo().bodySmall, color = scheme.error) }
                    } else {
                        null
                    },
            ) {
                var rank = 0
                rows.forEachIndexed { index, (source, on) ->
                    val position = if (on) ++rank else null
                    key(source) {
                        val isDragged = dragging == source
                        val divider = scheme.outlineVariant
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .onSizeChanged { heights[source] = it.height }
                                .zIndex(if (isDragged) 1f else 0f)
                                .graphicsLayer { translationY = if (isDragged) dragOffset else 0f }
                                .background(if (isDragged) scheme.surfaceContainerHighest else Color.Transparent)
                                .drawBehind {
                                    if (index > 0 && !isDragged) drawLine(divider, Offset(56.dp.toPx(), 0f), Offset(size.width, 0f), 0.5.dp.toPx())
                                }.padding(end = 16.dp, top = 10.dp, bottom = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                Modifier
                                    .size(48.dp)
                                    .pointerInput(source) {
                                        detectDragGestures(
                                            onDragStart = {
                                                dragging = source
                                                dragOffset = 0f
                                            },
                                            onDrag = { change, amount ->
                                                change.consume()
                                                dragBy(amount.y)
                                            },
                                            onDragEnd = { endDrag() },
                                            onDragCancel = { endDrag() },
                                        )
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(LimusicIcons.DragHandle, contentDescription = null, tint = scheme.onSurfaceVariant.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
                            }
                            // Where it sits among the sources that are on: the order they're asked in.
                            Box(
                                Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(if (on) scheme.primary.copy(alpha = 0.14f) else scheme.onSurface.copy(alpha = 0.06f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    position?.toString() ?: "-",
                                    style = typo().labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                                    color = if (on) scheme.primary else scheme.onSurfaceVariant.copy(alpha = 0.5f),
                                )
                            }
                            Column(Modifier.weight(1f).padding(start = 12.dp, end = 10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        source.displayName,
                                        style = typo().labelMedium,
                                        color = if (on) scheme.onSurface else scheme.onSurfaceVariant,
                                        maxLines = 1,
                                        modifier = Modifier.weight(1f, fill = false),
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    LyricsKindBadge(source.bestKind, on)
                                }
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    stringResource(source.about),
                                    style = typo().bodySmall,
                                    modifier = Modifier.graphicsLayer { alpha = if (on) 1f else 0.6f },
                                )
                            }
                            Switch(
                                checked = on,
                                onCheckedChange = { checked -> commit(rows.map { if (it.first == source) source to checked else it }) },
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(innerPadding.calculateBottomPadding() + 24.dp))
        }
    }
}
