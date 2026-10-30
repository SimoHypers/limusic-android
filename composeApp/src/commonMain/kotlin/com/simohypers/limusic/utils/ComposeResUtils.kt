package com.simohypers.limusic.utils

import org.jetbrains.compose.resources.getString
import limusic.composeapp.generated.resources.Res
import limusic.composeapp.generated.resources.added_to_queue
import limusic.composeapp.generated.resources.explicit_content_blocked
import limusic.composeapp.generated.resources.ltd_guest_added
import limusic.composeapp.generated.resources.ltd_host_controls
import limusic.composeapp.generated.resources.new_albums
import limusic.composeapp.generated.resources.new_singles
import limusic.composeapp.generated.resources.playback_refused
import limusic.composeapp.generated.resources.this_app_needs_to_access_your_notification
import limusic.composeapp.generated.resources.time_out_check_internet_connection_or_change_piped_instance_in_settings
import limusic.composeapp.generated.resources.youtube_write_failed

object ComposeResUtils {
    suspend fun getResString(
        type: StringType,
        vararg format: String,
    ): String =
        when (type) {
            StringType.EXPLICIT_CONTENT_BLOCKED -> {
                getString(Res.string.explicit_content_blocked)
            }

            StringType.NOTIFICATION_REQUEST -> {
                getString(Res.string.this_app_needs_to_access_your_notification)
            }

            StringType.TIME_OUT_ERROR -> {
                getString(Res.string.time_out_check_internet_connection_or_change_piped_instance_in_settings, *format)
            }

            StringType.PLAYBACK_REFUSED -> {
                getString(Res.string.playback_refused, *format)
            }

            StringType.NEW_SINGLES -> {
                getString(Res.string.new_singles)
            }

            StringType.NEW_ALBUMS -> {
                getString(Res.string.new_albums)
            }

            StringType.YOUTUBE_WRITE_FAILED -> {
                getString(Res.string.youtube_write_failed)
            }

            StringType.HOST_CONTROLS_PLAYBACK -> {
                getString(Res.string.ltd_host_controls)
            }

            StringType.ADDED_TO_QUEUE -> {
                getString(Res.string.added_to_queue)
            }

            StringType.GUEST_ADDED -> {
                getString(Res.string.ltd_guest_added, *format)
            }

        }

    enum class StringType {
        EXPLICIT_CONTENT_BLOCKED,
        NOTIFICATION_REQUEST,
        TIME_OUT_ERROR,
        PLAYBACK_REFUSED,
        NEW_SINGLES,
        NEW_ALBUMS,
        YOUTUBE_WRITE_FAILED,
        HOST_CONTROLS_PLAYBACK,
        ADDED_TO_QUEUE,
        GUEST_ADDED,
    }
}