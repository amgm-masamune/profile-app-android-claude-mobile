package com.amgm.personallog.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** 日記(1日1件)。date は "yyyy-MM-dd"。 */
@Entity(tableName = "diary_entries", indices = [Index(value = ["date"], unique = true)])
data class DiaryEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val text: String,
    val createdAt: Long,
    val updatedAt: Long,
)

/** 日記に紐づく写真。filePath はアプリ内部ストレージ(filesDir)からの相対パス。 */
@Entity(
    tableName = "diary_photos",
    foreignKeys = [
        ForeignKey(
            entity = DiaryEntry::class,
            parentColumns = ["id"],
            childColumns = ["entryId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("entryId")],
)
data class DiaryPhoto(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val entryId: Long,
    val filePath: String,
    val takenAt: Long? = null,
    val sortOrder: Int = 0,
)

/** レシート。purchasedAt は "yyyy-MM-dd" または "yyyy-MM-dd HH:mm"。status は ReceiptStatus。 */
@Entity(tableName = "receipts")
data class Receipt(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val purchasedAt: String,
    val storeName: String,
    val totalAmount: Int,
    val imagePath: String,
    /** Claude応答の原文(JSON文字列) */
    val rawJson: String,
    val status: String = ReceiptStatus.DRAFT,
    val createdAt: Long,
)

object ReceiptStatus {
    const val DRAFT = "draft"
    const val CONFIRMED = "confirmed"
}

@Entity(
    tableName = "receipt_items",
    foreignKeys = [
        ForeignKey(
            entity = Receipt::class,
            parentColumns = ["id"],
            childColumns = ["receiptId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("receiptId")],
)
data class ReceiptItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val receiptId: Long,
    val name: String,
    val quantity: Double = 1.0,
    val unitPrice: Int = 0,
    val amount: Int = 0,
    val category: String = Categories.UNCATEGORIZED,
)

/** 家計簿の1行。amount は円で、支出は正の値。receiptId はレシート由来の場合のみ。 */
@Entity(
    tableName = "expenses",
    foreignKeys = [
        ForeignKey(
            entity = Receipt::class,
            parentColumns = ["id"],
            childColumns = ["receiptId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("receiptId"), Index("date")],
)
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val amount: Int,
    val category: String = Categories.UNCATEGORIZED,
    val memo: String = "",
    val receiptId: Long? = null,
    val createdAt: Long,
)

/** 月別カテゴリ集計の結果行(Roomのクエリ結果としても使う)。 */
data class CategoryTotal(
    val category: String,
    val total: Long,
)

object Categories {
    const val UNCATEGORIZED = "未分類"
    val DEFAULTS = listOf(
        "食費", "日用品", "交通費", "外食", "医療・健康", "衣服・美容",
        "趣味・娯楽", "光熱費・通信費", "住居", "その他", UNCATEGORIZED,
    )
}
