package com.amgm.personallog.receipt

import com.amgm.personallog.data.model.ReceiptStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReceiptParserTest {
    private val sample = """{"date":"2026-10-09","time":"18:32","store":"スーパーA","items":[{"name":"牛乳","quantity":1,"unitPrice":198,"amount":198,"category":"食費"},{"name":"洗剤","quantity":2,"unitPrice":300,"amount":600,"category":"日用品"},{"name":"クーポン値引き","quantity":1,"unitPrice":-50,"amount":-50,"category":"食費"}],"total":748,"note":null}"""

    private fun success(text: String): ParsedReceipt {
        val r = ReceiptParser.parse(text)
        assertTrue("expected success but was $r", r is ParseResult.Success)
        return (r as ParseResult.Success).receipt
    }

    @Test
    fun parsesPlainJson() {
        val p = success(sample)
        assertEquals("2026-10-09", p.date)
        assertEquals("18:32", p.time)
        assertEquals("スーパーA", p.store)
        assertEquals(3, p.items.size)
        assertEquals(748, p.total)
        assertNull(p.note)
        assertEquals(2.0, p.items[1].quantity, 0.0)
    }

    @Test
    fun parsesFencedJsonWithSurroundingText() {
        val p = success("読み取り結果です。\n```json\n$sample\n```\n以上です。")
        assertEquals("スーパーA", p.store)
        assertEquals(3, p.items.size)
    }

    @Test
    fun keepsNegativeDiscountAmount() {
        val p = success(sample)
        assertEquals(-50, p.items[2].amount)
        assertEquals(748L, ReceiptCalc.itemsSum(p.items.map { it.amount }))
        assertNull(ReceiptCalc.mismatch(p.items.map { it.amount }, p.total))
    }

    @Test
    fun brokenJsonFails() {
        assertTrue(ReceiptParser.parse("""{"date":"2026-10-09","items":[{"name":""") is ParseResult.Failure)
        assertTrue(ReceiptParser.parse("読み取れませんでした") is ParseResult.Failure)
        assertTrue(ReceiptParser.parse("""{"date": 2026-10-09, }""") is ParseResult.Failure)
    }

    @Test
    fun extractIgnoresBracesInStrings() {
        val t = """前置き {"store":"A}店","items":[]} 後ろ {"x":1}"""
        assertEquals("""{"store":"A}店","items":[]}""", ReceiptParser.extractJsonObject(t))
    }

    @Test
    fun toleratesNullsStringsAndMissingFields() {
        val p = success("""{"date":"2026/10/9","time":null,"store":null,"items":[{"name":"X","amount":"1,200円","category":"交通"}],"total":null}""")
        assertEquals("2026-10-09", p.date)
        assertNull(p.time)
        assertEquals("", p.store)
        assertEquals(1200, p.items[0].amount)
        assertEquals(1.0, p.items[0].quantity, 0.0)
        assertEquals("交通費", p.items[0].category)
        assertNull(p.total)
    }

    @Test
    fun unknownCategoryBecomesOther() {
        assertEquals("その他", ReceiptParser.normalizeCategory("ペット"))
        assertEquals("未分類", ReceiptParser.normalizeCategory(null))
        assertEquals("医療・健康", ReceiptParser.normalizeCategory("医療"))
    }

    @Test
    fun mismatchDetected() {
        assertEquals(10L, ReceiptCalc.mismatch(listOf(100, 200), 310))
        assertNull(ReceiptCalc.mismatch(listOf(100, 200), 300))
        assertNull(ReceiptCalc.mismatch(listOf(100, 200), null))
    }

    @Test
    fun validators() {
        assertTrue(ReceiptCalc.isValidDate("2026-10-10"))
        assertTrue(!ReceiptCalc.isValidDate("2026-13-10"))
        assertTrue(ReceiptCalc.isValidTime("09:05"))
        assertTrue(!ReceiptCalc.isValidTime("9:5"))
    }

    @Test
    fun requestBodyStructure() {
        val body = ClaudeRequest.buildBody("claude-haiku-5-5", "QUJD")
        val root = ReceiptParser.parseObject(body)
        assertNotNull(root)
        val s = root.toString()
        assertTrue(s.contains("\"model\":\"claude-haiku-5-5\""))
        assertTrue(s.contains("\"max_tokens\":4096"))
        assertTrue(s.contains("\"media_type\":\"image/jpeg\""))
        assertTrue(s.contains("\"data\":\"QUJD\""))
        assertTrue(s.contains("\"type\":\"base64\""))
        assertTrue(s.indexOf("\"type\":\"image\"") < s.indexOf("\"type\":\"text\""))
        assertTrue(ClaudeRequest.PROMPT.contains("JSONだけ"))
    }

    @Test
    fun responseTextExtraction() {
        val resp = """{"id":"m","type":"message","content":[{"type":"text","text":"{\"a\":1}"}],"stop_reason":"end_turn"}"""
        assertEquals("{\"a\":1}", ClaudeRequest.extractText(resp))
        assertNull(ClaudeRequest.extractText("not json"))
        assertEquals("bad key", ClaudeRequest.extractErrorMessage("""{"type":"error","error":{"type":"authentication_error","message":"bad key"}}"""))
    }

    @Test
    fun httpErrorMessages() {
        assertTrue(ClaudeRequest.messageForHttpError(401, null).contains("APIキー"))
        assertTrue(ClaudeRequest.messageForHttpError(429, null).contains("429"))
        assertTrue(ClaudeRequest.messageForHttpError(503, null).contains("503"))
    }

    @Test
    fun mapperBuildsDraftReceipts() {
        val parsed = success(sample)
        val (r, items) = ReceiptMapper.draftReceipt(ScanOutcome.Parsed(parsed, sample), "receipts/a.jpg", 1L, today = "2026-01-01")
        assertEquals("2026-10-09 18:32", r.purchasedAt)
        assertEquals(748, r.totalAmount)
        assertEquals(ReceiptStatus.DRAFT, r.status)
        assertEquals(3, items.size)
        val (r2, items2) = ReceiptMapper.draftReceipt(ScanOutcome.Unparsable("oops", "x"), "receipts/a.jpg", 1L, today = "2026-01-01")
        assertEquals("2026-01-01", r2.purchasedAt)
        assertEquals("oops", r2.rawJson)
        assertTrue(items2.isEmpty())
    }
}
