package com.simohypers.limusic.common

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities

/** Feeds [Connectivity] from Android's default network for the life of the process. Call once, at launch. */
fun watchConnectivity(context: Context) {
    val manager = context.getSystemService(ConnectivityManager::class.java) ?: return
    fun validated(capabilities: NetworkCapabilities?) =
        capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
    // Seeded now, so nothing reads the optimistic default once the app is running.
    val active = manager.activeNetwork
    Connectivity.update(active?.networkHandle, validated(manager.getNetworkCapabilities(active)))
    manager.registerDefaultNetworkCallback(
        object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities,
            ) = Connectivity.update(network.networkHandle, validated(networkCapabilities))

            override fun onLost(network: Network) = Connectivity.lost(network.networkHandle)
        },
    )
}
