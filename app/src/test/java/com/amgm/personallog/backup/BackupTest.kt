package com.amgm.personallog.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class BackupTest {
    @Test
    fun json_roundTrip() {
        val data = BackupData(
            formatVersion = BACKUP_FORMAT_VERSION,
            dbVersion = 1,
            exportedAt = 123L,
            diaryEntries = listOf(BackupDiaryEntry(1, "2026-10-01", "今日は\"晴れ\"\n改行あり", 10, 20)),
            diaryPhotos = listOf(BackupDiaryPhoto(1, 1, "diary/a.jpg", null, 0)),
            receipts = listOf(BackupReceipt(5, "2026-10-02 12:30", "店", 980, "receipts/r.jpg", "{\"a\":1}", "confirmed", 30)),
            receiptItems = listOf(BackupReceiptItem(1, 5, "牛乳", 1.5, 100, 150, "食費")),
            expenses = listOf(
                BackupExpense(1, "2026-10-02", 980, "食費", "店", 5, 40),
                BackupExpense(2, "2026-10-03", 500, "その他", "", null, 41),
            ),
        )
        val text = BackupJson.encodeToString(BackupData.serializer(), data)
        val back = BackupJson.decodeFromString(BackupData.serializer(), text)
        assertEquals(data, back)
        assertTrue(text.contains("\"formatVersion\""))
        assertTrue(text.contains("\"dbVersion\""))
    }

    @Test
    fun json_ignoresUnknownKeysAndUsesDefaults() {
        val text = """{"formatVersion":1,"future":"x","expenses":[{"id":1,"date":"2026-10-01","amount":1,"category":"食費","createdAt":0,"extra":true}]}"""
        val d = BackupJson.decodeFromString(BackupData.serializer(), text)
        assertEquals(1, d.expenses.size)
        assertEquals("", d.expenses[0].memo)
        assertNull(d.expenses[0].receiptId)
    }

    @Test
    fun entityConversion_isLossless() {
        val e = BackupExpense(3, "2026-01-01", 10, "食費", "m", 7, 99)
        assertEquals(e, BackupExpense.from(e.toEntity()))
        val r = BackupReceipt(1, "2026-01-01", "s", 1, "receipts/x.jpg", "{}", "draft", 2)
        assertEquals(r, BackupReceipt.from(r.toEntity()))
    }

    @Test
    fun safeRelativePath_acceptsNormal() {
        assertEquals("diary/a.jpg", BackupPaths.safeRelativePath("diary/a.jpg"))
        assertEquals("receipts/2026/10/x.png", BackupPaths.safeRelativePath("receipts/2026/10/x.png"))
        assertEquals("diary/a.jpg", BackupPaths.entryNameToRelative("files/diary/a.jpg"))
    }

    @Test
    fun zipSlip_isRejected() {
        val bad = listOf(
            "files/../evil.txt",
            "files/diary/../../evil.txt",
            "files//etc/passwd",
            "files/diary/../receipts/../../x",
            "/etc/passwd",
            "files/diary\\..\\x",
            "files/C:/x.jpg",
            "files/other/a.jpg",
            "files/a.jpg",
            "files/diary/./a.jpg",
            "files/",
            "",
        )
        for (name in bad) {
            assertNull("should reject: $name", BackupPaths.entryNameToRelative(name))
        }
        assertNull(BackupPaths.entryNameToRelative("data.json"))
        assertNull(BackupPaths.safeRelativePath("diary/\u0000x"))
    }

    @Test
    fun storedPath_validation() {
        assertTrue(BackupPaths.isSafeStoredPath(""))
        assertTrue(BackupPaths.isSafeStoredPath("diary/a.jpg"))
        assertFalse(BackupPaths.isSafeStoredPath("../a.jpg"))
        assertFalse(BackupPaths.isSafeStoredPath("diary/../../a.jpg"))
        assertFalse(BackupPaths.isSafeStoredPath("/data/x.jpg"))
    }

    @Test
    fun checkVersion_rejectsNewer() {
        BackupManager.checkVersion(BACKUP_FORMAT_VERSION)
        try {
            BackupManager.checkVersion(BACKUP_FORMAT_VERSION + 1)
            fail("expected BackupException")
        } catch (e: BackupException) {
            assertTrue(e.message!!.contains("新しい"))
        }
    }
}
