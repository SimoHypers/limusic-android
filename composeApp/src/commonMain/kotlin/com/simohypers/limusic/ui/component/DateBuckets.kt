package com.simohypers.limusic.ui.component

import androidx.compose.runtime.Composable
import com.simohypers.limusic.common.Config.REMOVED_SONG_DATE_TIME
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.number
import limusic.composeapp.generated.resources.Res
import limusic.composeapp.generated.resources.liked_earlier
import limusic.composeapp.generated.resources.liked_this_month
import limusic.composeapp.generated.resources.liked_this_week
import limusic.composeapp.generated.resources.today
import limusic.composeapp.generated.resources.yesterday
import org.jetbrains.compose.resources.stringResource
import java.time.format.TextStyle
import java.util.Locale

/** A list heading for when something happened: liked songs by when they were liked, plays by day. */
sealed interface DateBucket {
    data object Today : DateBucket

    data object Yesterday : DateBucket

    data object Week : DateBucket

    data object Month : DateBucket

    data class InMonth(
        val year: Int,
        val month: Int,
    ) : DateBucket

    data object Earlier : DateBucket
}

/**
 * Which heading [at] falls under, seen from [today]. [days] splits Today and Yesterday out of
 * This week, for a history.
 */
// ponytail: "this week" is the last seven days, not the calendar week, so Monday doesn't empty it.
fun dateBucket(
    at: LocalDateTime?,
    today: LocalDate,
    days: Boolean = false,
): DateBucket {
    // SimpMusic stamps rows that predate the timestamp with this placeholder date.
    if (at == null || at == REMOVED_SONG_DATE_TIME) return DateBucket.Earlier
    val day = at.date
    val ago = today.toEpochDays() - day.toEpochDays()
    return when {
        days && ago <= 0 -> DateBucket.Today
        days && ago == 1L -> DateBucket.Yesterday
        ago < 7 -> DateBucket.Week
        day.year == today.year && day.month == today.month -> DateBucket.Month
        else -> DateBucket.InMonth(day.year, day.month.number)
    }
}

@Composable
fun DateBucket.label(today: LocalDate): String =
    when (this) {
        DateBucket.Today -> stringResource(Res.string.today)
        DateBucket.Yesterday -> stringResource(Res.string.yesterday)
        DateBucket.Week -> stringResource(Res.string.liked_this_week)
        DateBucket.Month -> stringResource(Res.string.liked_this_month)
        DateBucket.Earlier -> stringResource(Res.string.liked_earlier)
        is DateBucket.InMonth -> {
            val name =
                java.time.Month
                    .of(month)
                    .getDisplayName(TextStyle.FULL_STANDALONE, Locale.getDefault())
                    .replaceFirstChar { it.titlecase(Locale.getDefault()) }
            if (year == today.year) name else "$name $year"
        }
    }
