package com.simohypers.limusic.ui.component

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.simohypers.limusic.domain.data.model.download.CollectionDownload
import com.simohypers.limusic.ui.icon.DownloadForOffline
import com.simohypers.limusic.ui.icon.LimusicIcons
import limusic.composeapp.generated.resources.Res
import limusic.composeapp.generated.resources.baseline_downloaded
import limusic.composeapp.generated.resources.download
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * The round download button on a playlist or album page. Downloaded means asked for: while songs
 * are still coming it shows how far they got; tapping it then opens [CollectionDownloadSheet].
 * [rereadsList]: Download missing reads the list again (a YouTube playlist, which can gain songs
 * after it was downloaded), so it is offered whenever nothing is downloading.
 */
@Composable
fun CollectionDownloadButton(
    download: CollectionDownload,
    title: String,
    thumbnail: String?,
    onDownload: () -> Unit,
    onRemove: () -> Unit,
    onDownloadMissing: () -> Unit,
    rereadsList: Boolean = false,
) {
    var sheet by remember { mutableStateOf(false) }
    val ink = MaterialTheme.colorScheme.onSurface
    // Borderless: it sits in the pill on the cover beside like and ⋮.
    Box(
        Modifier.size(40.dp).clip(CircleShape).clickable { if (download.kept) sheet = true else onDownload() },
        contentAlignment = Alignment.Center,
    ) {
        Crossfade(targetState = !download.kept to (download.active > 0)) { (off, active) ->
            when {
                off -> Icon(LimusicIcons.DownloadForOffline, stringResource(Res.string.download), Modifier.size(22.dp), ink)
                active ->
                    CircularProgressIndicator(
                        progress = { download.downloaded.toFloat() / (download.total - download.refused).coerceAtLeast(1) },
                        modifier = Modifier.size(24.dp),
                        color = ink,
                        trackColor = ink.copy(alpha = 0.24f),
                        strokeWidth = 3.dp,
                    )
                else -> Icon(painterResource(Res.drawable.baseline_downloaded), null, Modifier.size(22.dp), Color(0xFF00A0CB))
            }
        }
    }
    if (sheet) {
        CollectionDownloadSheet(
            title = title,
            thumbnail = thumbnail,
            download = download,
            rereadsList = rereadsList,
            onDownloadMissing = onDownloadMissing,
            onRemove = onRemove,
            onDismiss = { sheet = false },
        )
    }
}
