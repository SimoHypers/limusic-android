package org.simpmusic.lastfm

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** The DataStore key the config blob lives under, same name as desktop's setting. */
const val LASTFM_CONFIG_KEY = "lastfm_config"

/**
 * How scrobbling behaves: desktop Limusic's `ScrobbleConfig` (src-tauri/src/lastfm.rs), same field
 * names and defaults, one JSON blob. The defaults are Last.fm's own rule with nothing rewritten.
 * Unlike desktop, the two primary-artist switches live in the blob too: there is no older settings
 * row to keep them in.
 */
@Serializable
data class ScrobbleConfig(
    /** Scrobble at all. Off pauses scrobbling without signing out. */
    val enabled: Boolean = true,
    /** Send `track.updateNowPlaying` when a track starts. */
    @SerialName("now_playing") val nowPlaying: Boolean = true,
    /** Scrobble once this share of the track has played, 1 to 100. */
    val percent: Int = 50,
    /** ...or once this many minutes have played, whichever comes first. 0 turns the cap off. */
    val minutes: Int = 4,
    /** "Artist - Song" out of a video upload's title. Off by default: an official video can carry a
     *  dash in the song title itself, and only the user knows which kind they listen to. */
    @SerialName("split_video_titles") val splitVideoTitles: Boolean = false,
    val rules: List<ScrobbleRule> = emptyList(),
    val edits: List<ScrobbleEdit> = emptyList(),
    /** Send only the first artist of a multi-artist byline. */
    @SerialName("primary_artist") val primaryArtist: Boolean = false,
    /** ...including a two-artist "A & B", which can be a real joint act on Last.fm. */
    @SerialName("primary_strict") val primaryStrict: Boolean = false,
) {
    fun toJson(): String = json.encodeToString(serializer(), this)

    companion object {
        private val json =
            Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
            }

        /** Anything unparseable is the default: a corrupt blob must not stop scrobbling. */
        fun parse(stored: String?): ScrobbleConfig =
            stored?.let { runCatching { json.decodeFromString(serializer(), it) }.getOrNull() } ?: ScrobbleConfig()
    }
}

/**
 * Find and replace on one field. [find] is a regular expression matched ignoring case (`(?-i)` turns
 * that off). Named groups `artist`, `title` and `album` make it an extraction: a match moves each
 * group into its field instead.
 */
@Serializable
data class ScrobbleRule(
    val enabled: Boolean = true,
    /** `title` | `artist` | `album`. */
    val field: String = "title",
    val find: String = "",
    val replace: String = "",
)

/** A fix for one track, applied every time it plays. Last.fm can't change a scrobble already sent. */
@Serializable
data class ScrobbleEdit(
    /** The video id. Empty on imports, which match on the `from` fields instead. */
    val key: String = "",
    @SerialName("from_artist") val fromArtist: String = "",
    @SerialName("from_title") val fromTitle: String = "",
    @SerialName("from_album") val fromAlbum: String = "",
    /** What Last.fm gets. Empty keeps YouTube's value. */
    val artist: String = "",
    val title: String = "",
    val album: String = "",
    /** Don't scrobble this track at all. */
    val skip: Boolean = false,
) {
    fun matches(t: ScrobbleTrack): Boolean {
        if (key.isNotEmpty()) return key == t.videoId
        fun same(
            want: String,
            have: String,
        ) = want.isBlank() || want.trim().equals(have.trim(), ignoreCase = true)
        val any = listOf(fromArtist, fromTitle, fromAlbum).any { it.isNotBlank() }
        return any && same(fromArtist, t.byline) && same(fromTitle, t.title) && same(fromAlbum, t.album.orEmpty())
    }
}

/** The track as YouTube describes it. */
data class ScrobbleTrack(
    val videoId: String,
    val title: String,
    /** One entry per credited artist, in order. */
    val artists: List<String>,
    val album: String?,
    /** A video upload rather than a song: only these get their titles split. */
    val isVideo: Boolean,
) {
    /** The byline the way YouTube Music writes it: "A", "A & B", "A, B & C". */
    val byline: String
        get() =
            when (artists.size) {
                0 -> ""
                1 -> artists[0]
                else -> artists.dropLast(1).joinToString(", ") + " & " + artists.last()
            }
}

/** What a track scrobbles as, and why. The scrobbler sends it unless [skip] is set. */
data class Resolved(
    val artist: String,
    val title: String,
    val album: String,
    /** `edit` (the user's edit says skip) or `incomplete` (no title or artist left). */
    val skip: String? = null,
    /** Index of the edit that decided this track. */
    val edit: Int? = null,
    /** The video title was split into artist and song. */
    val split: Boolean = false,
    /** Indices of the rules that changed something, in order. */
    val rules: List<Int> = emptyList(),
    /** Rules whose pattern doesn't compile, by index, with the reason. They are skipped. */
    val errors: List<Pair<Int, String>> = emptyList(),
)

private val GROUPS = listOf("artist", "title", "album")

/**
 * Apply the settings to a track, in desktop's order: a matching edit (which ends it), the video-title
 * split, the primary-artist cut, then each rule in list order.
 */
fun resolve(
    t: ScrobbleTrack,
    cfg: ScrobbleConfig,
): Resolved {
    val fields = mutableMapOf("artist" to t.byline, "title" to t.title, "album" to t.album.orEmpty())
    val errors = mutableListOf<Pair<Int, String>>()
    val compiled =
        cfg.rules.mapIndexed { i, rule ->
            if (!rule.enabled || rule.find.isEmpty()) {
                null
            } else {
                runCatching { Regex(rule.find, RegexOption.IGNORE_CASE) }
                    .onFailure { errors += i to (it.message ?: "invalid pattern").lineSequence().first() }
                    .getOrNull()
            }
        }

    cfg.edits.indexOfFirst { it.matches(t) }.takeIf { it >= 0 }?.let { i ->
        val e = cfg.edits[i]
        if (e.skip) return Resolved(fields.getValue("artist"), t.title, fields.getValue("album"), skip = "edit", edit = i, errors = errors)
        if (e.artist.isNotBlank()) fields["artist"] = e.artist.trim()
        if (e.title.isNotBlank()) fields["title"] = e.title.trim()
        if (e.album.isNotBlank()) fields["album"] = e.album.trim()
        return finish(fields, Resolved("", "", "", edit = i, errors = errors))
    }

    var credits = t.artists
    var split = false
    if (cfg.splitVideoTitles && t.isVideo) {
        splitTitle(t.title)?.let { (artist, title) ->
            fields["artist"] = artist
            fields["title"] = title
            credits = listOf(artist)
            split = true
        }
    }
    if (cfg.primaryArtist) fields["artist"] = primaryArtist(credits, cfg.primaryStrict)
    val applied = mutableListOf<Int>()
    cfg.rules.forEachIndexed { i, rule ->
        val re = compiled[i] ?: return@forEachIndexed
        if (applyRule(re, rule, fields)) applied += i
    }
    return finish(fields, Resolved("", "", "", split = split, rules = applied, errors = errors))
}

private fun finish(
    fields: Map<String, String>,
    r: Resolved,
): Resolved {
    val artist = fields.getValue("artist").trim()
    val title = fields.getValue("title").trim()
    // A rule that wipes the title or artist leaves nothing Last.fm would take.
    val skip = if (title.isEmpty() || artist.isEmpty()) "incomplete" else r.skip
    return r.copy(artist = artist, title = title, album = fields.getValue("album").trim(), skip = skip)
}

/** One rule against the fields. True when it changed something. */
private fun applyRule(
    re: Regex,
    rule: ScrobbleRule,
    fields: MutableMap<String, String>,
): Boolean {
    val value = fields[rule.field] ?: return false
    // Read off the pattern: the JVM can't list a compiled regex's group names on every Android.
    val named = GROUPS.filter { "(?<$it>" in rule.find }
    if (named.isEmpty()) {
        val out = runCatching { re.replace(value, rule.replace) }.getOrElse { return false }
        if (out == value) return false
        fields[rule.field] = out
        return true
    }
    val match = re.find(value) ?: return false
    var changed = false
    for (name in named) {
        val v = runCatching { match.groups[name]?.value }.getOrNull()?.trim()?.takeIf { it.isNotEmpty() } ?: continue
        changed = changed || fields[name] != v
        fields[name] = v
    }
    return changed
}

/** "Artist - Song" out of a video title: the first spaced dash (hyphen, en or em) with text both sides. */
fun splitTitle(title: String): Pair<String, String>? {
    val (at, sep) = listOf(" - ", " \u2013 ", " \u2014 ").mapNotNull { s -> title.indexOf(s).takeIf { it >= 0 }?.let { it to s } }.minByOrNull { it.first } ?: return null
    val artist = title.substring(0, at).trim()
    val song = title.substring(at + sep.length).trim()
    return if (artist.isNotEmpty() && song.isNotEmpty()) artist to song else null
}

/**
 * The first credit of a multi-artist byline. Last.fm has no notion of a joined credit, so the full
 * byline creates one artist literally named "A, B & C". A two-artist "A & B" stays whole unless
 * [strict]: a joint act ("Future & Metro Boomin") is usually a real Last.fm artist. Desktop has to
 * find the joiners in YouTube's runs; here the artists come as a list, so the cut is exact in any
 * language. A single credit is still cut at a comma (and "&" when strict), as desktop does.
 */
fun primaryArtist(
    artists: List<String>,
    strict: Boolean,
): String {
    if (artists.size >= 3 || (artists.size == 2 && strict)) return artists.first()
    if (artists.size == 2) return ScrobbleTrack("", "", artists, null, false).byline
    val one = artists.firstOrNull() ?: return ""
    val end = listOfNotNull(",", "&".takeIf { strict }).mapNotNull { c -> one.indexOf(c).takeIf { it >= 0 } }.minOrNull() ?: return one
    return one.substring(0, end).trim().ifEmpty { one }
}

/**
 * When the scrobble fires, in seconds into the track: [ScrobbleConfig.percent] of it or
 * [ScrobbleConfig.minutes] in, whichever comes first. Null means never: tracks under 30s don't
 * scrobble (Last.fm's floor), and with the cap off a track of unknown length has nothing to time from.
 */
fun scrobbleAt(
    durationSec: Double,
    cfg: ScrobbleConfig,
): Double? {
    if (durationSec > 0 && durationSec < 30) return null
    val share = if (durationSec > 0) durationSec * cfg.percent.coerceIn(1, 100) / 100.0 else Double.POSITIVE_INFINITY
    val cap = if (cfg.minutes > 0) cfg.minutes * 60.0 else Double.POSITIVE_INFINITY
    return minOf(share, cap).takeIf { it.isFinite() }
}
