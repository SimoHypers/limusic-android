package com.simohypers.limusic.data.listentogether

/** How this device appears in the desktop's device list. */
expect fun deviceName(): String

/** Milliseconds since boot, deep sleep included. */
expect fun elapsedRealtime(): Long
