package com.maxrave.simpmusic.utils

import com.eygraber.uri.Uri
import com.maxrave.common.LibraryChipType

// Every path the deep-link router in App.kt opens; anything else gets its "link not supported" toast.
private val APP_LINK_PATHS = setOf("notification", "watch", "playlist", "channel", "c", "album", "library", "search")

/** Whether a `simpmusic://` link names a screen the router opens. */
fun isAppLinkAvailable(appPath: String?): Boolean = appPath in APP_LINK_PATHS

/** The Library chip a link asks for with `?tab=`. A `?type=` opens one of the lists instead and wins. */
fun libraryTabOf(link: Uri): LibraryChipType? =
    if (link.getQueryParameter("type").isNullOrBlank()) {
        link.getQueryParameter("tab")?.let { LibraryChipType.fromStringValue(it) }
    } else {
        null
    }
