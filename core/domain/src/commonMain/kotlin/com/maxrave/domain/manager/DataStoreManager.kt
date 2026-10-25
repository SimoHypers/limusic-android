package com.maxrave.domain.manager

import com.maxrave.domain.data.model.network.ProxyConfiguration
import kotlinx.coroutines.flow.Flow

interface DataStoreManager {
    val appVersion: Flow<String>

    suspend fun setAppVersion(version: String)

    val location: Flow<String>

    suspend fun setLocation(location: String)

    val quality: Flow<String>

    suspend fun setQuality(quality: String)

    val downloadQuality: Flow<String>

    suspend fun setDownloadQuality(quality: String)

    val videoDownloadQuality: Flow<String>

    suspend fun setVideoDownloadQuality(quality: String)

    val language: Flow<String>

    /**
     * Serialized "Moods & Genres" browse result, so the search and home screens can paint their
     * category grid immediately instead of waiting on the network every time. Null until the
     * first successful fetch.
     */
    val moodAndGenresCache: Flow<String?>

    suspend fun setMoodAndGenresCache(json: String)

    /**
     * Cover art resolved per browse category, keyed by its params. The category list itself
     * carries no artwork, so each cover costs one full category browse — worth remembering on
     * disk rather than paying again every time the search screen opens.
     */
    val moodArtworkCache: Flow<String?>

    suspend fun setMoodArtworkCache(json: String)

    fun getString(key: String): Flow<String?>

    suspend fun putString(
        key: String,
        value: String,
    )

    val loggedIn: Flow<String>
    val cookie: Flow<String>
    val pageId: Flow<String>
    val authUser: Flow<Int>

    suspend fun setCookie(
        cookie: String,
        pageId: String?,
        authUser: Int = 0,
    )

    suspend fun setLoggedIn(logged: Boolean)

    val normalizeVolume: Flow<String>

    suspend fun setNormalizeVolume(normalize: Boolean)

    val skipSilent: Flow<String>

    suspend fun setSkipSilent(skip: Boolean)

    val saveStateOfPlayback: Flow<String>

    suspend fun setSaveStateOfPlayback(save: Boolean)

    val shuffleKey: Flow<String>
    val repeatKey: Flow<String>

    suspend fun recoverShuffleAndRepeatKey(
        shuffle: Boolean,
        repeat: Int,
    )

    val saveRecentSongAndQueue: Flow<String>

    suspend fun setSaveRecentSongAndQueue(save: Boolean)

    val recentMediaId: Flow<String>
    val recentPosition: Flow<String>

    suspend fun saveRecentSong(
        mediaId: String,
        position: Long,
    )

    val playlistFromSaved: Flow<String>

    suspend fun setPlaylistFromSaved(playlist: String)

    val sendBackToGoogle: Flow<String>

    suspend fun setSendBackToGoogle(send: Boolean)


    /** Lyrics sources in the user's order, `-id` for one switched off; read with LyricsSource.order. */
    val lyricsProviders: Flow<String>

    suspend fun setLyricsProviders(order: String)

    val maxSongCacheSize: Flow<Int>

    suspend fun setMaxSongCacheSize(size: Int)

    val watchVideoInsteadOfPlayingAudio: Flow<String>

    suspend fun setWatchVideoInsteadOfPlayingAudio(watch: Boolean)

    /**
     * Whether a radio queue should carry audio only, dropping the video entries YouTube mixes in.
     *
     * Scoped to radios on purpose — it is not a global "hide every video" switch, so a playlist or
     * an album the user picked themselves still plays exactly what it contains.
     */
    val radioAudioOnly: Flow<String>

    suspend fun setRadioAudioOnly(audioOnly: Boolean)

    val playerVolume: Flow<Float>

    suspend fun setPlayerVolume(volume: Float)

    val videoQuality: Flow<String>

    suspend fun setVideoQuality(quality: String)

    /**
     * Whether following an artist in the app also subscribes to their YouTube channel.
     *
     * Off by default and only settable while signed in: Follow has always been local-only, and
     * writing to someone's account is not something to start doing without being asked.
     */
    /**
     * Ten equalizer band gains in dB, comma separated, or empty for flat.
     *
     * Stored as text rather than ten keys because the bands are only ever read and written
     * together — a curve is one setting, not ten.
     */
    /**
     * Whether the equalizer is applied at all.
     *
     * Separate from the curve so switching it off keeps the shape the user built — turning it back
     * on returns to their settings rather than to flat.
     */
    val equalizerEnabled: Flow<String>

    suspend fun setEqualizerEnabled(enabled: Boolean)

    val equalizerBands: Flow<String>

    suspend fun setEqualizerBands(bandsDb: List<Float>)

    /** Equalizer preamp in dB. Usually negative: it is what stops a boosted curve from clipping. */
    val equalizerPreamp: Flow<Float>

    suspend fun setEqualizerPreamp(preampDb: Float)

    /**
     * One of [EQUALIZER_TYPE_BUILT_IN], [EQUALIZER_TYPE_SYSTEM]. Android only — Desktop has no system
     * equalizer and always runs the built-in one. The two never run together.
     */
    val equalizerType: Flow<String>

    suspend fun setEqualizerType(type: String)

    /**
     * The AutoEq profile last imported, as `"<label>\n<comma-separated gains>"`.
     *
     * Label and curve share one key on purpose. The label is only shown while the equalizer still
     * holds exactly those gains, so storing them apart would create two values that have to agree
     * — and a moment during the write where they do not.
     */
    val equalizerAutoEqProfile: Flow<String>

    suspend fun setEqualizerAutoEqProfile(
        label: String,
        bandsDb: List<Float>,
    )

    /**
     * Animated album artwork from the hidden AM catalog. Needs no account. Also gates the AM
     * artist logo and motion header on artist pages, so with it off nothing is sent to AM.
     */
    val amAnimatedArtwork: Flow<String>

    suspend fun setAMAnimatedArtwork(enabled: Boolean)


    val homeLimit: Flow<Int>

    suspend fun setHomeLimit(limit: Int)

    val chartKey: Flow<String>

    suspend fun setChartKey(key: String)

    val usingProxy: Flow<String>

    suspend fun setUsingProxy(usingProxy: Boolean)

    val proxyType: Flow<ProxyType>

    suspend fun setProxyType(proxyType: ProxyType)

    val proxyHost: Flow<String>

    suspend fun setProxyHost(proxyHost: String)

    val proxyPort: Flow<Int>

    suspend fun setProxyPort(proxyPort: Int)

    val proxyUsername: Flow<String>

    suspend fun setProxyUsername(proxyUsername: String)

    val proxyPassword: Flow<String>

    suspend fun setProxyPassword(proxyPassword: String)

    fun getJVMProxy(): ProxyConfiguration?

    val endlessQueue: Flow<String>

    suspend fun setEndlessQueue(endlessQueue: Boolean)

    val keepYouTubePlaylistOffline: Flow<String>

    suspend fun setKeepYouTubePlaylistOffline(keep: Boolean)

    val combineLocalAndYouTubeLiked: Flow<String>

    suspend fun setCombineLocalAndYouTubeLiked(combine: Boolean)

    val shouldShowLogInRequiredAlert: Flow<String>

    suspend fun setShouldShowLogInRequiredAlert(shouldShow: Boolean)

    val playbackSpeed: Flow<Float>

    fun setPlaybackSpeed(speed: Float)

    val pitch: Flow<Int>

    fun setPitch(pitch: Int)

    val dataSyncId: Flow<String>

    suspend fun setDataSyncId(dataSyncId: String)

    val visitorData: Flow<String>

    suspend fun setVisitorData(visitorData: String)

    val localPlaylistFilter: Flow<String>

    suspend fun setLocalPlaylistFilter(filter: String)

    val killServiceOnExit: Flow<String>

    suspend fun setKillServiceOnExit(kill: Boolean)

    val crossfadeEnabled: Flow<String>

    suspend fun setCrossfadeEnabled(enabled: Boolean)

    val crossfadeDuration: Flow<Int>

    suspend fun setCrossfadeDuration(duration: Int)

    val crossfadeDjMode: Flow<String>

    suspend fun setCrossfadeDjMode(enabled: Boolean)

    /**
     * When on, transitions *between tracks of the same album* skip the crossfade, so an album that
     * was sequenced to run continuously keeps doing so. Edges still crossfade: the last album track
     * into whatever follows it fades normally. Off by default — it changes how crossfade behaves
     * for anyone already using it.
     */
    val crossfadeSkipAlbum: Flow<String>

    suspend fun setCrossfadeSkipAlbum(enabled: Boolean)

    /**
     * Keep library downloaded: the liked songs, library playlists and saved albums stay downloaded,
     * checked for new songs once a day, and a song liked meanwhile downloads straight away. Turning
     * it off stops that; nothing already downloaded goes.
     */
    val keepLibraryDownloaded: Flow<String>

    suspend fun setKeepLibraryDownloaded(enabled: Boolean)

    /** Downloads wait for an unmetered network (Wi-Fi) while on. Off by default. */
    val downloadWifiOnly: Flow<String>

    suspend fun setDownloadWifiOnly(enabled: Boolean)

    /**
     * Language code (e.g. "vi") of the audio track to prefer on videos that ship several — dubbed
     * podcasts, mostly. Empty means the original track. Deliberately NOT defaulted to the app
     * language: that would swap the speaker's own voice for an AI dub for every user whose app
     * language has one.
     */
    val preferredAudioLanguage: Flow<String>

    suspend fun setPreferredAudioLanguage(language: String)

    val backupDownloaded: Flow<String>

    suspend fun setBackupDownloaded(backupDownloaded: Boolean)

    /** One of [THEME_MODE_SYSTEM], [THEME_MODE_DARK], [THEME_MODE_LIGHT]. */
    val themeMode: Flow<String>

    suspend fun setThemeMode(mode: String)

    /** One of [THEME_COLOR_DEFAULT], [THEME_COLOR_WALLPAPER], [THEME_COLOR_CUSTOM]. */
    val themeColorSource: Flow<String>

    suspend fun setThemeColorSource(source: String)

    /** "Adapt colors to artwork": the playing cover's colour layered over the theme colour. */
    val artworkColors: Flow<String>

    suspend fun setArtworkColors(enable: Boolean)

    /** Seed color for the custom theme as an 8-digit ARGB hex string (e.g. "FF8ECAE6"). */
    val customThemeColor: Flow<String>

    suspend fun setCustomThemeColor(argbHex: String)

    /** One of [NOW_PLAYING_STYLE_SPOTIFY], [NOW_PLAYING_STYLE_M3_EXPRESSIVE], [NOW_PLAYING_STYLE_APPLE_MUSIC]. */
    val nowPlayingStyle: Flow<String>

    suspend fun setNowPlayingStyle(style: String)

    /**
     * One of [LYRICS_STYLE_CLASSIC], [LYRICS_STYLE_APPLE_MUSIC]. Deliberately independent of
     * [nowPlayingStyle]: it governs how a lyric line is drawn, everywhere lyrics are drawn —
     * the fullscreen sheet included, whichever player style is in use.
     */
    val lyricsStyle: Flow<String>

    suspend fun setLyricsStyle(style: String)

    /**
     * Which languages get a Latin-script reading shown for their lyrics, as a comma-separated list
     * of [org.simpmusic.lyrics.romanization.RomanizationLanguage] NAMES — empty string means the
     * feature is off, which is the default.
     *
     * Stored as names rather than ordinals because an enum's ordinal is a position, not an
     * identity: inserting a language into the middle of that enum would silently repoint every
     * saved preference. Stored as ONE key rather than twelve booleans so reading it is a single
     * flow rather than a combine of twelve.
     */
    val romanizationLanguages: Flow<String>

    suspend fun setRomanizationLanguages(languages: String)

    /**
     * How far the audio a listener actually HEARS lags the player's own position, in milliseconds.
     * Bluetooth is the reason this exists: the sink buffers, so at player position P the ear is
     * hearing P - offset, and every lyric display was lighting its line that much too early.
     *
     * Applied at READ time — a display picks its line from `position - offset` — so nothing is
     * written back into the cached [com.maxrave.domain.data.model.metadata.Line] rows, the
     * community lyrics database never sees a local correction, and a change lands on the next
     * frame instead of the next track.
     *
     * Signed and deliberately unbounded: positive pushes lyrics later (the Bluetooth case),
     * negative pulls them earlier, and how far is the listener's call. 0 by default.
     */
    val lyricsOffsetMs: Flow<Int>

    suspend fun setLyricsOffsetMs(offsetMs: Int)

    /** The lyrics page's text scale, one of desktop's steps (0.85 to 1.5). */
    val lyricsTextSize: Flow<Float>

    suspend fun setLyricsTextSize(scale: Float)

    /** The lyrics page's Romanized toggle; on by default, so the languages picked in Settings show. */
    val lyricsRomanized: Flow<Boolean>

    suspend fun setLyricsRomanized(on: Boolean)

    val explicitContentEnabled: Flow<String>

    suspend fun setExplicitContentEnabled(enabled: Boolean)

    /** Discord OAuth access token; empty when signed out. */
    val discordToken: Flow<String>

    val discordRefreshToken: Flow<String>

    /** Epoch seconds the access token expires at. */
    val discordTokenExpiresAt: Flow<Long>

    /** Written together; empty strings sign out. */
    suspend fun setDiscordTokens(
        access: String,
        refresh: String,
        expiresAt: Long,
    )

    val richPresenceEnabled: Flow<String>

    suspend fun setRichPresenceEnabled(enabled: Boolean)

    /** Last.fm session key. Has no expiry — it stays valid until the user revokes it on last.fm. */
    val lastfmSessionKey: Flow<String>

    /** The logged-in Last.fm username, kept only so settings can show whose account is connected. */
    val lastfmUsername: Flow<String>

    suspend fun setLastfmSession(
        sessionKey: String,
        username: String,
    )


    // Auto Backup
    val autoBackupEnabled: Flow<String>

    suspend fun setAutoBackupEnabled(enabled: Boolean)

    val autoBackupFrequency: Flow<String>

    suspend fun setAutoBackupFrequency(frequency: String)

    val autoBackupMaxFiles: Flow<Int>

    suspend fun setAutoBackupMaxFiles(max: Int)

    val autoBackupLastTime: Flow<Long>

    suspend fun setAutoBackupLastTime(time: Long)

    enum class ProxyType {
        PROXY_TYPE_HTTP,
        PROXY_TYPE_SOCKS,
    }

    companion object Values {
        const val REPEAT_MODE_OFF = "REPEAT_MODE_OFF"
        const val REPEAT_ONE = "REPEAT_ONE"
        const val REPEAT_ALL = "REPEAT_ALL"

        const val TRUE = "TRUE"
        const val FALSE = "FALSE"

        const val THEME_MODE_SYSTEM = "SYSTEM"
        const val THEME_MODE_DARK = "DARK"
        const val THEME_MODE_LIGHT = "LIGHT"

        const val THEME_COLOR_DEFAULT = "DEFAULT"
        const val THEME_COLOR_WALLPAPER = "WALLPAPER"
        const val THEME_COLOR_CUSTOM = "CUSTOM"

        const val DEFAULT_THEME_COLOR_HEX = "FFA50036"

        const val NOW_PLAYING_STYLE_SPOTIFY = "SPOTIFY"
        const val NOW_PLAYING_STYLE_M3_EXPRESSIVE = "M3_EXPRESSIVE"
        const val NOW_PLAYING_STYLE_APPLE_MUSIC = "APPLE_MUSIC"

        const val LYRICS_STYLE_CLASSIC = "CLASSIC"
        const val LYRICS_STYLE_APPLE_MUSIC = "APPLE_MUSIC"

        const val EQUALIZER_TYPE_BUILT_IN = "BUILT_IN"
        const val EQUALIZER_TYPE_SYSTEM = "SYSTEM"

        const val CROSSFADE_DURATION_AUTO = 0

        const val PROXY_TYPE_HTTP = "http"
        const val PROXY_TYPE_SOCKS = "socks"

        const val LOCAL_PLAYLIST_FILTER_OLDER_FIRST = "older_first"
        const val LOCAL_PLAYLIST_FILTER_NEWER_FIRST = "newer_first"
        const val LOCAL_PLAYLIST_FILTER_TITLE = "title"
        const val LOCAL_PLAYLIST_FILTER_CUSTOM_ORDER = "custom_order"

        // Auto Backup Frequency
        const val AUTO_BACKUP_FREQUENCY_DAILY = "daily"
        const val AUTO_BACKUP_FREQUENCY_WEEKLY = "weekly"
        const val AUTO_BACKUP_FREQUENCY_MONTHLY = "monthly"
    }
}