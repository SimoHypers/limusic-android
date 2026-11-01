package com.simohypers.limusic.common

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow

/**
 * The phone's connection as Android reports it (the app starts the watch at launch; see
 * `watchConnectivity`). Everything that has to notice going offline, coming back, or moving from one
 * network to another (Wi-Fi to cellular, one Wi-Fi to another) reads it here.
 */
object Connectivity {
    data class State(
        /** A default network exists. False is airplane mode, no signal, Wi-Fi off with no data. */
        val hasNetwork: Boolean = true,
        /** Android checked that the network reaches the internet. A Wi-Fi that died while connected loses this. */
        val validated: Boolean = true,
        /** Bumped when the default network changes or gains validation: a connection opened before may be dead. */
        val generation: Int = 0,
    )

    // Online until Android says otherwise: a wrong "offline" would stop the app from even trying.
    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private var network: Long? = null

    /** The default network is now [network] (null: none), validated or not. */
    fun update(
        network: Long?,
        validated: Boolean,
    ) = synchronized(this) {
        val old = _state.value
        val fresh = network != null && (network != this.network || (validated && !old.validated))
        this.network = network
        _state.value = State(network != null, network != null && validated, if (fresh) old.generation + 1 else old.generation)
    }

    /** [network] went away. Only the default one's loss matters; an old one dropping after a switch does not. */
    fun lost(network: Long) =
        synchronized(this) {
            if (network == this.network) update(null, false)
        }

    /**
     * Emits each time a working connection appears after the moment of collection: back online, or
     * moved to another network. That is when requests that failed are worth making again, and
     * when connections opened on the old network should be let go.
     */
    fun reconnects(): Flow<Unit> =
        flow {
            var seen = state.value.takeIf { it.validated }?.generation
            state.collect {
                if (it.validated && it.generation != seen) {
                    seen = it.generation
                    emit(Unit)
                }
            }
        }
}
