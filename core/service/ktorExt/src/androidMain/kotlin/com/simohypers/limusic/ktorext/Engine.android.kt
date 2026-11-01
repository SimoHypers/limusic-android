package com.simohypers.limusic.ktorext

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.engine.okhttp.OkHttpConfig
import okhttp3.ConnectionPool

/**
 * One connection pool under every Ktor client, so that moving to another network can let go of the
 * connections opened on the old one in one call (see [dropIdleConnections]). Only the pool is
 * shared: a shared OkHttpClient would share its dispatcher too, and Ktor shuts a client's dispatcher
 * down whenever any HttpClient on it is closed.
 */
private val sharedPool = ConnectionPool()

private object PooledOkHttp : HttpClientEngineFactory<OkHttpConfig> {
    override fun create(block: OkHttpConfig.() -> Unit): HttpClientEngine =
        OkHttp.create {
            config { connectionPool(sharedPool) }
            block()
        }
}

actual fun getEngine(): HttpClientEngineFactory<HttpClientEngineConfig> = PooledOkHttp

/**
 * Closes the idle connections of every Ktor client. After a switch from Wi-Fi to cellular (or
 * between two Wi-Fis) they lead to the old network, and a request sent down one waits out its whole
 * timeout before anything notices.
 */
fun dropIdleConnections() = sharedPool.evictAll()
