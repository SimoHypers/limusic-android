package com.maxrave.kotlinytmusicscraper.extractor

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private const val BASE = "https://rr1---sn-x.googlevideo.com/videoplayback?expire=1&mn=sn-a%2Csn-b&n=Ab%3Dc&itag=251"

class StreamSigningTest {
    @Test
    fun cipherIsSplitAndSigned() {
        val cipher = parseSignatureCipher("s=AB%3DCD&sp=sig&url=https%3A%2F%2Frr1.googlevideo.com%2Fvideoplayback%3Fexpire%3D1")!!
        assertEquals(SignatureCipher("AB=CD", "sig", "https://rr1.googlevideo.com/videoplayback?expire=1"), cipher)
        assertEquals("https://rr1.googlevideo.com/videoplayback?expire=1&sig=X%3DY", cipher.signedUrl("X=Y"))
    }

    @Test
    fun signatureKeyDefaultsAndMissingPartsFail() {
        assertEquals("signature", parseSignatureCipher("s=a&url=https%3A%2F%2Fh%2Fv")!!.sp)
        assertNull(parseSignatureCipher("sp=sig&url=https%3A%2F%2Fh%2Fv"))
        assertNull(parseSignatureCipher("s=a&sp=sig"))
    }

    @Test
    fun onlyTheRealNParameterIsReplaced() {
        assertEquals("Ab=c", nChallengeOf(BASE))
        // `mn=` ends in "n=" too and must survive, encoding and all.
        assertEquals(
            "https://rr1---sn-x.googlevideo.com/videoplayback?expire=1&mn=sn-a%2Csn-b&n=solved_-1&itag=251",
            withN(BASE, "solved_-1"),
        )
        assertNull(nChallengeOf("https://h/v?mn=x"))
        assertEquals("https://h/v?mn=x", withN("https://h/v?mn=x", "y"))
    }
}

class ExtractSourceTest {
    @Test
    fun savedUrlNamesItsClient() {
        assertEquals("WEB_REMIX · PoToken", ExtractSource.fromStreamUrl("https://h/videoplayback?sc=x&c=WEB_REMIX&n=a&pot=b"))
        assertEquals("VISIONOS", ExtractSource.fromStreamUrl("https://h/videoplayback?c=VISIONOS&spot=1"))
        assertNull(ExtractSource.fromStreamUrl("https://h/videoplayback?sc=x"))
    }
}
