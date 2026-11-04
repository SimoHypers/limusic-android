package com.simohypers.limusic.viewModel

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// A card menu decides Add or Remove from sameAs, so a playlist pinned from Library must match its feed card.
class ShortcutTest {
    @Test
    fun samePlaylistAcrossLibraryAndFeed() {
        val pinned = Shortcut(Shortcut.Kind.OWN_PLAYLIST, "VLPL123", "Mine")
        assertTrue(pinned.sameAs(Shortcut(Shortcut.Kind.PLAYLIST, "PL123", "Mine")))
        assertTrue(Shortcut(Shortcut.Kind.PLAYLIST, "PL123", "a").sameAs(Shortcut(Shortcut.Kind.PLAYLIST, "PL123", "b")))
    }

    @Test
    fun differentKindsOrIdsDoNotMatch() {
        assertFalse(Shortcut(Shortcut.Kind.ALBUM, "MPRE1", "x").sameAs(Shortcut(Shortcut.Kind.PLAYLIST, "MPRE1", "x")))
        assertFalse(Shortcut(Shortcut.Kind.PLAYLIST, "PL1", "x").sameAs(Shortcut(Shortcut.Kind.PLAYLIST, "PL2", "x")))
        assertFalse(Shortcut(Shortcut.Kind.LOCAL_PLAYLIST, "5", "x").sameAs(Shortcut(Shortcut.Kind.PLAYLIST, "5", "x")))
    }
}
