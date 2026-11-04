package com.simohypers.limusic.ui.screen.player.content

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.simohypers.limusic.ui.theme.typo
import limusic.composeapp.generated.resources.Res
import limusic.composeapp.generated.resources.song
import limusic.composeapp.generated.resources.video
import org.jetbrains.compose.resources.stringResource

/**
 * Song | Video, YouTube Music's switch between a recording's two versions (desktop #442).
 * [isVideo] is the side picked; tapping the other plays it from the same moment, or on a video
 * with no song version, shows or hides its picture.
 */
@Composable
internal fun VersionSwitch(
    isVideo: Boolean,
    onSwitch: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    selectedColor: Color = MaterialTheme.colorScheme.primary,
    selectedContentColor: Color = MaterialTheme.colorScheme.onPrimary,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Row(modifier.clip(CircleShape).background(containerColor).padding(3.dp)) {
        for ((video, label) in listOf(false to Res.string.song, true to Res.string.video)) {
            val selected = video == isVideo
            val background by animateColorAsState(if (selected) selectedColor else Color.Transparent, label = "versionSide")
            Text(
                text = stringResource(label),
                style = typo().labelMedium,
                color = if (selected) selectedContentColor else contentColor,
                modifier =
                    Modifier
                        .clip(CircleShape)
                        .background(background)
                        .clickable(enabled = !selected, onClick = onSwitch)
                        .padding(horizontal = 16.dp, vertical = 6.dp),
            )
        }
    }
}
