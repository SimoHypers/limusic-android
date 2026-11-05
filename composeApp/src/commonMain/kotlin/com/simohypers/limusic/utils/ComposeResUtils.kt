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
import limusic.composeapp.generated.resources.playback_skipped
import limusic.composeapp.generated.resources.time_out_check_internet_connection_or_change_piped_instance_in_settings
import limusic.composeapp.generated.resources.youtube_write_failed
import limusic.composeapp.generated.resources.update_notification_channel
import limusic.composeapp.generated.resources.update_notification_text
import limusic.composeapp.generated.resources.update_notification_title

object ComposeResUtils {
    suspend fun getResString(
        type: StringType,
        vararg format: String,
    ): String =
        when (type) {
            StringType.EXPLICIT_CONTENT_BLOCKED -> {
                getString(Res.string.explicit_content_blocked)
            }

            StringType.TIME_OUT_ERROR -> {
                getString(Res.string.time_out_check_internet_connection_or_change_piped_instance_in_settings, *format)
            }

            StringType.PLAYBACK_REFUSED -> {
                getString(Res.string.playback_refused, *format)
            }

            StringType.PLAYBACK_SKIPPED -> {
                getString(Res.string.playback_skipped, *format)
            }

            StringType.NEW_SINGLES -> {
                getString(Res.string.new_singles)
            }

            StringType.NEW_ALBUMS -> {
                getString(Res.string.new_albums)
            }

            StringType.UPDATE_NOTIFICATION_TITLE -> {
                getString(Res.string.update_notification_title, *format)
            }

            StringType.UPDATE_NOTIFICATION_TEXT -> {
                getString(Res.string.update_notification_text)
            }

            StringType.UPDATE_NOTIFICATION_CHANNEL -> {
                getString(Res.string.update_notification_channel)
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
        TIME_OUT_ERROR,
        PLAYBACK_REFUSED,
        PLAYBACK_SKIPPED,
        NEW_SINGLES,
        NEW_ALBUMS,
        UPDATE_NOTIFICATION_TITLE,
        UPDATE_NOTIFICATION_TEXT,
        UPDATE_NOTIFICATION_CHANNEL,
        YOUTUBE_WRITE_FAILED,
        HOST_CONTROLS_PLAYBACK,
        ADDED_TO_QUEUE,
        GUEST_ADDED,
    }
}