package com.simohypers.limusic.ui.component

import com.simohypers.limusic.ui.theme.rounded
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.simohypers.limusic.domain.utils.PlaylistSort
import com.simohypers.limusic.ui.theme.typo
import com.simohypers.limusic.viewModel.PLAYLIST_SORTS
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import limusic.composeapp.generated.resources.Res
import limusic.composeapp.generated.resources.album
import limusic.composeapp.generated.resources.sort_artist
import limusic.composeapp.generated.resources.sort_by
import limusic.composeapp.generated.resources.sort_default
import limusic.composeapp.generated.resources.sort_newest
import limusic.composeapp.generated.resources.sort_oldest
import limusic.composeapp.generated.resources.sort_plays
import limusic.composeapp.generated.resources.sort_reverse
import limusic.composeapp.generated.resources.sort_top
import limusic.composeapp.generated.resources.title

/**
 * Desktop's playlist Sort menu and direction toggle as one sheet. A pick applies and closes it;
 * Reverse applies and leaves it open, since it is usually flipped right after a pick.
 */
@Composable
fun PlaylistSortSheet(
    sort: PlaylistSort,
    desc: Boolean,
    onSort: (PlaylistSort) -> Unit,
    onReverse: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    options: List<PlaylistSort> = PLAYLIST_SORTS,
    // What Default means on this list ("Recently downloaded"); a playlist calls it Default.
    defaultLabel: StringResource = Res.string.sort_default,
) {
    SettingSheet(title = stringResource(Res.string.sort_by), onDismiss = onDismiss) { colors ->
        Column {
            options.forEach { option ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(rounded(12.dp))
                        .selectable(selected = option == sort, role = Role.RadioButton) { onSort(option) },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = option == sort, onClick = null, modifier = Modifier.padding(12.dp))
                    Text(stringResource(if (option == PlaylistSort.DEFAULT) defaultLabel else option.label()), style = typo().bodyMedium, color = colors.content)
                }
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .clip(rounded(12.dp))
                .toggleable(value = desc, role = Role.Switch, onValueChange = onReverse)
                .padding(start = 12.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(Res.string.sort_reverse), style = typo().bodyMedium, color = colors.content, modifier = Modifier.weight(1f))
            Switch(checked = desc, onCheckedChange = null)
        }
    }
}

private fun PlaylistSort.label() =
    when (this) {
        PlaylistSort.DEFAULT -> Res.string.sort_default
        PlaylistSort.NEWEST -> Res.string.sort_newest
        PlaylistSort.OLDEST -> Res.string.sort_oldest
        PlaylistSort.TITLE -> Res.string.title
        PlaylistSort.ARTIST -> Res.string.sort_artist
        PlaylistSort.ALBUM -> Res.string.album
        PlaylistSort.TOP -> Res.string.sort_top
        PlaylistSort.PLAYS -> Res.string.sort_plays
    }
