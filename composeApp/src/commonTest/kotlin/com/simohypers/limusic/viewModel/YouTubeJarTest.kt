package com.simohypers.limusic.viewModel

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class YouTubeJarTest {
    @Test
    fun sapisidIsReadFromEitherName() {
        assertEquals("abc/def", sapisid("PREF=x; SAPISID=abc/def; SID=1"))
        assertEquals("xyz", sapisid("__Secure-3PAPISID=xyz;PREF=x"))
    }

    @Test
    fun sapisidPrefersThePlainCookie() {
        assertEquals("plain", sapisid("__Secure-3PAPISID=secure; SAPISID=plain"))
    }

    @Test
    fun aJarWithoutSapisidIsNotSignedIn() {
        assertNull(sapisid("PREF=x; VISITOR_INFO1_LIVE=y"))
        assertNull(sapisid("SAPISID="))
        assertNull(sapisid(""))
        assertNull(sapisid(null))
        // A name that merely ends in SAPISID is another cookie.
        assertNull(sapisid("XSAPISID=nope"))
    }

    @Test
    fun aReorderedJarIsTheSameJar() {
        assertTrue(sameJar("A=1; B=2", "B=2;A=1"))
        assertFalse(sameJar("A=1; B=2", "A=1; B=3"))
    }
}
