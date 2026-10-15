package com.my.kizzy

import com.maxrave.domain.data.entities.SongEntity
import com.my.kizzy.rpc.KizzyRPC
import com.my.kizzy.rpc.RpcImage
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class DiscordRPC(
    token: String,
) : KizzyRPC(token) {
    @OptIn(ExperimentalTime::class)
    suspend fun updateSong(
        currentPlaybackTimeMillis: Long,
        durationMillis: Long,
        playbackSpeed: Float = 1.0f,
        song: SongEntity,
    ) = runCatching {
        val currentTime = Clock.System.now().toEpochMilliseconds()

        val adjustedPlaybackTime = (currentPlaybackTimeMillis / playbackSpeed).toLong()
        val calculatedStartTime = currentTime - adjustedPlaybackTime

        val remainingDuration = durationMillis - currentPlaybackTimeMillis
        val adjustedRemainingDuration = (remainingDuration / playbackSpeed).toLong()

        setActivity(
            name = APP_NAME,
            details = song.title,
            state = song.artistName?.joinToString(", "),
            largeImage = song.thumbnails?.let { RpcImage.ExternalImage(it) },
            smallImage = RpcImage.ExternalImage(APP_ICON),
            largeText = song.albumName,
            smallText = song.artistName?.firstOrNull(),
            buttons =
                listOf(
                    "Listen on YouTube Music" to "https://music.youtube.com/watch?v=${song.videoId}",
                    "Get Limusic" to "https://github.com/SimoHypers/Limusic",
                ),
            type = Type.LISTENING,
            since = currentTime,
            startTime = calculatedStartTime,
            endTime = currentTime + adjustedRemainingDuration,
            applicationId = APPLICATION_ID,
        )
    }

    companion object {
        private const val APPLICATION_ID = "1525891596804161727" // Limusic desktop's Discord app
        private const val APP_NAME: String = "Limusic"
        private const val APP_ICON: String =
            "https://raw.githubusercontent.com/SimoHypers/limusic/master/src-tauri/icons/128x128.png"
    }
}