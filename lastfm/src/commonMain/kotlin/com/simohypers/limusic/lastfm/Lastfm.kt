package com.simohypers.limusic.lastfm

import com.simohypers.limusic.logger.Logger

// LAST.FM build: the real scrobbler. `lastfm-empty` mirrors every declaration in this file as a
// no-op, so callers never branch on the build flavour — see `cast` / `cast-empty` for the same
// arrangement.

/** A logged-in Last.fm account. The session key has no expiry; the user revokes it from last.fm. */
data class LastfmSession(
    val username: String,
    val sessionKey: String,
)

/** What the app knows about a track, in the shape Last.fm's parameters expect. */
data class LastfmTrack(
    val artist: String,
    val track: String,
    val album: String? = null,
    val albumArtist: String? = null,
    val durationSeconds: Int? = null,
)

/**
 * The outcome of a write call.
 *
 * [Ignored] is the case worth spelling out: Last.fm answers `status="ok"` even when it threw the
 * scrobble away, and only says so in `ignoredMessage`. Treating a 200 as success would hide bad
 * metadata forever — codes 1 and 2 mean the artist or track name failed Last.fm's filter, which is
 * exactly what a polluted artist like "13M plays" produces.
 */
sealed interface LastfmOutcome {
    data object Ok : LastfmOutcome

    data class Ignored(
        val code: Int,
        val message: String,
    ) : LastfmOutcome

    data class Error(
        val code: Int,
        val message: String,
    ) : LastfmOutcome {
        /** Code 9 means the stored session key is dead — the user has to log in again. */
        val needsReauth: Boolean get() = code == ERROR_INVALID_SESSION

        /** 11 offline, 16 temporary, 29 rate limited. Every other code is a malformed request. */
        val retryable: Boolean get() = code == 11 || code == 16 || code == 29
    }

    companion object {
        const val ERROR_INVALID_SESSION = 9
    }
}

private const val TAG = "Lastfm"

private var apiKey: String = ""
private var sharedSecret: String = ""

/**
 * Hands the module its credentials.
 *
 * They arrive from the caller rather than living here because BuildKonfig generates into
 * `composeApp`, which depends on this module and not the other way round — the same reason
 * `configCrashlytics` takes the Sentry DSN as a parameter.
 */
fun configLastfm(
    key: String,
    secret: String,
) {
    apiKey = key
    sharedSecret = secret
    Logger.d(TAG, "Last.fm configured: ${if (isLastfmAvailable()) "credentials present" else "no credentials"}")
}

/**
 * Whether this build can talk to Last.fm at all.
 *
 * False in a FOSS build, where the empty module answers for this function, and also false in a full
 * build whose `local.properties` carries no key — so the settings entry hides itself rather than
 * offering a login that could never succeed.
 */
fun isLastfmAvailable(): Boolean = apiKey.isNotEmpty() && sharedSecret.isNotEmpty()

/** Where a sign-in stands while the user approves it in the browser. */
sealed interface LastfmAuth {
    data class Approved(
        val session: LastfmSession,
    ) : LastfmAuth

    /** Not approved yet, or Last.fm was briefly unreachable: ask again. */
    data object Pending : LastfmAuth

    data class Failed(
        val message: String,
    ) : LastfmAuth
}

/**
 * Step one of desktop's flow: a request token, valid for 60 minutes. The app opens [authorizeUrl]
 * with it and polls [checkApproval] until the user approves. Unlike Last.fm's web flow, no callback
 * URL has to be registered on the API account, so desktop's key works here unchanged.
 */
suspend fun requestToken(): String? {
    if (!isLastfmAvailable()) return null
    return LastfmClient.getToken(apiKey, sharedSecret)
}

fun authorizeUrl(token: String): String = "https://www.last.fm/api/auth/?api_key=$apiKey&token=$token"

/** Step two, repeated: has the user approved [token] yet? */
suspend fun checkApproval(token: String): LastfmAuth = LastfmClient.getSession(apiKey, sharedSecret, token)

/** Tells Last.fm what is playing right now. Does not affect charts, and is not worth retrying. */
suspend fun updateNowPlaying(
    sessionKey: String,
    track: LastfmTrack,
): LastfmOutcome {
    if (!isLastfmAvailable()) return LastfmOutcome.Error(0, "Last.fm is not configured")
    return LastfmClient.updateNowPlaying(apiKey, sharedSecret, sessionKey, track)
}

/**
 * Records a play.
 *
 * [startedAtEpochSeconds] is when playback STARTED. Last.fm's own docs disagree with themselves
 * here — the method page says the track started, the scrobbling guide says it finished — and every
 * scrobbler in the wild sends the start time, so that is what this takes.
 */
suspend fun scrobble(
    sessionKey: String,
    track: LastfmTrack,
    startedAtEpochSeconds: Long,
): LastfmOutcome {
    if (!isLastfmAvailable()) return LastfmOutcome.Error(0, "Last.fm is not configured")
    return LastfmClient.scrobble(apiKey, sharedSecret, sessionKey, track, startedAtEpochSeconds)
}

/** A public profile, for the Last.fm page's account header. */
data class LastfmProfile(
    /** The avatar, or null for an account that never set one. */
    val image: String?,
    val url: String?,
    val scrobbles: Long,
    val artists: Long,
    val tracks: Long,
    /** Epoch seconds the account was created. */
    val since: Long,
)

/** Null when Last.fm didn't answer: the page falls back to the name it already has. */
suspend fun profile(username: String): LastfmProfile? {
    if (!isLastfmAvailable()) return null
    return LastfmClient.getProfile(apiKey, sharedSecret, username)
}

/** Mirrors a like onto Last.fm's loved tracks. */
suspend fun setLoved(
    sessionKey: String,
    artist: String,
    track: String,
    loved: Boolean,
): LastfmOutcome {
    if (!isLastfmAvailable()) return LastfmOutcome.Error(0, "Last.fm is not configured")
    return LastfmClient.setLoved(apiKey, sharedSecret, sessionKey, artist, track, loved)
}
