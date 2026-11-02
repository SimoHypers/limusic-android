package com.simohypers.limusic.ui.screen.library

import com.simohypers.limusic.common.Config.REMOVED_SONG_DATE_TIME
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals

class LikedWhenTest {
    private val today = LocalDate(2026, 11, 2)

    private fun at(
        y: Int,
        m: Int,
        d: Int,
    ) = LocalDateTime(LocalDate(y, m, d), LocalTime(12, 0))

    @Test
    fun lastSevenDaysAreThisWeekEvenAcrossAMonth() {
        assertEquals(LikedWhen.Week, likedWhen(at(2026, 11, 2), today))
        assertEquals(LikedWhen.Week, likedWhen(at(2026, 10, 27), today))
    }

    @Test
    fun restOfTheMonthThenMonthsBack() {
        val later = LocalDate(2026, 11, 20)
        assertEquals(LikedWhen.Month, likedWhen(at(2026, 11, 2), later))
        assertEquals(LikedWhen.InMonth(2026, 10), likedWhen(at(2026, 10, 26), today))
        assertEquals(LikedWhen.InMonth(2025, 11), likedWhen(at(2025, 11, 30), today))
    }

    @Test
    fun noDateOrThePlaceholderIsEarlier() {
        assertEquals(LikedWhen.Earlier, likedWhen(null, today))
        assertEquals(LikedWhen.Earlier, likedWhen(REMOVED_SONG_DATE_TIME, today))
    }
}
