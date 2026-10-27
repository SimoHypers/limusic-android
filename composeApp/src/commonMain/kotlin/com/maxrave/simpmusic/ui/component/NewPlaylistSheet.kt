package com.maxrave.simpmusic.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.maxrave.simpmusic.ui.icon.Cloud
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.icon.Smartphone
import com.maxrave.simpmusic.ui.theme.typo
import org.jetbrains.compose.resources.stringResource
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.create
import simpmusic.composeapp.generated.resources.new_playlist
import simpmusic.composeapp.generated.resources.new_playlist_device_only
import simpmusic.composeapp.generated.resources.new_playlist_where_device
import simpmusic.composeapp.generated.resources.new_playlist_where_device_desc
import simpmusic.composeapp.generated.resources.new_playlist_where_youtube
import simpmusic.composeapp.generated.resources.new_playlist_where_youtube_desc
import simpmusic.composeapp.generated.resources.playlist_name

/**
 * Desktop's New playlist: a name, and where it lives. Signed in that is the YouTube Music account
 * or this phone, starting from [onYouTube] (the last pick); signed out it can only be this phone,
 * and so it is wherever [canPickYouTube] is false (a picker that only adds to phone playlists).
 * [onCreate] calls back with whether it worked, so the button comes back after a failure.
 */
@Composable
fun NewPlaylistSheet(
    signedIn: Boolean,
    onYouTube: Boolean,
    canPickYouTube: Boolean = signedIn,
    onDismiss: () -> Unit,
    onCreate: (title: String, onYouTube: Boolean, onResult: (Boolean) -> Unit) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var youTube by remember(onYouTube) { mutableStateOf(onYouTube) }
    var creating by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    SettingSheet(title = stringResource(Res.string.new_playlist), onDismiss = onDismiss) { colors ->
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text(stringResource(Res.string.playlist_name)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().focusRequester(focus),
        )
        LaunchedEffect(Unit) { focus.requestFocus() }
        if (canPickYouTube) {
            WhereOption(
                icon = SimpIcons.Cloud,
                title = stringResource(Res.string.new_playlist_where_youtube),
                description = stringResource(Res.string.new_playlist_where_youtube_desc),
                selected = youTube,
                colors = colors,
            ) { youTube = true }
            WhereOption(
                icon = SimpIcons.Smartphone,
                title = stringResource(Res.string.new_playlist_where_device),
                description = stringResource(Res.string.new_playlist_where_device_desc),
                selected = !youTube,
                colors = colors,
            ) { youTube = false }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(SimpIcons.Smartphone, null, tint = colors.subtitle, modifier = Modifier.size(16.dp))
                Text(
                    stringResource(if (signedIn) Res.string.new_playlist_where_device_desc else Res.string.new_playlist_device_only),
                    style = typo().bodySmall,
                    color = colors.subtitle,
                )
            }
        }
        Button(
            enabled = !creating && title.isNotBlank(),
            onClick = {
                creating = true
                onCreate(title.trim(), canPickYouTube && youTube) { ok -> if (!ok) creating = false }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(Res.string.create))
        }
    }
}

@Composable
private fun WhereOption(
    icon: ImageVector,
    title: String,
    description: String,
    selected: Boolean,
    colors: SurfaceDarkColors,
    onClick: () -> Unit,
) {
    val accent = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .border(1.dp, if (selected) accent else colors.disabled, shape)
                .background(if (selected) accent.copy(alpha = 0.08f) else Color.Transparent)
                .clickable(onClick = onClick)
                .padding(end = 12.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Icon(icon, null, tint = colors.subtitle, modifier = Modifier.size(22.dp))
        Column(Modifier.weight(1f).padding(start = 4.dp)) {
            Text(title, style = typo().bodyMedium, color = colors.content)
            Text(description, style = typo().bodySmall, color = colors.subtitle)
        }
    }
}
