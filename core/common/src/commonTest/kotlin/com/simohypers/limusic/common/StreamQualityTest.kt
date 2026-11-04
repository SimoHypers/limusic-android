package com.simohypers.limusic.common

import kotlin.test.Test
import kotlin.test.assertEquals

class StreamQualityTest {
    @Test
    fun oldPicksMapOntoTheThreeAndNothingFallsToDataSaver() {
        assertEquals(STREAM_QUALITY.items[0].toString(), STREAM_QUALITY.normalize("Low - 66kps"))
        assertEquals(STREAM_QUALITY.items[1].toString(), STREAM_QUALITY.normalize("Medium - 129kps"))
        assertEquals(STREAM_QUALITY.HIGH, STREAM_QUALITY.normalize("High Opus - 256kps (YT Premium)"))
        assertEquals(STREAM_QUALITY.HIGH, STREAM_QUALITY.normalize("High AAC - 256kps (YT Premium)"))
        assertEquals(STREAM_QUALITY.HIGH, STREAM_QUALITY.normalize("High - 256kps (YT Premium)"))
        assertEquals(STREAM_QUALITY.HIGH, STREAM_QUALITY.normalize(null))
        assertEquals(STREAM_QUALITY.HIGH, STREAM_QUALITY.normalize("something else"))
        assertEquals(ITAG.AUDIO_OPUS_HIGH, STREAM_QUALITY.itagOf("High AAC - 256kps (YT Premium)"))
        assertEquals(ITAG.AUDIO_OPUS_LOW, STREAM_QUALITY.itagOf(STREAM_QUALITY.items[0].toString()))
    }
}
