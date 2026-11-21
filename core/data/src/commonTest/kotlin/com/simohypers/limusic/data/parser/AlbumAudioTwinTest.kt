package com.simohypers.limusic.data.parser

import com.simohypers.limusic.domain.data.model.browse.album.AlbumBrowse
import com.simohypers.limusic.domain.data.model.browse.album.Track
import com.simohypers.limusic.domain.data.model.searchResult.songs.Album
import com.simohypers.limusic.domain.data.model.searchResult.songs.Artist
import com.simohypers.limusic.domain.data.model.streams.YouTubeWatchEndpoint.WatchEndpointMusicSupportedConfigs.WatchEndpointMusicConfig.Companion.MUSIC_VIDEO_TYPE_ATV
import com.simohypers.limusic.domain.data.model.streams.YouTubeWatchEndpoint.WatchEndpointMusicSupportedConfigs.WatchEndpointMusicConfig.Companion.MUSIC_VIDEO_TYPE_OMV
import com.simohypers.limusic.kotlinytmusicscraper.models.Artist as ScraperArtist
import com.simohypers.limusic.kotlinytmusicscraper.models.SongItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class AlbumAudioTwinTest {
    @Test
    fun videoTrackTakesTheAudioIdAndKeepsTheVideoAsCounterpart() {
        val video = track("Dust", "9Qpe2qlERWY", 190, MUSIC_VIDEO_TYPE_OMV)
        val resolved = album(video).withSongVersions(listOf(song("Dust", "spjUd9KtIbo", 190))).tracks.single()

        assertEquals("spjUd9KtIbo", resolved.videoId)
        assertEquals(MUSIC_VIDEO_TYPE_ATV, resolved.videoType)
        assertEquals("03:10", resolved.duration)
        assertEquals("9Qpe2qlERWY", resolved.counterpart?.videoId)
    }

    @Test
    fun aReorderedPlaylistLeavesTheShelfAlone() {
        val dust = track("Dust", "9Qpe2qlERWY", 190, MUSIC_VIDEO_TYPE_OMV)
        val whisper = track("Whisper My Name", "YhUqxWR4mnE", 223, MUSIC_VIDEO_TYPE_OMV)

        val resolved =
            album(dust, whisper)
                .withSongVersions(
                    listOf(
                        song("Whisper My Name", "g1zVw8drM5k", 223),
                        song("Dust", "spjUd9KtIbo", 190),
                    ),
                ).tracks

        assertEquals(listOf("9Qpe2qlERWY", "YhUqxWR4mnE"), resolved.map { it.videoId })
    }

    @Test
    fun aPlaylistOfADifferentLengthLeavesTheShelfAlone() {
        val video = track("Dust", "9Qpe2qlERWY", 190, MUSIC_VIDEO_TYPE_OMV)

        val resolved =
            album(video)
                .withSongVersions(
                    listOf(
                        song("Dust", "spjUd9KtIbo", 190),
                        song("Interlude", "another", 40),
                    ),
                ).tracks
                .single()

        assertEquals("9Qpe2qlERWY", resolved.videoId)
        assertNull(resolved.counterpart)
    }

    @Test
    fun aPlaylistRowWithoutALengthKeepsTheShelfLength() {
        val video = track("Dust", "9Qpe2qlERWY", 190, MUSIC_VIDEO_TYPE_OMV)

        val resolved = album(video).withSongVersions(listOf(song("Dust", "spjUd9KtIbo", null))).tracks.single()

        assertEquals("spjUd9KtIbo", resolved.videoId)
        assertEquals("03:10", resolved.duration)
        assertEquals(190, resolved.durationSeconds)
    }

    @Test
    fun videosOnFlipsTheSwappedRowBackToTheVideo() {
        val video = track("Dust", "9Qpe2qlERWY", 190, MUSIC_VIDEO_TYPE_OMV)

        val resolved =
            album(video)
                .withSongVersions(listOf(song("Dust", "spjUd9KtIbo", 190)))
                .withVideosPreferred()
                .tracks
                .single()

        assertEquals("9Qpe2qlERWY", resolved.videoId)
        assertEquals(MUSIC_VIDEO_TYPE_OMV, resolved.videoType)
        assertEquals("spjUd9KtIbo", resolved.counterpart?.videoId)
    }

    @Test
    fun aVideoRowInThePlaylistIsNotATwin() {
        val video = track("Dust", "9Qpe2qlERWY", 190, MUSIC_VIDEO_TYPE_OMV)

        val resolved =
            album(video)
                .withSongVersions(listOf(song("Dust", "otherVideo", 190, MUSIC_VIDEO_TYPE_OMV)))
                .tracks
                .single()

        assertEquals("9Qpe2qlERWY", resolved.videoId)
        assertNull(resolved.counterpart)
    }

    @Test
    fun anEmptyPlaylistLeavesTheShelfAlone() {
        val page = album(track("Dust", "9Qpe2qlERWY", 190, MUSIC_VIDEO_TYPE_OMV))

        assertSame(page, page.withSongVersions(emptyList()))
    }

    @Test
    fun anAlreadyAudioRowGetsNoCounterpart() {
        val audio = track("Make Them Cry", "uxNuuys19XE", 308, MUSIC_VIDEO_TYPE_ATV)

        val resolved = album(audio).withSongVersions(listOf(song("Make Them Cry", "uxNuuys19XE", 308))).tracks.single()

        assertEquals("uxNuuys19XE", resolved.videoId)
        assertNull(resolved.counterpart)
    }

    private fun album(vararg tracks: Track) =
        AlbumBrowse(
            artists = listOf(Artist("Drake", "UCU6cE7pdJPc6DU2jSrKEsdQ")),
            audioPlaylistId = "OLAK5uy_lcDE9n6x4ySCRHAQ3xhqEAeIQQXpusa4A",
            description = null,
            duration = "52:00",
            durationSeconds = 0,
            thumbnails = null,
            title = "ICEMAN",
            trackCount = tracks.size,
            tracks = tracks.toList(),
            type = "Album",
            year = "2025",
        )

    private fun track(
        title: String,
        videoId: String,
        seconds: Int,
        videoType: String,
    ) = Track(
        album = Album(name = "ICEMAN", id = "MPREb_LjIW4hKEFIk"),
        artists = listOf(Artist("Drake", "UCU6cE7pdJPc6DU2jSrKEsdQ")),
        duration = "%02d:%02d".format(seconds / 60, seconds % 60),
        durationSeconds = seconds,
        isAvailable = false,
        isExplicit = false,
        likeStatus = null,
        thumbnails = null,
        title = title,
        videoId = videoId,
        videoType = videoType,
        category = null,
        feedbackTokens = null,
        resultType = null,
        year = "2025",
    )

    private fun song(
        title: String,
        videoId: String,
        seconds: Int?,
        videoType: String = MUSIC_VIDEO_TYPE_ATV,
    ) = SongItem(
        id = videoId,
        title = title,
        artists = listOf(ScraperArtist("Drake", "UCU6cE7pdJPc6DU2jSrKEsdQ")),
        duration = seconds,
        thumbnail = "",
        musicVideoType = videoType,
    )
}
