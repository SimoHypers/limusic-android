package com.simohypers.limusic.common

/** Supplied by the application entry point so build variants retain their real identity. */
data class AppIdentity(
    val applicationId: String,
    val versionName: String,
    val platform: String,
    /** Debug builds and Gradle runs. They read the test remote config and keep their cached copy of it apart. */
    val isDevBuild: Boolean = false,
    /** A Limusic Lyrics server in place of the live one, for a debug build talking to a local copy. */
    val lyricsServer: String = "",
) {
    val userAgent: String
        get() = "Limusic/$versionName ($applicationId; $platform)"
}
