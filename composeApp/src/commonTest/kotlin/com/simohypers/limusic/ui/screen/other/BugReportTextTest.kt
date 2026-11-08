package com.simohypers.limusic.ui.screen.other

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BugReportTextTest {
    @Test
    fun redactionStripsWhatAPublicIssueMustNotCarry() {
        val log =
            """
            Limusic 1.0.0 · Nothing A069P, Android 16 (SDK 36)
            Settings: signed in on, quality HIGH, speed 1.0, pitch 0, player style Classic

            2026-11-09T10:00:00.000Z I/Net: cookie: SAPISID=aB3dEfGhIjKlMnOpQr; __Secure-3PAPISID=zZ9
            2026-11-09T10:00:01.000Z E/Stream: GET https://rr5---sn-abc.googlevideo.com/videoplayback?pot=AbCdEf&ip=203.0.113.9&sig=xyz failed
            2026-11-09T10:00:02.000Z I/Auth: visitorData=CgtabcdefghijklmnopqrstuvwxyzABCDEFGHIJ0123456789
            2026-11-09T10:00:03.000Z I/PoToken: minted, sapisid: Qw3rTy
            2026-11-09T10:00:04.000Z W/Stream: dQw4w9WgXcQ from network via 127.0.0.1:8080, server 198.51.100.7
            """.trimIndent()
        val out = redactSecrets(log)
        for (leaked in listOf("aB3dEfGhIjKlMnOpQr", "zZ9", "AbCdEf", "203.0.113.9", "0123456789", "Qw3rTy", "198.51.100.7")) {
            assertFalse(leaked in out, "$leaked survived redaction:\n$out")
        }
        for (kept in listOf(
            "Limusic 1.0.0 · Nothing A069P, Android 16 (SDK 36)",
            "speed 1.0, pitch 0, player style Classic",
            "I/PoToken: minted",
            "googlevideo.com/videoplayback",
            "dQw4w9WgXcQ",
            "127.0.0.1:8080",
            "2026-11-09T10:00:04.000Z W/Stream:",
        )) {
            assertTrue(kept in out, "$kept lost to redaction:\n$out")
        }
    }

    @Test
    fun logTailKeepsTheEndOnWholeLines() {
        val log = "first line\nsecond line\nthird line\n"
        assertEquals(log, logTail(log, 100))
        assertEquals("third line\n", logTail(log, 15))
        assertEquals("line", logTail("one long line", 4))
    }
}
