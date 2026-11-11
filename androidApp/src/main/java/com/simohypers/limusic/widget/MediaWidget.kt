package com.simohypers.limusic.widget

import android.content.Context
import android.graphics.Bitmap
import android.os.PowerManager
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartService
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.color.ColorProvider as DayNight
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import com.simohypers.limusic.MainActivity
import com.simohypers.limusic.R
import com.simohypers.limusic.domain.data.player.GenericMediaItem
import com.simohypers.limusic.domain.extension.toGenericMediaItem
import com.simohypers.limusic.domain.manager.DataStoreManager
import com.simohypers.limusic.domain.mediaservice.handler.ControlState
import com.simohypers.limusic.domain.mediaservice.handler.MediaPlayerHandler
import com.simohypers.limusic.domain.mediaservice.handler.NowPlayingTrackState
import com.simohypers.limusic.domain.mediaservice.handler.PlayerEvent
import com.simohypers.limusic.domain.repository.SongRepository
import com.simohypers.limusic.domain.utils.connectArtists
import com.simohypers.limusic.expect.ui.wallpaperColorScheme
import com.simohypers.limusic.media3.di.playButtonIntent
import com.simohypers.limusic.ui.theme.Appearance
import com.simohypers.limusic.ui.theme.ThemePreset
import com.simohypers.limusic.ui.theme.themeScheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import com.simohypers.limusic.media3.R as MediaR

/**
 * The home-screen player, one layout per shape (see [Widget]). It shows what the notification
 * shows, the app's handler, so it is the linked computer's track while this phone controls one.
 *
 * Play goes to the playback service as a media button, the notification's own path: it starts the
 * app if it was killed, and with nothing loaded it resumes the saved queue, which is the song the
 * widget shows then. Pause and skip run in the app's process through the handler, which keeps a
 * Listen Together guest's limits. With nothing saved either, play opens the app instead: a service
 * start that never plays would miss Android's foreground deadline.
 */
class MediaWidget :
    GlanceAppWidget(),
    KoinComponent {
    // Exact, not Responsive: the layout switches on the real size and aspect, which no fixed size list knows.
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        val handler = get<MediaPlayerHandler>()
        val colors = colors(context, get())
        val firstPalette = colors.first()
        val saved = if (handler.nowPlayingState.value.isNotEmpty()) null else savedSong()?.shown()
        val firstArt = cover(context, handler.nowPlayingState.value.shownOr(saved)?.art)
        // A session lives about 45 seconds; while it does these flows redraw it, after that
        // [followMediaWidget] starts the next one.
        provideContent {
            val now by handler.nowPlayingState.collectAsState()
            val control by handler.controlState.collectAsState()
            val palette by colors.collectAsState(firstPalette)
            val progress by widgetProgress.collectAsState()
            val shown = now.shownOr(saved)
            var art by remember { mutableStateOf(firstArt) }
            LaunchedEffect(shown?.art) { art = cover(context, shown?.art) }
            Widget(shown, now.isNotEmpty(), control, art, palette, progress)
        }
    }

    /** The song a resume would start: the saved queue's current one. */
    private suspend fun savedSong(): GenericMediaItem? {
        val tracks =
            get<SongRepository>()
                .getSavedQueue()
                .first()
                ?.firstOrNull()
                ?.listTrack
                .orEmpty()
        if (tracks.isEmpty()) return null
        val recent = get<DataStoreManager>().recentMediaId.first()
        return (tracks.firstOrNull { it.videoId == recent } ?: tracks.first()).toGenericMediaItem()
    }
}

class MediaWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MediaWidget()
}

/** Pause, next and previous, on the handler like the mini player's buttons. */
class MediaWidgetAction :
    ActionCallback,
    KoinComponent {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val handler = get<MediaPlayerHandler>()
        // The app was killed since the widget drew; starting it redrew the widget without these buttons.
        if (!handler.nowPlayingState.value.isNotEmpty()) return
        val event =
            when (parameters[Key]) {
                NEXT -> PlayerEvent.Next
                PREVIOUS -> PlayerEvent.Previous
                else -> PlayerEvent.PlayPause
            }
        // The player lives on the main thread.
        withContext(Dispatchers.Main) { handler.onPlayerEvent(event) }
    }

    companion object {
        val Key = ActionParameters.Key<String>("event")
        const val PAUSE = "pause"
        const val NEXT = "next"
        const val PREVIOUS = "previous"
    }
}

/**
 * Redraws the placed widgets whenever the track, the play state or the theme changes. Runs for the
 * app process's life ([com.simohypers.limusic.LimusicApplication]); with no widget placed,
 * [updateAll] finds nothing to do.
 */
suspend fun followMediaWidget(
    context: Context,
    handler: MediaPlayerHandler,
    store: DataStoreManager,
) = coroutineScope {
    launch { tickProgress(context, handler) }
    combine(
        handler.nowPlayingState,
        handler.controlState.map { listOf(it.isPlaying, it.isNextAvailable, it.isPreviousAvailable) },
        colors(context, store),
    ) { now, control, palette -> Triple(now, control, palette) }
        .distinctUntilChanged()
        .collectLatest { MediaWidget().updateAll(context) }
}

/** Where the track is, 0..1 in the bar's 1% steps, or null with nothing loaded. */
private val widgetProgress = MutableStateFlow<Float?>(null)

/**
 * Moves the bar while something plays. Every move redraws the whole widget, cover included, so it
 * only runs while the screen is on and a widget is placed, and redraws only when the percent
 * changes: Glance's bar has 100 steps, about one every 2 seconds for a song. A pause or a new
 * track sets it once.
 */
private suspend fun tickProgress(
    context: Context,
    handler: MediaPlayerHandler,
) {
    val power = context.getSystemService(PowerManager::class.java)
    val widgets = GlanceAppWidgetManager(context)
    combine(handler.controlState.map { it.isPlaying }, handler.nowPlayingState.map { it.mediaItem.mediaId }) { playing, id -> playing to id }
        .distinctUntilChanged()
        .collectLatest { (playing, _) ->
            do {
                if (power.isInteractive && widgets.getGlanceIds(MediaWidget::class.java).isNotEmpty()) {
                    val at =
                        withContext(Dispatchers.Main) {
                            if (!handler.nowPlayingState.value.isNotEmpty()) return@withContext null
                            // Unknown while a track loads (and for a live stream): an empty bar, so the layout holds still.
                            val length = handler.getPlayerDuration()
                            if (length <= 0) 0f else (handler.getProgress() * 100 / length).coerceIn(0, 100) / 100f
                        }
                    if (at != widgetProgress.value) {
                        widgetProgress.value = at
                        MediaWidget().updateAll(context)
                    }
                }
                if (playing) delay(1_000)
            } while (playing)
        }
}

private data class Shown(
    val title: String?,
    val artist: String?,
    val art: String?,
)

/** What the mini player shows, or [saved] while nothing is loaded. */
private fun NowPlayingTrackState.shownOr(saved: Shown?): Shown? =
    if (!isNotEmpty()) {
        saved
    } else {
        Shown(
            title = songEntity?.title ?: mediaItem.metadata.title,
            artist = songEntity?.artistName?.connectArtists() ?: mediaItem.metadata.artist,
            art = songEntity?.thumbnails ?: mediaItem.metadata.artworkUri,
        )
    }

private fun GenericMediaItem.shown() = Shown(metadata.title, metadata.artist, metadata.artworkUri)

/** The cover from Coil's cache (the app has usually drawn it already), small enough for RemoteViews. */
private suspend fun cover(
    context: Context,
    url: String?,
): Bitmap? {
    url ?: return null
    val request =
        ImageRequest
            .Builder(context)
            .data(url)
            .size(COVER_PX)
            .allowHardware(false)
            .build()
    return (SingletonImageLoader.get(context).execute(request) as? SuccessResult)?.image?.toBitmap()
}

private const val COVER_PX = 360

/** The few roles the widget paints with, from one mode's scheme. */
private data class Tones(
    val surface: Color,
    val raised: Color,
    val text: Color,
    val subtext: Color,
    val accent: Color,
    val onAccent: Color,
)

private fun ColorScheme.tones() = Tones(surfaceContainer, surfaceContainerHighest, onSurface, onSurfaceVariant, primary, onPrimary)

/** Light and dark tones; the launcher picks by the system's mode, so the app's System setting follows it. */
private data class Palette(
    val day: Tones,
    val night: Tones,
) {
    fun of(
        alpha: Float = 1f,
        pick: Tones.() -> Color,
    ): ColorProvider = DayNight(day = day.pick().copy(alpha = alpha), night = night.pick().copy(alpha = alpha))
}

/** The app's theme as AppTheme builds it, without "Adapt colors to artwork". */
private fun colors(
    context: Context,
    store: DataStoreManager,
): Flow<Palette> =
    combine(
        store.themeMode,
        store.getString(Appearance.KEY),
        store.themeColorSource,
        store.customThemeColor,
    ) { mode, raw, source, hex ->
        val look = Appearance.parse(raw, source, hex)

        fun tones(dark: Boolean): Tones {
            val isDark = look.theme.alwaysDark || dark
            val wallpaper =
                if (look.theme == ThemePreset.MATERIAL_YOU || look.accent == Appearance.WALLPAPER) wallpaperColorScheme(context, isDark) else null
            return themeScheme(look, isDark, wallpaper).tones()
        }
        when (mode) {
            DataStoreManager.THEME_MODE_SYSTEM -> Palette(tones(false), tones(true))
            DataStoreManager.THEME_MODE_LIGHT -> tones(false).let { Palette(it, it) }
            else -> tones(true).let { Palette(it, it) }
        }
    }.distinctUntilChanged()

private val OUTER = android.R.dimen.system_app_widget_background_radius
private val INNER = android.R.dimen.system_app_widget_inner_radius

/** What every layout draws from: the texts, the cover, the colours and the three actions. */
private class Ui(
    val palette: Palette,
    val title: String,
    val subtitle: String,
    val art: Bitmap?,
    val playing: Boolean,
    val play: Action,
    val previous: Action?,
    val next: Action?,
    val progress: Float?,
)

/**
 * Picks the layout from the size the launcher granted (SizeMode.Exact), so each shape gets one
 * drawn for it instead of one stretched. On a 5-column grid at 480 dpi the cells come out about
 * 59/137/215/293/371 dp wide and 60/139/218/296 dp tall:
 * one row is the [Strip]; wide and at least two rows is [Side], or [Stacked] when the cover would
 * leave the text no room (3x2); anything squarer is the [Poster].
 */
@Composable
private fun Widget(
    shown: Shown?,
    loaded: Boolean,
    control: ControlState,
    art: Bitmap?,
    palette: Palette,
    progress: Float?,
) {
    val context = LocalContext.current
    val size = LocalSize.current
    val playing = loaded && control.isPlaying
    val ui =
        Ui(
            palette = palette,
            title = shown?.title ?: context.getString(MediaR.string.widget_nothing_playing),
            subtitle = if (shown == null) context.getString(MediaR.string.widget_pick_something) else shown.artist.orEmpty(),
            art = art,
            playing = playing,
            play =
                when {
                    playing -> event(MediaWidgetAction.PAUSE)
                    shown != null -> actionStartService(playButtonIntent(context), isForegroundService = true)
                    else -> actionStartActivity<MainActivity>()
                },
            previous = if (loaded && control.isPreviousAvailable) event(MediaWidgetAction.PREVIOUS) else null,
            next = if (loaded && control.isNextAvailable) event(MediaWidgetAction.NEXT) else null,
            progress = if (loaded) progress else null,
        )
    val root =
        GlanceModifier
            .fillMaxSize()
            .cornerRadius(OUTER)
            .background(palette.of { surface })
            .clickable(actionStartActivity<MainActivity>())
    val wide = size.width >= size.height * 1.5f
    when {
        size.height < 100.dp -> Strip(root, ui)
        wide && size.width >= size.height + 146.dp -> Side(root, ui)
        wide -> Stacked(root, ui)
        else -> Poster(root, ui)
    }
}

private fun event(event: String): Action = actionRunCallback<MediaWidgetAction>(actionParametersOf(MediaWidgetAction.Key to event))

/**
 * One row: the badged cover, the title once there is room for it, the controls. Below 180 dp
 * (2x1) the cover, play and next share the width equally; below 260 dp (3x1) only play stays.
 */
@Composable
private fun Strip(
    root: GlanceModifier,
    ui: Ui,
) {
    val size = LocalSize.current
    val narrow = size.width < 180.dp
    val cover = if (narrow) minOf(size.height - 16.dp, (size.width - 28.dp) / 3) else size.height - 16.dp
    val button = minOf(44.dp, cover)
    Row(root.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
        BadgedCover(ui, cover)
        if (narrow) {
            Spacer(GlanceModifier.defaultWeight())
            Play(ui, button)
            Spacer(GlanceModifier.width(4.dp))
            Skip(ui, R.drawable.rounded_skip_next_24, MediaR.string.widget_next, ui.next, button)
        } else {
            Column(GlanceModifier.defaultWeight().padding(horizontal = 12.dp)) {
                Titles(ui, title = 15.sp, subtitle = 13.sp)
                Progress(ui, above = 5.dp)
            }
            if (size.width >= 260.dp) Controls(ui, button) else Play(ui, button)
        }
    }
}

/** Wide and tall enough: the cover at full height, beside it the titles under the logo row and the controls. */
@Composable
private fun Side(
    root: GlanceModifier,
    ui: Ui,
) {
    val size = LocalSize.current
    val cover = size.height - 24.dp
    val tall = size.height >= 180.dp
    // The column beside the cover; a tall cover can leave it short of three full-size buttons (5x3).
    val button = minOf(if (tall) 52.dp else 44.dp, (size.width - 24.dp - cover - 14.dp - 8.dp) / 3)
    Row(root.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Cover(ui, GlanceModifier.size(cover).cornerRadius(INNER), logo = cover / 2)
        Spacer(GlanceModifier.width(14.dp))
        Column(GlanceModifier.defaultWeight().fillMaxHeight()) {
            Row(GlanceModifier.fillMaxWidth()) {
                Column(GlanceModifier.defaultWeight()) {
                    Titles(ui, title = if (tall) 20.sp else 16.sp, subtitle = if (tall) 15.sp else 13.sp, titleLines = if (tall) 2 else 1)
                }
                Spacer(GlanceModifier.width(8.dp))
                Logo(ui.palette, if (tall) 26.dp else 22.dp)
            }
            Spacer(GlanceModifier.defaultWeight())
            Progress(ui, below = 8.dp)
            Controls(ui, button, spread = true)
        }
    }
}

/** 3x2: a small cover with the titles and the logo on top, the controls spread under them. */
@Composable
private fun Stacked(
    root: GlanceModifier,
    ui: Ui,
) {
    Column(root.padding(12.dp)) {
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Cover(ui, GlanceModifier.size(52.dp).cornerRadius(INNER), logo = 26.dp)
            Spacer(GlanceModifier.width(10.dp))
            Column(GlanceModifier.defaultWeight()) { Titles(ui, title = 15.sp, subtitle = 13.sp) }
            Spacer(GlanceModifier.width(6.dp))
            Logo(ui.palette, 20.dp)
        }
        Spacer(GlanceModifier.defaultWeight())
        Progress(ui, below = 8.dp)
        Controls(ui, 44.dp, spread = true)
    }
}

/**
 * The cover edge to edge, fading into the theme's surface at the foot, where the titles and the
 * controls sit; the logo in a chip on the cover's corner. 2x2 has room for the controls only. With
 * room beside the controls the artist moves up next to them.
 */
@Composable
private fun Poster(
    root: GlanceModifier,
    ui: Ui,
) {
    val size = LocalSize.current
    val titled = size.height >= 180.dp
    val button = minOf(48.dp, (size.width - 36.dp) / 3)
    val inline = titled && size.width - 24.dp - (button * 3 + 12.dp) >= 72.dp
    Box(root, contentAlignment = Alignment.BottomStart) {
        Cover(ui, GlanceModifier.fillMaxSize(), logo = size.width / 3)
        Image(
            ImageProvider(R.drawable.widget_scrim),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            colorFilter = ColorFilter.tint(ui.palette.of { surface }),
            modifier = GlanceModifier.fillMaxWidth().height(size.height * if (titled) 0.72f else 0.66f),
        )
        Box(GlanceModifier.fillMaxSize().padding(10.dp), contentAlignment = Alignment.TopStart) {
            Logo(ui.palette, 30.dp, chip = true)
        }
        Column(GlanceModifier.fillMaxWidth().padding(12.dp)) {
            if (titled) {
                val big = size.width >= 260.dp
                Text(
                    ui.title,
                    maxLines = 1,
                    style = TextStyle(color = ui.palette.of { text }, fontSize = if (big) 20.sp else 17.sp, fontWeight = FontWeight.Bold),
                )
                if (inline) {
                    Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(ui.subtitle, modifier = GlanceModifier.defaultWeight(), maxLines = 1, style = subtitleStyle(ui, if (big) 15.sp else 14.sp))
                        Controls(ui, button)
                    }
                    Progress(ui, above = 6.dp)
                } else {
                    Text(ui.subtitle, maxLines = 1, style = subtitleStyle(ui, 14.sp))
                    Progress(ui, above = 8.dp)
                    Spacer(GlanceModifier.height(6.dp))
                    Controls(ui, button, spread = true)
                }
            } else {
                Progress(ui, below = 8.dp)
                Controls(ui, button, spread = true)
            }
        }
    }
}

/** A thin accent bar where the track is, with its gaps; nothing at all while the position is unknown. */
@Composable
private fun Progress(
    ui: Ui,
    above: Dp = 0.dp,
    below: Dp = 0.dp,
) {
    val at = ui.progress ?: return
    if (above > 0.dp) Spacer(GlanceModifier.height(above))
    LinearProgressIndicator(
        progress = at,
        modifier = GlanceModifier.fillMaxWidth().height(4.dp).cornerRadius(2.dp),
        color = ui.palette.of { accent },
        backgroundColor = ui.palette.of(0.18f) { text },
    )
    if (below > 0.dp) Spacer(GlanceModifier.height(below))
}

@Composable
private fun Titles(
    ui: Ui,
    title: TextUnit,
    subtitle: TextUnit,
    titleLines: Int = 1,
) {
    Text(
        ui.title,
        maxLines = titleLines,
        style = TextStyle(color = ui.palette.of { text }, fontSize = title, fontWeight = FontWeight.Bold),
    )
    if (ui.subtitle.isNotEmpty()) Text(ui.subtitle, maxLines = 1, style = subtitleStyle(ui, subtitle))
}

private fun subtitleStyle(
    ui: Ui,
    size: TextUnit,
) = TextStyle(color = ui.palette.of { subtext }, fontSize = size)

/** The cover, or the Limusic mark on a raised tile while there is none. */
@Composable
private fun Cover(
    ui: Ui,
    modifier: GlanceModifier,
    logo: Dp,
) {
    if (ui.art != null) {
        Image(ImageProvider(ui.art), contentDescription = null, contentScale = ContentScale.Crop, modifier = modifier)
    } else {
        Box(modifier.background(ui.palette.of { raised }), contentAlignment = Alignment.Center) {
            Image(
                ImageProvider(R.drawable.mono),
                contentDescription = null,
                colorFilter = ColorFilter.tint(ui.palette.of { subtext }),
                modifier = GlanceModifier.size(logo),
            )
        }
    }
}

/** The strip's cover with the Limusic mark on its corner, like an app badge on a notification. */
@Composable
private fun BadgedCover(
    ui: Ui,
    size: Dp,
) {
    Box(GlanceModifier.size(size), contentAlignment = Alignment.BottomEnd) {
        Cover(ui, GlanceModifier.size(size).cornerRadius(INNER), logo = size / 2)
        if (ui.art != null) Logo(ui.palette, size * 0.4f, chip = true)
    }
}

/**
 * The Limusic mark in one colour, the theme's text, the way Android draws the notification icon;
 * [chip] puts it on a surface circle so it reads over any cover.
 */
@Composable
private fun Logo(
    palette: Palette,
    size: Dp,
    chip: Boolean = false,
) {
    val mark = ColorFilter.tint(palette.of { text })
    if (chip) {
        Box(
            GlanceModifier.size(size).cornerRadius(size / 2).background(palette.of { surface }),
            contentAlignment = Alignment.Center,
        ) {
            Image(ImageProvider(R.drawable.mono), contentDescription = null, colorFilter = mark, modifier = GlanceModifier.size(size * 0.62f))
        }
    } else {
        Image(ImageProvider(R.drawable.mono), contentDescription = null, colorFilter = mark, modifier = GlanceModifier.size(size))
    }
}

/** Previous, play, next: packed, or [spread] across the full width with play in the middle. */
@Composable
private fun Controls(
    ui: Ui,
    size: Dp,
    spread: Boolean = false,
) {
    Row(if (spread) GlanceModifier.fillMaxWidth() else GlanceModifier, verticalAlignment = Alignment.CenterVertically) {
        Skip(ui, R.drawable.rounded_skip_previous_24, MediaR.string.widget_previous, ui.previous, size)
        Spacer(if (spread) GlanceModifier.defaultWeight() else GlanceModifier.width(4.dp))
        Play(ui, size)
        Spacer(if (spread) GlanceModifier.defaultWeight() else GlanceModifier.width(4.dp))
        Skip(ui, R.drawable.rounded_skip_next_24, MediaR.string.widget_next, ui.next, size)
    }
}

/** The accent button: round while paused, a rounded square while playing, the shape M3 Expressive's play button morphs between. */
@Composable
private fun Play(
    ui: Ui,
    size: Dp,
) {
    val context = LocalContext.current
    Box(
        GlanceModifier
            .size(size)
            .cornerRadius(if (ui.playing) size * 0.3f else size / 2)
            .background(ui.palette.of { accent })
            .clickable(ui.play),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            ImageProvider(if (ui.playing) R.drawable.rounded_pause_24 else R.drawable.rounded_play_arrow_24),
            contentDescription = context.getString(if (ui.playing) MediaR.string.widget_pause else MediaR.string.widget_play),
            colorFilter = ColorFilter.tint(ui.palette.of { onAccent }),
            modifier = GlanceModifier.size(size * 0.55f),
        )
    }
}

/** A skip that can't run is dimmed and takes no tap, as in the players. */
@Composable
private fun Skip(
    ui: Ui,
    icon: Int,
    description: Int,
    action: Action?,
    size: Dp,
) {
    val box = GlanceModifier.size(size).cornerRadius(size / 2)
    Box(if (action != null) box.clickable(action) else box, contentAlignment = Alignment.Center) {
        Image(
            ImageProvider(icon),
            contentDescription = LocalContext.current.getString(description),
            colorFilter = ColorFilter.tint(ui.palette.of(if (action != null) 1f else 0.4f) { text }),
            modifier = GlanceModifier.size(size * 0.55f),
        )
    }
}
