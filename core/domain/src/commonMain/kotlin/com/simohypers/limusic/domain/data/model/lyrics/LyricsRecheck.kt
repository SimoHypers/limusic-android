package com.simohypers.limusic.domain.data.model.lyrics

import com.simohypers.limusic.domain.data.entities.LyricsEntity

/** How old a cached Limusic lyric gets before its revision is looked at again: an edit approved since should reach people who already have the song. */
const val RECHECK_LIMUSIC_SECS = 24 * 3600L

/** How old any other cached lyric gets before Limusic is asked whether it has the song by now. */
const val RECHECK_OTHER_SECS = 7 * 24 * 3600L

/** What a cached lyric is due, desktop's `recheck_due`. */
enum class LyricsRecheck {
    /** Limusic's own lyric: has its revision moved? */
    REVISION,

    /** Another source's lyric: does Limusic have the song by now? */
    ASK,
    ;

    companion object {
        /**
         * Whether a cached lyric of this age is due a look at Limusic. Never a pinned one (picked by
         * hand or nudged: the user's, not a cache), never a "none found" verdict, and never with
         * the Limusic source switched off. Another source's lyric asks only where a fresh walk down
         * [order] would pick Limusic's word-by-word answer over it: Limusic sits above that source,
         * or the lyric is plain text, which the walk doesn't stop at.
         */
        fun due(
            l: LyricsEntity,
            ageSecs: Long,
            order: List<Pair<LyricsSource, Boolean>>,
        ): LyricsRecheck? {
            fun at(s: LyricsSource?) = order.indexOfFirst { it.first == s && it.second }.takeIf { it >= 0 }
            val limusic = at(LyricsSource.LIMUSIC) ?: return null
            if (l.pinned || l.error || l.fetchedAt <= 0) return null
            if (l.provider == LyricsSource.LIMUSIC.id) return REVISION.takeIf { ageSecs > RECHECK_LIMUSIC_SECS }
            val settles = l.syncType != null && l.syncType != "UNSYNCED"
            val wins = !settles || at(LyricsSource.fromId(l.provider))?.let { limusic < it } ?: true
            return ASK.takeIf { wins && ageSecs > RECHECK_OTHER_SECS }
        }
    }
}
