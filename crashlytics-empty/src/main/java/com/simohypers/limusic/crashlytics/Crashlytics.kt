package com.simohypers.limusic.crashlytics

import android.content.Context
import com.simohypers.limusic.domain.data.player.PlayerError
import com.simohypers.limusic.logger.Logger

// Sent crash to Sentry
fun reportCrash(throwable: Throwable) {
    Logger.e("Crashlytics", "NON-SENTRY crash: ${throwable.localizedMessage}")
}

fun configCrashlytics(applicationContext: Context, dsn: String) {
    Logger.d("Crashlytics", "NON-SENTRY start app")
}

// The player already logs its error under "Player Error"; nothing is sent anywhere.
fun pushPlayerError(error: PlayerError) = Unit