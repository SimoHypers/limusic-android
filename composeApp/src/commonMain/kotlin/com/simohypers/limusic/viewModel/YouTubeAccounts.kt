package com.simohypers.limusic.viewModel

import com.simohypers.limusic.common.Config
import com.simohypers.limusic.domain.data.entities.GoogleAccountEntity
import com.simohypers.limusic.domain.data.model.account.AccountInfo
import com.simohypers.limusic.domain.extension.toNetScapeString
import com.simohypers.limusic.domain.manager.DataStoreManager
import com.simohypers.limusic.domain.repository.AccountRepository
import com.simohypers.limusic.domain.repository.CommonRepository
import com.simohypers.limusic.logger.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TimeMark
import kotlin.time.TimeSource
import limusic.composeapp.generated.resources.Res
import limusic.composeapp.generated.resources.session_expired_sign_in_again
import limusic.composeapp.generated.resources.sign_in_failed
import limusic.composeapp.generated.resources.signed_in_as
import limusic.composeapp.generated.resources.switched_to
import multiplatform.network.cmptoast.ToastDuration
import multiplatform.network.cmptoast.showToast
import org.jetbrains.compose.resources.getString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.lastOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * The YouTube accounts, as desktop Limusic keeps them (AccountMenu, state.rs `sign_in`): one saved
 * row per Google sign-in (one cookie jar), carrying the channel picked on it. Sign out drops to
 * guest and keeps the row, so coming back is one tap; [remove] is what forgets one.
 *
 * A row's id (the `email` column) is the channel's handle, or `ga-<hash of SAPISID>` for a channel
 * without one. Two rows for one jar never coexist: the jar's SAPISID is what tells logins apart.
 * One shared instance, so Home, Settings and the sign-in screen read the same state.
 */
class YouTubeAccounts(
    private val dataStoreManager: DataStoreManager,
    private val accountRepository: AccountRepository,
    private val commonRepository: CommonRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // One change at a time: a switch racing a sign-out would leave the transport on one account
    // and the rows on another.
    private val turn = Mutex()

    private val _saved = MutableStateFlow<List<GoogleAccountEntity>>(emptyList())

    /** Every saved account, the active one included. */
    val saved: StateFlow<List<GoogleAccountEntity>> = _saved

    /** The account requests go out as; null when signed out. */
    val active: StateFlow<GoogleAccountEntity?> =
        combine(dataStoreManager.cookie, _saved) { cookie, rows ->
            if (cookie.isEmpty()) null else rows.firstOrNull { it.isUsed && sapisid(it.cache) == sapisid(cookie) }
        }.stateIn(scope, SharingStarted.Eagerly, null)

    init {
        scope.launch { turn.withLock { tidy() } }
        scope.launch { heal() }
    }

    /**
     * Desktop's session refresh (session.rs `refresh_session`, KI-2): the saved cookie is a snapshot
     * and Google rotates its short-lived tokens out from under it, until every signed-in request is
     * turned down. The sign-in WebView's own jar is live, so loading music.youtube.com in it hands
     * out fresh cookies, which replace the saved ones when they are still the same account.
     * One at a time (collect runs them in turn) and at most once per five minutes: a dead cookie
     * throws a burst of 401s, all one thing to fix.
     */
    private suspend fun heal() {
        var last: TimeMark? = null
        accountRepository.authRejected.collect {
            if (last?.let { it.elapsedNow() < 5.minutes } == true) return@collect
            val live = dataStoreManager.cookie.first()
            if (sapisid(live) == null) return@collect
            last = TimeSource.Monotonic.markNow()
            val fresh = freshYouTubeJar(live)
            if (fresh == null) {
                Logger.d("Auth", "YouTube: the sign-in WebView had nothing fresher")
                return@collect
            }
            replaceCookie(fresh)
            Logger.i("Auth", "YouTube: re-minted the session from the sign-in WebView")
        }
    }

    sealed interface SignIn {
        data object Done : SignIn

        /** The jar can act as several channels and none was picked on it before. */
        data class PickChannel(
            val channels: List<AccountInfo>,
        ) : SignIn

        data object Failed : SignIn
    }

    /**
     * Sign in with a jar from the sign-in WebView. One channel finishes at once, and so does a jar
     * whose channel was picked before (signing back in after Sign out). Otherwise the caller shows
     * the picker and finishes with [pickChannel]; nothing is saved until then.
     */
    suspend fun signIn(cookie: String): SignIn =
        op {
            val key = sapisid(cookie)
            val channels = channelsOf(cookie)
            if (key == null || channels.isNullOrEmpty()) {
                Logger.w("Auth", "YouTube: sign-in failed, the jar lists no account")
                toast(getString(Res.string.sign_in_failed))
                return@op SignIn.Failed
            }
            val before = load().firstOrNull { sapisid(it.cache) == key }
            val chosen =
                before?.let { row -> channels.firstOrNull { it.pageId == row.pageId && it.authUser == row.authUser } }
                    ?: channels.singleOrNull()
            if (chosen == null) return@op SignIn.PickChannel(channels)
            save(cookie, chosen, fromWebView = true)
            Logger.i("Auth", "YouTube: signed in")
            toast(getString(Res.string.signed_in_as, chosen.name))
            SignIn.Done
        }

    suspend fun pickChannel(
        cookie: String,
        channel: AccountInfo,
    ) = op {
        save(cookie, channel, fromWebView = true)
        Logger.i("Auth", "YouTube: signed in, channel picked")
        toast(getString(Res.string.signed_in_as, channel.name))
    }

    /**
     * Make a saved account the active one, no WebView. Its jar is checked first: Google rotates
     * its short-lived tokens, and switching to a dead session would leave the app half signed in.
     * False when YouTube no longer takes it; the current session is left as it was.
     */
    suspend fun switchTo(row: GoogleAccountEntity): Boolean =
        op {
            val channels = row.cache?.let { channelsOf(it) }
            val channel =
                channels?.firstOrNull { it.pageId == row.pageId && it.authUser == row.authUser }
                    ?: channels?.firstOrNull()
            if (channel == null) {
                Logger.w("Auth", "YouTube: saved account no longer signs in")
                toast(getString(Res.string.session_expired_sign_in_again))
                return@op false
            }
            save(row.cache!!, channel, fromWebView = false)
            Logger.i("Auth", "YouTube: switched account")
            toast(getString(Res.string.switched_to, channel.name))
            true
        }

    /** The channels the live jar can act as, fetched fresh for the picker. Null if YouTube didn't answer. */
    suspend fun channels(): List<AccountInfo>? = channelsOf(dataStoreManager.cookie.first())?.takeIf { it.isNotEmpty() }

    suspend fun switchChannel(channel: AccountInfo) =
        op {
            val cookie = dataStoreManager.cookie.first().takeIf { it.isNotEmpty() } ?: return@op
            save(cookie, channel, fromWebView = false)
            Logger.i("Auth", "YouTube: switched channel")
            toast(getString(Res.string.switched_to, channel.name))
        }

    /** Back to guest. The saved row stays, one tap from coming back. */
    suspend fun signOut() =
        op {
            clearSession()
            load()
            Logger.i("Auth", "YouTube: signed out")
        }

    /** Forget a saved account. Removing the active one signs out too. */
    suspend fun remove(row: GoogleAccountEntity) =
        op {
            val wasActive = active.value?.email == row.email
            accountRepository.deleteGoogleAccount(row.email)
            if (wasActive) clearSession()
            load()
            Logger.i("Auth", "YouTube: removed a saved account")
        }

    /**
     * The jar the silent refresh brought back, for the account it already is. Only the cookie
     * changes; a jar for a different account is refused, or the app would move behind the user.
     */
    suspend fun replaceCookie(fresh: String) =
        op {
            val live = dataStoreManager.cookie.first()
            if (sapisid(fresh) == null || sapisid(fresh) != sapisid(live)) return@op
            val row = _saved.value.firstOrNull { sapisid(it.cache) == sapisid(live) }
            row?.let { accountRepository.insertGoogleAccount(it.copy(cache = fresh)).firstOrNull() }
            dataStoreManager.setCookie(fresh, dataStoreManager.pageId.first().ifEmpty { null }, dataStoreManager.authUser.first())
            load()
        }

    /**
     * One change at a time, and run to the end in this scope: a sheet closing mid-switch must not
     * cancel it between deleting the old row and writing the new one.
     */
    private suspend fun <T> op(block: suspend () -> T): T = scope.async { turn.withLock { block() } }.await()

    private suspend fun toast(message: String) =
        withContext(Dispatchers.Main) { showToast(message = message, duration = ToastDuration.Short) }

    private suspend fun save(
        cookie: String,
        channel: AccountInfo,
        fromWebView: Boolean,
    ) {
        val key = sapisid(cookie)
        val id = channel.email.ifEmpty { "ga-" + key.hashCode().toUInt().toString(16) }
        val rows = load()
        val before = rows.firstOrNull { sapisid(it.cache) == key }
        // The jar's old row, and the same channel saved earlier under another jar: both are this one now.
        rows.filter { sapisid(it.cache) == key || it.email == id }.forEach { accountRepository.deleteGoogleAccount(it.email) }
        rows.filter { it.isUsed }.forEach { accountRepository.updateGoogleAccountUsed(it.email, false).firstOrNull() }
        // yt-dlp's copy of the jar. Only the WebView's own store has it in Netscape form, and only
        // right after it signed in; a switch reuses the copy saved with the row.
        val netscape =
            if (fromWebView) {
                runCatching {
                    commonRepository.getCookiesFromInternalDatabase(Config.YOUTUBE_MUSIC_MAIN_URL, getPackageName()).toNetScapeString()
                }.getOrNull()
            } else {
                before?.netscapeCookie
            }
        val row =
            GoogleAccountEntity(
                email = id,
                name = channel.name,
                thumbnailUrl = photo(channel),
                pageId = channel.pageId,
                authUser = channel.authUser,
                cache = cookie,
                isUsed = true,
                netscapeCookie = netscape ?: before?.netscapeCookie,
            )
        accountRepository.insertGoogleAccount(row).firstOrNull()
        dataStoreManager.setCookie(cookie, channel.pageId, channel.authUser)
        dataStoreManager.setLoggedIn(true)
        // Projections other code still reads (playlist author); Home reads [active].
        dataStoreManager.putString("AccountName", row.name)
        dataStoreManager.putString("AccountThumbUrl", row.thumbnailUrl)
        row.netscapeCookie?.let { commonRepository.writeTextToFile(it, getFileDir() + "/ytdlp-cookie.txt") }
        load()
    }

    private suspend fun clearSession() {
        _saved.value.filter { it.isUsed }.forEach { accountRepository.updateGoogleAccountUsed(it.email, false).firstOrNull() }
        dataStoreManager.putString("AccountName", "")
        dataStoreManager.putString("AccountThumbUrl", "")
        dataStoreManager.setLoggedIn(false)
        dataStoreManager.setCookie("", null)
    }

    /**
     * SimpMusic saved a row per channel, several sharing one jar, and only saved any once Settings
     * asked. Rows sharing a jar fold into the one in use (or the first), rows that can't sign in go,
     * and a live jar with no row of its own gets one (a single account check, only in that state).
     */
    private suspend fun tidy() {
        val rows = load()
        rows.filter { sapisid(it.cache) == null }.forEach { accountRepository.deleteGoogleAccount(it.email) }
        rows.filter { sapisid(it.cache) != null }.groupBy { sapisid(it.cache) }.values.forEach { jar ->
            val keep = jar.firstOrNull { it.isUsed } ?: jar.first()
            jar.filter { it.email != keep.email }.forEach { accountRepository.deleteGoogleAccount(it.email) }
        }
        load()
        val cookie = dataStoreManager.cookie.first()
        if (sapisid(cookie) != null && _saved.value.none { it.isUsed && sapisid(it.cache) == sapisid(cookie) }) {
            val pageId = dataStoreManager.pageId.first().ifEmpty { null }
            val authUser = dataStoreManager.authUser.first()
            val channels = channelsOf(cookie) ?: return
            val channel = channels.firstOrNull { it.pageId == pageId && it.authUser == authUser } ?: channels.firstOrNull() ?: return
            save(cookie, channel, fromWebView = false)
        }
    }

    private suspend fun channelsOf(cookie: String): List<AccountInfo>? =
        if (sapisid(cookie) == null) null else accountRepository.getAccountInfo(cookie).lastOrNull()

    private suspend fun load(): List<GoogleAccountEntity> =
        accountRepository.getGoogleAccounts().firstOrNull().orEmpty().also { _saved.value = it }
}

/**
 * Load music.youtube.com in a hidden WebView and return its jar once it is a fresher one for the
 * same account as [current]; null when it isn't (another account's session, or none, or no page).
 */
internal expect suspend fun freshYouTubeJar(current: String): String?

/** Same cookies, whatever order they came out in. */
internal fun sameJar(
    a: String,
    b: String,
): Boolean {
    fun pairs(s: String) = s.split(';').map { it.trim() }.filter { it.isNotEmpty() }.sorted()
    return pairs(a) == pairs(b)
}

/** The jar's SAPISID: one per Google sign-in, so it tells two saved logins apart. Null = not signed in. */
internal fun sapisid(cookie: String?): String? {
    val parts = cookie?.split(';')?.map { it.trim() } ?: return null
    return listOf("SAPISID=", "__Secure-3PAPISID=")
        .firstNotNullOfOrNull { name -> parts.firstOrNull { it.startsWith(name) }?.removePrefix(name) }
        ?.takeIf { it.isNotEmpty() }
}

/** The channel photo at its largest. The switcher can hand out a scheme-relative URL, which Coil can't load. */
private fun photo(channel: AccountInfo): String =
    channel.thumbnails
        .lastOrNull()
        ?.url
        ?.let { if (it.startsWith("//")) "https:$it" else it }
        .orEmpty()
