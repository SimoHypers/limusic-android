package com.simohypers.limusic.data.listentogether

import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import org.koin.mp.KoinPlatform.getKoin

/** The name set in the phone's About screen, else the model. */
actual fun deviceName(): String {
    val name = runCatching { Settings.Global.getString(getKoin().get<Context>().contentResolver, Settings.Global.DEVICE_NAME) }.getOrNull()
    return name?.trim()?.takeIf { it.isNotEmpty() } ?: Build.MODEL
}

actual fun elapsedRealtime(): Long = SystemClock.elapsedRealtime()
