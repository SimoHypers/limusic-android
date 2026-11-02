package com.simohypers.limusic.ui.component

import com.simohypers.limusic.common.Config.REMOVED_SONG_DATE_TIME
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals

class DateBucketTest {
    private val today = LocalDate(2026, 11, 2)

    private fun at(
        y: Int,
        m: Int,
        d: Int,
    ) = LocalDateTime(LocalDate(y, m, d), LocalTime(12, 0))

    @Test
    fun lastSevenDaysAreThisWeekEvenAcrossAMonth() {
        assertEquals(DateBucket.Week, dateBucket(at(2026, 11, 2), today))
        assertEquals(DateBucket.Week, dateBucket(at(2026, 10, 27), today))
    }

    @Test
    fun aHistorySplitsOutTodayAndYesterday() {
        assertEquals(DateBucket.Today, dateBucket(at(2026, 11, 2), today, days = true))
        assertEquals(DateBucket.Yesterday, dateBucket(at(2026, 11, 1), today, days = true))
        assertEquals(DateBucket.Week, dateBucket(at(2026, 10, 31), today, days = true))
    }

    @Test
    fun restOfTheMonthThenMonthsBack() {
        val later = LocalDate(2026, 11, 20)
        assertEquals(DateBucket.Month, dateBucket(at(2026, 11, 2), later))
        assertEquals(DateBucket.InMonth(2026, 10), dateBucket(at(2026, 10, 26), today))
        assertEquals(DateBucket.InMonth(2025, 11), dateBucket(at(2025, 11, 30), today))
    }

    @Test
    fun noDateOrThePlaceholderIsEarlier() {
        assertEquals(DateBucket.Earlier, dateBucket(null, today))
        assertEquals(DateBucket.Earlier, dateBucket(REMOVED_SONG_DATE_TIME, today))
    }
}
