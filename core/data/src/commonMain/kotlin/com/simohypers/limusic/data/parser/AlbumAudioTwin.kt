package com.simohypers.limusic.data.parser

import com.simohypers.limusic.domain.data.model.browse.album.AlbumBrowse
import com.simohypers.limusic.domain.data.model.browse.album.Track
import com.simohypers.limusic.domain.utils.MusicVideoType
import com.simohypers.limusic.kotlinytmusicscraper.models.SongItem

/**
 * Album shelves say `MUSIC_VIDEO_TYPE_OMV` for any song that has a music video, so without this the
 * album plays the video while search for the same song plays the audio.
 */
internal fun AlbumBrowse.withSongVersions(audio: List<SongItem>): AlbumBrowse {
    if (audio.isEmpty()) return this
    val byTitle = audio.filter { MusicVideoType.isAudio(it.musicVideoType) }.associateBy { it.title }
    return copy(
        tracks =
            tracks.mapIndexed { index, track ->
                if (!MusicVideoType.isVideoSong(track.videoType)) return@mapIndexed track
                val twin =
                    audio.getOrNull(index)
                        ?.takeIf { it.title == track.title && MusicVideoType.isAudio(it.musicVideoType) }
                        ?: byTitle[track.title]
                if (twin == null) track else track.withAudio(twin)
            },
    )
}

private fun Track.withAudio(audio: SongItem) =
    copy(
        videoId = audio.id,
        duration = formatDuration(audio.duration),
        durationSeconds = audio.duration ?: 0,
        likeStatus = audio.likeStatus?.name ?: likeStatus,
        videoType = audio.musicVideoType,
        counterpart = this,
    )
