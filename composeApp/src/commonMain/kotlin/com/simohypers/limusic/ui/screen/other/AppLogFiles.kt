package com.simohypers.limusic.ui.screen.other

import com.simohypers.limusic.domain.extension.epochMillisToLocalDateTime
import com.simohypers.limusic.domain.extension.now
import com.simohypers.limusic.logger.Logger
import com.simohypers.limusic.expect.shareImage
import com.simohypers.limusic.deviceSummary
import com.simohypers.limusic.domain.manager.DataStoreManager
import com.simohypers.limusic.utils.VersionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.number
import java.io.File
import java.io.RandomAccessFile
import kotlin.time.Clock
import kotlin.time.Instant
import org.koin.mp.KoinPlatform.getKoin

// Reading back the files Logger.enableFileLogging writes: parsing, tailing and exporting.
// The screen that shows them is AppLogScreen.

/** One entry of the on-disk log: its head line, plus whatever followed it (a stack trace). */
internal data class LogLine(
    val id: Long,
    val epochMillis: Long,
    val level: Char,
    val tag: String,
    val message: String,
    val detail: String,
) {
    val isError get() = level == 'E' || level == 'A'

    fun matches(query: String): Boolean =
        query.isBlank() || tag.contains(query, ignoreCase = true) || message.contains(query, ignoreCase = true)

    fun render(): String =
        buildString {
            append(clockTime(epochMillis)).append(' ').append(level).append('/').append(tag).append(": ").append(message)
            if (detail.isNotEmpty()) append('\n').append(detail)
        }
}

internal fun levelRank(level: Char): Int = "VDIWEA".indexOf(level)

/**
 * Follows one rolling log ([Logger.logFiles]) the way `tail -f` follows a file: the first call
 * reads every file of it, later calls only what was appended to the live file since. Not
 * thread-safe; one caller at a time.
 */
internal class LogTail(
    private val name: String,
    private val keep: Int,
) {
    private var liveOffset = -1L
    private var nextId = 0L

    var lines: List<LogLine> = emptyList()
        private set

    /** Forgets what was read; the next [poll] takes every file again from the top. */
    fun reset() {
        liveOffset = -1L
        lines = emptyList()
    }

    /** True when [lines] changed. */
    fun poll(): Boolean {
        val files = Logger.logFiles(name).map(::File)
        val live = files.lastOrNull() ?: return false
        val size = live.length()
        return when {
            // First read, or the live file rolled over and started again from empty.
            liveOffset < 0 || size < liveOffset -> {
                reload(files)
                true
            }

            size == liveOffset -> false

            else -> {
                val (text, end) = readCompleteLines(live, liveOffset)
                liveOffset = end
                if (text.isEmpty()) return false
                append(text)
                true
            }
        }
    }

    private fun reload(files: List<File>) {
        nextId = 0
        val text = StringBuilder()
        files.dropLast(1).filter(File::exists).forEach { text.append(it.readText()) }
        val (liveText, end) = readCompleteLines(files.last(), 0)
        text.append(liveText)
        liveOffset = end
        lines = emptyList()
        append(text.toString())
    }

    private fun append(text: String) {
        val parsed = parseLog(text, nextId)
        nextId += parsed.lines.size
        var current = lines
        // A read can end between a head line and its stack trace; the rest belongs to the last entry.
        if (parsed.leading.isNotEmpty() && current.isNotEmpty()) {
            val last = current.last()
            val detail = listOf(last.detail, parsed.leading).filter(String::isNotEmpty).joinToString("\n")
            current = current.dropLast(1) + last.copy(detail = detail)
        }
        lines = (current + parsed.lines).takeLast(keep)
    }
}

/** Everything from [from] to the last complete line, and the offset just past it. */
private fun readCompleteLines(
    file: File,
    from: Long,
): Pair<String, Long> {
    if (!file.exists()) return "" to 0L
    RandomAccessFile(file, "r").use { raf ->
        val length = raf.length()
        if (length <= from) return "" to from
        val bytes = ByteArray((length - from).toInt())
        raf.seek(from)
        raf.readFully(bytes)
        val end = bytes.lastIndexOf('\n'.code.toByte())
        if (end < 0) return "" to from
        return bytes.decodeToString(0, end + 1) to from + end + 1
    }
}

private class ParsedLog(
    val leading: String,
    val lines: List<LogLine>,
)

// "2026-10-06T07:02:31.802Z I/Tag: message" — RollingFileLogWriter's timestamp, then the
// "L/Tag: message" that Logger's LogLineFormatter writes.
private val HEAD_LINE = Regex("""^(\S+) ([VDIWEA])/([^:]*): (.*)$""")

private fun parseLog(
    text: String,
    firstId: Long,
): ParsedLog {
    val leading = StringBuilder()
    val lines = ArrayList<LogLine>()
    var head: MatchResult? = null
    var time = 0L
    val detail = StringBuilder()

    fun close() {
        val match = head ?: return
        lines +=
            LogLine(
                id = firstId + lines.size,
                epochMillis = time,
                level = match.groupValues[2].first(),
                tag = match.groupValues[3],
                message = match.groupValues[4],
                detail = detail.toString().trimEnd(),
            )
        detail.clear()
    }

    for (raw in text.lineSequence()) {
        val match = HEAD_LINE.matchEntire(raw)
        val millis = match?.let { runCatching { Instant.parse(it.groupValues[1]).toEpochMilliseconds() }.getOrNull() }
        if (match != null && millis != null) {
            close()
            head = match
            time = millis
        } else if (raw.isNotBlank()) {
            (if (head == null) leading else detail).appendLine(raw)
        }
    }
    close()
    return ParsedLog(leading.toString().trimEnd(), lines)
}

/** `HH:mm:ss.SSS` for today, with the date in front for anything older. */
internal fun clockTime(epochMillis: Long): String {
    val time = epochMillisToLocalDateTime(epochMillis)
    val today = now().date
    val clock =
        "${time.hour.pad()}:${time.minute.pad()}:${time.second.pad()}.${(time.nanosecond / 1_000_000).toString().padStart(3, '0')}"
    return if (time.date == today) clock else "${time.day.pad()}/${time.month.number.pad()} $clock"
}

private fun Int.pad(): String = toString().padStart(2, '0')

internal fun nowEpochMillis(): Long = Clock.System.now().toEpochMilliseconds()

/**
 * Cookies, tokens, signed links and IP addresses never leave the device in an export or a bug
 * report, both of which end up in public GitHub issues. Patterns, not a parser: a log line has no
 * grammar to parse. Over-eager on purpose, like desktop's `diagnostics::redact`: a lost detail
 * costs one follow-up question, a leaked SAPISID costs the user their account.
 */
private val SECRETS =
    listOf(
        Regex("""(?i)\b(authorization|cookie|set-cookie)(\s*[:=]\s*)[^\r\n]+""") to "$1$2[redacted]",
        Regex("""\b(SAPISID|APISID|HSID|SSID|SID|SIDCC|LOGIN_INFO|sp_dc|sp_key|__Secure-[\w-]+)=[^;\s&"']+""") to "$1=[redacted]",
        Regex("""(?i)([?&](?:key|token|access_token|refresh_token|api_key|sig|signature)=)[^&\s"']+""") to "$1[redacted]",
        Regex("""(?i)\b(bearer\s+)[\w.~+/=-]+""") to "$1[redacted]",
        // AI provider keys: OpenAI, OpenRouter, Anthropic and most compatible routers start with sk-.
        Regex("""\bsk-[\w-]{16,}""") to "sk-[redacted]",
        // Netscape cookie-file rows (domain, flag, path, secure, expiry, name, value), tab separated.
        Regex("""((?:TRUE|FALSE)\t\d+\t[^\t\r\n]+\t)[^\t\r\n]+""") to "$1[redacted]",
        // A googlevideo URL carries the listener's IP, as ip=… or as an /ip/… path segment.
        Regex("""(?i)([?&/]ip[=/])[0-9a-f:.]+""") to "$1[redacted]",
        // Desktop's rules from here. Known secret keys lose their value however short it is; the
        // leading class keeps a log tag (`I/PoToken: …`) from eating its own message.
        Regex(
            """(?im)(^|[^\w:/])(sapisidhash|sapisid|hsid|ssid|apisid|psid|sid|__secure[a-z0-9_-]*|potoken|po_token|pot|signature|sig|api_sig|session_key|session_token|sk|token|visitor_?data|data_?sync_?id|api_?key)\s*[:=]\s*"?([^\s",;&)]+)""",
        ) to "$1$2=[redacted]",
        // Every query string, which is where pot, sig, ip and ei live.
        Regex("""(https?://[^\s"'<>]+?)\?[^\s"'<>]*""") to "$1?[redacted]",
        // IPv4, but loopback stays: it is a local proxy or adb, never the listener.
        Regex("""\b(?!127\.|0\.)(?:\d{1,3}\.){3}\d{1,3}\b""") to "[ip]",
        // Any run of 40+ token characters is taken for a credential.
        Regex("""[A-Za-z0-9_\-]{40,}""") to "[redacted]",
    )

internal fun redactSecrets(text: String): String = SECRETS.fold(text) { acc, (pattern, replacement) -> pattern.replace(acc, replacement) }

/**
 * Deletes the log on disk, so a restart or an export starts after this. The writer keeps each live
 * file open for appending, so those are emptied in place (its next line lands at the new end); a
 * deleted one would leave it writing to a file nobody can open. Rolled-over files just go.
 */
internal suspend fun clearLogFiles() =
    withContext(Dispatchers.IO) {
        for (name in listOf(Logger.APP_LOG, Logger.ERROR_LOG)) {
            for (file in Logger.logFiles(name).map(::File).filter(File::exists)) {
                runCatching {
                    if (file.name == "$name.log") RandomAccessFile(file, "rw").use { it.setLength(0) } else file.delete()
                }
            }
        }
    }

/**
 * The settings a bug report usually turns on, in one line. Values only, never an account, token,
 * proxy host or id. A failed read leaves the line out rather than the export.
 */
private suspend fun settingsSummary(): String? =
    runCatching {
        val s = getKoin().get<DataStoreManager>()
        fun Flow<String>.onOff() = map { if (it == DataStoreManager.TRUE) "on" else "off" }
        val crossfade =
            if (s.crossfadeEnabled.first() == DataStoreManager.TRUE) {
                "${s.crossfadeDuration.first() / 1000} s" + if (s.crossfadeSkipAlbum.first() == DataStoreManager.TRUE) ", not in albums" else ""
            } else {
                "off"
            }
        val cache = s.maxSongCacheSize.first().let { if (it < 0) "unlimited" else "$it MB" }
        listOf(
            "signed in ${s.loggedIn.onOff().first()}",
            "quality ${s.quality.first()}",
            "download quality ${s.downloadQuality.first()}",
            "crossfade $crossfade",
            "normalize ${s.normalizeVolume.onOff().first()}",
            "skip silence ${s.skipSilent.onOff().first()}",
            "equalizer ${s.equalizerEnabled.onOff().first()}",
            "speed ${s.playbackSpeed.first()}",
            "pitch ${s.pitch.first()}",
            "proxy ${s.usingProxy.onOff().first()}",
            "downloads on Wi-Fi only ${s.downloadWifiOnly.onOff().first()}",
            "download on like ${s.downloadLiked.onOff().first()}",
            "keep library downloaded ${s.keepLibraryDownloaded.onOff().first()}",
            "player cache $cache",
            "Discord ${s.richPresenceEnabled.onOff().first()}",
            "Last.fm ${if (s.lastfmSessionKey.first().isNotBlank()) "on" else "off"}",
            "player style ${s.nowPlayingStyle.first().replace("SPOTIFY", "Classic")}",
            "open player on play ${if (s.openPlayerOnPlay.first()) "on" else "off"}",
        ).joinToString(", ")
    }.getOrNull()

/** The version, the phone and the settings, above the log in an export or a bug report. */
private suspend fun logHeader(): String =
    buildString {
        appendLine("Limusic ${VersionManager.getVersionName()} · ${deviceSummary()}")
        settingsSummary()?.let { appendLine("Settings: $it") }
        appendLine()
    }

/** app.log, oldest first. error.log stays out: every line in it is in app.log too. */
private fun appLogText(): String = Logger.logFiles(Logger.APP_LOG).map(::File).filter(File::exists).joinToString("") { it.readText() }

/**
 * GitHub refuses an issue body over 65,536 characters, and the bug form's other fields need room
 * next to the pasted log.
 */
internal const val BUG_REPORT_CHARS = 50_000

/**
 * [logHeader] plus the end of the app log, redacted, at most [BUG_REPORT_CHARS]: the text a user
 * pastes into the bug form's App log box. Null when the log can't be read.
 */
internal suspend fun bugReportText(): String? =
    withContext(Dispatchers.IO) {
        runCatching {
            val header = redactSecrets(logHeader())
            val budget = BUG_REPORT_CHARS - header.length
            // Cut before redacting, so no secret is split from the key that marks it, and again
            // after, since a short value can grow into "[redacted]".
            header + logTail(redactSecrets(logTail(appLogText(), budget)), budget)
        }.getOrNull()
    }

/** The end of [log] in at most [budget] characters, from the start of a line when there is one. */
internal fun logTail(
    log: String,
    budget: Int,
): String {
    if (log.length <= budget) return log
    val start = log.indexOf('\n', log.length - budget)
    return if (start < 0) log.takeLast(budget) else log.substring(start + 1)
}

/** The app log as one redacted text file handed to [shareImage], under [logHeader]. */
internal suspend fun exportLog(chooserTitle: String): Boolean {
    val bytes =
        withContext(Dispatchers.IO) {
            runCatching { redactSecrets(logHeader() + appLogText()).encodeToByteArray() }.getOrNull()
        } ?: return false
    val stamp = now()
    val fileName =
        "limusic-log-${stamp.year}${stamp.month.number.pad()}${stamp.day.pad()}-" +
            "${stamp.hour.pad()}${stamp.minute.pad()}${stamp.second.pad()}.txt"
    return shareImage(bytes, fileName, chooserTitle, mimeType = "text/plain")
}

private const val NEW_ISSUE = "https://github.com/SimoHypers/limusic-android/issues/new"

/**
 * GitHub's bug form (`.github/ISSUE_TEMPLATE/bug_report.yml`) with the version and the phone filled
 * in. GitHub prefills a form field from the query parameter named after its id.
 */
internal fun bugFormUrl(): String {
    fun enc(s: String) = java.net.URLEncoder.encode(s, "UTF-8")
    return "$NEW_ISSUE?template=bug_report.yml&version=${enc(VersionManager.getVersionName())}&phone=${enc(deviceSummary())}"
}

internal const val FEATURE_FORM_URL = "$NEW_ISSUE?template=feature_request.yml"
