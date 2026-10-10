package com.amgm.personallog.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.amgm.personallog.data.model.CategoryTotal
import com.amgm.personallog.data.model.DiaryEntry
import com.amgm.personallog.data.model.DiaryPhoto
import com.amgm.personallog.data.model.Expense
import com.amgm.personallog.data.model.Receipt
import com.amgm.personallog.data.model.ReceiptItem
import kotlinx.coroutines.flow.Flow

@Dao
interface DiaryDao {
    @Query("SELECT * FROM diary_entries ORDER BY date DESC")
    fun observeAll(): Flow<List<DiaryEntry>>

    @Query("SELECT * FROM diary_entries WHERE date BETWEEN :from AND :to ORDER BY date DESC")
    fun observeBetween(from: String, to: String): Flow<List<DiaryEntry>>

    @Query("SELECT * FROM diary_entries WHERE date = :date LIMIT 1")
    fun observeByDate(date: String): Flow<DiaryEntry?>

    @Query("SELECT * FROM diary_entries WHERE date = :date LIMIT 1")
    suspend fun getByDate(date: String): DiaryEntry?

    @Query("SELECT * FROM diary_entries WHERE id = :id")
    suspend fun getById(id: Long): DiaryEntry?

    @Insert
    suspend fun insert(entry: DiaryEntry): Long

    @Update
    suspend fun update(entry: DiaryEntry)

    @Delete
    suspend fun delete(entry: DiaryEntry)

    @Query("SELECT * FROM diary_photos WHERE entryId = :entryId ORDER BY sortOrder, id")
    fun observePhotos(entryId: Long): Flow<List<DiaryPhoto>>

    @Query("SELECT * FROM diary_photos ORDER BY id")
    fun observeAllPhotos(): Flow<List<DiaryPhoto>>

    @Query("SELECT * FROM diary_photos WHERE entryId = :entryId ORDER BY sortOrder, id")
    suspend fun getPhotos(entryId: Long): List<DiaryPhoto>

    @Insert
    suspend fun insertPhoto(photo: DiaryPhoto): Long

    @Update
    suspend fun updatePhoto(photo: DiaryPhoto)

    @Delete
    suspend fun deletePhoto(photo: DiaryPhoto)
}

@Dao
interface ReceiptDao {
    @Query("SELECT * FROM receipts ORDER BY purchasedAt DESC, id DESC")
    fun observeAll(): Flow<List<Receipt>>

    @Query("SELECT * FROM receipts WHERE purchasedAt >= :from AND purchasedAt < :toExclusive ORDER BY purchasedAt DESC, id DESC")
    fun observeBetween(from: String, toExclusive: String): Flow<List<Receipt>>

    @Query("SELECT * FROM receipts WHERE id = :id")
    fun observeById(id: Long): Flow<Receipt?>

    @Query("SELECT * FROM receipts WHERE id = :id")
    suspend fun getById(id: Long): Receipt?

    @Insert
    suspend fun insert(receipt: Receipt): Long

    @Update
    suspend fun update(receipt: Receipt)

    @Delete
    suspend fun delete(receipt: Receipt)

    @Query("SELECT * FROM receipt_items WHERE receiptId = :receiptId ORDER BY id")
    fun observeItems(receiptId: Long): Flow<List<ReceiptItem>>

    @Query("SELECT * FROM receipt_items WHERE receiptId = :receiptId ORDER BY id")
    suspend fun getItems(receiptId: Long): List<ReceiptItem>

    @Insert
    suspend fun insertItems(items: List<ReceiptItem>)

    @Update
    suspend fun updateItem(item: ReceiptItem)

    @Delete
    suspend fun deleteItem(item: ReceiptItem)

    @Query("DELETE FROM receipt_items WHERE receiptId = :receiptId")
    suspend fun deleteItemsByReceipt(receiptId: Long)
}

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY date DESC, id DESC")
    fun observeAll(): Flow<List<Expense>>

    /** from / to は "yyyy-MM-dd"(両端を含む)。 */
    @Query("SELECT * FROM expenses WHERE date BETWEEN :from AND :to ORDER BY date DESC, id DESC")
    fun observeBetween(from: String, to: String): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun getById(id: Long): Expense?

    @Query("SELECT * FROM expenses WHERE receiptId = :receiptId ORDER BY id")
    suspend fun getByReceiptId(receiptId: Long): List<Expense>

    @Insert
    suspend fun insert(expense: Expense): Long

    @Insert
    suspend fun insertAll(expenses: List<Expense>)

    @Update
    suspend fun update(expense: Expense)

    @Delete
    suspend fun delete(expense: Expense)

    @Query("DELETE FROM expenses WHERE receiptId = :receiptId")
    suspend fun deleteByReceiptId(receiptId: Long)

    /** 月別(期間別)のカテゴリ集計。支出の多い順。 */
    @Query(
        "SELECT category AS category, SUM(amount) AS total FROM expenses " +
            "WHERE date BETWEEN :from AND :to GROUP BY category ORDER BY total DESC",
    )
    fun observeCategoryTotals(from: String, to: String): Flow<List<CategoryTotal>>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM expenses WHERE date BETWEEN :from AND :to")
    fun observeTotal(from: String, to: String): Flow<Long>
}
