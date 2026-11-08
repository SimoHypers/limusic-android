package com.simohypers.limusic.lyrics

import com.simohypers.limusic.domain.extension.decodeHtmlEntities
import com.simohypers.limusic.ktorext.getEngine
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import com.simohypers.limusic.lyrics.domain.Lyrics
import kotlin.io.encoding.Base64
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * What every provider is asked with. [durationSeconds] is the length of the cut playing, null when
 * unknown. [searched]: title and artist were typed in the source sheet, so the video id no longer
 * says what to look up.
 */
data class LyricsQuery(
    val videoId: String,
    val title: String,
    val artist: String,
    val album: String?,
    val durationSeconds: Int?,
    val searched: Boolean = false,
)

/**
 * The lyrics providers Limusic asks, ported from its `lyrics.rs`. The chain itself (order, cache,
 * synced before plain) lives with the caller, which also owns the YouTube Music provider.
 *
 * [ask] answers null when the provider has no lyrics for the song, and throws when it could not be
 * asked at all (offline, 5xx, 429). The difference decides whether a miss may be cached.
 */
class LyricsProviders {
    private val http =
        HttpClient(getEngine()) {
            expectSuccess = false
            install(HttpTimeout) { requestTimeoutMillis = 10_000 }
            install(ContentEncoding) {
                gzip()
                deflate()
            }
        }

    suspend fun ask(
        id: String,
        q: LyricsQuery,
    ): Lyrics? =
        when (id) {
            "boidu" -> boidu(q)
            "unison" -> unison(q)
            "lyricsplus" -> lyricsPlus(q)
            "lrclib" -> lrclib(q)
            "netease" -> netease(q)
            "qq" -> qqMusic(q)
            "kugou" -> kugou(q)
            else -> null
        }?.toLyrics()

    // Boidu (Better Lyrics API). Without a key it answers only from its cache, keyed on title,
    // artist, album and duration, and 401s a miss. A song may be cached with its album or without
    // one, so the album form first and the bare one after a 401.
    private suspend fun boidu(q: LyricsQuery): Found? {
        val d = q.durationSeconds?.takeIf { it > 0 } ?: return null
        for (album in listOf(q.album, null).distinct()) {
            val r =
                http.get("https://lyrics-api.boidu.dev/getLyrics") {
                    ua()
                    parameter("s", q.title)
                    parameter("a", q.artist)
                    parameter("d", d)
                    album?.let { parameter("al", it) }
                }
            if (r.status == HttpStatusCode.Unauthorized) continue
            val body = r.json() ?: return null
            val text = listOf("ttml", "syncedLyrics", "lyrics", "lrc").firstNotNullOfOrNull { body[it].str?.takeIf(String::isNotBlank) }
            return text?.let { Found(parseLrcOrTtml(it)) }
        }
        return null
    }

    // LyricsPlus, YouLyPlus' backend: Apple Music's word-timed lyrics as KPOE JSON. Its search
    // strays onto other songs, so the length has to agree.
    // ponytail: one mirror, the only one of six alive on 2026-09-27; point it at the next when it dies.
    private suspend fun lyricsPlus(q: LyricsQuery): Found? {
        val title = searchTitle(q.title).ifEmpty { q.title }
        val k =
            http
                .get("https://lyricsplus.binimum.org/v2/lyrics/get") {
                    ua()
                    parameter("title", title)
                    parameter("artist", q.artist)
                    q.album?.let { parameter("album", it) }
                    q.durationSeconds?.takeIf { it > 0 }?.let { parameter("duration", it) }
                }.json() ?: return null
        val theirs = k["metadata"]["totalDuration"].str?.let(::parseTtmlTime)
        val ours = q.durationSeconds?.takeIf { it > 0 }
        if (ours != null && theirs != null && abs(ours - theirs / 1000.0) > MATCH_TOLERANCE_SECS) return null
        val wordTimed = k["type"].str == "Word"
        val lines =
            k["lyrics"].arr.map { l ->
                val time = l["time"].num?.toLong() ?: 0L
                LyricLine(
                    timeMs = time,
                    endMs = time + (l["duration"].num?.toLong() ?: 0L),
                    text = l["text"].str.orEmpty(),
                    words =
                        l["syllabus"].arr
                            .takeIf { wordTimed && it.isNotEmpty() }
                            ?.map { LyricWord(it["text"].str.orEmpty(), it["time"].num?.toLong() ?: time) },
                )
            }
        return Found(lines)
    }

    // LRCLIB: the exact match, then the fuzzy search for synced lyrics, then whichever plain text
    // either found. Errors only when both requests failed.
    private suspend fun lrclib(q: LyricsQuery): Found? {
        val exact = runCatching { lrclibGet(q) }
        exact.getOrNull()?.lrclibFound()?.takeIf { it.synced || it.instrumental }?.let { return it }
        val searched = runCatching { lrclibSearch(q) }
        searched.getOrNull()?.lrclibFound()?.takeIf { it.synced }?.let { return it }
        if (exact.isFailure && searched.isFailure) throw exact.exceptionOrNull()!!
        return exact.getOrNull()?.lrclibFound() ?: searched.getOrNull()?.lrclibFound()
    }

    private suspend fun lrclibGet(q: LyricsQuery): JsonElement? =
        http
            .get("$LRCLIB/get") {
                ua()
                parameter("track_name", q.title)
                parameter("artist_name", q.artist)
                q.album?.let { parameter("album_name", it) }
                q.durationSeconds?.takeIf { it > 0 }?.let { parameter("duration", it) }
            }.json()

    // The title and artist fields first, then free text when those find nothing synced: the tidied
    // title with the artist, then without. Free text only with a known length, which keeps the
    // looser match on the right song.
    private suspend fun lrclibSearch(q: LyricsQuery): JsonElement? {
        var hit = lrclibSearchBy(q, "track_name" to q.title, "artist_name" to q.artist)
        val title = searchTitle(q.title)
        if (hit.hasSynced() || (q.durationSeconds ?: 0) <= 0 || title.isEmpty()) return hit
        for (text in listOf("$title ${q.artist}", title)) {
            val free = runCatching { lrclibSearchBy(q, "q" to text) }.getOrNull()
            if (free.hasSynced()) return free
            hit = hit ?: free
        }
        return hit
    }

    // Prefers the synced candidate whose length is closest to ours: LRCLIB carries several cuts of
    // popular songs, and a cut 4s longer plays its lyrics 4s off the audio.
    private suspend fun lrclibSearchBy(
        q: LyricsQuery,
        vararg params: Pair<String, String>,
    ): JsonElement? {
        val list =
            http
                .get("$LRCLIB/search") {
                    ua()
                    params.forEach { (k, v) -> parameter(k, v) }
                }.json()
                .arr
        val ours = q.durationSeconds?.takeIf { it > 0 }
        val dist = { t: JsonElement -> t["duration"].num?.let { d -> ours?.let { abs(it - d) } } ?: Double.POSITIVE_INFINITY }
        val close = list.filter { ours == null || dist(it) <= MATCH_TOLERANCE_SECS }
        return close.filter { it.hasSynced() }.minByOrNull(dist) ?: close.firstOrNull { !it.hasSynced() }
    }

    private fun JsonElement?.hasSynced() = !this["syncedLyrics"].str.isNullOrBlank()

    private fun JsonElement.lrclibFound(): Found? {
        if (this["instrumental"].bool == true) return Found(emptyList(), instrumental = true)
        this["syncedLyrics"]
            .str
            ?.takeIf(String::isNotBlank)
            ?.let(::parseLrc)
            ?.takeIf { it.isNotEmpty() }
            ?.let { return Found(it) }
        return plainFound(this["plainLyrics"].str)
    }

    // Unison, Better Lyrics' crowdsourced lyrics, filed by video id, so timed to the upload playing.
    // By title only for a search typed in the source sheet: a `v` miss is a 404 without trying song
    // and artist, so asking both ways would cost a second request on every miss. Read only: a vote
    // is signed with a per-user key, so the source sheet links the song's page on their site.
    private suspend fun unison(q: LyricsQuery): Found? =
        unisonFound(
            http
                .get("https://unison.betterlyrics.org/lyrics") {
                    ua()
                    if (q.searched) {
                        parameter("song", q.title)
                        parameter("artist", q.artist)
                        q.durationSeconds?.takeIf { it > 0 }?.let { parameter("duration", it) }
                    } else {
                        parameter("v", q.videoId)
                    }
                }.json(),
        )

    // Netease, QQ and Kugou rank remixes and edits right next to the original, so a hit is picked
    // by length (bestByDuration), not by rank.
    private suspend fun netease(q: LyricsQuery): Found? {
        // POST /api/search/get: the GET .../web form now answers with an encrypted blob.
        val found =
            http
                .submitForm(
                    "https://music.163.com/api/search/get",
                    parameters {
                        append("s", "${q.title} ${q.artist}")
                        append("type", "1")
                        append("limit", "5")
                        append("offset", "0")
                    },
                ) { neteaseHeaders() }
                .json()
        // Netease reports length in milliseconds.
        val id =
            found["result"]["songs"]
                .arr
                .bestByDuration(q.durationSeconds) { s -> s["duration"].num?.div(1000) }
                ?.get("id")
                .str ?: return null
        val lyric =
            http
                .get("https://music.163.com/api/song/lyric?id=$id&lv=1&kv=1&tv=-1") { neteaseHeaders() }
                .json()
        // ponytail: Netease's tlyric translations are left out; they are Chinese for most songs.
        return lyric["lrc"]["lyric"].str?.let { Found(parseLrcOrTtml(it)) }
    }

    private fun HttpRequestBuilder.neteaseHeaders() {
        ua()
        header(HttpHeaders.Referrer, "https://music.163.com/")
    }

    // QQ Music. Its search wants a signed request now; the search box's suggestions still answer but
    // carry no lengths, so one batch song-detail call fetches those for the length match.
    private suspend fun qqMusic(q: LyricsQuery): Found? {
        val mids =
            qq("splcloud/fcgi-bin/smartbox_new.fcg", "key" to "${q.title} ${q.artist}")["data"]["song"]["itemlist"]
                .arr
                .mapNotNull { it["mid"].str }
        if (mids.isEmpty()) return null
        // QQ reports length in whole seconds, as `interval`.
        val mid =
            qq("v8/fcg-bin/fcg_play_single_song.fcg", "songmid" to mids.joinToString(","))["data"]
                .arr
                .bestByDuration(q.durationSeconds) { it["interval"].num }
                ?.get("mid")
                .str ?: return null
        val raw = qq("lyric/fcgi-bin/fcg_query_lyric_new.fcg", "songmid" to mid, "nobase64" to "1")["lyric"].str ?: return null
        // Without base64 QQ escapes the LRC as HTML entities, `[00&#58;12&#46;34]`.
        return Found(parseLrcOrTtml(decodeHtmlEntities(raw.base64OrSelf())))
    }

    private suspend fun qq(
        path: String,
        vararg params: Pair<String, String>,
    ): JsonElement? =
        http
            .get("https://c.y.qq.com/$path") {
                ua()
                header(HttpHeaders.Referrer, "https://y.qq.com/")
                parameter("format", "json")
                params.forEach { (k, v) -> parameter(k, v) }
            }.json()

    private suspend fun kugou(q: LyricsQuery): Found? {
        val hash =
            http
                .get("https://songsearch.kugou.com/song_search_v2") {
                    parameter("keyword", "${q.title} ${q.artist}")
                    parameter("page", 1)
                    parameter("pagesize", 5)
                }.json()
                .let { it["data"]["lists"].arr }
                // Kugou reports length in whole seconds, as `Duration`.
                .bestByDuration(q.durationSeconds) { it["Duration"].num }
                ?.get("FileHash")
                .str ?: return null
        // `hash=`, not `h=`: the endpoint answers "empty hash and keyword" to the latter.
        val candidate =
            http
                .get("https://krcs.kugou.com/search?ver=1&man=yes&client=mobi&hash=$hash")
                .json()
                ?.get("candidates")
                .arr
                .firstOrNull()
        val id = candidate["id"].str ?: return null
        val key = candidate["accesskey"].str ?: return null
        val content =
            http
                .get("https://lyrics.kugou.com/download?ver=1&client=pc&id=$id&accesskey=$key&fmt=lrc")
                .json()
                ?.get("content")
                .str ?: return null
        return Found(parseLrcOrTtml(content.base64OrSelf()))
    }

    private fun HttpRequestBuilder.ua() = header(HttpHeaders.UserAgent, USER_AGENT)

    /** The body as JSON; null on a 404, which every provider uses to say it has nothing. */
    private suspend fun HttpResponse.json(): JsonElement? {
        if (status == HttpStatusCode.NotFound) return null
        check(status.isSuccess()) { "HTTP ${status.value} from ${call.request.url.host}" }
        return Json.parseToJsonElement(bodyAsText())
    }

    private companion object {
        const val LRCLIB = "https://lrclib.net/api"

        // LRCLIB asks integrations to say who they are; the others get the same.
        const val USER_AGENT = "Limusic Android (https://github.com/SimoHypers/limusic)"
    }
}

/** How far a search hit's length may sit from the cut playing. */
internal const val MATCH_TOLERANCE_SECS = 5.0

internal data class LyricWord(
    val text: String,
    val startMs: Long,
)

/** One display line; [timeMs] null when the lyrics are plain. */
internal data class LyricLine(
    val timeMs: Long?,
    val endMs: Long? = null,
    val text: String,
    val words: List<LyricWord>? = null,
)

/** A provider's answer before it becomes the app's model. */
internal class Found(
    val lines: List<LyricLine>,
    val instrumental: Boolean = false,
) {
    // Two different cues at least: an LRC with a few untimed credit lines is still synced, but one
    // that stamps every line [00:00.00] (or LyricsPlus' untimed Apple lyrics, all at 0) is plain
    // text dressed as synced and must not stop the chain above real timings (Limusic #539). Word
    // starts count, so a single word-timed line keeps its sweep.
    val synced get() = lines.flatMap { l -> listOfNotNull(l.timeMs) + l.words.orEmpty().map { it.startMs } }.distinct().size > 1
}

/**
 * Into the shape the lyrics view reads: RICH_SYNCED lines carry `<mm:ss.mmm>word` runs, one per
 * word. Null when there is nothing to show. An instrumental is one timed line, so it counts as a
 * synced answer and stops the chain the way it does in Limusic.
 */
internal fun Found.toLyrics(): Lyrics? {
    if (instrumental) return lyrics("LINE_SYNCED", listOf(line("0", "♪")))
    if (lines.isEmpty()) return null
    val rich = synced && lines.any { it.words != null }
    val syncType = if (rich) "RICH_SYNCED" else if (synced) "LINE_SYNCED" else "UNSYNCED"
    return lyrics(
        syncType,
        lines.map { l ->
            line(
                start = (l.timeMs ?: 0L).toString(),
                words = if (rich && l.words != null) l.words.toRichSync() else l.text,
                end = (l.endMs ?: 0L).toString(),
            )
        },
    )
}

private fun lyrics(
    syncType: String,
    lines: List<Lyrics.LyricsX.Line>,
) = Lyrics(Lyrics.LyricsX(lines = lines, syncType = syncType))

private fun line(
    start: String,
    words: String,
    end: String = "0",
) = Lyrics.LyricsX.Line(endTimeMs = end, startTimeMs = start, syllables = listOf(), words = words)

/**
 * Apple times syllables ("beau", "ti", "ful "), and the view draws one run per word with a space
 * between runs. So syllables join into words, a word ending where a syllable ends in whitespace,
 * and the word starts with its first syllable.
 */
internal fun List<LyricWord>.toRichSync(): String =
    buildString {
        var wordEnded = true
        for (w in this@toRichSync) {
            if (w.text.isBlank()) {
                wordEnded = true
                continue
            }
            if (wordEnded || w.text.first().isWhitespace()) {
                if (isNotEmpty()) append(' ')
                append(richStamp(w.startMs))
            }
            append(w.text.trim())
            wordEnded = w.text.last().isWhitespace()
        }
    }

private fun richStamp(ms: Long): String {
    fun pad(v: Long, n: Int) = v.toString().padStart(n, '0')
    return "<${pad(ms / 60_000, 2)}:${pad(ms / 1000 % 60, 2)}.${pad(ms % 1000, 3)}>"
}

/** Unison's answer: `{data: {lyrics, format}}`, format "ttml" (Apple's dialect), "lrc" or "plain". */
internal fun unisonFound(body: JsonElement?): Found? {
    val data = body["data"]
    val text = data["lyrics"].str ?: return null
    return if (data["format"].str == "plain") plainFound(text) else Found(parseLrcOrTtml(text))
}

private fun plainFound(text: String?): Found? {
    val t = text?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    return Found(t.lines().map { LyricLine(timeMs = null, text = it.trimEnd()) })
}

/**
 * The search hit closest in length to what is playing, nothing further off than
 * [MATCH_TOLERANCE_SECS]. Closest rather than first-within-tolerance: the remix is often inside
 * the window too. With no length of ours the first hit stands; a hit with no length ranks last.
 */
internal fun <T> List<T>.bestByDuration(
    ours: Int?,
    secs: (T) -> Double?,
): T? {
    val o = ours?.takeIf { it > 0 } ?: return firstOrNull()
    return map { (secs(it)?.let { d -> abs(d - o) } ?: Double.POSITIVE_INFINITY) to it }
        .filter { (d, _) -> d <= MATCH_TOLERANCE_SECS || d.isInfinite() }
        .minByOrNull { it.first }
        ?.second
}

/**
 * A YouTube title without what no lyrics catalogue files a song under: everything after a `|` and
 * every bracketed aside, "(Official Video)", "[English]", "【MV】" alike.
 */
internal fun searchTitle(title: String): String {
    var depth = 0
    val out = StringBuilder()
    for (c in title.substringBefore('|')) {
        when (c) {
            '(', '[', '【', '（' -> depth++
            ')', ']', '】', '）' -> depth = (depth - 1).coerceAtLeast(0)
            else -> if (depth == 0) out.append(c)
        }
    }
    return out.split(Regex("\\s+")).filter { it.isNotEmpty() }.joinToString(" ")
}

/**
 * LRC (`[mm:ss.xx] line`) into lines sorted by time. A line with several stamps (`[t1][t2]text`)
 * repeats at each; metadata tags (`[ar:…]`) are skipped; bracketed lyric text ("[Chorus]") stays.
 */
internal fun parseLrc(lrc: String): List<LyricLine> {
    val out = mutableListOf<LyricLine>()
    for (raw in lrc.lines()) {
        var rest = raw.trim()
        val times = mutableListOf<Long>()
        while (rest.startsWith('[')) {
            val end = rest.indexOf(']')
            if (end < 0) break
            times += parseLrcTime(rest.substring(1, end)) ?: break
            rest = rest.substring(end + 1).trimStart()
        }
        times.forEach { out += LyricLine(timeMs = it, text = rest) }
    }
    return out.sortedBy { it.timeMs }
}

/** `mm:ss`, `mm:ss.xx` or `mm:ss.xxx` into milliseconds. */
internal fun parseLrcTime(tag: String): Long? {
    val m = tag.substringBefore(':', "").trim().toLongOrNull() ?: return null
    val rest = tag.substringAfter(':')
    val s = rest.substringBefore('.').trim().toLongOrNull() ?: return null
    val frac = if ('.' in rest) rest.substringAfter('.').filter(Char::isDigit).take(3) else ""
    val ms =
        when (frac.length) {
            0 -> 0L
            1 -> frac.toLong() * 100
            2 -> frac.toLong() * 10
            else -> frac.toLong()
        }
    return (m * 60 + s) * 1000 + ms
}

/**
 * Enhanced LRC: `[00:10.50]<00:10.50>Hello <00:11.20>world`. An inline stamp is when the word after
 * it starts; text before the first stamp starts with the line.
 */
internal fun parseElrc(lrc: String): List<LyricLine> =
    parseLrc(lrc).map { line ->
        val start = line.timeMs
        if (start == null || '<' !in line.text) return@map line
        val runs = mutableListOf(start to StringBuilder())
        var rest = line.text
        while (true) {
            val open = rest.indexOf('<')
            if (open < 0) break
            runs.last().second.append(rest, 0, open)
            val after = rest.substring(open + 1)
            val end = after.indexOf('>')
            val ms = if (end >= 0) parseLrcTime(after.substring(0, end)) else null
            if (ms != null) {
                runs += ms to StringBuilder()
                rest = after.substring(end + 1)
            } else {
                runs.last().second.append('<')
                rest = after
            }
        }
        runs.last().second.append(rest)
        if (runs.size == 1) return@map line
        val words = runs.filter { it.second.isNotEmpty() }.map { (ms, text) -> LyricWord(text.toString(), ms) }
        line.copy(text = words.joinToString("") { it.text }.trim(), words = words.ifEmpty { null })
    }

/** TTML when it looks like XML, else (e)LRC. */
internal fun parseLrcOrTtml(text: String): List<LyricLine> {
    val t = text.trim()
    if (t.startsWith('<') || "<p " in t || "<tt" in t) parseTtml(t).takeIf { it.isNotEmpty() }?.let { return it }
    return parseElrc(text)
}

/**
 * Apple-style TTML: `<p begin end>` lines of timed `<span>`s. Text between spans, the spaces that
 * separate words, belongs to the word before it.
 * ponytail: Apple's own romanization (`<transliteration>`) is skipped; the local romanizer covers it.
 */
internal fun parseTtml(xml: String): List<LyricLine> {
    val lines = mutableListOf<LyricLine>()
    var pos = 0
    while (true) {
        val pStart = xml.indexOf("<p", pos).takeIf { it >= 0 } ?: break
        // `<p` also opens tags like `<parsererror>`; only a real paragraph has a space or `>` next.
        val next = xml.getOrNull(pStart + 2)
        val tagEnd = xml.indexOf('>', pStart).takeIf { it >= 0 } ?: break
        if (next != ' ' && next != '>') {
            pos = tagEnd + 1
            continue
        }
        val close = xml.indexOf("</p>", tagEnd).takeIf { it >= 0 } ?: break
        val tag = xml.substring(pStart, tagEnd + 1)
        val inner = xml.substring(tagEnd + 1, close)
        pos = close + 4
        val begin = xmlAttr(tag, "begin")?.let(::parseTtmlTime)
        val end = xmlAttr(tag, "end")?.let(::parseTtmlTime)
        val (text, words) = ttmlSpans(inner, begin)
        if (text.isNotEmpty() || begin != null) lines += LyricLine(begin, end, text, words)
    }
    return lines.sortedBy { it.timeMs }
}

private fun ttmlSpans(
    inner: String,
    lineBegin: Long?,
): Pair<String, List<LyricWord>?> {
    val words = mutableListOf<LyricWord>()
    val text = StringBuilder()
    var pos = 0
    while (true) {
        val sStart = inner.indexOf("<span", pos).takeIf { it >= 0 } ?: break
        val tagEnd = inner.indexOf('>', sStart).takeIf { it >= 0 } ?: break
        val before = stripTags(inner.substring(pos, sStart))
        if (before.isNotEmpty()) {
            text.append(before)
            words.lastOrNull()?.let { words[words.lastIndex] = it.copy(text = it.text + before) }
        }
        val close = inner.indexOf("</span>", tagEnd).takeIf { it >= 0 } ?: break
        val wordText = stripTags(inner.substring(tagEnd + 1, close))
        val begin = xmlAttr(inner.substring(sStart, tagEnd + 1), "begin")?.let(::parseTtmlTime) ?: lineBegin
        if (begin != null && wordText.isNotEmpty()) words += LyricWord(wordText, begin)
        text.append(wordText)
        pos = close + "</span>".length
    }
    if (pos < inner.length) text.append(stripTags(inner.substring(pos)))
    return text.toString().trim() to words.ifEmpty { null }
}

/** `12.5s`, `450ms`, `1:02.5`, `1:02:03.5` or bare seconds into milliseconds. */
internal fun parseTtmlTime(s: String): Long? {
    val t = s.trim()
    if (t.endsWith("ms")) return t.dropLast(2).toLongOrNull()
    if (t.endsWith("s")) return t.dropLast(1).toDoubleOrNull()?.let { (it * 1000).toLong() }
    val parts = t.split(':')
    val secs = parts.last().toDoubleOrNull() ?: return null
    val whole = parts.dropLast(1).map { it.toLongOrNull() ?: return null }.fold(0L) { acc, v -> acc * 60 + v }
    return whole * 60_000 + (secs * 1000).roundToInt()
}

private fun xmlAttr(
    tag: String,
    attr: String,
): String? =
    Regex("""(?:^|\s)${Regex.escape(attr)}=(["'])(.*?)\1""").find(tag)?.groupValues?.get(2)

private fun stripTags(s: String) = s.replace(Regex("<[^>]*>"), "")

/** QQ and Kugou hand lyrics over base64-encoded, QQ only sometimes; raw LRC is not valid base64. */
private fun String.base64OrSelf(): String = runCatching { Base64.decode(trim()).decodeToString() }.getOrDefault(this)

private operator fun JsonElement?.get(key: String): JsonElement? = (this as? JsonObject)?.get(key)

private val JsonElement?.arr: List<JsonElement> get() = (this as? JsonArray).orEmpty()

private val JsonElement?.str: String? get() = (this as? JsonPrimitive)?.takeUnless { it is JsonNull }?.content

private val JsonElement?.num: Double? get() = (this as? JsonPrimitive)?.doubleOrNull

private val JsonElement?.bool: Boolean? get() = (this as? JsonPrimitive)?.booleanOrNull
