package com.simohypers.limusic.data.parser

import com.simohypers.limusic.domain.data.model.browse.album.AlbumBrowse
import com.simohypers.limusic.domain.data.model.browse.album.Track
import com.simohypers.limusic.domain.mediaservice.queue.swappedVersion
import com.simohypers.limusic.domain.utils.MusicVideoType
import com.simohypers.limusic.kotlinytmusicscraper.models.SongItem

/**
 * Album shelves say `MUSIC_VIDEO_TYPE_OMV` for any song that has a music video, so without this the
 * album plays the video while search for the same song plays the audio.
 */
internal fun AlbumBrowse.withSongVersions(audio: List<SongItem>): AlbumBrowse {
    if (tracks.isEmpty() || audio.size != tracks.size) return this
    return copy(
        tracks =
            tracks.mapIndexed { index, track ->
                if (!MusicVideoType.isVideoSong(track.videoType)) return@mapIndexed track
                val twin =
                    audio.getOrNull(index)
                        ?.takeIf { it.title == track.title && MusicVideoType.isAudio(it.musicVideoType) }
                if (twin == null) track else track.withAudio(twin)
            },
    )
}

internal fun AlbumBrowse.withVideosPreferred(): AlbumBrowse =
    copy(tracks = tracks.map { it.swappedVersion() ?: it })

private fun Track.withAudio(audio: SongItem) =
    copy(
        videoId = audio.id,
        duration = audio.duration?.let(::formatDuration) ?: duration,
        durationSeconds = audio.duration ?: durationSeconds,
        likeStatus = audio.likeStatus?.name ?: likeStatus,
        videoType = audio.musicVideoType,
        counterpart = this,
    )
