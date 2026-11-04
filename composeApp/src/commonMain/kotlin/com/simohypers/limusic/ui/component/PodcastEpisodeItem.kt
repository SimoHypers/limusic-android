package com.simohypers.limusic.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.simohypers.limusic.domain.data.model.podcast.PodcastBrowse
import com.simohypers.limusic.ui.icon.LimusicIcons
import com.simohypers.limusic.ui.icon.MoreVert
import com.simohypers.limusic.ui.theme.rounded
import com.simohypers.limusic.ui.theme.typo

/**
 * One episode of a podcast page: its thumbnail in its own shape, the title on up to two lines over
 * "date • duration", and two lines of the episode's description across the row's full width.
 */
@Composable
fun PodcastEpisodeFullWidthItem(
    modifier: Modifier = Modifier,
    episode: PodcastBrowse.EpisodeItem,
    playing: Boolean = false,
    onClick: (String) -> Unit,
    onMoreClickListener: (() -> Unit)? = null,
) {
    val thumb = episode.thumbnail.lastOrNull()
    // Square podcast art and 16:9 video episodes both keep their shape, within reason.
    val ratio = thumb?.let { if (it.height > 0) it.width.toFloat() / it.height else null }?.coerceIn(1f, 16f / 9f) ?: 1f
    Column(
        modifier
            .fillMaxWidth()
            .clickable { onClick(episode.videoId) }
            .padding(start = 16.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = thumb?.url,
                placeholder = rememberHolderPainter(),
                error = rememberHolderPainter(),
                fallback = rememberHolderPainter(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.height(56.dp).aspectRatio(ratio).clip(rounded(10.dp)),
            )
            Column(Modifier.weight(1f).padding(start = 12.dp, end = 4.dp)) {
                Text(
                    text = episode.title,
                    style = typo().labelMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = if (playing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                )
                val meta = listOfNotNull(episode.createdDay, episode.durationString).filter { it.isNotBlank() }
                if (meta.isNotEmpty()) {
                    Text(
                        text = meta.joinToString(" • "),
                        style = typo().bodySmall,
                        maxLines = 1,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
            if (onMoreClickListener != null) {
                RippleIconButton(imageVector = LimusicIcons.MoreVert, fillMaxSize = false, tint = MaterialTheme.colorScheme.onSurface) {
                    onMoreClickListener.invoke()
                }
            }
        }
        episode.description?.takeIf { it.isNotBlank() }?.let {
            Text(
                text = it,
                style = typo().bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, end = 12.dp),
            )
        }
    }
}
