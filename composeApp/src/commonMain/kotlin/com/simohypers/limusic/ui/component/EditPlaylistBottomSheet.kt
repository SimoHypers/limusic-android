package com.simohypers.limusic.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.simohypers.limusic.data.io.readLocalImageBytes
import com.simohypers.limusic.expect.ui.photoPickerResult
import com.simohypers.limusic.ui.theme.AppThemeContent
import com.simohypers.limusic.ui.theme.typo
import kotlinx.coroutines.launch
import multiplatform.network.cmptoast.ToastGravity
import multiplatform.network.cmptoast.showToast
import org.jetbrains.compose.resources.stringResource
import limusic.composeapp.generated.resources.Res
import limusic.composeapp.generated.resources.cancel
import limusic.composeapp.generated.resources.change_image
import limusic.composeapp.generated.resources.crop_cover
import limusic.composeapp.generated.resources.delete
import limusic.composeapp.generated.resources.delete_playlist_confirm
import limusic.composeapp.generated.resources.description
import limusic.composeapp.generated.resources.edit_playlist
import limusic.composeapp.generated.resources.playlist_name_cannot_be_empty
import limusic.composeapp.generated.resources.public_playlist
import limusic.composeapp.generated.resources.public_playlist_off
import limusic.composeapp.generated.resources.public_playlist_on
import limusic.composeapp.generated.resources.save
import limusic.composeapp.generated.resources.title

/**
 * Limusic desktop's Edit playlist dialog as a sheet: image, name, description, public switch.
 *
 * [description] or [privacy] null hides that row (a local playlist has neither). [onSave] gets
 * only what changed, null for the rest, and calls back with whether it worked so the button can
 * come back after a failure.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPlaylistBottomSheet(
    title: String,
    description: String?,
    privacy: String?,
    thumbnail: String?,
    onDismiss: () -> Unit,
    onSave: (
        title: String?,
        description: String?,
        privacy: String?,
        cover: ByteArray?,
        onResult: (Boolean) -> Unit,
    ) -> Unit,
) {
    AppThemeContent {
        val colors = rememberSurfaceDarkColors()
        val coroutineScope = rememberCoroutineScope()
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var newTitle by remember { mutableStateOf(title) }
        var newDescription by remember { mutableStateOf(description.orEmpty()) }
        var isPublic by remember { mutableStateOf(privacy == "PUBLIC") }
        var cover by remember { mutableStateOf<ByteArray?>(null) }
        var imageAwaitingCrop by remember { mutableStateOf<ByteArray?>(null) }
        var saving by remember { mutableStateOf(false) }

        val picker =
            photoPickerResult { uri ->
                uri?.let { coroutineScope.launch { imageAwaitingCrop = readLocalImageBytes(it) } }
            }
        imageAwaitingCrop?.let { bytes ->
            ImageCropperDialog(
                imageBytes = bytes,
                titleText = stringResource(Res.string.crop_cover),
                confirmText = stringResource(Res.string.save),
                cancelText = stringResource(Res.string.cancel),
                onDismiss = { imageAwaitingCrop = null },
                onCropped = {
                    imageAwaitingCrop = null
                    cover = it
                },
            )
        }

        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            containerColor = Color.Transparent,
            contentColor = Color.Transparent,
            dragHandle = null,
            scrimColor = Color.Black.copy(alpha = .5f),
            contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
        ) {
            Card(
                modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                colors = CardDefaults.cardColors().copy(containerColor = colors.container),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().imePadding().padding(horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Spacer(modifier = Modifier.height(5.dp))
                    Card(
                        modifier = Modifier.width(60.dp).height(4.dp),
                        colors = CardDefaults.cardColors().copy(containerColor = colors.handle),
                        shape = RoundedCornerShape(50),
                    ) {}
                    Text(
                        text = stringResource(Res.string.edit_playlist),
                        style = typo().titleMedium,
                        color = colors.content,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        AsyncImage(
                            model = cover ?: thumbnail,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier =
                                Modifier
                                    .size(96.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { picker.launch() },
                        )
                        TextButton(onClick = { picker.launch() }) {
                            Text(text = stringResource(Res.string.change_image))
                        }
                    }
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text(text = stringResource(Res.string.title)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (description != null) {
                        OutlinedTextField(
                            value = newDescription,
                            onValueChange = { newDescription = it },
                            label = { Text(text = stringResource(Res.string.description)) },
                            minLines = 2,
                            maxLines = 5,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    if (privacy != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(Res.string.public_playlist),
                                    style = typo().bodyMedium,
                                    color = colors.content,
                                )
                                Text(
                                    text =
                                        stringResource(
                                            if (isPublic) Res.string.public_playlist_on else Res.string.public_playlist_off,
                                        ),
                                    style = typo().bodySmall,
                                    color = colors.subtitle,
                                )
                            }
                            Switch(checked = isPublic, onCheckedChange = { isPublic = it })
                        }
                    }
                    val emptyName = stringResource(Res.string.playlist_name_cannot_be_empty)
                    Button(
                        enabled = !saving,
                        onClick = {
                            if (newTitle.isBlank()) {
                                showToast(emptyName, ToastGravity.Bottom)
                                return@Button
                            }
                            // An UNLISTED playlist reads as "not public"; only a flip of the switch
                            // sends a privacy, so leaving it alone keeps it unlisted.
                            val newPrivacy =
                                when {
                                    privacy == null || isPublic == (privacy == "PUBLIC") -> null
                                    isPublic -> "PUBLIC"
                                    else -> "PRIVATE"
                                }
                            saving = true
                            onSave(
                                newTitle.trim().takeIf { it != title },
                                newDescription.trim().takeIf { description != null && it != description },
                                newPrivacy,
                                cover,
                            ) { ok -> if (!ok) saving = false }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(text = stringResource(Res.string.save))
                    }
                    EndOfModalBottomSheet()
                }
            }
        }
    }
}

/** The confirm before a playlist is deleted. [message] says where from (account or device). */
@Composable
fun DeletePlaylistDialog(
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = rememberSurfaceDarkColors()
    AlertDialog(
        containerColor = colors.container,
        titleContentColor = colors.content,
        textContentColor = colors.content,
        title = { Text(text = stringResource(Res.string.delete_playlist_confirm)) },
        text = { Text(text = message) },
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = stringResource(Res.string.delete), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(Res.string.cancel))
            }
        },
    )
}
