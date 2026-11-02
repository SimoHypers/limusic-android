package com.simohypers.limusic.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.simohypers.limusic.extension.artworkScrimBrush
import com.simohypers.limusic.extension.getScreenSizeInfo
import com.simohypers.limusic.ui.icon.ArrowBackIosNew
import com.simohypers.limusic.ui.icon.Pause
import com.simohypers.limusic.ui.icon.PlayArrow
import com.simohypers.limusic.ui.icon.Shuffle
import com.simohypers.limusic.ui.icon.LimusicIcons
import com.simohypers.limusic.ui.theme.typo
import org.jetbrains.compose.resources.stringResource
import limusic.composeapp.generated.resources.Res
import limusic.composeapp.generated.resources.pause
import limusic.composeapp.generated.resources.play
import limusic.composeapp.generated.resources.shuffle

/**
 * The top of an album or playlist page: SimpMusic's edge-to-edge cover, fading into the page's own
 * background like desktop's art wash, with [content] (the title block) over its foot. Back floats
 * top left, [actions] (download, like, ⋮) top right.
 */
@Composable
fun CollectionHero(
    thumbnail: String?,
    onBack: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
    placeholder: Painter = rememberHolderPainter(),
    // Drawn in place of [thumbnail], filling the hero: a list with no cover of its own makes one.
    art: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val screen = getScreenSizeInfo()
    // Half the screen, as SimpMusic had it; the floor keeps room for the title block in landscape.
    Box(Modifier.fillMaxWidth().height(max((screen.hDP / 2).dp, 320.dp))) {
        if (art != null) {
            art()
        } else {
            AsyncImage(
                model = thumbnail,
                placeholder = placeholder,
                error = placeholder,
                fallback = placeholder,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        // 70% of the cover: a short ramp is steep, and a steep ramp reads as an edge.
        Box(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f)
                .align(Alignment.BottomCenter)
                .background(artworkScrimBrush(MaterialTheme.colorScheme.background)),
        )
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            content = content,
        )
        Row(
            Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PillIconButton(LimusicIcons.ArrowBackIosNew, tint = MaterialTheme.colorScheme.onSurface, onClick = onBack)
            Spacer(Modifier.weight(1f))
            actions()
        }
    }
}

/**
 * Desktop's title block: a small uppercase kicker ("ALBUM • 2024") over the big title, with a round
 * Shuffle and Play (Pause while this list plays) at the title's right. Icons only: a "Play" label
 * takes ~50dp more and wraps most titles to three lines.
 */
@Composable
fun CollectionTitle(
    kicker: String,
    title: String,
    playing: Boolean,
    // Null hides both buttons: an empty list has nothing to play.
    onPlay: (() -> Unit)?,
    onShuffle: (() -> Unit)?,
) {
    Text(
        text = kicker.uppercase(),
        style = typo().bodySmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    TitleRow(
        title = { CollectionTitleText(title) },
    ) {
        if (onPlay != null) {
            if (onShuffle != null) RoundIconButton(LimusicIcons.Shuffle, stringResource(Res.string.shuffle), onShuffle)
            RoundAccentButton(
                if (playing) LimusicIcons.Pause else LimusicIcons.PlayArrow,
                stringResource(if (playing) Res.string.pause else Res.string.play),
                onPlay,
            )
        }
    }
}

/** The big title with its round buttons at its right, centred on it. */
@Composable
fun TitleRow(
    title: @Composable () -> Unit,
    buttons: @Composable RowScope.() -> Unit,
) {
    Row(
        Modifier.padding(top = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f).padding(end = 4.dp)) { title() }
        buttons()
    }
}

@Composable
fun CollectionTitleText(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        style = typo().headlineLarge.copy(fontSize = 28.sp, lineHeight = 32.sp),
        color = MaterialTheme.colorScheme.onBackground,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

/** 52dp, borderless: Shuffle beside an album's Play, Radio beside an artist's Shuffle. */
@Composable
fun RoundIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .size(52.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription, Modifier.size(26.dp), MaterialTheme.colorScheme.onSurface)
    }
}

/** 52dp, filled with the accent: the page's main action (Play, an artist's Shuffle). */
@Composable
fun RoundAccentButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription, Modifier.size(28.dp), MaterialTheme.colorScheme.onPrimary)
    }
}

/**
 * Desktop's artist line under the title: the round picture and the name (tappable when [onClick]
 * is given), then the [meta] parts ("20 songs", "1 hour, 4 minutes"), blanks left out.
 */
@Composable
fun CollectionAuthorLine(
    name: String,
    thumbnail: String?,
    meta: List<String>,
    onClick: (() -> Unit)?,
) {
    Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                Modifier
                    .clip(RoundedCornerShape(50))
                    .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                    .padding(end = 6.dp),
        ) {
            if (thumbnail != null) {
                AsyncImage(
                    model = thumbnail,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(32.dp).clip(CircleShape),
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = name,
                style = typo().titleSmall.copy(fontSize = 14.sp),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
            )
        }
        Text(
            text = meta.filter { it.isNotBlank() }.joinToString(" • ", prefix = "• "),
            style = typo().bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
