package com.maxrave.media3

import com.maxrave.media3.service.download.totalFromContentRange
import com.maxrave.media3.service.download.withoutRangeParam
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChunkedDataSourceTest {
    @Test
    fun dropsOnlyTheRangeParam() {
        assertEquals(
            "https://rr1.googlevideo.com/videoplayback?expire=1&sig=AB%3D%3D&cpn=x",
            withoutRangeParam("https://rr1.googlevideo.com/videoplayback?expire=1&sig=AB%3D%3D&cpn=x&range=0-7654321"),
        )
        assertEquals("https://h/v?a=1&b=2", withoutRangeParam("https://h/v?a=1&range=0-99&b=2"))
        assertEquals("https://h/v?b=2", withoutRangeParam("https://h/v?range=0-99&b=2"))
        assertEquals("https://h/v?a=1", withoutRangeParam("https://h/v?a=1"))
    }

    @Test
    fun readsTheTotalFromContentRange() {
        assertEquals(7654321L, totalFromContentRange("bytes 0-4194303/7654321"))
        assertNull(totalFromContentRange("bytes 0-4194303/*"))
        assertNull(totalFromContentRange(null))
    }
}
