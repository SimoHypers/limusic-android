package com.simohypers.limusic.domain.repository

import com.simohypers.limusic.domain.data.model.listentogether.DeviceLink
import com.simohypers.limusic.domain.data.model.listentogether.LinkRemote
import com.simohypers.limusic.domain.data.model.listentogether.PairResult
import kotlinx.coroutines.flow.StateFlow

/**
 * The device link: this phone as a remote for Limusic desktop, and Continue here both ways. The
 * implementation owns the socket, the stored link and the player bridge.
 */
interface DeviceLinkRepository {
    val link: StateFlow<DeviceLink>

    /** MainActivity's onStart/onStop. The socket is open while paired and (in front or playing). */
    fun setForeground(foreground: Boolean)

    /** Trade a code from the desktop for the link. [server] is empty for the default server. */
    suspend fun pair(
        server: String,
        code: String,
    ): PairResult

    fun unlink(deviceId: String)

    fun unlinkThisPhone()

    /** A control for the device that plays, or for [to]. */
    fun remote(
        action: LinkRemote,
        to: String? = null,
    )

    /** Play what the other device plays, from where it is now, with its queue. */
    fun continueHere()

    /**
     * Where the music plays, as a device picker: on this phone when [deviceId] is null; the device
     * that played last picks up its own song; any other takes over what this phone plays.
     */
    fun playOn(deviceId: String?)
}
