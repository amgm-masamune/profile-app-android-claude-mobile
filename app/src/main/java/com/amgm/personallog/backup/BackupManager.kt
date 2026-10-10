package com.amgm.personallog.backup

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.room.withTransaction
import com.amgm.personallog.data.db.AppDatabase
import com.amgm.personallog.data.db.DiaryDao
import com.amgm.personallog.data.db.ExpenseDao
import com.amgm.personallog.data.db.ReceiptDao
import com.amgm.personallog.data.model.Receipt
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject

/** 全データ+画像のZIPエクスポート/インポート。設定(APIキー)は含めない。 */
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: AppDatabase,
    private val diaryDao: DiaryDao,
    private val receiptDao: ReceiptDao,
    private val expenseDao: ExpenseDao,
) {
    private val filesDir: File get() = context.filesDir

    // ---------------------------------------------------------------- export

    suspend fun export(uri: Uri, onProgress: (BackupProgress) -> Unit): ExportResult = withContext(Dispatchers.IO) {
        try {
            onProgress(BackupProgress("データを集めています"))
            val entries = diaryDao.observeAll().first()
            val photos = diaryDao.observeAllPhotos().first()
            val receipts = receiptDao.observeAll().first()
            val items = receipts.flatMap { receiptDao.getItems(it.id) }
            val expenses = expenseDao.observeAll().first()
            val data = BackupData(
                formatVersion = BACKUP_FORMAT_VERSION,
                dbVersion = db.openHelper.readableDatabase.version,
                exportedAt = System.currentTimeMillis(),
                diaryEntries = entries.map { BackupDiaryEntry.from(it) },
                diaryPhotos = photos.map { BackupDiaryPhoto.from(it) },
                receipts = receipts.map { BackupReceipt.from(it) },
                receiptItems = items.map { BackupReceiptItem.from(it) },
                expenses = expenses.map { BackupExpense.from(it) },
            )
            val json = BackupJson.encodeToString(BackupData.serializer(), data).toByteArray(Charsets.UTF_8)
            val files = collectFiles()

            val out = context.contentResolver.openOutputStream(uri, "wt")
                ?: throw BackupException("保存先を開けませんでした")
            ZipOutputStream(BufferedOutputStream(out)).use { zip ->
                onProgress(BackupProgress("データを書き込んでいます"))
                zip.putNextEntry(ZipEntry(BACKUP_DATA_ENTRY))
                zip.write(json)
                zip.closeEntry()
                files.forEachIndexed { i, (rel, file) ->
                    currentCoroutineContext().ensureActive()
                    onProgress(BackupProgress("画像を書き込んでいます", i + 1, files.size))
                    zip.putNextEntry(ZipEntry(BACKUP_FILES_PREFIX + rel))
                    file.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                }
            }
            ExportResult(entries.size, receipts.size, expenses.size, files.size)
        } catch (e: Throwable) {
            try {
                DocumentsContract.deleteDocument(context.contentResolver, uri)
            } catch (_: Throwable) {
                // 削除できなくても元のエラーを優先する
            }
            throw e
        }
    }

    /** filesDir 配下の対象ファイルを (相対パス, File) で列挙。 */
    private fun collectFiles(): List<Pair<String, File>> {
        val result = ArrayList<Pair<String, File>>()
        for (dirName in BackupPaths.ROOT_DIRS) {
            val root = File(filesDir, dirName)
            if (!root.isDirectory) continue
            root.walkTopDown().filter { it.isFile }.forEach { f ->
                val rel = f.relativeTo(filesDir).path.replace(File.separatorChar, '/')
                if (BackupPaths.safeRelativePath(rel) != null) result.add(rel to f)
            }
        }
        return result.sortedBy { it.first }
    }

    // ---------------------------------------------------------------- import

    suspend fun importZip(uri: Uri, mode: ImportMode, onProgress: (BackupProgress) -> Unit): ImportResult =
        withContext(Dispatchers.IO) {
            val tempDir = File(context.cacheDir, "import_" + System.currentTimeMillis())
            try {
                if (!tempDir.mkdirs()) throw BackupException("一時フォルダを作れませんでした")
                onProgress(BackupProgress("ZIPを読み込んでいます"))
                val input = context.contentResolver.openInputStream(uri)
                    ?: throw BackupException("ファイルを開けませんでした")
                var data: BackupData? = null
                var fileCount = 0
                ZipInputStream(BufferedInputStream(input)).use { zip ->
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val entry = zip.nextEntry ?: break
                        val name = entry.name
                        if (entry.isDirectory) continue
                        if (name == BACKUP_DATA_ENTRY) {
                            data = parseData(readLimited(zip, MAX_JSON_BYTES))
                        } else if (name.startsWith(BACKUP_FILES_PREFIX)) {
                            val rel = BackupPaths.entryNameToRelative(name)
                                ?: throw BackupException("安全でないファイル名が含まれているため取り込みを中止しました: $name")
                            val dest = File(tempDir, rel)
                            if (!isInside(tempDir, dest)) {
                                throw BackupException("安全でないファイル名が含まれているため取り込みを中止しました: $name")
                            }
                            dest.parentFile?.mkdirs()
                            dest.outputStream().use { zip.copyTo(it) }
                            fileCount++
                            if (fileCount % 10 == 0) onProgress(BackupProgress("画像を展開しています", fileCount))
                        }
                        // それ以外のエントリは無視
                    }
                }
                val backup = data ?: throw BackupException("このZIPは個人記録のバックアップではありません(data.json がありません)")
                validatePaths(backup)

                onProgress(BackupProgress("データベースに取り込んでいます"))
                val result = importData(backup, mode, fileCount)

                onProgress(BackupProgress("画像を配置しています"))
                try {
                    placeFiles(tempDir, mode)
                } catch (e: Exception) {
                    throw BackupException("データは取り込みましたが、画像の配置に失敗しました: ${e.message}", e)
                }
                result
            } finally {
                tempDir.deleteRecursively()
            }
        }

    private fun parseData(bytes: ByteArray): BackupData {
        val parsed = try {
            BackupJson.decodeFromString(BackupData.serializer(), bytes.toString(Charsets.UTF_8))
        } catch (e: SerializationException) {
            throw BackupException("data.json を読み込めませんでした(形式が不正です)", e)
        } catch (e: IllegalArgumentException) {
            throw BackupException("data.json を読み込めませんでした(形式が不正です)", e)
        }
        checkVersion(parsed.formatVersion)
        return parsed
    }

    private fun validatePaths(data: BackupData) {
        val bad = data.diaryPhotos.map { it.filePath }.plus(data.receipts.map { it.imagePath })
            .firstOrNull { !BackupPaths.isSafeStoredPath(it) }
        if (bad != null) throw BackupException("安全でない画像パスが含まれているため取り込みを中止しました: $bad")
    }

    private suspend fun importData(data: BackupData, mode: ImportMode, fileCount: Int): ImportResult =
        db.withTransaction {
            if (mode == ImportMode.REPLACE) replaceAll(data, fileCount) else mergeAll(data, fileCount)
        }

    private suspend fun replaceAll(data: BackupData, fileCount: Int): ImportResult {
        expenseDao.observeAll().first().forEach { expenseDao.delete(it) }
        receiptDao.observeAll().first().forEach { receiptDao.delete(it) }
        diaryDao.observeAll().first().forEach { diaryDao.delete(it) }

        data.diaryEntries.forEach { diaryDao.insert(it.toEntity()) }
        data.diaryPhotos.forEach { diaryDao.insertPhoto(it.toEntity()) }
        data.receipts.forEach { receiptDao.insert(it.toEntity()) }
        if (data.receiptItems.isNotEmpty()) receiptDao.insertItems(data.receiptItems.map { it.toEntity() })
        val receiptIds = data.receipts.map { it.id }.toSet()
        if (data.expenses.isNotEmpty()) {
            expenseDao.insertAll(
                data.expenses.map { e ->
                    e.toEntity().copy(receiptId = e.receiptId?.takeIf { it in receiptIds })
                },
            )
        }
        return ImportResult(
            diaryEntries = data.diaryEntries.size,
            diaryPhotos = data.diaryPhotos.size,
            receipts = data.receipts.size,
            expenses = data.expenses.size,
            skipped = 0,
            files = fileCount,
        )
    }

    private data class ReceiptKey(val purchasedAt: String, val storeName: String, val totalAmount: Int, val imagePath: String)
    private data class ExpenseKey(val date: String, val amount: Int, val category: String, val memo: String, val receiptId: Long?)

    private fun receiptKey(r: Receipt) = ReceiptKey(r.purchasedAt, r.storeName, r.totalAmount, r.imagePath)

    private suspend fun mergeAll(data: BackupData, fileCount: Int): ImportResult {
        var skipped = 0
        var addedEntries = 0
        var addedPhotos = 0
        var addedReceipts = 0
        var addedExpenses = 0

        // 日記(日付で重複判定。既存があれば既存を優先)
        val entryByDate = diaryDao.observeAll().first().associate { it.date to it.id }.toMutableMap()
        val entryIdMap = HashMap<Long, Long>()
        for (e in data.diaryEntries) {
            val existing = entryByDate[e.date]
            if (existing != null) {
                entryIdMap[e.id] = existing
                skipped++
            } else {
                val newId = diaryDao.insert(e.toEntity().copy(id = 0))
                entryIdMap[e.id] = newId
                entryByDate[e.date] = newId
                addedEntries++
            }
        }
        val photoKeys = diaryDao.observeAllPhotos().first().map { it.entryId to it.filePath }.toMutableSet()
        for (p in data.diaryPhotos) {
            val entryId = entryIdMap[p.entryId]
            if (entryId == null || !photoKeys.add(entryId to p.filePath)) {
                skipped++
                continue
            }
            diaryDao.insertPhoto(p.toEntity().copy(id = 0, entryId = entryId))
            addedPhotos++
        }

        // レシート(日時・店名・合計・画像パスで重複判定)
        val receiptByKey = receiptDao.observeAll().first().associate { receiptKey(it) to it.id }.toMutableMap()
        val receiptIdMap = HashMap<Long, Long>()
        val itemsByReceipt = data.receiptItems.groupBy { it.receiptId }
        for (r in data.receipts) {
            val entity = r.toEntity()
            val key = receiptKey(entity)
            val existing = receiptByKey[key]
            if (existing != null) {
                receiptIdMap[r.id] = existing
                skipped++
            } else {
                val newId = receiptDao.insert(entity.copy(id = 0))
                receiptIdMap[r.id] = newId
                receiptByKey[key] = newId
                val items = itemsByReceipt[r.id].orEmpty()
                if (items.isNotEmpty()) {
                    receiptDao.insertItems(items.map { it.toEntity().copy(id = 0, receiptId = newId) })
                }
                addedReceipts++
            }
        }

        // 支出(同一内容は件数ベースで重複判定。同じ日に同額の買い物が2件あっても保たれる)
        val remaining = HashMap<ExpenseKey, Int>()
        expenseDao.observeAll().first().forEach {
            val k = ExpenseKey(it.date, it.amount, it.category, it.memo, it.receiptId)
            remaining[k] = (remaining[k] ?: 0) + 1
        }
        val toInsert = ArrayList<com.amgm.personallog.data.model.Expense>()
        for (e in data.expenses) {
            val mappedReceipt = e.receiptId?.let { receiptIdMap[it] }
            val k = ExpenseKey(e.date, e.amount, e.category, e.memo, mappedReceipt)
            val left = remaining[k] ?: 0
            if (left > 0) {
                remaining[k] = left - 1
                skipped++
            } else {
                toInsert.add(e.toEntity().copy(id = 0, receiptId = mappedReceipt))
                addedExpenses++
            }
        }
        if (toInsert.isNotEmpty()) expenseDao.insertAll(toInsert)

        return ImportResult(addedEntries, addedPhotos, addedReceipts, addedExpenses, skipped, fileCount)
    }

    /** 一時ディレクトリに展開済みの画像を filesDir へ移す。 */
    private fun placeFiles(tempDir: File, mode: ImportMode) {
        val stamp = System.currentTimeMillis()
        if (mode == ImportMode.REPLACE) {
            val moved = ArrayList<Pair<File, File>>() // (元の場所, 退避先)
            try {
                for (name in BackupPaths.ROOT_DIRS) {
                    val dir = File(filesDir, name)
                    if (dir.exists()) {
                        val old = File(filesDir, "$name.old_$stamp")
                        if (!dir.renameTo(old)) throw java.io.IOException("既存フォルダを退避できません: $name")
                        moved.add(dir to old)
                    }
                }
                copyTree(tempDir, overwrite = true)
            } catch (e: Exception) {
                // 元に戻す
                for ((dir, old) in moved) {
                    dir.deleteRecursively()
                    old.renameTo(dir)
                }
                throw e
            }
            moved.forEach { it.second.deleteRecursively() }
        } else {
            copyTree(tempDir, overwrite = false)
        }
    }

    private fun copyTree(tempDir: File, overwrite: Boolean) {
        tempDir.walkTopDown().filter { it.isFile }.forEach { src ->
            val rel = src.relativeTo(tempDir).path.replace(File.separatorChar, '/')
            if (BackupPaths.safeRelativePath(rel) == null) return@forEach
            val dest = File(filesDir, rel)
            if (!isInside(filesDir, dest)) return@forEach
            if (dest.exists() && !overwrite) return@forEach
            dest.parentFile?.mkdirs()
            if (!src.renameTo(dest)) {
                src.inputStream().use { i -> dest.outputStream().use { o -> i.copyTo(o) } }
            }
        }
    }

    private fun isInside(base: File, target: File): Boolean {
        val b = base.canonicalFile
        val t = target.canonicalFile
        return t.path.startsWith(b.path + File.separator)
    }

    companion object {
        private const val MAX_JSON_BYTES = 128 * 1024 * 1024

        /** formatVersion が新しすぎる場合は拒否。 */
        fun checkVersion(formatVersion: Int) {
            if (formatVersion > BACKUP_FORMAT_VERSION) {
                throw BackupException(
                    "このバックアップは新しい形式(v$formatVersion)です。アプリを最新版に更新してから取り込んでください(このアプリは v$BACKUP_FORMAT_VERSION まで対応)。",
                )
            }
        }

        private fun readLimited(input: InputStream, limit: Int): ByteArray {
            val out = ByteArrayOutputStream()
            val buf = ByteArray(8192)
            var total = 0
            while (true) {
                val n = input.read(buf)
                if (n < 0) break
                total += n
                if (total > limit) throw BackupException("data.json が大きすぎます")
                out.write(buf, 0, n)
            }
            return out.toByteArray()
        }
    }
}
