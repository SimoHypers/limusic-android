package com.maxrave.domain.data.model.lyrics

/**
 * Every lyrics provider, in the order a fresh install asks them (Limusic's order). [id] is what the
 * `lyrics_providers` setting and the lyrics cache store; [displayName] is the attribution shown.
 */
enum class LyricsSource(
    val id: String,
    val displayName: String,
) {
    BOIDU("boidu", "Boidu"),
    LYRICSPLUS("lyricsplus", "LyricsPlus"),
    LRCLIB("lrclib", "LRCLIB"),
    YOUTUBE("youtube", "YouTube Music"),
    SIMPMUSIC("simpmusic", "SimpMusic"),
    NETEASE("netease", "Netease Cloud Music"),
    QQ("qq", "QQ Music"),
    KUGOU("kugou", "Kugou"),
    ;

    companion object {
        fun fromId(id: String?): LyricsSource? = entries.find { it.id == id }

        /**
         * The `lyrics_providers` setting as (source, on) in the user's order: ids top to bottom,
         * `-id` for one switched off. Whatever it doesn't name (all of them on a fresh install, or a
         * provider a later release adds) follows in the default order, switched on.
         */
        fun order(setting: String?): List<Pair<LyricsSource, Boolean>> {
            val out = mutableListOf<Pair<LyricsSource, Boolean>>()
            for (token in setting.orEmpty().split(',').map(String::trim)) {
                val source = fromId(token.removePrefix("-")) ?: continue
                if (out.none { it.first == source }) out += source to !token.startsWith('-')
            }
            return out + entries.filter { s -> out.none { it.first == s } }.map { it to true }
        }
    }
}
