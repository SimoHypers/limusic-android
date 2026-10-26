package com.maxrave.simpmusic.viewModel

import com.maxrave.domain.data.model.browse.album.Track
import com.maxrave.domain.utils.PlaylistSort
import com.maxrave.domain.utils.PlaylistSort.ALBUM
import com.maxrave.domain.utils.PlaylistSort.ARTIST
import com.maxrave.domain.utils.PlaylistSort.DEFAULT
import com.maxrave.domain.utils.PlaylistSort.NEWEST
import com.maxrave.domain.utils.PlaylistSort.OLDEST
import com.maxrave.domain.utils.PlaylistSort.PLAYS
import com.maxrave.domain.utils.PlaylistSort.TITLE
import com.maxrave.domain.utils.PlaylistSort.TOP
import java.text.Collator

// Desktop's playlist sorting (ui/src/lib/sort.ts). Wherever YouTube offers a sort menu it orders the
// playlist itself, and remembers it, so the page asks it for an ordered list (PlaylistViewModel).
// sortTracks is the fallback for what YouTube cannot do: rank by this phone's play counts, reverse a
// playlist's stored order, and lists with no menu at all.

/** What the sort sheet offers. Not [TOP]: a list can already be in it, but nothing here computes it. */
val PLAYLIST_SORTS = listOf(DEFAULT, NEWEST, OLDEST, TITLE, ARTIST, ALBUM, PLAYS)

/**
 * The nearest order YouTube can store for this choice, or null when it has none. There is no
 * descending Title/Artist/Album and no reversed manual order, so a reversed sort stores the
 * ascending one and keeps the reverse on the phone: YouTube Music then shows the same sort the
 * right way up. Reversing a date sort simply is the other one.
 */
fun persistedSort(
    sort: PlaylistSort,
    desc: Boolean,
): PlaylistSort? =
    when (sort) {
        PLAYS -> null
        NEWEST, OLDEST -> if ((sort == NEWEST) != desc) NEWEST else OLDEST
        else -> sort
    }

/** Whether YouTube's stored order reproduces this choice exactly, so the phone has nothing to keep. */
fun storedExactly(
    sort: PlaylistSort,
    desc: Boolean,
) = sort != PLAYS && (!desc || sort == NEWEST || sort == OLDEST)

/** What to ask YouTube for. Most played ranks the playlist's own order, not whatever it is in. */
fun fetchSort(sort: PlaylistSort) = if (sort == PLAYS) DEFAULT else sort

// Desktop's Intl.Collator(numeric, base): case and accents ignored, digit runs compared as numbers.
private val collator = Collator.getInstance().apply { strength = Collator.PRIMARY }
private val runs = Regex("\\d+|\\D+")

private fun compareText(
    a: String,
    b: String,
): Int {
    val x = runs.findAll(a).iterator()
    val y = runs.findAll(b).iterator()
    while (x.hasNext() && y.hasNext()) {
        val p = x.next().value
        val q = y.next().value
        val c =
            if (p[0].isDigit() && q[0].isDigit()) {
                val (m, n) = p.trimStart('0') to q.trimStart('0')
                if (m.length != n.length) m.length.compareTo(n.length) else m.compareTo(n)
            } else {
                collator.compare(p, q)
            }
        if (c != 0) return c
    }
    return x.hasNext().compareTo(y.hasNext())
}

private val Track.artistText get() = artists?.joinToString(", ") { it.name }.orEmpty()

/**
 * Sort a playlist's tracks; returns [items] itself when there is nothing to do.
 *
 * [baseNewestFirst] says which end of the incoming order is the newest addition. Rows carry no
 * timestamp, so newest/oldest is add order: Liked Music arrives most recently liked first, every
 * other playlist oldest added first. [desc] reverses what the key ordered and nothing else: ties
 * keep playlist order and a track with no album still sorts last. [plays] is videoId to play count;
 * Most played puts the highest first, so descending means least played first.
 */
fun sortTracks(
    items: List<Track>,
    sort: PlaylistSort,
    baseNewestFirst: Boolean,
    desc: Boolean = false,
    plays: Map<String, Long> = emptyMap(),
): List<Track> {
    if (items.size < 2) return items
    val dir = if (desc) -1 else 1
    // List.sortedWith is stable, so ties (most of a big playlist, unplayed) keep playlist order.
    return when (sort) {
        // Top voted is YouTube's ranking; off the server it behaves like Default.
        DEFAULT, TOP -> if (desc) items.reversed() else items
        NEWEST, OLDEST -> if (((sort == NEWEST) != desc) == baseNewestFirst) items else items.reversed()
        PLAYS -> items.sortedWith { a, b -> dir * (plays[b.videoId] ?: 0L).compareTo(plays[a.videoId] ?: 0L) }
        // Album is a grouping: inside one album the playlist's own order stands, and descending
        // reverses the albums, not the tracks within one.
        ALBUM ->
            items.sortedWith(
                compareBy<Track> { it.album?.name.isNullOrEmpty() }
                    .then { a, b -> dir * compareText(a.album?.name.orEmpty(), b.album?.name.orEmpty()) },
            )
        // Title orders an artist's block; on a title sort the tiebreak is a no-op.
        TITLE, ARTIST -> {
            val key: (Track) -> String = if (sort == TITLE) Track::title else { t -> t.artistText }
            items.sortedWith { a, b ->
                dir * compareText(key(a), key(b)).let { if (it != 0) it else compareText(a.title, b.title) }
            }
        }
    }
}
