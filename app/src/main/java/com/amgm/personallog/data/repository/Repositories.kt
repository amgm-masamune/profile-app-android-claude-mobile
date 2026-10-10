package com.amgm.personallog.data.repository

import androidx.room.withTransaction
import com.amgm.personallog.data.db.AppDatabase
import com.amgm.personallog.data.db.DiaryDao
import com.amgm.personallog.data.db.ExpenseDao
import com.amgm.personallog.data.db.ReceiptDao
import com.amgm.personallog.data.model.CategoryTotal
import com.amgm.personallog.data.model.DiaryEntry
import com.amgm.personallog.data.model.DiaryPhoto
import com.amgm.personallog.data.model.Expense
import com.amgm.personallog.data.model.Receipt
import com.amgm.personallog.data.model.ReceiptItem
import com.amgm.personallog.data.model.ReceiptStatus
import com.amgm.personallog.domain.LedgerCalc
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DiaryRepository @Inject constructor(
    private val dao: DiaryDao,
) {
    fun observeEntries(): Flow<List<DiaryEntry>> = dao.observeAll()
    fun observeEntriesBetween(from: String, to: String): Flow<List<DiaryEntry>> = dao.observeBetween(from, to)
    fun observeEntry(date: String): Flow<DiaryEntry?> = dao.observeByDate(date)
    fun observePhotos(entryId: Long): Flow<List<DiaryPhoto>> = dao.observePhotos(entryId)
    fun observeAllPhotos(): Flow<List<DiaryPhoto>> = dao.observeAllPhotos()

    /** その日の日記を保存(無ければ作成、あれば本文を更新)。entryId を返す。 */
    suspend fun saveText(date: String, text: String, now: Long = System.currentTimeMillis()): Long {
        val existing = dao.getByDate(date)
        return if (existing == null) {
            dao.insert(DiaryEntry(date = date, text = text, createdAt = now, updatedAt = now))
        } else {
            dao.update(existing.copy(text = text, updatedAt = now))
            existing.id
        }
    }

    /** 写真追加用: その日の日記が無ければ空本文で作って entryId を返す。 */
    suspend fun getOrCreateEntryId(date: String, now: Long = System.currentTimeMillis()): Long =
        dao.getByDate(date)?.id
            ?: dao.insert(DiaryEntry(date = date, text = "", createdAt = now, updatedAt = now))

    suspend fun deleteEntry(entry: DiaryEntry) = dao.delete(entry)

    suspend fun addPhoto(entryId: Long, relativePath: String, takenAt: Long? = null): Long {
        val order = dao.getPhotos(entryId).size
        return dao.insertPhoto(DiaryPhoto(entryId = entryId, filePath = relativePath, takenAt = takenAt, sortOrder = order))
    }

    suspend fun updatePhoto(photo: DiaryPhoto) = dao.updatePhoto(photo)
    suspend fun deletePhoto(photo: DiaryPhoto) = dao.deletePhoto(photo)
}

@Singleton
class ReceiptRepository @Inject constructor(
    private val db: AppDatabase,
    private val receiptDao: ReceiptDao,
    private val expenseDao: ExpenseDao,
) {
    fun observeReceipts(): Flow<List<Receipt>> = receiptDao.observeAll()
    fun observeReceipt(id: Long): Flow<Receipt?> = receiptDao.observeById(id)
    fun observeItems(receiptId: Long): Flow<List<ReceiptItem>> = receiptDao.observeItems(receiptId)
    suspend fun getReceipt(id: Long): Receipt? = receiptDao.getById(id)
    suspend fun getItems(receiptId: Long): List<ReceiptItem> = receiptDao.getItems(receiptId)

    /** レシートと品目を保存(id=0なら新規)。品目は丸ごと置き換える。receiptId を返す。 */
    suspend fun save(receipt: Receipt, items: List<ReceiptItem>): Long = db.withTransaction {
        val id = if (receipt.id == 0L) {
            receiptDao.insert(receipt)
        } else {
            receiptDao.update(receipt)
            receipt.id
        }
        receiptDao.deleteItemsByReceipt(id)
        receiptDao.insertItems(items.map { it.copy(id = 0, receiptId = id) })
        id
    }

    /** レシートを確定し、家計簿に Expense を作る(再確定しても二重にならないよう作り直す)。 */
    suspend fun confirm(receiptId: Long, perItem: Boolean, now: Long = System.currentTimeMillis()) {
        db.withTransaction {
            val receipt = receiptDao.getById(receiptId) ?: return@withTransaction
            val items = receiptDao.getItems(receiptId)
            expenseDao.deleteByReceiptId(receiptId)
            expenseDao.insertAll(LedgerCalc.expensesFromReceipt(receipt, items, perItem, now))
            receiptDao.update(receipt.copy(status = ReceiptStatus.CONFIRMED))
        }
    }

    /** レシート削除。紐づく Expense は receiptId が NULL になって残る(家計簿は消さない)。 */
    suspend fun delete(receipt: Receipt) = receiptDao.delete(receipt)
}

@Singleton
class ExpenseRepository @Inject constructor(
    private val dao: ExpenseDao,
) {
    fun observeAll(): Flow<List<Expense>> = dao.observeAll()
    fun observeBetween(from: String, to: String): Flow<List<Expense>> = dao.observeBetween(from, to)

    /** yearMonth は "yyyy-MM"。 */
    fun observeMonth(yearMonth: String): Flow<List<Expense>> {
        val (from, to) = LedgerCalc.monthRange(yearMonth)
        return dao.observeBetween(from, to)
    }

    fun observeMonthCategoryTotals(yearMonth: String): Flow<List<CategoryTotal>> {
        val (from, to) = LedgerCalc.monthRange(yearMonth)
        return dao.observeCategoryTotals(from, to)
    }

    fun observeMonthTotal(yearMonth: String): Flow<Long> {
        val (from, to) = LedgerCalc.monthRange(yearMonth)
        return dao.observeTotal(from, to)
    }

    suspend fun add(expense: Expense): Long = dao.insert(expense)
    suspend fun update(expense: Expense) = dao.update(expense)
    suspend fun delete(expense: Expense) = dao.delete(expense)
    suspend fun get(id: Long): Expense? = dao.getById(id)
}
