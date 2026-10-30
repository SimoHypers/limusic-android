/*
 * Ported from Metrolist (GPL-3.0, the same licence as this project).
 * Metrolist Project (C) 2026 — Licensed under GPL-3.0 | See git history for contributors
 */
package com.simohypers.limusic.listentogether

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlin.math.max

/**
 * Maps the server's wall clock onto this device's monotonic clock.
 *
 * Limusic's server stamps times with its wall clock and its pong carries none, so the offset is set
 * once per connection from a server time read at that moment (the device's own wall clock, as on
 * desktop). [elapsedRealtime] must be MONOTONIC so a later NTP step cannot make [positionAt] jump.
 */
internal class ServerClock(
    private val elapsedRealtime: () -> Long,
) {
    private val lock = SynchronizedObject()
    private var serverOffsetMs: Long? = null

    /** Sets the offset from a server time read just now. */
    fun assumeServerTime(serverNowMs: Long) = synchronized(lock) { serverOffsetMs = serverNowMs - elapsedRealtime() }

    /** Server wall time now, or null until the clock has been set. */
    fun now(): Long? = synchronized(lock) { serverOffsetMs?.let { elapsedRealtime() + it } }

    /** [position] advanced by the time since [effectiveAtServerTime], while playing and once the clock is set. */
    fun positionAt(
        position: Long,
        effectiveAtServerTime: Long?,
        isPlaying: Boolean,
    ): Long {
        if (!isPlaying || effectiveAtServerTime == null || effectiveAtServerTime <= 0L) return position
        val serverNow = now() ?: return position
        return position + max(0L, serverNow - effectiveAtServerTime)
    }
}
