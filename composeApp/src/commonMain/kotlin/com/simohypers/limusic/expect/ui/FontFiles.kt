package com.simohypers.limusic.expect.ui

import androidx.compose.ui.text.font.FontFamily

/** A font file the user loaded, as a family; null once the file is gone. */
expect fun fileFontFamily(path: String): FontFamily?

/**
 * Copies a picked font ([uri], from filePickerResult) into the app's own storage and returns the
 * copy's path, or null when the system can't read it as a font. A file with the same name replaces
 * the earlier copy.
 */
expect suspend fun importFontFile(uri: String): String?

expect fun deleteFontFile(path: String)
