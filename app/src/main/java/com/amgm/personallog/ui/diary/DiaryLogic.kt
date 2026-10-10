package com.amgm.personallog.ui.diary

import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Locale

/** 日記機能の純粋ロジック(JVMでテスト可能)。 */
object DiaryLogic {
    private val WEEKDAYS = arrayOf("月", "火", "水", "木", "金", "土", "日")

    fun today(): String = LocalDate.now().toString()

    fun isValidDate(s: String?): Boolean =
        s != null && s.length == 10 && runCatching { LocalDate.parse(s) }.isSuccess

    /** "2026-10-10" -> "2026年10月10日(土)"。不正な文字列はそのまま返す。 */
    fun formatDateLabel(date: String): String {
        val d = runCatching { LocalDate.parse(date) }.getOrNull() ?: return date
        return "${d.year}年${d.monthValue}月${d.dayOfMonth}日(${WEEKDAYS[d.dayOfWeek.value - 1]})"
    }

    /** 一覧用の本文冒頭。改行は空白に畳み、max文字を超えたら末尾に「…」。 */
    fun previewText(text: String, max: Int = 80): String {
        val flat = text.replace(Regex("\\s+"), " ").trim()
        return if (flat.length <= max) flat else flat.take(max) + "…"
    }

    /** 本文も写真も無い(保存しない/作らない)状態か。 */
    fun isEmptyEntry(text: String, photoCount: Int): Boolean = text.isBlank() && photoCount == 0

    /** 写真ファイル名。attempt>0 は衝突回避用の接尾辞。 */
    fun photoFileName(nowMillis: Long, index: Int, attempt: Int = 0): String =
        if (attempt == 0) "${nowMillis}_$index.jpg" else "${nowMillis}_${index}_$attempt.jpg"

    /** filesDir からの相対パス。 */
    fun photoRelativePath(date: String, fileName: String): String = "diary/$date/$fileName"

    /** デコード時の inSampleSize(2のべき乗、結果が要求サイズ以上を保つ最大値)。 */
    fun calcInSampleSize(srcWidth: Int, srcHeight: Int, reqWidth: Int, reqHeight: Int): Int {
        if (srcWidth <= 0 || srcHeight <= 0 || reqWidth <= 0 || reqHeight <= 0) return 1
        var sample = 1
        while (srcWidth / (sample * 2) >= reqWidth && srcHeight / (sample * 2) >= reqHeight) {
            sample *= 2
        }
        return sample
    }

    /** EXIFの "yyyy:MM:dd HH:mm:ss" をエポックミリ秒に。失敗は null。 */
    fun parseExifDateTime(value: String?): Long? {
        if (value.isNullOrBlank()) return null
        return runCatching {
            val f = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US)
            f.isLenient = false
            f.parse(value.trim())?.time
        }.getOrNull()
    }

    /** DatePicker は UTC 0時のミリ秒を使う。 */
    fun dateToUtcMillis(date: String): Long =
        LocalDate.parse(date).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    fun utcMillisToDate(millis: Long): String =
        Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().toString()
}
