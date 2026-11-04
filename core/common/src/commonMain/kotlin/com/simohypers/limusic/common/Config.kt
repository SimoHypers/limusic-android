@file:Suppress("ktlint:standard:class-naming")

package com.simohypers.limusic.common

import com.simohypers.limusic.logger.Logger
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.Month

object Config {
    const val YOUTUBE_MUSIC_MAIN_URL = "https://music.youtube.com/"
    const val LOG_IN_URL =
        "https://accounts.google.com/ServiceLogin?ltmpl=music&service=youtube&uilel=3&passive=true&continue=https%3A%2F%2Fwww.youtube.com%2Fsignin%3Faction_handle_signin%3Dtrue%26app%3Ddesktop%26hl%3Den%26next%3Dhttps%253A%252F%252Fmusic.youtube.com%252F%26feature%3D__FEATURE__&hl=en"

    const val SONG_CLICK = "SONG_CLICK"
    const val VIDEO_CLICK = "VIDEO_CLICK"
    const val PLAYLIST_CLICK = "PLAYLIST_CLICK"
    const val ALBUM_CLICK = "ALBUM_CLICK"
    const val RADIO_CLICK = "RADIO_CLICK"
    const val MINIPLAYER_CLICK = "MINIPLAYER_CLICK"
    const val SHARE = "SHARE"
    const val RECOVER_TRACK_QUEUE = "RECOVER_TRACK_QUEUE"

    const val PLAYER_CACHE = "playerCache"
    const val DOWNLOAD_CACHE = "downloadCache"
    const val CANVAS_CACHE = "canvasCache"
    const val SERVICE_SCOPE = "serviceScope"
    const val MAIN_PLAYER = "mainPlayer"
    const val SECONDARY_PLAYER = "secondaryPlayer"

    val REMOVED_SONG_DATE_TIME: LocalDateTime = LocalDateTime(LocalDate(2003, Month.AUGUST, 26), LocalTime(3, 0))
}

/*** Update supported location from sigma67/ytmusicapi
 *
 */
object SUPPORTED_LOCATION {
    val items: Array<CharSequence> =
        arrayOf(
            "AE",
            "AR",
            "AT",
            "AU",
            "AZ",
            "BA",
            "BD",
            "BE",
            "BG",
            "BH",
            "BO",
            "BR",
            "BY",
            "CA",
            "CH",
            "CL",
            "CO",
            "CR",
            "CY",
            "CZ",
            "DE",
            "DK",
            "DO",
            "DZ",
            "EC",
            "EE",
            "EG",
            "ES",
            "FI",
            "FR",
            "GB",
            "GE",
            "GH",
            "GR",
            "GT",
            "HK",
            "HN",
            "HR",
            "HU",
            "ID",
            "IE",
            "IL",
            "IN",
            "IQ",
            "IS",
            "IT",
            "JM",
            "JO",
            "JP",
            "KE",
            "KH",
            "KR",
            "KW",
            "KZ",
            "LA",
            "LB",
            "LI",
            "LK",
            "LT",
            "LU",
            "LV",
            "LY",
            "MA",
            "ME",
            "MK",
            "MT",
            "MX",
            "MY",
            "NG",
            "NI",
            "NL",
            "NO",
            "NP",
            "NZ",
            "OM",
            "PA",
            "PE",
            "PG",
            "PH",
            "PK",
            "PL",
            "PR",
            "PT",
            "PY",
            "QA",
            "RO",
            "RS",
            "RU",
            "SA",
            "SE",
            "SG",
            "SI",
            "SK",
            "SN",
            "SV",
            "TH",
            "TN",
            "TR",
            "TW",
            "TZ",
            "UA",
            "UG",
            "US",
            "UY",
            "VE",
            "VN",
            "YE",
            "ZA",
            "ZW",
        )
}

object SUPPORTED_LANGUAGE {
    val items: Array<CharSequence> =
        arrayOf(
            "English",
            "Tiếng Việt",
            "Italiano",
            "Deutsch",
            "Русский",
            "Türkçe",
            "Suomi",
            "Polski",
            "Português",
            "Français",
            "Español",
            "简体中文 (Simplified Chinese)",
            "Bahasa Indonesia",
            "اللغة العربية",
            "日本語",
            "繁體中文 (Traditional Chinese)",
            "Українська",
            "עברית",
            "Azerbaijani",
            "हिन्दी",
            "ภาษาไทย",
            "Nederlands",
            "한국어",
            "Català",
            "فارسی",
            "български",
            "Svenska",
            "Hrvatski",
        )
    val codes: Array<String> =
        arrayOf(
            "en-US",
            "vi-VN",
            "it-IT",
            "de-DE",
            "ru-RU",
            "tr-TR",
            "fi-FI",
            "pl-PL",
            "pt-PT",
            "fr-FR",
            "es-ES",
            "zh-CN",
            "id-ID",
            "ar-SA",
            "ja-JP",
            "zh-Hant-TW",
            "uk-UA",
            "iw-IL",
            "az-AZ",
            "hi-IN",
            "th-TH",
            "nl-NL",
            "ko-KR",
            "ca-ES",
            "fa-AF",
            "bg-BG",
            "sv-SE",
            "hr-HR",
        )

    fun getLanguageFromCode(code: String?): String {
        val index =
            codes.indexOf(
                if (code == "he-IL") {
                    "iw-IL"
                } else {
                    code
                },
            )
        Logger.d("Config", "getLanguageFromCode: $code")
        Logger.d("Config", "getLanguageFromCode: ${items.getOrNull(index)}")
        if (index == -1) {
            return "English"
        }
        return (items.getOrNull(index) ?: "English").toString()
    }

    fun getCodeFromLanguage(language: String?): String {
        val index = items.indexOf(language ?: "English")
        Logger.d("Config", "getCodeFromLanguage: $index")
        if (index == -1) {
            return "en-US"
        }
        Logger.d("Config", "getCodeFromLanguage: ${codes.getOrNull(index)}")
        return (codes.getOrNull(index) ?: "en-US")
    }
}

/**
 * The YouTube stream format ids (`itag`) the app asks for, in one place.
 *
 * YouTube publishes dozens; these are the handful the player, the downloader and the extractor
 * health-check all have to agree on. Every itag reference belongs here — a bare number inside a
 * `find { it.itag == ... }` reads as an arbitrary constant, and the next person cannot tell that it
 * is the same stream the quality setting three modules away is supposed to produce.
 */
object ITAG {
    /** Opus, adaptive audio — what the "Low" audio-quality setting selects. */
    const val AUDIO_OPUS_LOW: Int = 250

    /** Opus, adaptive audio — what the "Medium" audio-quality setting selects. */
    const val AUDIO_OPUS_MEDIUM: Int = 251

    /** Opus 256 kbps, adaptive audio — the "High" setting. YouTube serves it to Premium accounts only. */
    const val AUDIO_OPUS_HIGH: Int = 774

    /**
     * AAC 256 kbps, adaptive audio — the AAC twin of [AUDIO_OPUS_HIGH]. An account entitled to
     * high-quality audio is given one family or the other, so this is the fallback when 774 is absent.
     */
    const val AUDIO_AAC_HIGH: Int = 141

    /** H.264 360p, adaptive video — what the "360p" video-quality setting selects. */
    const val VIDEO_360P: Int = 134

    /** H.264 720p, adaptive video — what the "720p" video-quality setting selects. */
    const val VIDEO_720P: Int = 136

    /** H.264 1080p, adaptive video — what the "1080p" video-quality setting selects. */
    const val VIDEO_1080P: Int = 137

    /** H.264 360p and AAC muxed into ONE progressive stream, for the single-URL (muxed) path. */
    const val MUXED_360P: Int = 18

    /** Every adaptive audio itag the app knows how to play. */
    val AUDIO: Set<Int> = setOf(AUDIO_OPUS_LOW, AUDIO_OPUS_MEDIUM, AUDIO_OPUS_HIGH, AUDIO_AAC_HIGH)

    /** Every adaptive video itag the app knows how to play. */
    val VIDEO: Set<Int> = setOf(VIDEO_1080P, VIDEO_720P, VIDEO_360P)

    /**
     * The other 256 kbps rendition of the same audio, or null for an itag that has no twin.
     *
     * YouTube hands a high-quality account ONE of the two families, so the one the user picked can
     * simply be absent from a response that still carries its counterpart. Asking for the twin is
     * therefore a better fallback than dropping to "any audio stream", which would silently serve
     * a 70 kbps one instead.
     */
    fun highQualityTwinOf(itag: Int?): Int? =
        when (itag) {
            AUDIO_OPUS_HIGH -> AUDIO_AAC_HIGH
            AUDIO_AAC_HIGH -> AUDIO_OPUS_HIGH
            else -> null
        }
}

object QUALITY {
    val items: Array<CharSequence> =
        arrayOf(
            "Low - 66kps",
            "Medium - 129kps",
            "High Opus - 256kps (YT Premium)",
            "High AAC - 256kps (YT Premium)",
        )

    /** Parallel to [items]: an index into one is an index into the other, so the order is load-bearing. */
    val itags: Array<Int> =
        arrayOf(
            ITAG.AUDIO_OPUS_LOW,
            ITAG.AUDIO_OPUS_MEDIUM,
            ITAG.AUDIO_OPUS_HIGH,
            ITAG.AUDIO_AAC_HIGH,
        )

    /**
     * Labels written by older builds, mapped to the item that replaced them.
     *
     * The setting is persisted as the label STRING, not as an index, so renaming an entry orphans
     * every device that already stored the old text. Without this map such a device falls through
     * to [items]`[0]` — silently dropping a Premium account from 256 kbps to 66 kbps.
     */
    private val LEGACY_ITEMS: Map<String, String> =
        mapOf(
            // Split into the Opus and AAC families; the plain "High" was always the Opus one.
            "High - 256kps (YT Premium)" to items[2].toString(),
        )

    /**
     * The stored label as a currently-valid one: itself when still valid, its replacement when it
     * is a known older label, and [items]`[0]` for anything unrecognised.
     *
     * Migration is lazy on purpose — nothing rewrites the stored value, so a read never races a
     * write. The stored text is replaced the next time the user picks an entry themselves.
     */
    fun normalize(saved: String?): String {
        val label = saved ?: return items[0].toString()
        if (items.any { it.toString() == label }) return label
        return LEGACY_ITEMS[label] ?: items[0].toString()
    }

    /** The itag a stored label selects. Goes through [normalize], so older labels still resolve. */
    fun itagOf(saved: String?): Int = itags[items.indexOfFirst { it.toString() == normalize(saved) }]
}

/**
 * Settings > Playback > Quality, the streaming audio quality. Download quality keeps [QUALITY]'s four
 * labels. The two 256 kbps codecs are one choice here: High asks for Opus, its AAC twin answers when
 * that is what the account gets, and without Premium the best stream there is plays.
 *
 * Stored as the label string, like [QUALITY]; "Name - detail" is split by the settings sheet.
 */
object STREAM_QUALITY {
    val items: Array<CharSequence> =
        arrayOf(
            "Data saver - 66 kbps",
            "Normal - 129 kbps",
            "High - 256 kbps with Premium",
        )

    /** Parallel to [items]. */
    val itags: Array<Int> = arrayOf(ITAG.AUDIO_OPUS_LOW, ITAG.AUDIO_OPUS_MEDIUM, ITAG.AUDIO_OPUS_HIGH)

    /** The default, and where anything unreadable lands: never quietly down to 66 kbps. */
    val HIGH: String get() = items[2].toString()

    // Labels from before the split, including QUALITY's older "High", mapped onto the three.
    private val LEGACY_ITEMS: Map<String, String> =
        mapOf(
            QUALITY.items[0].toString() to items[0].toString(),
            QUALITY.items[1].toString() to items[1].toString(),
            QUALITY.items[2].toString() to HIGH,
            QUALITY.items[3].toString() to HIGH,
            "High - 256kps (YT Premium)" to HIGH,
        )

    fun normalize(saved: String?): String = saved?.let { s -> items.firstOrNull { it.toString() == s }?.toString() ?: LEGACY_ITEMS[s] } ?: HIGH

    fun itagOf(saved: String?): Int = itags[items.indexOfFirst { it.toString() == normalize(saved) }]
}

object VIDEO_QUALITY {
    val items: Array<CharSequence> = arrayOf("1080p", "720p", "360p")

    /** Parallel to [items]: an index into one is an index into the other, so the order is load-bearing. */
    val itags: Array<Int> = arrayOf(ITAG.VIDEO_1080P, ITAG.VIDEO_720P, ITAG.VIDEO_360P)
}

object LIMIT_CACHE_SIZE {
    val items: Array<CharSequence> = arrayOf("100MB", "250MB", "500MB", "1GB", "2GB", "5GB", "8GB", "∞")
    val data: Array<Int> = arrayOf(100, 250, 500, 1000, 2000, 5000, 8000, -1)

    fun getDataFromItem(item: CharSequence?): Int {
        val index = items.indexOf(item)
        return data.getOrNull(index) ?: -1
    }

    fun getItemFromData(input: Int?): CharSequence {
        val index = data.indexOf(input)
        return items.getOrNull(index) ?: "∞"
    }
}

object CHART_SUPPORTED_COUNTRY {
    val items =
        arrayOf(
            "US",
            "ZZ",
            "AR",
            "AU",
            "AT",
            "BE",
            "BO",
            "BR",
            "CA",
            "CL",
            "CO",
            "CR",
            "CZ",
            "DK",
            "DO",
            "EC",
            "EG",
            "SV",
            "EE",
            "FI",
            "FR",
            "DE",
            "GT",
            "HN",
            "HK",
            "HU",
            "IS",
            "IN",
            "ID",
            "IE",
            "IL",
            "IT",
            "JP",
            "KE",
            "LU",
            "MY",
            "MX",
            "NL",
            "NZ",
            "NI",
            "NG",
            "NO",
            "PA",
            "PY",
            "PE",
            "PH",
            "PL",
            "PT",
            "RO",
            "RU",
            "SA",
            "RS",
            "SG",
            "ZA",
            "KR",
            "ES",
            "SE",
            "CH",
            "TW",
            "TZ",
            "TH",
            "TR",
            "UG",
            "UA",
            "AE",
            "GB",
            "UY",
            "VN",
            "ZW",
        )
    val itemsData =
        arrayOf(
            "United States",
            "Global",
            "Argentina",
            "Australia",
            "Austria",
            "Belgium",
            "Bolivia",
            "Brazil",
            "Canada",
            "Chile",
            "Colombia",
            "Costa Rica",
            "Czech Republic",
            "Denmark",
            "Dominican Republic",
            "Ecuador",
            "Egypt",
            "El Salvador",
            "Estonia",
            "Finland",
            "France",
            "Germany",
            "Guatemala",
            "Honduras",
            "Hong Kong",
            "Hungary",
            "Iceland",
            "India",
            "Indonesia",
            "Ireland",
            "Israel",
            "Italy",
            "Japan",
            "Kenya",
            "Luxembourg",
            "Malaysia",
            "Mexico",
            "Netherlands",
            "New Zealand",
            "Nicaragua",
            "Nigeria",
            "Norway",
            "Panama",
            "Paraguay",
            "Peru",
            "Philippines",
            "Poland",
            "Portugal",
            "Romania",
            "Russia",
            "Saudi Arabia",
            "Serbia",
            "Singapore",
            "South Africa",
            "South Korea",
            "Spain",
            "Sweden",
            "Switzerland",
            "Taiwan",
            "Tanzania",
            "Thailand",
            "Turkey",
            "Uganda",
            "Ukraine",
            "United Arab Emirates",
            "United Kingdom",
            "Uruguay",
            "Vietnam",
            "Zimbabwe",
        )
}

object MEDIA_CUSTOM_COMMAND {
    const val LIKE = "like"
    const val REPEAT = "repeat"
    const val RADIO = "radio"
    const val SHUFFLE = "shuffle"

    // Android Auto (Car App Library): asks the session for its platform token
    const val GET_PLATFORM_TOKEN = "get_platform_token"
    const val KEY_PLATFORM_TOKEN = "platform_token"
}

object MEDIA_NOTIFICATION {
    const val NOTIFICATION_ID = 200
    const val NOTIFICATION_CHANNEL_NAME = "Limusic Playback Notification"
    const val NOTIFICATION_CHANNEL_ID = "playback"
}

const val SETTINGS_FILENAME = "settings"

const val DOWNLOAD_EXOPLAYER_FOLDER = "download"

const val DB_NAME = "Music Database"

const val EXOPLAYER_DB_NAME = "exoplayer_internal.db"

const val FIRST_TIME_MIGRATION = "first_time_migration"
const val SELECTED_LANGUAGE = "selected_language"

const val STATUS_DONE = "status_done"

const val RESTORE_SUCCESSFUL = "restore_successful"

const val LOCAL_PLAYLIST_ID_SAVED_QUEUE = "LOCAL_PLAYLIST_ID_SAVED_QUEUE"
const val LOCAL_PLAYLIST_ID_DOWNLOADED = "LOCAL_PLAYLIST_ID_DOWNLOADED"
const val LOCAL_PLAYLIST_ID_LIKED = "LOCAL_PLAYLIST_ID_LIKED"
const val LOCAL_PLAYLIST_ID = "LOCAL_PLAYLIST_ID"

object MERGING_DATA_TYPE {
    const val SONG = "Song"
    const val VIDEO = "Video"
}

/** Library's chips, desktop's tabs plus Downloads and Podcasts. */
enum class LibraryChipType {
    YOUR_LIBRARY,
    LOCAL_PLAYLIST,
    ALBUMS,
    ARTISTS,
    SONGS,
    DOWNLOADED_PLAYLIST,
    FAVORITE_PODCAST,
    ;

    fun toStringValue(): String =
        when (this) {
            YOUR_LIBRARY -> "your_library"
            LOCAL_PLAYLIST -> "local_playlist"
            ALBUMS -> "albums"
            ARTISTS -> "artists"
            SONGS -> "songs"
            DOWNLOADED_PLAYLIST -> "downloaded_playlist"
            FAVORITE_PODCAST -> "favorite_podcast"
        }

    companion object {
        fun fromStringValue(value: String): LibraryChipType? =
            when (value) {
                "your_library" -> YOUR_LIBRARY
                // The old YouTube playlists and Mixes chips, and Saved, which split into
                // Playlists and Albums: a stored choice or a link naming them lands next door.
                "local_playlist", "youtube_music_playlist", "youtube_mix_for_you" -> LOCAL_PLAYLIST
                "albums", "favorite_playlist" -> ALBUMS
                "artists" -> ARTISTS
                "songs" -> SONGS
                "downloaded_playlist" -> DOWNLOADED_PLAYLIST
                "favorite_podcast" -> FAVORITE_PODCAST
                else -> null
            }
    }
}