package com.simohypers.limusic.discord

import com.simohypers.limusic.domain.manager.DataStoreManager
import com.simohypers.limusic.ktorext.getEngine
import com.simohypers.limusic.logger.Logger
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Parameters
import io.ktor.http.encodeURLParameter
import io.ktor.http.isSuccess
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import okio.ByteString.Companion.encodeUtf8
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private const val TAG = "Discord"

data class DiscordTokens(
    val access: String,
    val refresh: String,
    /** Epoch seconds. */
    val expiresAt: Long,
)

data class DiscordUser(
    val name: String,
    val username: String,
    val avatar: String?,
)

/**
 * Discord sign-in with OAuth2 + PKCE, Metrolist's route: Discord's own "Authorize Limusic" page in
 * the browser, back through [REDIRECT], for a token that may only set presence. No password or
 * account token ever passes through the app, and no client secret ships in it (the Discord app is
 * a public client).
 */
object DiscordOAuth {
    /** Registered on the Discord application under OAuth2 > Redirects, character for character. */
    const val REDIRECT = "limusic://discord-auth"
    private const val SCOPES = "openid sdk.social_layer_presence"
    private const val TOKEN_URL = "https://discord.com/api/v10/oauth2/token"

    private val http =
        HttpClient(getEngine()) {
            install(HttpTimeout) { requestTimeoutMillis = 15_000 }
        }
    private val json = Json { ignoreUnknownKeys = true }

    /** Verifier and state of the sign-in in flight.
     *  ponytail: in memory, so a process killed while the browser is open means signing in again. */
    private var pending: Pair<String, String>? = null

    @OptIn(ExperimentalUuidApi::class)
    fun authorizeUrl(): String {
        // Uuid.random() draws from a secure RNG: 256 bits, 64 unreserved characters, a valid verifier.
        val verifier = Uuid.random().toHexString() + Uuid.random().toHexString()
        val state = Uuid.random().toHexString()
        pending = verifier to state
        val challenge = verifier.encodeUtf8().sha256().base64Url().trimEnd('=')
        return "https://discord.com/oauth2/authorize?client_id=$APP_ID&response_type=code" +
            "&redirect_uri=${REDIRECT.encodeURLParameter()}&scope=${SCOPES.encodeURLParameter()}" +
            "&state=$state&code_challenge_method=S256&code_challenge=$challenge"
    }

    /** Trade the code from the redirect for tokens. Null when it came from no sign-in of ours. */
    suspend fun complete(
        code: String,
        state: String?,
    ): DiscordTokens? {
        val (verifier, expected) = pending ?: return null
        if (state != expected) {
            Logger.w(TAG, "oauth: state mismatch")
            return null
        }
        pending = null
        return exchange(
            "authorization_code",
            "code" to code,
            "redirect_uri" to REDIRECT,
            "code_verifier" to verifier,
        ).getOrNull()
    }

    /** Success(null) means Discord refused the refresh token (revoked); failure is the network. */
    suspend fun refresh(refreshToken: String): Result<DiscordTokens?> = exchange("refresh_token", "refresh_token" to refreshToken)

    @OptIn(ExperimentalTime::class)
    private suspend fun exchange(
        grant: String,
        vararg params: Pair<String, String>,
    ): Result<DiscordTokens?> =
        runCatching {
            val resp =
                http.submitForm(
                    TOKEN_URL,
                    Parameters.build {
                        append("client_id", APP_ID)
                        append("grant_type", grant)
                        params.forEach { (k, v) -> append(k, v) }
                    },
                )
            val body = json.parseToJsonElement(resp.bodyAsText()).jsonObject
            if (!resp.status.isSuccess()) {
                val error = body.str("error")
                Logger.w(TAG, "oauth $grant: ${resp.status.value} $error")
                if (error == "invalid_grant") return@runCatching null
                error(error ?: "HTTP ${resp.status.value}")
            }
            DiscordTokens(
                access = body.str("access_token") ?: return@runCatching null,
                refresh = body.str("refresh_token").orEmpty(),
                expiresAt = Clock.System.now().epochSeconds + (body["expires_in"]?.jsonPrimitive?.longOrNull ?: 0),
            )
        }

    suspend fun user(accessToken: String): DiscordUser? =
        runCatching {
            val resp = http.get("https://discord.com/api/v10/users/@me") { header("Authorization", "Bearer $accessToken") }
            if (!resp.status.isSuccess()) return null
            val o = json.parseToJsonElement(resp.bodyAsText()).jsonObject
            val id = o.str("id") ?: return null
            val username = o.str("username") ?: return null
            DiscordUser(
                name = o.str("global_name") ?: username,
                username = username,
                avatar = o.str("avatar")?.let { "https://cdn.discordapp.com/avatars/$id/$it.png?size=128" },
            )
        }.getOrNull()

    private fun JsonObject.str(key: String): String? = get(key)?.jsonPrimitive?.contentOrNull
}

/**
 * The stored access token, refreshed first when [force] is set or it has under an hour left.
 * Null when signed out. A refused refresh signs out; a network failure hands back the old token.
 */
@OptIn(ExperimentalTime::class)
suspend fun DataStoreManager.discordAccessToken(force: Boolean = false): String? {
    val access = discordToken.first()
    val refresh = discordRefreshToken.first()
    if (access.isEmpty()) return null
    // A token from SimpMusic's WebView sign-in has no refresh token and is a whole account's
    // credential. Drop it; the user signs in again through OAuth.
    if (refresh.isEmpty()) {
        setDiscordTokens("", "", 0)
        return null
    }
    if (!force && discordTokenExpiresAt.first() - Clock.System.now().epochSeconds > 3_600) return access
    val result = DiscordOAuth.refresh(refresh)
    result.getOrNull()?.let {
        setDiscordTokens(it.access, it.refresh.ifEmpty { refresh }, it.expiresAt)
        return it.access
    }
    if (result.isSuccess) {
        Logger.w(TAG, "refresh refused, signing out of Discord")
        setDiscordTokens("", "", 0)
        return null
    }
    return access
}
