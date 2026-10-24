package com.maxrave.simpmusic.ui.screen.player.content

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.maxrave.domain.data.model.lyrics.RomanizationLanguage
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.domain.repository.LyricsRomanizerRepository
import com.maxrave.simpmusic.extension.elapsedLabel
import com.maxrave.simpmusic.extension.lengthLabel
import com.maxrave.simpmusic.ui.component.AnimatedLyricsGradientBackground
import com.maxrave.simpmusic.ui.component.LyricsSourceSheet
import com.maxrave.simpmusic.ui.component.LyricsView
import com.maxrave.simpmusic.ui.component.Stepper
import com.maxrave.simpmusic.ui.component.formatSeconds
import com.maxrave.simpmusic.ui.icon.KeyboardArrowDown
import com.maxrave.simpmusic.ui.icon.MoreVert
import com.maxrave.simpmusic.ui.icon.PushPin
import com.maxrave.simpmusic.ui.icon.Search
import com.maxrave.simpmusic.ui.icon.Share
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.screen.player.content.expressive.WavySeekBar
import com.maxrave.simpmusic.ui.theme.LocalArtworkColors
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.NowPlayingScreenData
import com.maxrave.simpmusic.viewModel.SharedViewModel
import com.maxrave.simpmusic.viewModel.UIEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.lyrics_find
import simpmusic.composeapp.generated.resources.lyrics_from
import simpmusic.composeapp.generated.resources.lyrics_none_found
import simpmusic.composeapp.generated.resources.lyrics_none_found_hint
import simpmusic.composeapp.generated.resources.lyrics_romanize
import simpmusic.composeapp.generated.resources.lyrics_text_size
import simpmusic.composeapp.generated.resources.lyrics_timing_value
import simpmusic.composeapp.generated.resources.share_lyrics
import kotlin.math.roundToInt

/** Desktop's text sizes, 85% to 150%. */
private val TEXT_SIZES = listOf(0.85f, 1f, 1.15f, 1.3f, 1.5f)

/**
 * The Classic player's lyrics page, after desktop Limusic's lyrics panel: the cover's tint behind
 * everything as on the player itself, the lines in the theme's ink, and a footer with the source
 * switch, romanization and text size above the player's own seek bar and transport pills. The pills
 * tuck away after a few seconds and come back on a tap.
 */
@Composable
internal fun ClassicLyricsPage(
    sharedViewModel: SharedViewModel,
    state: NowPlayingContentState,
    actions: NowPlayingContentActions,
    onDismiss: () -> Unit,
    onShare: () -> Unit,
    onMore: () -> Unit,
    onOpenLyricsSettings: () -> Unit,
) {
    if (LocalArtworkColors.current) {
        Page(sharedViewModel, state, actions, onDismiss, onShare, onMore, onOpenLyricsSettings)
    } else {
        NowPlayingExpressiveTheme(state = state) {
            Page(sharedViewModel, state, actions, onDismiss, onShare, onMore, onOpenLyricsSettings)
        }
    }
}

@Composable
private fun Page(
    sharedViewModel: SharedViewModel,
    state: NowPlayingContentState,
    actions: NowPlayingContentActions,
    onDismiss: () -> Unit,
    onShare: () -> Unit,
    onMore: () -> Unit,
    onOpenLyricsSettings: () -> Unit,
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

    var showControls by rememberSaveable { mutableStateOf(true) }
    LaunchedEffect(showControls) {
        if (showControls) {
            delay(4000)
            showControls = false
        }
    }
    // A new song closes the picker, like desktop's.
    var picker by remember(videoId) { mutableStateOf(false) }

    val canRomanize = rememberCanRomanize(lyricsData?.lyrics?.lines?.map { it.words })

    val background = MaterialTheme.colorScheme.background
    Box(
        Modifier
            .fillMaxSize()
            .background(background)
            .clickable(interactionSource = null, indication = null) { showControls = true },
    ) {
        // SimpMusic's breathing gradient in the Classic player's tint: the cover colour at the
        // player's strength, drifting into the theme background. The target colour, not the
        // animating one, so the gradient runs its own glide instead of restarting every frame.
        AnimatedLyricsGradientBackground(color = lerp(background, state.startColor.targetValue, 0.55f), end = background)
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Header(screenData.thumbnailURL, screenData.nowPlayingTitle, screenData.artistName, lyricsData != null, onDismiss, actions.onNavigateToArtist, onShare, onMore)

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

            Column(Modifier.padding(horizontal = 24.dp)) {
                WavySeekBar(
                    progressFraction = state.sliderValue / 100f,
                    isPlaying = state.controllerState.isPlaying,
                    activeColor = if (state.timelineState.isCrossfading) state.sliderTrackColor else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.secondaryContainer,
                    thumbColor = MaterialTheme.colorScheme.primary,
                    onSliderChange = actions.onSliderChange,
                    onSliderChangeFinished = actions.onSliderChangeFinished,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                    Text(state.timelineState.elapsedLabel(state.sliderValue / 100f), style = typo().bodySmall, modifier = Modifier.weight(1f))
                    Text(state.timelineState.lengthLabel(), style = typo().bodySmall)
                }
                AnimatedVisibility(
                    visible = showControls,
                    enter = expandVertically(tween(300)) + fadeIn(tween(300)),
                    exit = shrinkVertically(tween(300)) + fadeOut(tween(200)),
                ) {
                    Column {
                        Spacer(Modifier.height(10.dp))
                        ClassicTransportRow(state, actions)
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
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

@Composable
private fun Header(
    thumbnail: String?,
    title: String,
    artist: String,
    hasLyrics: Boolean,
    onDismiss: () -> Unit,
    onArtist: () -> Unit,
    onShare: () -> Unit,
    onMore: () -> Unit,
) {
    Row(Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onDismiss) {
            Icon(SimpIcons.KeyboardArrowDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
        }
        AsyncImage(
            model = thumbnail,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)),
        )
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(
                title,
                style = typo().titleSmall.copy(fontSize = 15.sp),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                modifier = Modifier.basicMarquee(),
            )
            Text(
                artist,
                style = typo().bodySmall,
                maxLines = 1,
                modifier = Modifier.padding(top = 2.dp).basicMarquee().clickable(onClick = onArtist),
            )
        }
        if (hasLyrics) {
            IconButton(onClick = onShare) {
                Icon(SimpIcons.Share, contentDescription = stringResource(Res.string.share_lyrics), tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(22.dp))
            }
        }
        IconButton(onClick = onMore) {
            Icon(SimpIcons.MoreVert, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
        }
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
                        Icon(SimpIcons.Search, contentDescription = null, modifier = Modifier.size(14.dp), tint = scheme.onSurface.copy(alpha = 0.8f))
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        if (provider != null) stringResource(Res.string.lyrics_from, provider.displayName) else stringResource(Res.string.lyrics_find),
                        style = typo().labelSmall,
                        color = scheme.onSurface.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (lyricsData?.pinned == true) {
                        Spacer(Modifier.width(4.dp))
                        Icon(SimpIcons.PushPin, contentDescription = null, modifier = Modifier.size(12.dp), tint = scheme.primary)
                    }
                    val offset = lyricsData?.offsetMs ?: 0
                    if (offset != 0) {
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(Res.string.lyrics_timing_value, formatSeconds(offset)), style = typo().labelSmall, color = scheme.primary, maxLines = 1)
                    }
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        SimpIcons.KeyboardArrowDown,
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
                DropdownMenu(expanded = open, onDismissRequest = { open = false }, shape = RoundedCornerShape(16.dp)) {
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
            Icon(SimpIcons.Search, contentDescription = null, modifier = Modifier.size(16.dp))
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
                    .clip(RoundedCornerShape(8.dp))
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
