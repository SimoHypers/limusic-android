package com.simohypers.limusic.lyrics

import com.simohypers.limusic.domain.data.model.lyrics.LIMUSIC_REPORT_REASONS
import com.simohypers.limusic.domain.data.model.lyrics.LIMUSIC_UNAVAILABLE
import com.simohypers.limusic.domain.data.model.lyrics.LimusicException
import com.simohypers.limusic.domain.data.model.lyrics.LimusicRating
import com.simohypers.limusic.ktorext.getEngine
import com.simohypers.limusic.lyrics.domain.Lyrics
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.timeout
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

/**
 * Limusic Lyrics (`lyrics.limusic.app`), our own word-synced lyrics, one per song, filed by video
 * id. Some are mirrored from Unison, Boidu and LyricsPlus (which [hint] feeds), the rest written by
 * contributors. The wire contract is the service's README; desktop's `lyrics.rs` and `limusic.rs`
 * are the reference.
 *
 * Reading needs nothing. A vote or a report carries the session of an anonymous account, which the
 * caller makes with [anonymous] and keeps. Failures the service names come back as
 * [LimusicException] with its code; no connection, a 5xx or an unreadable answer is `unavailable`.
 */
class LimusicLyrics(
    root: String = URL,
) {
    private val root = root.trimEnd('/')

    private val http =
        HttpClient(getEngine()) {
            expectSuccess = false
            install(HttpTimeout) { requestTimeoutMillis = 8_000 }
            install(ContentEncoding) { gzip() }
        }

    /** A lyric as the service has it, already in the shape the lyrics view reads. */
    class Lyric(
        val lyrics: Lyrics,
        /** "community", or the source it mirrors. Kept a string: a newer server may add one. */
        val origin: String,
        val revision: Long?,
        val authors: List<String>,
    )

    sealed interface Fresh {
        /** Still the revision that was named (a 304). */
        data object Same : Fresh

        /** No lyric for the song. */
        data object Gone : Fresh

        class New(
            val lyric: Lyric,
        ) : Fresh
    }

    /**
     * The song's lyric, or with [known] (the revision the cache holds) only whether it moved: the
     * revision is the answer's ETag, so an unchanged lyric is a 304 off Cloudflare's edge. Throws
     * when the service couldn't be asked, which the chain treats as "no answer", not "none".
     */
    suspend fun get(
        videoId: String,
        known: Long? = null,
    ): Fresh {
        if (!plausible(videoId)) return Fresh.Gone
        val r =
            http.get("$root/v1/lyrics/$videoId") {
                ua()
                // Answers are read in the user's order, so while Limusic is first a stalled request
                // holds back lyrics another source already has. A hit or a miss comes off the edge in
                // well under a second; past this the chain moves on.
                timeout { requestTimeoutMillis = GET_TIMEOUT_MS }
                known?.let { header(HttpHeaders.IfNoneMatch, "\"$it\"") }
            }
        return when (r.status) {
            HttpStatusCode.NotModified -> Fresh.Same
            HttpStatusCode.NotFound -> Fresh.Gone
            HttpStatusCode.OK -> answerToLyric(r.bodyAsText())?.let { Fresh.New(it) } ?: Fresh.Gone
            else -> error("HTTP ${r.status.value} from Limusic Lyrics")
        }
    }

    /**
     * Tell the service that [provider] has this song word by word, so it fetches the lyric itself.
     * Never lyric text. The answer says nothing either way (202), so it isn't read.
     */
    suspend fun hint(
        videoId: String,
        provider: String,
        title: String,
        artist: String,
        album: String?,
        durationSeconds: Int?,
    ) {
        if (!plausible(videoId)) return
        val body =
            buildJsonObject {
                put("video_id", videoId)
                put("provider", provider)
                put("title", title.clipBytes(FIELD_MAX_BYTES))
                put("artist", artist.clipBytes(FIELD_MAX_BYTES))
                album?.takeIf { it.isNotBlank() }?.let { put("album", it.clipBytes(FIELD_MAX_BYTES)) }
                durationSeconds?.takeIf { it > 0 }?.let { put("duration", it) }
            }
        http.post("$root/v1/hints") { json(body) }
    }

    /** A new anonymous account's session token. */
    suspend fun anonymous(): String {
        val body = expect(send { http.post("$root/v1/auth/anonymous") { ua() } }, HttpStatusCode.OK)
        return body["token"]?.jsonPrimitive?.content?.takeIf { it.isNotEmpty() } ?: throw LimusicException(LIMUSIC_UNAVAILABLE)
    }

    /**
     * The live lyric's votes and this account's own, or null when the song has no live lyric. A
     * [token] the service no longer knows is `unauthorized`, even though reading needs none.
     */
    suspend fun rating(
        videoId: String,
        token: String?,
    ): LimusicRating? {
        if (!plausible(videoId)) return null
        val answer =
            send {
                http.get("$root/v1/lyrics/$videoId/state") {
                    ua()
                    token?.let { bearerAuth(it) }
                }
            }
        if (answer?.first == HttpStatusCode.NotFound) return null
        val body = expect(answer, HttpStatusCode.OK)
        val revision = body["revision"]?.jsonPrimitive?.longOrNull ?: return null
        val votes = body["votes"]?.jsonObject
        return LimusicRating(
            revision = revision,
            up = votes?.get("up")?.jsonPrimitive?.int?.coerceAtLeast(0) ?: 0,
            down = votes?.get("down")?.jsonPrimitive?.int?.coerceAtLeast(0) ?: 0,
            myVote = body["my_vote"]?.jsonPrimitive?.int?.coerceIn(-1, 1) ?: 0,
        )
    }

    /** 1 up, -1 down, 0 takes the vote back. Answers the counts it left, with [before]'s revision. */
    suspend fun vote(
        videoId: String,
        value: Int,
        token: String,
        before: LimusicRating,
    ): LimusicRating {
        require(value in -1..1 && plausible(videoId))
        val body =
            expect(
                send {
                    http.put("$root/v1/lyrics/$videoId/vote") {
                        bearerAuth(token)
                        json(buildJsonObject { put("value", value) })
                    }
                },
                HttpStatusCode.OK,
            )
        return before.copy(
            up = body["up"]?.jsonPrimitive?.int?.coerceAtLeast(0) ?: 0,
            down = body["down"]?.jsonPrimitive?.int?.coerceAtLeast(0) ?: 0,
            myVote = body["my_vote"]?.jsonPrimitive?.int?.coerceIn(-1, 1) ?: value,
        )
    }

    /** Report the live lyric. One per account and revision: a repeat is accepted and changes nothing. */
    suspend fun report(
        videoId: String,
        reason: String,
        note: String?,
        token: String,
    ) {
        require(reason in LIMUSIC_REPORT_REASONS && plausible(videoId))
        val body =
            buildJsonObject {
                put("reason", reason)
                note?.trim()?.takeIf { it.isNotEmpty() }?.let { put("note", it.clipBytes(NOTE_MAX_BYTES)) }
            }
        expect(
            send {
                http.post("$root/v1/lyrics/$videoId/reports") {
                    bearerAuth(token)
                    json(body)
                }
            },
            HttpStatusCode.Accepted,
        )
    }

    private fun HttpRequestBuilder.ua() = header(HttpHeaders.UserAgent, USER_AGENT)

    private fun HttpRequestBuilder.json(body: JsonObject) {
        ua()
        contentType(ContentType.Application.Json)
        setBody(body.toString())
    }

    /** The status and the JSON body (empty when there is none); null when the request never got an answer. */
    private suspend fun send(call: suspend () -> HttpResponse): Pair<HttpStatusCode, JsonObject>? =
        try {
            val r = call()
            r.status to (runCatching { json.parseToJsonElement(r.bodyAsText()).jsonObject }.getOrNull() ?: JsonObject(emptyMap()))
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }

    private fun expect(
        answer: Pair<HttpStatusCode, JsonObject>?,
        status: HttpStatusCode,
    ): JsonObject {
        if (answer?.first == status) return answer.second
        throw LimusicException(failure(answer))
    }

    companion object {
        const val URL = "https://lyrics.limusic.app"

        /** The sources the service mirrors, the only ones a hint may name. */
        val MIRRORED = setOf("unison", "boidu", "lyricsplus")

        private const val GET_TIMEOUT_MS = 2_000L
        private const val FIELD_MAX_BYTES = 300
        private const val NOTE_MAX_BYTES = 300
        private const val USER_AGENT = "Limusic Android (https://github.com/SimoHypers/limusic)"
        private val json = Json { ignoreUnknownKeys = true }

        /** A song the service can know: exactly 11 of `[A-Za-z0-9_-]`, as its paths take them. */
        fun plausible(videoId: String) = videoId.length == 11 && videoId.all { it.isLetterOrDigit() && it.code < 128 || it == '-' || it == '_' }

        /** The error code of a failed answer, as desktop reads it. */
        internal fun failure(answer: Pair<HttpStatusCode, JsonObject>?): String {
            val (status, body) = answer ?: return LIMUSIC_UNAVAILABLE
            val code = runCatching { body["error"]?.jsonPrimitive?.content }.getOrNull()
            return when {
                status == HttpStatusCode.Unauthorized -> "unauthorized"
                status == HttpStatusCode.TooManyRequests && code == null -> "rate_limited"
                status.value in 400..499 && code != null -> code
                else -> LIMUSIC_UNAVAILABLE
            }
        }

        /** The service's answer in the app's shape. Null when it holds nothing to show. */
        internal fun answerToLyric(text: String): Lyric? {
            val a = runCatching { json.decodeFromString<Answer>(text) }.getOrNull() ?: return null
            val lines =
                a.lines.map { l ->
                    LyricLine(
                        timeMs = l.timeMs,
                        endMs = l.endTimeMs,
                        text = l.text,
                        words = l.words.map { LyricWord(it.text, it.startMs, it.bg) }.ifEmpty { null },
                        agent = l.agent,
                    )
                }
            val lyrics = Found(lines).toLyrics() ?: return null
            // The service sends five at most; no more than that is ever kept or shown.
            return Lyric(lyrics, a.origin, a.revision, a.authors.filter { it.isNotBlank() }.take(5))
        }

        /** At most [max] bytes of UTF-8, never splitting a character. */
        internal fun String.clipBytes(max: Int): String {
            var s = this
            while (s.encodeToByteArray().size > max) s = s.dropLast(if (s.last().isLowSurrogate()) 2 else 1)
            return s
        }
    }

    @Serializable
    private class Answer(
        val origin: String = "",
        val revision: Long? = null,
        val authors: List<String> = emptyList(),
        val lines: List<WireLine> = emptyList(),
    )

    @Serializable
    private class WireLine(
        @SerialName("time_ms") val timeMs: Long,
        @SerialName("end_time_ms") val endTimeMs: Long? = null,
        val text: String = "",
        val words: List<WireWord> = emptyList(),
        val agent: String? = null,
    )

    @Serializable
    private class WireWord(
        val text: String,
        @SerialName("start_ms") val startMs: Long,
        val bg: Boolean = false,
    )
}
