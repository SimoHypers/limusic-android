package com.maxrave.simpmusic.ui.component

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.maxrave.domain.data.model.download.CollectionDownload
import com.maxrave.simpmusic.ui.icon.DownloadForOffline
import com.maxrave.simpmusic.ui.icon.SimpIcons
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.baseline_downloaded
import simpmusic.composeapp.generated.resources.cancel
import simpmusic.composeapp.generated.resources.download
import simpmusic.composeapp.generated.resources.download_kept_note
import simpmusic.composeapp.generated.resources.download_missing
import simpmusic.composeapp.generated.resources.download_progress_text
import simpmusic.composeapp.generated.resources.download_refused
import simpmusic.composeapp.generated.resources.remove
import simpmusic.composeapp.generated.resources.remove_download_confirm

/**
 * The round download button on a playlist or album page. Downloaded means asked for: while songs
 * are still coming it shows how far they got; tapping it then asks before removing the download.
 */
@Composable
fun CollectionDownloadButton(
    download: CollectionDownload,
    onDownload: () -> Unit,
    onRemove: () -> Unit,
    onDownloadMissing: () -> Unit,
) {
    var confirm by remember { mutableStateOf(false) }
    Box(
        modifier =
            Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.12f))
                .clickable { if (download.kept) confirm = true else onDownload() },
        contentAlignment = Alignment.Center,
    ) {
        Crossfade(targetState = !download.kept to (download.active > 0)) { (off, active) ->
            when {
                off -> Icon(SimpIcons.DownloadForOffline, stringResource(Res.string.download), Modifier.size(22.dp), Color.White)
                active ->
                    CircularProgressIndicator(
                        progress = { if (download.total == 0) 0f else download.downloaded.toFloat() / download.total },
                        modifier = Modifier.size(26.dp),
                        color = Color.White,
                        trackColor = Color.White.copy(alpha = 0.24f),
                        strokeWidth = 3.dp,
                    )
                else -> Icon(painterResource(Res.drawable.baseline_downloaded), null, Modifier.size(22.dp), Color(0xFF00A0CB))
            }
        }
    }
    if (confirm) {
        val colors = rememberSurfaceDarkColors()
        val missing = download.downloaded + download.refused < download.total && download.active == 0
        AlertDialog(
            containerColor = colors.container,
            titleContentColor = colors.content,
            textContentColor = colors.content,
            title = { Text(stringResource(Res.string.remove_download_confirm)) },
            text = {
                Text(
                    listOfNotNull(
                        stringResource(Res.string.download_progress_text, download.downloaded, download.total),
                        if (download.refused > 0) pluralStringResource(Res.plurals.download_refused, download.refused, download.refused) else null,
                        stringResource(Res.string.download_kept_note),
                    ).joinToString(" "),
                )
            },
            onDismissRequest = { confirm = false },
            confirmButton = {
                TextButton(onClick = {
                    confirm = false
                    onRemove()
                }) { Text(stringResource(Res.string.remove), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                if (missing) {
                    TextButton(onClick = {
                        confirm = false
                        onDownloadMissing()
                    }) { Text(stringResource(Res.string.download_missing)) }
                }
                TextButton(onClick = { confirm = false }) { Text(stringResource(Res.string.cancel)) }
            },
        )
    }
}
