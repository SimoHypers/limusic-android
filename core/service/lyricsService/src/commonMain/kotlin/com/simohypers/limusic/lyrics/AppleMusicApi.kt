package com.simohypers.limusic.lyrics

import com.simohypers.limusic.ktorext.curl.CurlLogger
import com.simohypers.limusic.ktorext.encoding.brotli
import com.simohypers.limusic.ktorext.getEngine
import com.simohypers.limusic.logger.Logger
import io.ktor.client.HttpClient
import io.ktor.client.engine.ProxyConfig
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.headers
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import com.simohypers.limusic.lyrics.am.AMTokenManager

class AppleMusicApi {
    private var httpClient = createClient()
    var proxy: ProxyConfig? = null
        set(value) {
            field = value
            httpClient.close()
            httpClient = createClient()
        }

    private val amTokenManager = AMTokenManager()

    private fun createClient() =
        HttpClient(getEngine()) {
            expectSuccess = false
            followRedirects = true
            install(HttpCache)
            install(CurlLogger) {
                logger = { Logger.d("AppleMusicApi", it) }
            }
            install(HttpSend) {
                maxSendCount = 100
            }
            install(HttpCookies) {
                storage = AcceptAllCookiesStorage()
            }
            install(ContentNegotiation) {
                json(
                    Json {
                        prettyPrint = true
                        isLenient = true
                        ignoreUnknownKeys = true
                        explicitNulls = false
                        encodeDefaults = true
                    },
                )
            }
            install(ContentEncoding) {
                brotli(1.0F)
                gzip(0.9F)
                deflate(0.8F)
            }
            if (proxy != null) {
                engine {
                    proxy = this@AppleMusicApi.proxy
                }
            }
        }

    private fun HttpRequestBuilder.buildAMHeaders(token: String) {
        headers {
            header("Authorization", "Bearer $token")
            header("Origin", "https://music.apple.com")
            header("Referer", "https://music.apple.com/")
            header(
                HttpHeaders.UserAgent,
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:152.0) Gecko/20100101 Firefox/152.0",
            )
            header(HttpHeaders.Accept, "application/json")
        }
    }

    suspend fun searchAMArtist(
        name: String,
        limit: Int,
    ): HttpResponse {
        val response = requestAMSearch(name, limit, amTokenManager.getToken(httpClient))
        // Expired bearer -> refresh the token once and retry on a fresh request.
        if (response.status.value == 401) {
            amTokenManager.clearToken()
            return requestAMSearch(name, limit, amTokenManager.getToken(httpClient))
        }
        return response
    }

    private suspend fun requestAMSearch(
        name: String,
        limit: Int,
        token: String,
    ) = httpClient.get("https://amp-api-edge.music.apple.com/v1/catalog/us/search") {
        parameter("term", name)
        parameter("types", "artists")
        parameter("fields[artists]", "url,name,artwork")
        parameter("art[url]", "f")
        parameter("format[resources]", "map")
        parameter("extend", "artistUrl")
        parameter("l", "en-US")
        parameter("limit", limit)
        parameter("platform", "web")
        buildAMHeaders(token)
    }

    suspend fun getAMArtist(id: String): HttpResponse {
        val response = requestAMArtist(id, amTokenManager.getToken(httpClient))
        // Expired bearer -> refresh the token once and retry on a fresh request.
        if (response.status.value == 401) {
            amTokenManager.clearToken()
            return requestAMArtist(id, amTokenManager.getToken(httpClient))
        }
        return response
    }

    private suspend fun requestAMArtist(
        id: String,
        token: String,
    ) = httpClient.get("https://amp-api.music.apple.com/v1/catalog/us/artists/$id") {
        parameter("art[url]", "c,f")
        // editorialVideo is what brings the animated artist artwork back. Without it named here
        // the field is simply absent from the response — the request still succeeds.
        parameter("extend", "editorialArtwork,editorialVideo,hero,keyColor")
        parameter("format[resources]", "map")
        parameter("l", "en-US")
        parameter("platform", "web")
        buildAMHeaders(token)
    }

    /**
     * Search the AM catalog for an album. `extend=editorialVideo` fills the animated artwork in
     * on the search response itself, so — unlike the artist flow — no per-album follow-up request
     * is needed. Mirrors [searchAMArtist], including the refresh-once-on-401 retry.
     */
    suspend fun searchAMAlbum(
        query: String,
        limit: Int,
    ): HttpResponse {
        val response = requestAMAlbumSearch(query, limit, amTokenManager.getToken(httpClient))
        // Expired bearer -> refresh the token once and retry on a fresh request.
        if (response.status.value == 401) {
            amTokenManager.clearToken()
            return requestAMAlbumSearch(query, limit, amTokenManager.getToken(httpClient))
        }
        return response
    }

    private suspend fun requestAMAlbumSearch(
        query: String,
        limit: Int,
        token: String,
    ) = httpClient.get("https://amp-api-edge.music.apple.com/v1/catalog/us/search") {
        parameter("term", query)
        parameter("types", "albums")
        // Narrowing to the fields actually read drops ~23% off the response without losing
        // editorialVideo, and this runs once per played track.
        parameter("fields[albums]", "name,artistName,url,artwork,editorialVideo")
        parameter("format[resources]", "map")
        parameter("extend", "editorialVideo")
        parameter("l", "en-US")
        parameter("limit", limit)
        parameter("platform", "web")
        buildAMHeaders(token)
    }

    /**
     * Find the album a *track* belongs to, and bring that album's animated artwork back with it.
     *
     * Searching `types=albums` with a track title cannot do this: AM answers with the singles that
     * share the title — "Blinding Lights - Single" rather than "After Hours" — and a single never
     * carries animated artwork, so the search succeeds while the result is always empty. Asking for
     * songs and including their albums returns the parent album itself, already extended.
     */
    suspend fun searchAMSongs(
        query: String,
        limit: Int,
    ): HttpResponse {
        val response = requestAMSongSearch(query, limit, amTokenManager.getToken(httpClient))
        // Expired bearer -> refresh the token once and retry on a fresh request.
        if (response.status.value == 401) {
            amTokenManager.clearToken()
            return requestAMSongSearch(query, limit, amTokenManager.getToken(httpClient))
        }
        return response
    }

    private suspend fun requestAMSongSearch(
        query: String,
        limit: Int,
        token: String,
    ) = httpClient.get("https://amp-api-edge.music.apple.com/v1/catalog/us/search") {
        parameter("term", query)
        parameter("types", "songs")
        parameter("include[songs]", "albums")
        parameter("format[resources]", "map")
        parameter("extend", "editorialVideo")
        parameter("l", "en-US")
        parameter("limit", limit)
        parameter("platform", "web")
        buildAMHeaders(token)
    }

    /**
     * Fetch an animated-artwork playlist off Apple's media CDN. Deliberately without the AM headers:
     * this host takes no bearer, serves no DRM and needs no origin — sending them only invites the
     * edge to treat the request as something it is not.
     */
    suspend fun fetchAMHlsPlaylist(url: String): HttpResponse = httpClient.get(url)
}
