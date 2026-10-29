package com.maxrave.simpmusic.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.maxrave.simpmusic.ui.theme.typo

/** The code's part of an invite, as the boxes show it. */
fun codeOf(
    invite: String,
    length: Int,
) = invite.substringBefore('@').uppercase().filter { it.isLetterOrDigit() }.take(length)

/**
 * One box per character of a code: a Listen Together room (8) or a device-link pairing code (6).
 * Typing keeps letters and digits; a pasted desktop invite (`CODE@host`) is kept whole so its
 * server still applies, with the boxes showing its code.
 */
@Composable
fun CodeInput(
    invite: String,
    length: Int,
    onInviteChange: (String) -> Unit,
) {
    val code = codeOf(invite, length)
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    // TextFieldValue, not String: a plain String re-places the caret at index 0 on every
    // externally-driven recomposition, and backspace at index 0 deletes nothing.
    val fieldValue = TextFieldValue(text = code, selection = TextRange(code.length))

    BasicTextField(
        value = fieldValue,
        onValueChange = { onInviteChange(if ('@' in it.text) it.text.trim() else codeOf(it.text, length)) },
        singleLine = true,
        textStyle = LocalTextStyle.current.copy(color = Color.Transparent),
        cursorBrush = SolidColor(Color.Transparent),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, autoCorrectEnabled = false),
        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
        decorationBox = { innerTextField ->
            // Zero-sized but composed: without it nothing holds the cursor and the field takes no input.
            Box(Modifier.size(0.dp)) { innerTextField() }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                repeat(length) { index ->
                    // The caret is drawn by hand because the field that owns it is zero-sized.
                    val isCaret = index == code.length
                    Box(
                        modifier =
                            Modifier
                                .weight(1f)
                                .height(52.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f))
                                .then(
                                    if (isCaret) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(14.dp)) else Modifier,
                                ).clickable {
                                    // The field keeps its focus when the keyboard is closed with
                                    // back, so focusing again shows nothing: ask for the keyboard.
                                    focusRequester.requestFocus()
                                    keyboard?.show()
                                },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = code.getOrNull(index)?.toString().orEmpty(),
                            style = typo().titleSmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                }
            }
        },
    )
}
