package com.maxrave.discord

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * How the card is assembled, as the Discord settings page edits it and its preview draws it.
 *
 * Same field names, string values and defaults as desktop Limusic's `RpcConfig`
 * (src-tauri/src/discord.rs), stored as one JSON blob in the `discord_rpc_config` setting. Unlike
 * desktop, the preview and the presence loop share [text], [link] and [button], so there is one copy
 * of the slot rules instead of a Rust one and a TypeScript one that have to agree.
 */
@Serializable
data class DiscordConfig(
    /** Which slot Discord repeats after "Listening to": `app` | `line1` | `line2`. */
    @SerialName("status_line") val statusLine: String = "line2",
    /** Replaces the registered application name. Empty keeps "Limusic". */
    @SerialName("app_name") val appName: String = "",
    /** `title` | `artist` | `album`. */
    val line1: String = "title",
    /** `title` | `artist` | `album` | `artist_album` | `off`. */
    val line2: String = "artist",
    /** Show the artwork. Also gates [line3], which Discord only renders with the assets block. */
    val cover: Boolean = true,
    /** `assets.large_text`: a third line under line 2, and the artwork's hover text. */
    val line3: String = "album",
    @SerialName("link_line1") val linkLine1: Boolean = false,
    @SerialName("link_line2") val linkLine2: Boolean = false,
    @SerialName("link_cover") val linkCover: Boolean = false,
    /** Ignored while paused: Discord has no paused state, so a bar left up keeps running. */
    val timestamps: Boolean = true,
    /** The Limusic badge in the artwork's corner. Needs [cover]: alone it becomes the image. */
    val badge: Boolean = true,
    /** Keep the card while paused, once something has played this session. */
    @SerialName("show_paused") val showPaused: Boolean = false,
    /** Say only that music is playing. Overrides every other field. */
    @SerialName("hide_details") val hideDetails: Boolean = false,
    /** `listen` | `album` | `artist` | `app` | `off`, in card order. */
    val button1: String = "listen",
    val button2: String = "app",
) {
    val name: String get() = appName.trim().ifEmpty { APP_NAME }

    fun toJson(): String = json.encodeToString(serializer(), this)

    companion object {
        private val json =
            Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
            }

        /** Anything unparseable is the default card, never an error. */
        fun parse(stored: String?): DiscordConfig =
            stored?.let { runCatching { json.decodeFromString(serializer(), it) }.getOrNull() } ?: DiscordConfig()
    }
}

/** What the card needs to know about a track. */
data class DiscordTrack(
    val videoId: String,
    val title: String,
    val artists: String,
    val artistId: String? = null,
    val album: String? = null,
    val albumId: String? = null,
    val thumbnail: String? = null,
)

/** The DataStore key the config blob lives under, same name as desktop's setting. */
const val DISCORD_CONFIG_KEY = "discord_rpc_config"

const val APP_NAME = "Limusic"
const val APP_URL = "https://github.com/SimoHypers/Limusic"

/** The badge Discord draws over the artwork's corner. Served from the desktop repo, like desktop. */
const val BADGE_URL = "https://raw.githubusercontent.com/SimoHypers/limusic/master/src-tauri/icons/128x128.png"

private const val SONG_URL = "https://music.youtube.com/watch?v="
private const val ARTIST_URL = "https://music.youtube.com/channel/"
private const val ALBUM_URL = "https://music.youtube.com/browse/"
private const val MAX_FIELD = 128
private const val MAX_BUTTON_URL = 512

/**
 * The text a slot resolves to, or null when the track has nothing for it: an album-less single
 * with line 2 set to Album drops the line rather than showing a blank one.
 */
fun DiscordTrack.text(slot: String): String? {
    val artist = artists.takeIf { it.isNotBlank() }
    val album = album?.takeIf { it.isNotBlank() }
    return when (slot) {
        "title" -> title.takeIf { it.isNotBlank() }
        "artist" -> artist
        "album" -> album
        "artist_album" -> if (artist != null && album != null) "$artist · $album" else artist ?: album
        else -> null
    }
}

/** Where a slot points when made clickable: the link follows the content it shows. */
fun DiscordTrack.link(slot: String): String? =
    when (slot) {
        "title" -> videoId.takeIf { it.isNotEmpty() }?.let { SONG_URL + it }
        "artist", "artist_album" -> artistId?.takeIf { it.isNotEmpty() }?.let { ARTIST_URL + it }
        "album" -> albumId?.takeIf { it.isNotEmpty() }?.let { ALBUM_URL + it }
        else -> null
    }

/**
 * One button slot as (label, url), or null when off or the track has nothing to open. The labels
 * stay English: they go over the wire, and the preview has to show what friends will read.
 */
fun DiscordTrack.button(kind: String): Pair<String, String>? {
    val (label, url) =
        when (kind) {
            "listen" -> "Listen on YouTube Music" to link("title")
            "album" -> "View album" to link("album")
            "artist" -> "View artist" to link("artist")
            "app" -> "Get Limusic" to APP_URL
            else -> return null
        }
    return url?.takeIf { it.length <= MAX_BUTTON_URL }?.let { label to it }
}

/** Line 1 is the one line Discord will not draw a card without, so an empty slot falls back to the title. */
fun DiscordTrack.line1Slot(cfg: DiscordConfig): String = if (text(cfg.line1) != null) cfg.line1 else "title"

/**
 * Clamp to Discord's 2 to 128 character window: either violation rejects the whole payload.
 * Short values are padded with braille blanks, which render empty and survive Discord's trimming.
 */
internal fun field(s: String): String {
    var out = if (s.length > MAX_FIELD) s.take(MAX_FIELD - 1) + "…" else s
    while (out.length < 2) out += "⠀"
    return out
}
