package com.amgm.personallog.backup

import com.amgm.personallog.data.model.DiaryEntry
import com.amgm.personallog.data.model.DiaryPhoto
import com.amgm.personallog.data.model.Expense
import com.amgm.personallog.data.model.Receipt
import com.amgm.personallog.data.model.ReceiptItem
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** バックアップZIPのフォーマット版。互換性を壊す変更をしたら上げる。 */
const val BACKUP_FORMAT_VERSION = 1
const val BACKUP_DATA_ENTRY = "data.json"
const val BACKUP_FILES_PREFIX = "files/"

val BackupJson: Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    prettyPrint = false
}

/** data.json の中身。設定(APIキー等)は含めない。 */
@Serializable
data class BackupData(
    val formatVersion: Int = BACKUP_FORMAT_VERSION,
    val dbVersion: Int = 1,
    val exportedAt: Long = 0,
    val diaryEntries: List<BackupDiaryEntry> = emptyList(),
    val diaryPhotos: List<BackupDiaryPhoto> = emptyList(),
    val receipts: List<BackupReceipt> = emptyList(),
    val receiptItems: List<BackupReceiptItem> = emptyList(),
    val expenses: List<BackupExpense> = emptyList(),
)

@Serializable
data class BackupDiaryEntry(val id: Long, val date: String, val text: String, val createdAt: Long, val updatedAt: Long) {
    fun toEntity() = DiaryEntry(id, date, text, createdAt, updatedAt)
    companion object {
        fun from(e: DiaryEntry) = BackupDiaryEntry(e.id, e.date, e.text, e.createdAt, e.updatedAt)
    }
}

@Serializable
data class BackupDiaryPhoto(val id: Long, val entryId: Long, val filePath: String, val takenAt: Long? = null, val sortOrder: Int = 0) {
    fun toEntity() = DiaryPhoto(id, entryId, filePath, takenAt, sortOrder)
    companion object {
        fun from(e: DiaryPhoto) = BackupDiaryPhoto(e.id, e.entryId, e.filePath, e.takenAt, e.sortOrder)
    }
}

@Serializable
data class BackupReceipt(
    val id: Long,
    val purchasedAt: String,
    val storeName: String,
    val totalAmount: Int,
    val imagePath: String,
    val rawJson: String,
    val status: String,
    val createdAt: Long,
) {
    fun toEntity() = Receipt(id, purchasedAt, storeName, totalAmount, imagePath, rawJson, status, createdAt)
    companion object {
        fun from(e: Receipt) = BackupReceipt(e.id, e.purchasedAt, e.storeName, e.totalAmount, e.imagePath, e.rawJson, e.status, e.createdAt)
    }
}

@Serializable
data class BackupReceiptItem(
    val id: Long,
    val receiptId: Long,
    val name: String,
    val quantity: Double = 1.0,
    val unitPrice: Int = 0,
    val amount: Int = 0,
    val category: String,
) {
    fun toEntity() = ReceiptItem(id, receiptId, name, quantity, unitPrice, amount, category)
    companion object {
        fun from(e: ReceiptItem) = BackupReceiptItem(e.id, e.receiptId, e.name, e.quantity, e.unitPrice, e.amount, e.category)
    }
}

@Serializable
data class BackupExpense(
    val id: Long,
    val date: String,
    val amount: Int,
    val category: String,
    val memo: String = "",
    val receiptId: Long? = null,
    val createdAt: Long,
) {
    fun toEntity() = Expense(id, date, amount, category, memo, receiptId, createdAt)
    companion object {
        fun from(e: Expense) = BackupExpense(e.id, e.date, e.amount, e.category, e.memo, e.receiptId, e.createdAt)
    }
}

enum class ImportMode { MERGE, REPLACE }

/** 進捗表示用。total が 0 以下なら不定。 */
data class BackupProgress(val message: String, val current: Int = 0, val total: Int = 0)

data class ExportResult(val diaryEntries: Int, val receipts: Int, val expenses: Int, val files: Int)

data class ImportResult(
    val diaryEntries: Int,
    val diaryPhotos: Int,
    val receipts: Int,
    val expenses: Int,
    val skipped: Int,
    val files: Int,
)

/** ユーザーに理由を見せてよい失敗。 */
class BackupException(message: String, cause: Throwable? = null) : Exception(message, cause)
