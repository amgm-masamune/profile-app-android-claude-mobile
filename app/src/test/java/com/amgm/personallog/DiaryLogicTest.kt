package com.amgm.personallog

import com.amgm.personallog.ui.diary.DiaryLogic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DiaryLogicTest {
    @Test
    fun formatDateLabel_includesWeekday() {
        assertEquals("2026年10月10日(土)", DiaryLogic.formatDateLabel("2026-10-10"))
        assertEquals("2024年2月29日(木)", DiaryLogic.formatDateLabel("2024-02-29"))
        assertEquals("bad", DiaryLogic.formatDateLabel("bad"))
    }

    @Test
    fun isValidDate() {
        assertTrue(DiaryLogic.isValidDate("2026-10-10"))
        assertFalse(DiaryLogic.isValidDate("2026-13-10"))
        assertFalse(DiaryLogic.isValidDate(null))
    }

    @Test
    fun previewText_flattensAndTruncates() {
        assertEquals("a b c", DiaryLogic.previewText("a\n b\n\nc"))
        assertEquals("abc…", DiaryLogic.previewText("abcdef", max = 3))
    }

    @Test
    fun emptyEntry() {
        assertTrue(DiaryLogic.isEmptyEntry("  \n", 0))
        assertFalse(DiaryLogic.isEmptyEntry("", 1))
        assertFalse(DiaryLogic.isEmptyEntry("x", 0))
    }

    @Test
    fun photoNames() {
        assertEquals("100_2.jpg", DiaryLogic.photoFileName(100, 2))
        assertEquals("100_2_1.jpg", DiaryLogic.photoFileName(100, 2, 1))
        assertEquals("diary/2026-10-10/100_2.jpg", DiaryLogic.photoRelativePath("2026-10-10", "100_2.jpg"))
    }

    @Test
    fun inSampleSize() {
        assertEquals(1, DiaryLogic.calcInSampleSize(300, 200, 360, 360))
        assertEquals(8, DiaryLogic.calcInSampleSize(4000, 3000, 360, 360))
        assertEquals(4, DiaryLogic.calcInSampleSize(4000, 3000, 500, 700))
        assertEquals(1, DiaryLogic.calcInSampleSize(0, 0, 360, 360))
    }

    @Test
    fun exifDateTime() {
        assertNotNull(DiaryLogic.parseExifDateTime("2026:10:10 12:34:56"))
        assertNull(DiaryLogic.parseExifDateTime("garbage"))
        assertNull(DiaryLogic.parseExifDateTime(null))
    }

    @Test
    fun datePickerMillisRoundTrip() {
        val ms = DiaryLogic.dateToUtcMillis("2026-10-10")
        assertEquals("2026-10-10", DiaryLogic.utcMillisToDate(ms))
    }
}
