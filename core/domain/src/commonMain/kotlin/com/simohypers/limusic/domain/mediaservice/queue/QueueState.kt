package com.simohypers.limusic.domain.mediaservice.queue

import com.simohypers.limusic.common.LOCAL_PLAYLIST_ID
import com.simohypers.limusic.domain.data.model.browse.album.Track
import com.simohypers.limusic.domain.mediaservice.handler.RepeatState

/**
 * The play queue, ported from Limusic desktop (`QueueState` and its helpers in
 * src-tauri/src/state.rs). One flat list in play order and a pointer into it. Shuffle reorders the
 * list itself and keeps the real order aside for un-shuffle, so the player's playlist can simply
 * mirror [items]: there is no second index space.
 *
 * Plain state and plain functions, no player and no network: the handler does those and calls in
 * here for every decision about where a track goes.
 */
class QueueState {
    var items: MutableList<Track> = mutableListOf()
    var current = 0

    /** Pre-shuffle order. Non-null exactly while shuffle is on. */
    var shuffleOrig: MutableList<Track>? = null

    /**
     * Autoplay's seed: `RDAMPL<id>` when the queue came from a playlist or album, null for a single
     * song, where autoplay seeds `RDAMVM<last track>` instead and drifts with what is playing.
     */
    var radioSeed: String? = null

    /** What the queue plays from ("<song> Radio", a playlist title), for "Playing from". */
    var sourceName: String? = null

    /** The playlist this queue was started from, exactly as the page passed it. */
    var sourceId: String? = null

    /** YouTube generated every upcoming track, so Add to queue goes ahead of them ([enqueueAt]). */
    var radio = false

    /**
     * The play whose own radio is being fetched right now. Autoplay's early trigger holds off
     * meanwhile, or both would ask for the same page and the queue would hold it twice.
     */
    var hydrating: Long? = null

    /** The queue this one replaced, for Previous at the head of a fresh queue. */
    var prev: PrevContext? = null

    /** Bumped only when a different queue replaces this one, never by a skip within it. */
    var epoch = 0L

    val shuffled get() = shuffleOrig != null

    /** Move the play pointer within this queue. Past the head, Previous has a track of its own. */
    fun seekTo(index: Int) {
        current = index
        if (index > 0) prev = null
    }

    /**
     * Set this queue aside so Previous can come back to it. Takes [items] and [shuffleOrig] rather
     * than copying them: only a caller that replaces both right after may call this.
     */
    fun keepContext(positionMs: Long) {
        if (items.isEmpty()) return
        prev =
            PrevContext(
                items = items,
                current = current.coerceAtMost(items.lastIndex),
                shuffleOrig = shuffleOrig,
                radioSeed = radioSeed,
                sourceName = sourceName,
                sourceId = sourceId,
                radio = radio,
                positionMs = positionMs,
            )
        items = mutableListOf()
        shuffleOrig = null
        radioSeed = null
        sourceName = null
        sourceId = null
    }

    /** Put a kept queue back. Returns the track to start and where to resume it. */
    fun restoreContext(p: PrevContext): Pair<String, Long> {
        items = p.items
        epoch++
        current = p.current
        shuffleOrig = p.shuffleOrig
        radioSeed = p.radioSeed
        sourceName = p.sourceName
        sourceId = p.sourceId
        radio = p.radio
        hydrating = null
        return items[current].videoId to p.positionMs
    }
}

/**
 * What was playing before a click replaced the whole queue.
 *
 * ponytail: one level deep, like desktop. Two presses don't walk back two contexts.
 */
class PrevContext(
    val items: MutableList<Track>,
    val current: Int,
    val shuffleOrig: MutableList<Track>?,
    val radioSeed: String?,
    val sourceName: String?,
    val sourceId: String?,
    val radio: Boolean,
    val positionMs: Long,
)

/** Desktop's queue settings, as DataStore string keys holding "true" or "false" (default off). */
const val STICKY_SHUFFLE_KEY = "sticky_shuffle"
const val SHUFFLE_WHOLE_QUEUE_KEY = "shuffle_whole_queue"
const val PREVENT_DUPLICATES_KEY = "prevent_duplicates"

/** How many played rows an endlessly extending queue keeps behind the playing one. */
const val KEEP_PLAYED = 200

/** Autoplay tops the queue up with at most this many tracks per fetch. */
const val AUTOPLAY_BATCH = 20

fun Track.unmarked(): Track =
    if (queued || queuedEnd || queuedFrom != null || autoplay) {
        copy(queued = false, queuedEnd = false, queuedFrom = null, autoplay = false)
    } else {
        this
    }

/**
 * A song clicked outside the queue replaces it: the old queue is kept for Previous and the unplayed
 * manual adds follow the new song. [radio] is false when Autoplay is off: then nothing is hydrated
 * behind the song and the header promises no radio.
 */
fun QueueState.startSong(
    seed: Track,
    positionMs: Long,
    radio: Boolean,
    sticky: Boolean,
    gen: Long,
) {
    val carried = upcomingQueued(items, current)
    val wasShuffled = shuffled
    keepContext(positionMs)
    sourceName = if (radio) "${seed.title} Radio" else null
    items = (listOf(seed.unmarked()) + carried).toMutableList()
    epoch++
    current = 0
    radioSeed = null
    sourceId = null
    this.radio = false
    hydrating = if (radio) gen else null
    shuffleOrig = if (sticky && wasShuffled) items.toMutableList() else null
}

/**
 * The clicked song's radio arrived. It goes behind the song and the carried adds, deduped against
 * the queue; a track already there stays put but takes the album the radio's row knows.
 */
fun QueueState.absorbSongRadio(
    page: List<Track>,
    whole: Boolean,
) {
    val seen = items.mapTo(HashSet()) { it.videoId }
    for (track in page) {
        if (seen.add(track.videoId)) {
            items.add(track.unmarked())
        } else {
            val i = items.indexOfFirst { it.videoId == track.videoId }
            if (i >= 0 && items[i].album == null) items[i] = items[i].copy(album = track.album)
        }
    }
    if (shuffled) {
        shuffleOrig = items.toMutableList()
        shuffleUpcoming(items, current, whole)
    }
}

/**
 * Play a finite list (a playlist, an album). [start] is the clicked track; null means the header's
 * Play, which starts anywhere while shuffled. Carried manual adds keep their meaning: Play next
 * right after the clicked track, Add to queue behind the new list.
 */
fun QueueState.startTracks(
    tracks: List<Track>,
    start: Int?,
    sourceId: String?,
    sourceName: String?,
    shuffle: Boolean,
    sticky: Boolean,
    whole: Boolean,
    positionMs: Long,
) {
    if (tracks.isEmpty()) return
    val keepShuffled = shuffle || (sticky && shuffled)
    val first =
        when {
            start != null -> start.coerceIn(0, tracks.lastIndex)
            keepShuffled -> tracks.indices.random()
            else -> 0
        }
    val carried = upcomingQueued(items, current)
    keepContext(positionMs)
    items = tracks.mapTo(mutableListOf()) { it.unmarked() }
    epoch++
    current = first
    this.sourceId = sourceId
    radioSeed = radioSeedFor(sourceId)
    this.sourceName = sourceName
    radio = false
    hydrating = null
    if (keepShuffled) {
        shuffleOrig = items.toMutableList()
        current = shuffleNewQueue(items, current)
    } else {
        shuffleOrig = null
    }
    val (next, added) = carried.partition { it.queued }
    items.addAll(current + 1, next)
    items.addAll(added)
    if (keepShuffled && added.isNotEmpty() && whole) shuffleUpcoming(items, current, true)
}

/**
 * Install a radio behind the playing track: history and the current song stay, everything after
 * them becomes the radio, unplayed manual adds are carried. Not marked autoplay: this queue is what
 * the user asked for.
 */
fun QueueState.spliceRadio(
    page: List<Track>,
    seed: String,
    title: String?,
) {
    if (items.isEmpty()) return
    val carried = upcomingQueued(items, current)
    items = (items.subList(0, current + 1) + carried).toMutableList()
    val seen = items.mapTo(HashSet()) { it.videoId }
    page.filter { seen.add(it.videoId) }.mapTo(items) { it.unmarked() }
    radioSeed = seed
    sourceName = title
    sourceId = null
    radio = true
    if (shuffled) {
        shuffleOrig = items.toMutableList()
        // A radio's adds already sit right behind the playing track, pinned either way.
        shuffleUpcoming(items, current, false)
    }
}

/**
 * Play next ([next]) or Add to queue. Play next lands right after the playing track, behind earlier
 * Play next adds; Add to queue at [enqueueAt]. A copy already waiting among the manual adds is
 * moved, never doubled; with [dedupe] ("Prevent duplicates") every other copy goes too.
 */
fun QueueState.insertQueued(
    tracks: List<Track>,
    next: Boolean,
    from: String?,
    dedupe: Boolean,
    whole: Boolean,
) {
    if (tracks.isEmpty()) return
    val adds = tracks.mapTo(mutableListOf()) { it.copy(queued = next, queuedEnd = !next, queuedFrom = from, autoplay = false) }
    val ids = adds.mapTo(HashSet()) { it.videoId }
    var removed = dedupe && dropDuplicates(ids)
    val before = items.size
    val cur = current
    items = items.filterIndexed { i, t -> i <= cur || !((t.queued || t.queuedEnd) && t.videoId in ids) }.toMutableList()
    removed = removed || items.size != before
    if (removed) {
        shuffleOrig?.let { orig ->
            val kept = items.mapTo(HashSet()) { it.videoId }
            orig.retainAll { it.videoId in kept }
        }
    }
    val at = if (next) playNextIndex(items, current) else enqueueAt()
    shuffleOrig?.let { orig ->
        // In their real order: that is what un-shuffle restores.
        orig.addAll(adds)
        // Shuffle is on, so a block added from an album or playlist joins it right away.
        if (from != null) adds.shuffle()
    }
    if (!next && shuffled && whole) {
        // Shuffling the whole queue: each add lands somewhere random between Play next and autoplay.
        val lo = playNextIndex(items, current)
        for (add in adds) items.add((lo..maxOf(enqueueAt(), lo)).random(), add)
    } else {
        items.addAll(at, adds)
    }
}

/** Remove an upcoming or played track. The playing one stays. */
fun QueueState.removeAt(index: Int): Boolean {
    if (index !in items.indices || index == current) return false
    val gone = items.removeAt(index)
    if (index < current) current--
    // Off the snapshot too, or turning shuffle off brings it back.
    shuffleOrig?.let { orig -> orig.indexOfFirst { it.videoId == gone.videoId }.takeIf { it >= 0 }?.let(orig::removeAt) }
    return true
}

/**
 * Drag to reorder: only upcoming tracks move, and only among themselves. A track you put somewhere
 * yourself is yours, so it stops counting as autoplay's.
 */
fun QueueState.move(
    from: Int,
    to: Int,
): Boolean {
    val first = current + 1
    if (from < first || from >= items.size || to < first || to >= items.size || from == to) return false
    items.add(to, items.removeAt(from).copy(autoplay = false))
    return true
}

/**
 * Shuffle on: snapshot, then shuffle what is upcoming. Off: restore the snapshot without offering
 * again what the shuffle already played.
 */
fun QueueState.toggleShuffle(whole: Boolean) {
    if (items.isEmpty()) return
    val orig = shuffleOrig
    if (orig != null) {
        shuffleOrig = null
        val heard = items.subList(0, current + 1).mapTo(HashSet()) { it.videoId }
        val (restored, idx) = unshuffled(orig, heard, items[current].videoId, current)
        items = restored
        current = idx
    } else {
        shuffleOrig = items.toMutableList()
        shuffleUpcoming(items, current, whole)
    }
}

/**
 * Autoplay's batch at the tail, marked, deduped against the queue as it is now. Returns how many
 * landed. Trims the played history too, except while shuffled: the snapshot would need rebasing.
 */
fun QueueState.absorbAutoplay(
    fresh: List<Track>,
    cap: Int = AUTOPLAY_BATCH,
): Int {
    val added = mergeRadio(items, fresh, items.mapTo(HashSet()) { it.videoId }, cap)
    if (added > 0 && !shuffled) current -= trimPlayed(items, current, KEEP_PLAYED)
    return added
}

/**
 * A page of the playing playlist ([playing]) or of one added with Add to queue, walked in the
 * background. The playing playlist's pages go in front of the user's adds at the tail; an added
 * one's go right behind its own block. While shuffled, a page of the playing list is mixed through
 * the unplayed tail, behind the next track, which the player has already started loading.
 */
fun QueueState.appendPage(
    page: List<Track>,
    playing: Boolean,
    whole: Boolean,
) {
    shuffleOrig?.addAll(page)
    if (playing) {
        items.addAll(contextEnd(), page)
        if (shuffled) shuffleUpcoming(items, (current + 1).coerceAtMost(items.lastIndex), whole)
    } else {
        val from = page.firstOrNull()?.queuedFrom
        val last = items.indexOfLast { it.queuedEnd && it.queuedFrom == from }
        items.addAll(if (last > current) last + 1 else enqueueAt(), page)
    }
}

/**
 * Where Add to queue lands: the tail, behind the rest of the playing playlist and earlier adds,
 * ahead of autoplay's filler. In a radio, every upcoming track is filler, so behind the manual adds
 * right after the playing track.
 */
fun QueueState.enqueueAt(): Int {
    val start = (current + 1).coerceAtMost(items.size)
    if (radio) {
        var at = start
        while (at < items.size && (items[at].queued || items[at].queuedEnd)) at++
        return at
    }
    val p = items.subList(start, items.size).indexOfFirst { it.autoplay }
    return if (p < 0) items.size else start + p
}

/** Where the rest of the playing playlist goes: in front of the adds at the tail and of autoplay. */
fun QueueState.contextEnd(): Int {
    var at = (current + 1).coerceAtMost(items.size)
    while (at < items.size && items[at].queued) at++
    val p = items.subList(at, items.size).indexOfFirst { it.queuedEnd || it.autoplay }
    return if (p < 0) items.size else at + p
}

/** Where Play next goes: right after the playing track, behind earlier Play next adds (FIFO). */
fun playNextIndex(
    items: List<Track>,
    current: Int,
): Int {
    var at = (current + 1).coerceAtMost(items.size)
    while (at < items.size && items[at].queued) at++
    return at
}

/** Drop every copy of [ids] except the playing one; [QueueState.current] follows its own track. */
fun QueueState.dropDuplicates(ids: Set<String>): Boolean {
    var removed = false
    for (i in items.indices.reversed()) {
        if (i != current && items[i].videoId in ids) {
            items.removeAt(i)
            if (i < current) current--
            removed = true
        }
    }
    return removed
}

/** Keep only the upcoming tracks [keep] accepts; played and playing ones stay. The snapshot follows. */
fun QueueState.retainUpcoming(keep: (Track) -> Boolean): Boolean {
    val before = items.size
    val cur = current
    items = items.filterIndexed { i, t -> i <= cur || keep(t) }.toMutableList()
    if (items.size == before) return false
    shuffleOrig?.let { orig ->
        val left = items.mapTo(HashSet()) { it.videoId }
        orig.retainAll { keep(it) || it.videoId in left }
    }
    return true
}

/**
 * Autoplay's seed for a queue source: `RDAMPL<id>` for a YouTube playlist or album's audio
 * playlist, a mix as itself, nothing for a playlist on the phone.
 */
fun radioSeedFor(sourceId: String?): String? {
    if (sourceId == null || sourceId.startsWith(LOCAL_PLAYLIST_ID)) return null
    val id = sourceId.removePrefix("VL")
    return if (id.startsWith("RD")) id else "RDAMPL$id"
}

/** The track the "Back to …" line names, while Previous can still reach it. */
fun QueueState.prevTrackTitle(): String? = if (current != 0) null else prev?.let { it.items[it.current].title }

/** Drop played rows beyond [keep] from the front. Returns how many went. */
fun trimPlayed(
    items: MutableList<Track>,
    current: Int,
    keep: Int,
): Int {
    val drop = (current - keep).coerceAtLeast(0)
    if (drop > 0) items.subList(0, drop).clear()
    return drop
}

/**
 * Songs not heard lately keep YouTube's order and go first, then the heard ones, least recently
 * heard first. YouTube ranks a radio the same way every time, so every radio on a song used to open
 * on the same handful of tracks.
 */
fun <T : Comparable<T>> freshen(
    items: MutableList<Track>,
    heard: Map<String, T>,
) = items.sortBy { heard[it.videoId] }

/** Append autoplay tracks: deduped against [existing], at most [cap], marked [Track.autoplay]. */
fun mergeRadio(
    items: MutableList<Track>,
    fresh: List<Track>,
    existing: MutableSet<String>,
    cap: Int,
): Int {
    var added = 0
    for (track in fresh) {
        if (added >= cap) break
        if (!existing.add(track.videoId)) continue
        items.add(track.copy(queued = false, queuedEnd = false, queuedFrom = null, autoplay = true))
        added++
    }
    return added
}

/** The index that plays after [current]: wraps on repeat-all only. Repeat-one loops in the player. */
fun nextIndex(
    len: Int,
    current: Int,
    repeat: RepeatState,
): Int? =
    when {
        len == 0 -> null
        current + 1 < len -> current + 1
        repeat == RepeatState.All -> 0
        else -> null
    }

/**
 * Un-shuffle: the snapshot, the playing track found in it, and from what is still upcoming
 * everything the shuffle already played ([heard]). Manual adds go back where they belong: Play next
 * right after the playing track, Add to queue behind the playlist.
 */
fun unshuffled(
    orig: List<Track>,
    heard: Set<String>,
    playingId: String,
    fallback: Int,
): Pair<MutableList<Track>, Int> {
    val idx = orig.indexOfFirst { it.videoId == playingId }.takeIf { it >= 0 } ?: fallback.coerceIn(0, maxOf(orig.size - 1, 0))
    val kept = orig.filterIndexed { i, t -> i <= idx || t.videoId !in heard }
    val tail = kept.drop(idx + 1)
    val (next, rest) = tail.partition { it.queued }
    val (added, playlist) = rest.partition { it.queuedEnd }
    return (kept.take(idx + 1) + next + playlist + added).toMutableList() to idx
}

/** The unplayed manual adds, carried into a new queue on a context switch. */
fun upcomingQueued(
    items: List<Track>,
    current: Int,
): List<Track> = items.drop(current + 1).filter { it.queued || it.queuedEnd }

/**
 * Shuffle only what is upcoming. The leading Play next run keeps its place (an album queued that
 * way is shuffled within itself), then the playlist, then the Add to queue tracks in their order,
 * then autoplay, each group shuffled within itself. With [whole], the playlist and the adds are one
 * mix.
 */
fun shuffleUpcoming(
    items: MutableList<Track>,
    current: Int,
    whole: Boolean,
) {
    val first = (current + 1).coerceAtMost(items.size)
    var start = first
    while (start < items.size && (items[start].queued || (!whole && items[start].queuedEnd))) start++
    shuffleAddedRuns(items.subList(first, start))
    if (start >= items.size) return
    val rest = items.subList(start, items.size)
    if (whole) {
        rest.shuffle()
        rest.sortBy { it.autoplay }
    } else {
        rest.sortWith(compareBy<Track>({ it.autoplay }, { it.queuedEnd }))
        val playlist = rest.takeWhile { !it.autoplay && !it.queuedEnd }.size
        val added = playlist + rest.drop(playlist).takeWhile { !it.autoplay }.size
        rest.subList(0, playlist).shuffle()
        shuffleAddedRuns(rest.subList(playlist, added))
        rest.subList(added, rest.size).shuffle()
    }
}

/** Inside a run of manual adds, shuffle each block that came from one album or playlist. */
fun shuffleAddedRuns(items: MutableList<Track>) {
    var i = 0
    while (i < items.size) {
        val from = items[i].queuedFrom
        if (from == null) {
            i++
            continue
        }
        var end = i + 1
        while (end < items.size && items[end].queuedFrom == from) end++
        items.subList(i, end).shuffle()
        i = end
    }
}

/** A mix is an endless generated feed: autoplay tops it up, so its pages aren't walked. */
fun isMix(sourceId: String?): Boolean = sourceId?.removePrefix("VL")?.startsWith("RD") == true

/** A fresh queue shuffled around the clicked track: it plays first. Returns the new index, 0. */
fun shuffleNewQueue(
    items: MutableList<Track>,
    start: Int,
): Int {
    if (items.isNotEmpty()) {
        val i = start.coerceAtMost(items.lastIndex)
        items[0] = items[i].also { items[i] = items[0] }
        shuffleUpcoming(items, 0, false)
    }
    return 0
}

/**
 * How the queue sheet lays the queue out (desktop's queue.ts): one list in play order, split once,
 * where autoplay's upcoming tracks begin. Autoplay tracks already played are ordinary rows.
 */
class QueueView(
    /** First index drawn under the Autoplay divider; the list's size when there is none. */
    val autoplayFrom: Int,
    /** Upcoming Play next and Add to queue tracks: what Clear queue removes. */
    val queued: Int,
    /** What plays before the queue ends or autoplay takes over, and its length in seconds. */
    val leftCount: Int,
    val leftSecs: Int,
)

fun queueView(
    items: List<Track>,
    current: Int,
): QueueView {
    val upcoming = (current + 1).coerceAtLeast(0) until items.size
    // Once autoplay starts the rest goes with it: a row dragged in among its tracks plays there.
    val autoplayFrom = upcoming.firstOrNull { items[it].autoplay } ?: items.size
    val left = upcoming.first until autoplayFrom
    return QueueView(
        autoplayFrom = autoplayFrom,
        queued = upcoming.count { items[it].queued || items[it].queuedEnd },
        leftCount = left.count(),
        leftSecs = left.sumOf { items[it].durationSeconds ?: clockSecs(items[it].duration) },
    )
}

/** "3:45" or "1:02:03" in seconds; 0 for anything else. */
fun clockSecs(s: String?): Int {
    if (s == null || !Regex("""^\d+(:\d{1,2}){1,2}$""").matches(s)) return 0
    return s.split(':').fold(0) { acc, part -> acc * 60 + part.toInt() }
}
