package com.simohypers.limusic.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.simohypers.limusic.ui.theme.typo
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

/**
 * A date heading for a LazyColumn's stickyHeader: the name, a hairline, and [trailing] ("12 songs")
 * at the right. Opaque and drawn over the rows, or an animated row passing under it shows through.
 */
@Composable
fun PinnedHeading(
    title: String,
    trailing: String,
) {
    Row(
        Modifier
            .zIndex(1f)
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            style = typo().headlineMedium.copy(fontSize = 17.sp, fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Box(Modifier.weight(1f).padding(horizontal = 12.dp).height(1.dp).background(MaterialTheme.colorScheme.outlineVariant))
        Text(trailing, style = typo().bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
