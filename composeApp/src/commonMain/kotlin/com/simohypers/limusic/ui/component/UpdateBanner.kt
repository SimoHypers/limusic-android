package com.simohypers.limusic.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.simohypers.limusic.ui.icon.Close
import com.simohypers.limusic.ui.icon.LimusicIcons
import com.simohypers.limusic.ui.theme.rounded
import com.simohypers.limusic.ui.theme.typo
import com.simohypers.limusic.updater.Updater
import limusic.composeapp.generated.resources.Res
import limusic.composeapp.generated.resources.close
import limusic.composeapp.generated.resources.update_available
import limusic.composeapp.generated.resources.update_closes_to_install
import limusic.composeapp.generated.resources.update_downloading
import limusic.composeapp.generated.resources.update_install
import limusic.composeapp.generated.resources.update_installing
import org.jetbrains.compose.resources.stringResource

/** Desktop's update banner: "Version X is available" with Update and ✕, progress in place. */
@Composable
fun UpdateBanner(modifier: Modifier = Modifier) {
    val state by Updater.state.collectAsStateWithLifecycle()
    val update = state.available
    AnimatedVisibility(update != null, modifier, enter = fadeIn() + slideInVertically { it / 2 }, exit = fadeOut()) {
        if (update == null) return@AnimatedVisibility
        val progress = state.progress
        Column(
            Modifier
                .fillMaxWidth()
                .shadow(6.dp, rounded(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest, rounded(12.dp))
                .padding(start = 16.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(
                            when {
                                progress == null -> Res.string.update_available
                                progress < 1f -> Res.string.update_downloading
                                else -> Res.string.update_installing
                            },
                            update.version,
                        ),
                        style = typo().labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        if (progress != null && progress < 1f) {
                            "${(progress * 100).toInt()}%"
                        } else {
                            stringResource(Res.string.update_closes_to_install)
                        },
                        style = typo().bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (progress == null) {
                    Button(
                        onClick = Updater::install,
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        modifier = Modifier.height(36.dp),
                    ) {
                        Text(stringResource(Res.string.update_install), style = typo().labelLarge)
                    }
                    IconButton(onClick = Updater::dismiss) {
                        Icon(
                            LimusicIcons.Close,
                            contentDescription = stringResource(Res.string.close),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
            if (progress != null) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp, end = 12.dp),
                )
            }
        }
    }
}
