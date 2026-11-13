package com.simohypers.limusic.lyrics

import com.simohypers.limusic.lyrics.LimusicLyrics.Companion.clipBytes
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LimusicLyricsTest {
    // An invented duet: two singers, a background echo, an instrumental break, and a field this
    // build doesn't know.
    private val answer =
        """
        {"video_id":"aaaaaaaaaaa","origin":"community","revision":7,"something_new":1,
         "authors":["carla","","moss_fern","a","b","c","d"],
         "lines":[
          {"time_ms":1000,"end_time_ms":2600,"text":"Paper lanterns (lanterns)","agent":"v1",
           "words":[{"text":"Pa","start_ms":1000,"end_ms":1200},{"text":"per ","start_ms":1200,"end_ms":1500},
                    {"text":"lanterns ","start_ms":1500,"end_ms":2000},
                    {"text":"(lanterns)","start_ms":2100,"end_ms":2600,"bg":true}]},
          {"time_ms":3000,"text":"","words":[]},
          {"time_ms":4000,"end_time_ms":5000,"text":"On the river","agent":"v2",
           "words":[{"text":"On ","start_ms":4000,"end_ms":4300},{"text":"the ","start_ms":4300,"end_ms":4500},{"text":"river","start_ms":4500,"end_ms":5000}]}
         ]}
        """.trimIndent()

    @Test
    fun answerKeepsSingersAndBackgroundVocalsApart() {
        val l = LimusicLyrics.answerToLyric(answer)!!
        assertEquals("community", l.origin)
        assertEquals(7L, l.revision)
        assertEquals(listOf("carla", "moss_fern", "a", "b", "c"), l.authors, "five at most, no blanks")
        val lines = l.lyrics.lyrics!!.lines!!
        assertEquals("RICH_SYNCED", l.lyrics.lyrics!!.syncType)
        assertEquals("<00:01.000>Paper <00:01.500>lanterns", lines[0].words)
        assertEquals("<00:02.100>(lanterns)", lines[0].background)
        assertEquals("v1", lines[0].agent)
        assertEquals("" to null, lines[1].words to lines[1].background, "a break stays a timed empty line")
        assertEquals("v2", lines[2].agent)
        assertNull(lines[2].background)
    }

    @Test
    fun nothingToShowIsGone() {
        assertNull(LimusicLyrics.answerToLyric("""{"origin":"unison","revision":1,"lines":[]}"""))
        assertNull(LimusicLyrics.answerToLyric("not json"))
    }

    @Test
    fun failuresReadAsDesktopReadsThem() {
        fun f(
            status: HttpStatusCode,
            code: String?,
        ) = LimusicLyrics.failure(status to JsonObject(code?.let { mapOf("error" to JsonPrimitive(it)) }.orEmpty()))
        assertEquals("unavailable", LimusicLyrics.failure(null))
        assertEquals("unauthorized", f(HttpStatusCode.Unauthorized, "whatever"))
        assertEquals("rate_limited", f(HttpStatusCode.TooManyRequests, null))
        assertEquals("own", f(HttpStatusCode.Forbidden, "own"))
        assertEquals("unavailable", f(HttpStatusCode.BadGateway, "own"))
    }

    @Test
    fun idsAndClipping() {
        assertTrue(LimusicLyrics.plausible("aB3_-aB3_-x"))
        assertFalse(LimusicLyrics.plausible("aB3_-aB3_-"))
        assertFalse(LimusicLyrics.plausible("aB3_-aB3_é1"))
        assertEquals("ab", "abc".clipBytes(2))
        // "é" is two bytes and the emoji four: never half of either.
        assertEquals("aé", "aéé".clipBytes(4))
        assertEquals("a", "a🎵".clipBytes(4))
    }
}
