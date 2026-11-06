package com.simohypers.limusic

sealed class Platform {
    object Android : Platform()
    object iOS : Platform()
    object Desktop : Platform()

    fun osName(): String = when (this) {
        Android -> "android"
        iOS -> "iOS"
        Desktop -> System.getProperty("os.name") ?: "jvm"
    }
}

expect fun getPlatform(): Platform

/** Phone maker, model and OS version, for the App log export. */
expect fun deviceSummary(): String
