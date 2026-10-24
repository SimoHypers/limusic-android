package com.maxrave.media3.exoplayer

import androidx.annotation.OptIn
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import org.junit.Assert.assertEquals
import org.junit.Test
import java.lang.reflect.Proxy

@OptIn(UnstableApi::class)
class DelegatingForwardingPlayerTest {
    /** The fields of a Player that swapDelegate reads, behind a proxy that answers nothing else. */
    private class FakePlayer(
        val state: Int,
        val playWhenReady: Boolean,
    ) {
        val listeners = mutableListOf<Player.Listener>()
        val player =
            Proxy.newProxyInstance(Player::class.java.classLoader, arrayOf(Player::class.java)) { proxy, method, args ->
                when (method.name) {
                    "getPlaybackState" -> state
                    "getPlayWhenReady" -> playWhenReady
                    "isPlaying" -> playWhenReady && state == Player.STATE_READY
                    "addListener" -> listeners.add(args[0] as Player.Listener).let { null }
                    "removeListener" -> listeners.remove(args[0] as Player.Listener).let { null }
                    "equals" -> proxy === args[0]
                    "hashCode" -> System.identityHashCode(proxy)
                    else -> null
                }
            } as Player
    }

    /** Remembers state the way MediaSession does: only from what the events say. */
    private class SessionLikeListener : Player.Listener {
        var state = Player.STATE_IDLE
        var playWhenReady = false
        var isPlaying = false

        override fun onPlaybackStateChanged(playbackState: Int) {
            state = playbackState
        }

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            this.playWhenReady = playWhenReady
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            this.isPlaying = isPlaying
        }
    }

    @Test
    fun swapToAPrecachedPlayerReplacesTheEndedState() {
        val ended = FakePlayer(Player.STATE_ENDED, playWhenReady = true)
        val forwarding = DelegatingForwardingPlayer(ended.player)
        val session = SessionLikeListener()
        forwarding.addListener(session)
        ended.listeners.forEach { it.onPlaybackStateChanged(Player.STATE_ENDED) }

        val precached = FakePlayer(Player.STATE_READY, playWhenReady = true)
        forwarding.swapDelegate(precached.player)

        assertEquals(Player.STATE_READY, session.state)
        assertEquals(true, session.playWhenReady)
        assertEquals(true, session.isPlaying)
        assertEquals(1, precached.listeners.size)
        assertEquals(0, ended.listeners.size)
    }
}
