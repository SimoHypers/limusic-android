package com.simohypers.limusic.domain.data.model.lyrics

/**
 * Every lyrics provider, in the order a fresh install asks them (Limusic's order). [id] is what the
 * `lyrics_providers` setting and the lyrics cache store; [displayName] is the attribution shown.
 */
enum class LyricsSource(
    val id: String,
    val displayName: String,
) {
    LIMUSIC("limusic", "Limusic"),
    BOIDU("boidu", "Boidu"),
    UNISON("unison", "Unison"),
    LYRICSPLUS("lyricsplus", "LyricsPlus"),
    LRCLIB("lrclib", "LRCLIB"),
    YOUTUBE("youtube", "YouTube Music"),
    NETEASE("netease", "Netease Cloud Music"),
    QQ("qq", "QQ Music"),
    KUGOU("kugou", "Kugou"),
    ;

    companion object {
        fun fromId(id: String?): LyricsSource? = entries.find { it.id == id }

        /**
         * The `lyrics_providers` setting as (source, on) in the user's order: ids top to bottom,
         * `-id` for one switched off. Whatever it doesn't name (all of them on a fresh install, or a
         * provider a later release adds) follows in the default order, switched on, unless every
         * source it names is off: someone who wanted no lyrics doesn't get a new provider asked.
         * An id no release knows any more (`simpmusic`) is dropped. Limusic, our own, goes first
         * rather than last when the setting doesn't name it, as on desktop: it holds what the
         * three word-by-word sources hold, filed by video id.
         */
        fun order(setting: String?): List<Pair<LyricsSource, Boolean>> {
            val out = mutableListOf<Pair<LyricsSource, Boolean>>()
            for (token in setting.orEmpty().split(',').map(String::trim)) {
                val source = fromId(token.removePrefix("-")) ?: continue
                if (out.none { it.first == source }) out += source to !token.startsWith('-')
            }
            val allOff = out.isNotEmpty() && out.none { it.second }
            for (s in entries) {
                if (out.none { it.first == s }) out.add(if (s == LIMUSIC) 0 else out.size, s to !allOff)
            }
            return out
        }

        /** [order]'s inverse: the setting for a list in the user's order. */
        fun encode(order: List<Pair<LyricsSource, Boolean>>): String = order.joinToString(",") { (s, on) -> if (on) s.id else "-${s.id}" }
    }
}
