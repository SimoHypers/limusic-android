package com.maxrave.simpmusic.utils

import com.eygraber.uri.Uri
import com.maxrave.common.LibraryChipType

// Every path the deep-link router in App.kt opens; anything else gets its "link not supported" toast.
private val APP_LINK_PATHS = setOf("notification", "watch", "playlist", "channel", "c", "album", "library")

/**
 * Whether the screen a `simpmusic://` link opens exists for this user right now. The YouTube
 * playlists chip only exists while signed in; the router turns such a link away with a toast.
 */
fun isAppLinkAvailable(
    appPath: String?,
    libraryTab: LibraryChipType?,
    youTubeLoggedIn: Boolean,
): Boolean =
    when {
        appPath !in APP_LINK_PATHS -> false
        appPath == "library" && libraryTab == LibraryChipType.YOUTUBE_MUSIC_PLAYLIST -> youTubeLoggedIn
        else -> true
    }

/** The Library chip a link asks for with `?tab=`. A `?type=` opens one of the lists instead and wins. */
fun libraryTabOf(link: Uri): LibraryChipType? =
    if (link.getQueryParameter("type").isNullOrBlank()) {
        link.getQueryParameter("tab")?.let { LibraryChipType.fromStringValue(it) }
    } else {
        null
    }
