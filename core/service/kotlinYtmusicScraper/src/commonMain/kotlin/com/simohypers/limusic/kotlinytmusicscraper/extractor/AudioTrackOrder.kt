package com.simohypers.limusic.kotlinytmusicscraper.extractor

private val XTAGS = Regex("[?&]xtags=([^&]+)")

/**
 * A video with several audio tracks (dubbed podcasts, MrBeast-style channels) lists every one of
 * them under the SAME itag, ordered by language code — so matching by itag alone plays whichever
 * language sorts first. Measured on `0e3GPea1Tyg`: 25 tracks per itag, Arabic first, the original
 * English at position 23. Each googlevideo URL names its own track in the `xtags` query value, e.g.
 * `acont=original:drc=1:lang=en-US` or `acont=dubbed-auto:lang=vi`.
 *
 * Ranks a URL: a track in [preferredLanguage] 0, the original 1, everything else 2. A single-track
 * video carries no `xtags` at all and ranks as original, so songs are unaffected.
 */
internal fun audioTrackRank(
    url: String,
    preferredLanguage: String?,
): Int {
    val tags =
        XTAGS
            .find(url)
            ?.groupValues
            ?.get(1)
            ?.replace("%3D", "=", ignoreCase = true)
            ?.replace("%3A", ":", ignoreCase = true)
            ?.split(':')
            ?.associate { it.substringBefore('=') to it.substringAfter('=', "") }
            .orEmpty()
    val acont = tags["acont"]
    val lang = tags["lang"]?.substringBefore('-')
    return when {
        // "descriptive" is narration for blind viewers — never what a language preference means.
        !preferredLanguage.isNullOrBlank() &&
            acont != "descriptive" &&
            lang.equals(preferredLanguage.substringBefore('-'), ignoreCase = true) -> 0
        acont == null || acont == "original" -> 1
        else -> 2
    }
}

/**
 * Only the audio formats of the best-ranked track; video formats pass untouched. Sorting the best
 * track first was not enough: when the itag the quality setting wants is missing (a 256 kbps one
 * without Premium), the caller falls back to the highest bitrate, and with every dub still listed
 * that was whichever dub YouTube encoded largest (measured 2026-11-01 on The Diary Of A CEO:
 * Romanian, Hungarian, Russian, Hindi over the English original).
 */
internal fun <T> List<T>.keepBestAudioTrack(
    preferredLanguage: String?,
    isAudio: (T) -> Boolean,
    url: (T) -> String,
): List<T> {
    val best = filter(isAudio).minOfOrNull { audioTrackRank(url(it), preferredLanguage) } ?: return this
    return filter { !isAudio(it) || audioTrackRank(url(it), preferredLanguage) == best }
}
