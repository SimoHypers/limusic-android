package com.simohypers.limusic

actual fun getPlatform(): Platform = Platform.Android

actual fun deviceSummary(): String =
    "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}, Android ${android.os.Build.VERSION.RELEASE} (SDK ${android.os.Build.VERSION.SDK_INT})"