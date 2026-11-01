package com.simohypers.limusic.expect.ui

import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.mp.KoinPlatform.getKoin
import java.io.File

@OptIn(ExperimentalTextApi::class)
actual fun fileFontFamily(path: String): FontFamily? {
    val file = File(path).takeIf { it.isFile } ?: return null
    return FontFamily(
        listOf(FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold).map { weight ->
            Font(file, weight, variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)))
        },
    )
}

actual suspend fun importFontFile(uri: String): String? =
    withContext(Dispatchers.IO) {
        val context: Context = getKoin().get()
        val source = Uri.parse(uri)
        val name =
            context.contentResolver
                .query(source, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { if (it.moveToFirst()) it.getString(0) else null }
                ?: "font.ttf"
        val file = File(File(context.filesDir, "fonts").apply { mkdirs() }, name.replace(Regex("[^\\w .-]"), "_"))
        runCatching {
            context.contentResolver.openInputStream(source)!!.use { input -> file.outputStream().use { input.copyTo(it) } }
        }.onFailure { return@withContext null }
        // Checked here, once: a file Android can't read would otherwise fail the first time text lays out.
        if (runCatching { Typeface.Builder(file).build() }.getOrNull() == null) {
            file.delete()
            return@withContext null
        }
        file.path
    }

actual fun deleteFontFile(path: String) {
    File(path).delete()
}
