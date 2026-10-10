package com.amgm.personallog.receipt

import com.amgm.personallog.data.model.Categories
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.math.roundToInt

/** Claude が返した品目1行(正規化後)。値引きは amount が負。 */
data class ParsedItem(
    val name: String,
    val quantity: Double,
    val unitPrice: Int,
    val amount: Int,
    val category: String,
)

data class ParsedReceipt(
    val date: String?,
    val time: String?,
    val store: String,
    val items: List<ParsedItem>,
    val total: Int?,
    val note: String?,
)

sealed interface ParseResult {
    data class Success(val receipt: ParsedReceipt) : ParseResult
    data class Failure(val reason: String) : ParseResult
}

/** Claude 応答テキストからのJSON抽出とパース。純Kotlin(JVMテスト可能)。 */
object ReceiptParser {
    /** プロンプトのカテゴリ名 -> アプリのカテゴリ名(Categories.DEFAULTS)。 */
    private val categoryMap = mapOf(
        "食費" to "食費",
        "外食" to "外食",
        "日用品" to "日用品",
        "交通" to "交通費",
        "交通費" to "交通費",
        "医療" to "医療・健康",
        "衣服" to "衣服・美容",
        "娯楽" to "趣味・娯楽",
        "光熱費" to "光熱費・通信費",
        "その他" to "その他",
    )

    fun normalizeCategory(raw: String?): String {
        val s = raw?.trim().orEmpty()
        if (s.isEmpty()) return Categories.UNCATEGORIZED
        if (s in Categories.DEFAULTS) return s
        return categoryMap[s] ?: "その他"
    }

    /** 最初の { から対応する } までを返す(文字列内の括弧は無視)。閉じていなければ null。 */
    fun extractJsonObject(text: String): String? {
        val start = text.indexOf('{')
        if (start < 0) return null
        var depth = 0
        var inString = false
        var escape = false
        for (i in start until text.length) {
            val c = text[i]
            if (inString) {
                when {
                    escape -> escape = false
                    c == '\\' -> escape = true
                    c == '"' -> inString = false
                }
            } else {
                when (c) {
                    '"' -> inString = true
                    '{' -> depth++
                    '}' -> {
                        depth--
                        if (depth == 0) return text.substring(start, i + 1)
                    }
                }
            }
        }
        return null
    }

    fun parse(text: String): ParseResult {
        val jsonText = extractJsonObject(text) ?: return ParseResult.Failure("JSONが見つかりませんでした")
        val obj = parseObject(jsonText) ?: return ParseResult.Failure("JSONの形式が正しくありません")

        val items = (obj["items"] as? JsonArray).orEmpty().mapNotNull { parseItem(it) }
        val date = obj.str("date")?.let { normalizeDate(it) }
        val time = obj.str("time")?.let { normalizeTime(it) }
        return ParseResult.Success(
            ParsedReceipt(
                date = date,
                time = time,
                store = obj.str("store").orEmpty(),
                items = items,
                total = obj["total"].asInt(),
                note = obj.str("note"),
            ),
        )
    }

    fun parseObject(text: String): JsonObject? =
        try {
            Json.parseToJsonElement(text) as? JsonObject
        } catch (e: Exception) {
            null
        }

    private fun parseItem(e: JsonElement): ParsedItem? {
        val o = e as? JsonObject ?: return null
        val qty = o["quantity"].asDouble()?.takeIf { it > 0 } ?: 1.0
        val unit = o["unitPrice"].asInt()
        val amount = o["amount"].asInt() ?: unit?.let { (it * qty).roundToInt() } ?: 0
        return ParsedItem(
            name = o.str("name").orEmpty(),
            quantity = qty,
            unitPrice = unit ?: amount,
            amount = amount,
            category = normalizeCategory(o.str("category")),
        )
    }

    private fun JsonObject.str(key: String): String? {
        val p = this[key] as? JsonPrimitive ?: return null
        if (p is JsonNull) return null
        return p.content.trim().takeIf { it.isNotEmpty() && !it.equals("null", true) }
    }

    private fun JsonElement?.asDouble(): Double? {
        val p = this as? JsonPrimitive ?: return null
        if (p is JsonNull) return null
        return p.content.replace(",", "").replace("円", "").replace("\u00A5", "").replace("\uFFE5", "").trim().toDoubleOrNull()
    }

    private fun JsonElement?.asInt(): Int? = asDouble()?.roundToInt()

    private val dateRegex = Regex("""(\d{4})[-/.年](\d{1,2})[-/.月](\d{1,2})""")

    /** "2026/1/5" 等を "2026-01-05" に。解釈できなければ null。 */
    fun normalizeDate(s: String): String? {
        val m = dateRegex.find(s) ?: return null
        val (y, mo, d) = m.destructured
        val r = "%04d-%02d-%02d".format(y.toInt(), mo.toInt(), d.toInt())
        return if (ReceiptCalc.isValidDate(r)) r else null
    }

    private val timeRegex = Regex("""(\d{1,2}):(\d{2})""")

    fun normalizeTime(s: String): String? {
        val m = timeRegex.find(s) ?: return null
        val (h, mi) = m.destructured
        if (h.toInt() > 23 || mi.toInt() > 59) return null
        return "%02d:%02d".format(h.toInt(), mi.toInt())
    }
}

/** 金額検証などの純ロジック。 */
object ReceiptCalc {
    fun itemsSum(amounts: List<Int>): Long = amounts.sumOf { it.toLong() }

    /** 品目合計と total が一致しないなら差(total - 品目合計)を返す。一致 or total 不明なら null。 */
    fun mismatch(amounts: List<Int>, total: Int?): Long? {
        if (total == null) return null
        val diff = total.toLong() - itemsSum(amounts)
        return if (diff == 0L) null else diff
    }

    fun isValidDate(s: String): Boolean = try {
        java.time.LocalDate.parse(s)
        true
    } catch (e: Exception) {
        false
    }

    fun isValidTime(s: String): Boolean = Regex("""([01]\d|2[0-3]):[0-5]\d""").matches(s)
}
