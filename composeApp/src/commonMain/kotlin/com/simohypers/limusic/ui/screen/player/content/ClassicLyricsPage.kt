package com.simohypers.limusic.ui.screen.player.content

import com.simohypers.limusic.ui.component.credit
import com.simohypers.limusic.ui.theme.rounded
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.simohypers.limusic.domain.data.model.lyrics.RomanizationLanguage
import com.simohypers.limusic.domain.manager.DataStoreManager
import com.simohypers.limusic.domain.repository.LyricsRomanizerRepository
import com.simohypers.limusic.ui.component.LyricsSourceSheet
import com.simohypers.limusic.ui.component.LyricsView
import com.simohypers.limusic.ui.component.Stepper
import com.simohypers.limusic.ui.component.formatSeconds
import com.simohypers.limusic.ui.icon.KeyboardArrowDown
import com.simohypers.limusic.ui.icon.PushPin
import com.simohypers.limusic.ui.icon.Search
import com.simohypers.limusic.ui.icon.LimusicIcons
import com.simohypers.limusic.ui.theme.typo
import com.simohypers.limusic.viewModel.NowPlayingScreenData
import com.simohypers.limusic.viewModel.SharedViewModel
import com.simohypers.limusic.viewModel.UIEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import limusic.composeapp.generated.resources.Res
import limusic.composeapp.generated.resources.lyrics_find
import limusic.composeapp.generated.resources.lyrics_none_found
import limusic.composeapp.generated.resources.lyrics_none_found_hint
import limusic.composeapp.generated.resources.lyrics_romanize
import limusic.composeapp.generated.resources.lyrics_text_size
import limusic.composeapp.generated.resources.lyrics_timing_value
import kotlin.math.roundToInt

/** Desktop's text sizes, 85% to 150%. */
private val TEXT_SIZES = listOf(0.85f, 1f, 1.15f, 1.3f, 1.5f)

/**
 * The Classic player's Lyrics view, after desktop Limusic's lyrics panel: the lines in the theme's
 * ink, then a footer with the source switch, romanization and text size. The page around it draws
 * the song header and the controls.
 */
@Composable
internal fun ClassicLyricsBody(
    sharedViewModel: SharedViewModel,
    onOpenLyricsSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val screenData by sharedViewModel.nowPlayingScreenData.collectAsStateWithLifecycle()
    val nowPlaying by sharedViewModel.nowPlayingState.collectAsStateWithLifecycle()
    val missFor by sharedViewModel.lyricsMissFor.collectAsStateWithLifecycle()
    val textScale by sharedViewModel.getLyricsTextSize().collectAsStateWithLifecycle(1f)
    val romanizedOn by sharedViewModel.getLyricsRomanized().collectAsStateWithLifecycle(true)
    val videoId = nowPlaying?.songEntity?.videoId
    val lyricsData = screenData.lyricsData
    // Null lyrics mean "still looking" until the chain has come back empty for this very song.
    val missing = lyricsData == null && videoId != null && missFor == videoId

    // A new song closes the picker, like desktop's.
    var picker by remember(videoId) { mutableStateOf(false) }

    val canRomanize = rememberCanRomanize(lyricsData?.lyrics?.lines?.map { it.words })

    Column(modifier) {
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                // Lines fade out at both edges instead of being cut by them.
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    drawRect(
                        Brush.verticalGradient(0f to Color.Transparent, 0.06f to Color.Black, 0.9f to Color.Black, 1f to Color.Transparent),
                        blendMode = BlendMode.DstIn,
                    )
                }.padding(horizontal = 24.dp),
        ) {
            when {
                lyricsData != null ->
                    LyricsView(
                        lyricsData = lyricsData,
                        timeLine = sharedViewModel.timeline,
                        onLineClick = { sharedViewModel.onUIEvent(UIEvent.UpdateProgress(it)) },
                        modifier = Modifier.fillMaxSize(),
                        limusic = true,
                        textScale = textScale,
                        showRomanized = romanizedOn,
                    )
                missing -> NothingFound { picker = true }
                else -> Skeleton()
            }
        }

        Footer(
            lyricsData = lyricsData,
            missing = missing,
            canRomanize = canRomanize,
            romanizedOn = romanizedOn,
            textScale = textScale,
            onSource = { picker = true },
            onRomanized = { sharedViewModel.setLyricsRomanized(!romanizedOn) },
            onTextScale = { sharedViewModel.setLyricsTextSize(it) },
        )
    }

    if (picker) {
        LyricsSourceSheet(
            lyricsData = lyricsData,
            sharedViewModel = sharedViewModel,
            onOpenSettings = {
                picker = false
                onOpenLyricsSettings()
            },
            onDismiss = { picker = false },
        )
    }
}

/** The source chip (the picker's way in), then romanization and text size when they apply. */
@Composable
private fun Footer(
    lyricsData: NowPlayingScreenData.LyricsData?,
    missing: Boolean,
    canRomanize: Boolean,
    romanizedOn: Boolean,
    textScale: Float,
    onSource: () -> Unit,
    onRomanized: () -> Unit,
    onTextScale: (Float) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.weight(1f)) {
            val provider = lyricsData?.lyricsProvider
            if (provider != null || missing) {
                Chip(onClick = onSource) {
                    if (provider == null) {
                        Icon(LimusicIcons.Search, contentDescription = null, modifier = Modifier.size(14.dp), tint = scheme.onSurface.copy(alpha = 0.8f))
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        lyricsData?.credit() ?: stringResource(Res.string.lyrics_find),
                        style = typo().labelSmall,
                        color = scheme.onSurface.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (lyricsData?.pinned == true) {
                        Spacer(Modifier.width(4.dp))
                        Icon(LimusicIcons.PushPin, contentDescription = null, modifier = Modifier.size(12.dp), tint = scheme.primary)
                    }
                    val offset = lyricsData?.offsetMs ?: 0
                    if (offset != 0) {
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(Res.string.lyrics_timing_value, formatSeconds(offset)), style = typo().labelSmall, color = scheme.primary, maxLines = 1)
                    }
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        LimusicIcons.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp).graphicsLayer { rotationZ = 180f },
                        tint = scheme.onSurface.copy(alpha = 0.6f),
                    )
                }
            }
        }
        if (canRomanize) {
            Chip(onClick = onRomanized, active = romanizedOn) {
                Text("あ", style = typo().labelSmall.copy(fontWeight = FontWeight.Bold), color = if (romanizedOn) scheme.primary else scheme.onSurface.copy(alpha = 0.8f))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(Res.string.lyrics_romanize), style = typo().labelSmall, color = if (romanizedOn) scheme.primary else scheme.onSurface.copy(alpha = 0.8f))
            }
        }
        if (lyricsData != null) {
            var open by remember { mutableStateOf(false) }
            Box {
                Chip(onClick = { open = true }, active = textScale != 1f) {
                    Text(
                        "Aa",
                        style = typo().labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (textScale != 1f) scheme.primary else scheme.onSurface.copy(alpha = 0.8f),
                    )
                }
                DropdownMenu(expanded = open, onDismissRequest = { open = false }, shape = rounded(16.dp)) {
                    Row(Modifier.padding(horizontal = 14.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(Res.string.lyrics_text_size), style = typo().labelMedium, color = scheme.onSurface)
                        Spacer(Modifier.width(14.dp))
                        val i = TEXT_SIZES.indexOf(textScale).coerceAtLeast(0)
                        Stepper(
                            label = "${(textScale * 100).roundToInt()}%",
                            highlighted = textScale != 1f,
                            canDown = i > 0,
                            canUp = i < TEXT_SIZES.lastIndex,
                            onDown = { onTextScale(TEXT_SIZES[i - 1]) },
                            onUp = { onTextScale(TEXT_SIZES[i + 1]) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Chip(
    onClick: () -> Unit,
    active: Boolean = false,
    content: @Composable RowScope.() -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        Modifier
            .height(32.dp)
            .clip(CircleShape)
            .background(if (active) scheme.primary.copy(alpha = 0.14f) else scheme.onSurface.copy(alpha = 0.08f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
private fun NothingFound(onFind: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(bottom = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(stringResource(Res.string.lyrics_none_found), style = typo().bodyMedium, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
        Text(
            stringResource(Res.string.lyrics_none_found_hint),
            style = typo().bodySmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp, start = 24.dp, end = 24.dp),
        )
        OutlinedButton(onClick = onFind, modifier = Modifier.padding(top = 16.dp)) {
            Icon(LimusicIcons.Search, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(Res.string.lyrics_find))
        }
    }
}

/** Placeholder lines while the sources are asked, the widths desktop uses. */
@Composable
private fun Skeleton() {
    val pulse by rememberInfiniteTransition(label = "lyricsSkeleton").animateFloat(
        initialValue = 0.05f,
        targetValue = 0.12f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "lyricsSkeletonAlpha",
    )
    Column(Modifier.fillMaxSize().padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        repeat(8) { i ->
            Box(
                Modifier
                    .fillMaxWidth((55 + (i * 17) % 40) / 100f)
                    .height(24.dp)
                    .clip(rounded(8.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = pulse)),
            )
        }
    }
}

/**
 * Whether any line has a reading in the languages picked in Settings, so the Romanized chip only
 * shows where it does something. Off the main thread: Japanese runs a dictionary over every line.
 */
@Composable
private fun rememberCanRomanize(lines: List<String>?): Boolean {
    val dataStoreManager = koinInject<DataStoreManager>()
    val romanizer = koinInject<LyricsRomanizerRepository>()
    val stored by dataStoreManager.romanizationLanguages.collectAsStateWithLifecycle("")
    val dictionary by romanizer.japaneseDictionaryState.collectAsStateWithLifecycle()
    val languages = remember(stored) { RomanizationLanguage.parse(stored) }
    val can by produceState(false, lines, languages, dictionary) {
        value = languages.isNotEmpty() &&
            withContext(Dispatchers.Default) {
                lines.orEmpty().any { romanizer.romanize(it.stripRichSyncTimestamps(), languages) != null }
            }
    }
    return can
}
