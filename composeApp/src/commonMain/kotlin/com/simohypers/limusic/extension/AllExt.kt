package com.simohypers.limusic.extension

import androidx.compose.runtime.Composable
import com.simohypers.limusic.domain.data.model.browse.artist.ArtistBrowse
import com.simohypers.limusic.domain.data.model.streams.TimeLine
import com.simohypers.limusic.domain.extension.now
import com.simohypers.limusic.domain.utils.toTrack
import kotlin.math.roundToLong
import com.simohypers.limusic.viewModel.ArtistScreenData
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.periodUntil
import kotlinx.datetime.toInstant
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import limusic.composeapp.generated.resources.Res
import limusic.composeapp.generated.resources.days_ago
import limusic.composeapp.generated.resources.hours_ago
import limusic.composeapp.generated.resources.months_ago
import limusic.composeapp.generated.resources.live_badge
import limusic.composeapp.generated.resources.na_na
import limusic.composeapp.generated.resources.recently
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.time.ExperimentalTime

fun String?.removeDuplicateWords(): String {
    if (this == null) {
        return "null"
    } else {
        val regex = Regex("\\b(\\w+)\\b\\s*(?=.*\\b\\1\\b)")
        return this.replace(regex, "")
    }
}

fun <T> Iterable<T>.indexMap(): Map<T, Int> {
    val map = mutableMapOf<T, Int>()
    forEachIndexed { i, v ->
        map[v] = i
    }
    return map
}

/**
 * Whether a release from an artist's discography page is worth announcing. An unseen id is not
 * enough on its own, because YouTube re-keys old releases and pages them in and out, so only
 * releases dated this year count — plus last year's during January, so one that came out in
 * the last days of December is not lost.
 */
fun isNewRelease(
    browseId: String,
    year: String,
    known: Set<String>,
    today: LocalDate = now().date,
): Boolean {
    if (browseId in known) return false
    val released = year.toIntOrNull() ?: return false
    return released == today.year || (today.month == Month.JANUARY && released == today.year - 1)
}

@OptIn(ExperimentalTime::class)
@Composable
fun LocalDateTime.formatTimeAgo(): String {
    val now = now()
    val duration =
        this
            .toInstant(TimeZone.currentSystemDefault())
            .periodUntil(now.toInstant(TimeZone.currentSystemDefault()), TimeZone.currentSystemDefault())

    val monthsDiff = duration.months + (duration.years * 12)
    val daysDiff = duration.days

    // For hours, we need to calculate manually since Period doesn't include hours
    val thisInstant = this.toInstant(TimeZone.currentSystemDefault())
    val nowInstant = now.toInstant(TimeZone.currentSystemDefault())
    val hoursDiff = (nowInstant - thisInstant).inWholeHours

    return when {
        monthsDiff >= 1 -> pluralStringResource(Res.plurals.months_ago, monthsDiff, monthsDiff)
        daysDiff >= 1 -> pluralStringResource(Res.plurals.days_ago, daysDiff, daysDiff)
        hoursDiff >= 2 -> pluralStringResource(Res.plurals.hours_ago, hoursDiff.toInt(), hoursDiff.toInt())
        else -> stringResource(Res.string.recently)
    }
}

/**
 * The elapsed-time label under a seek bar sitting at [progress] (0..1) of the track. Blank for a live
 * broadcast: where playback sits in the broadcast's seek window means nothing to a listener.
 */
@Composable
fun TimeLine.elapsedLabel(progress: Float): String = if (isLive) "" else formatDuration((total * progress).roundToLong())

/** The track's length under a seek bar, or LIVE for a live broadcast, which has none. */
@Composable
fun TimeLine.lengthLabel(): String = if (isLive) stringResource(Res.string.live_badge) else formatDuration(total)

@Composable
fun formatDuration(duration: Long): String {
    if (duration < 0L) return stringResource(Res.string.na_na)
    val minutes: Long = TimeUnit.MINUTES.convert(duration, TimeUnit.MILLISECONDS)
    val seconds: Long = (
        TimeUnit.SECONDS.convert(duration, TimeUnit.MILLISECONDS) -
            minutes * TimeUnit.SECONDS.convert(1, TimeUnit.MINUTES)
    )
    return String.format(Locale.ENGLISH, "%02d:%02d", minutes, seconds)
}

fun parseTimestampToMilliseconds(timestamp: String): Double {
    val parts = timestamp.split(":")
    val totalSeconds =
        when (parts.size) {
            2 -> {
                try {
                    val minutes = parts[0].toDouble()
                    val seconds = parts[1].toDouble()
                    (minutes * 60 + seconds)
                } catch (e: NumberFormatException) {
                    // Handle parsing error
                    e.printStackTrace()
                    return 0.0
                }
            }

            3 -> {
                try {
                    val hours = parts[0].toDouble()
                    val minutes = parts[1].toDouble()
                    val seconds = parts[2].toDouble()
                    (hours * 3600 + minutes * 60 + seconds)
                } catch (e: NumberFormatException) {
                    // Handle parsing error
                    e.printStackTrace()
                    return 0.0
                }
            }

            else -> {
                // Handle incorrect format
                return 0.0
            }
        }
    return totalSeconds * 1000
}

fun InputStream.zipInputStream(): ZipInputStream = ZipInputStream(this)

fun OutputStream.zipOutputStream(): ZipOutputStream = ZipOutputStream(this)

fun Long?.bytesToMB(): Long {
    val mbInBytes = 1024 * 1024
    return this?.div(mbInBytes) ?: 0L
}

fun getSizeOfFile(dir: File): Long {
    var dirSize: Long = 0
    if (!dir.listFiles().isNullOrEmpty()) {
        for (f in dir.listFiles()!!) {
            dirSize += f.length()
            if (f.isDirectory) {
                dirSize += getSizeOfFile(f)
            }
        }
    }
    return dirSize
}

fun ArtistBrowse.toArtistScreenData(): ArtistScreenData =
    ArtistScreenData(
        title = this.name,
        imageUrl = this.thumbnails?.lastOrNull()?.url,
        subscribers = this.subscribers,
        playCount = this.views,
        isChannel = this.songs == null,
        channelId = this.channelId,
        subscribeChannelId = this.subscribeChannelId,
        radioParam = this.radioId,
        shuffleParam = this.shuffleId,
        description = this.description,
        listSongParam = this.songs?.browseId,
        popularSongs = this.songs?.results?.map { it.toTrack() } ?: emptyList(),
        singles = this.singles,
        albums = this.albums,
        video =
            this.video?.let { video ->
                ArtistBrowse.Videos(video.map { it.toTrack() }, this.videoList)
            },
        related = this.related,
        featuredOn = this.featuredOn ?: emptyList(),
    )

/**
 * Clamp a YouTube image URL's `w`/`h` size spec to the smaller dimension so it's square
 * (`...=w2880-h1200-...` -> `...=w1200-h1200-...`). Only the size spec after the last `=` is
 * touched. Returns the url unchanged when it has no `w`/`h` spec or is already square.
 */
/** A YouTube image URL asked for at [px] square, so a small stored thumbnail doesn't blur. */
fun String.sizedThumbnail(px: Int) = replace(Regex("=w\\d+-h\\d+"), "=w$px-h$px")

fun String.toSquareThumbnailUrl(): String {
    val eq = lastIndexOf('=')
    if (eq < 0 || eq == lastIndex) return this
    val base = substring(0, eq + 1)
    val spec = substring(eq + 1)
    val width = Regex("(?:^|-)w(\\d+)").find(spec)?.groupValues?.get(1)?.toIntOrNull()
    val height = Regex("(?:^|-)h(\\d+)").find(spec)?.groupValues?.get(1)?.toIntOrNull()
    if (width == null || height == null || width == height) return this
    val min = minOf(width, height)
    return base +
        spec.replace(Regex("(^|-)([wh])\\d+")) { m -> "${m.groupValues[1]}${m.groupValues[2]}$min" }
}

fun isValidProxyHost(host: String): Boolean {
    // Regular expression to validate proxy host (without port)
    val proxyHostRegex =
        Regex(
            pattern = "^(?!-)[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*(?<!-)\$",
            options = setOf(RegexOption.IGNORE_CASE),
        )

    // Return true if the host matches the regex or is an IP address
    return proxyHostRegex.matches(host) || isIPAddress(host)
}

private fun isIPAddress(host: String): Boolean {
    // Check if the host is an IPv4 address
    val ipv4Regex =
        Regex(
            pattern = "^([0-9]{1,3}\\.){3}[0-9]{1,3}\$",
        )
    if (ipv4Regex.matches(host)) {
        return host.split('.').all { it.toInt() in 0..255 }
    }

    // Check if the host is an IPv6 address
    val ipv6Regex =
        Regex(
            pattern = "^[0-9a-fA-F:]+$",
        )
    return ipv6Regex.matches(host)
}

fun String.isTwoLetterCode(): Boolean {
    val regex = "^[A-Za-z]{2}$".toRegex()
    return regex.matches(this)
}

@Composable
fun String?.ifNullOrEmpty(defaultValue: @Composable () -> String): String = if (isNullOrEmpty()) defaultValue() else this
