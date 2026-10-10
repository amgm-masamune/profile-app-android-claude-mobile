package com.amgm.personallog.ui.ledger

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amgm.personallog.backup.BackupException
import com.amgm.personallog.backup.BackupManager
import com.amgm.personallog.backup.BackupProgress
import com.amgm.personallog.backup.ImportMode
import com.amgm.personallog.data.model.Expense
import com.amgm.personallog.data.repository.ExpenseRepository
import com.amgm.personallog.domain.LedgerCalc
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.YearMonth
import javax.inject.Inject

data class BackupUiState(
    val progress: BackupProgress? = null,
    val pendingImport: Uri? = null,
    val resultTitle: String? = null,
    val resultMessage: String? = null,
)

data class LedgerUiState(
    val month: String = YearMonth.now().toString(),
    val loading: Boolean = true,
    val total: Long = 0,
    val shares: List<CategoryShare> = emptyList(),
    val days: List<DayGroup> = emptyList(),
    val trend: List<MonthTotal> = emptyList(),
    val error: String? = null,
    val backup: BackupUiState = BackupUiState(),
)

private data class MonthSnapshot(
    val month: String,
    val total: Long,
    val shares: List<CategoryShare>,
    val days: List<DayGroup>,
    val trend: List<MonthTotal>,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LedgerViewModel @Inject constructor(
    private val repository: ExpenseRepository,
    private val backupManager: BackupManager,
) : ViewModel() {

    private val month = MutableStateFlow(YearMonth.now().toString())
    private val _uiState = MutableStateFlow(LedgerUiState())
    val uiState: StateFlow<LedgerUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            month.flatMapLatest { ym ->
                _uiState.update { it.copy(month = ym, loading = true) }
                val months = LedgerLogic.recentMonths(ym)
                val from = LedgerCalc.monthRange(months.first()).first
                val to = LedgerCalc.monthRange(ym).second
                repository.observeBetween(from, to)
                    .map { all -> snapshot(ym, months, all) }
                    .catch { e ->
                        _uiState.update { it.copy(loading = false, error = "家計簿の読み込みに失敗しました: ${e.message}") }
                    }
            }.collect { s ->
                _uiState.update {
                    it.copy(
                        month = s.month,
                        loading = false,
                        total = s.total,
                        shares = s.shares,
                        days = s.days,
                        trend = s.trend,
                    )
                }
            }
        }
    }

    private fun snapshot(ym: String, months: List<String>, all: List<Expense>): MonthSnapshot {
        val current = LedgerLogic.filterMonth(all, ym)
        return MonthSnapshot(
            month = ym,
            total = current.sumOf { it.amount.toLong() },
            shares = LedgerLogic.categoryShares(LedgerCalc.totalsByCategory(current)),
            days = LedgerLogic.groupByDate(current),
            trend = LedgerLogic.monthlyTotals(all, months),
        )
    }

    fun previousMonth() { month.value = LedgerLogic.shiftMonth(month.value, -1) }
    fun nextMonth() { month.value = LedgerLogic.shiftMonth(month.value, 1) }
    fun dismissError() { _uiState.update { it.copy(error = null) } }

    /** original が null なら新規。onDone にはエラーメッセージ(成功なら null)を渡す。 */
    fun saveExpense(
        original: Expense?,
        date: String,
        amount: Int,
        category: String,
        memo: String,
        onDone: (String?) -> Unit,
    ) {
        viewModelScope.launch {
            try {
                if (original == null) {
                    repository.add(
                        Expense(
                            date = date,
                            amount = amount,
                            category = category,
                            memo = memo.trim(),
                            createdAt = System.currentTimeMillis(),
                        ),
                    )
                } else {
                    repository.update(original.copy(date = date, amount = amount, category = category, memo = memo.trim()))
                }
                onDone(null)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                onDone("保存に失敗しました: ${e.message ?: e.javaClass.simpleName}")
            }
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            try {
                repository.delete(expense)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "削除に失敗しました: ${e.message ?: e.javaClass.simpleName}") }
            }
        }
    }

    // ------------------------------------------------------------ backup

    private fun setProgress(p: BackupProgress?) {
        _uiState.update { it.copy(backup = it.backup.copy(progress = p)) }
    }

    private fun showResult(title: String, message: String) {
        _uiState.update {
            it.copy(backup = it.backup.copy(progress = null, resultTitle = title, resultMessage = message))
        }
    }

    private fun describe(e: Throwable): String =
        if (e is BackupException) (e.message ?: "失敗しました") else "失敗しました: ${e.message ?: e.javaClass.simpleName}"

    fun exportTo(uri: Uri) {
        if (_uiState.value.backup.progress != null) return
        setProgress(BackupProgress("準備しています"))
        viewModelScope.launch {
            try {
                val r = backupManager.export(uri) { setProgress(it) }
                showResult(
                    "エクスポート完了",
                    "日記 ${r.diaryEntries}件、レシート ${r.receipts}件、支出 ${r.expenses}件、画像 ${r.files}枚を書き出しました。",
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                showResult("エクスポート失敗", describe(e))
            }
        }
    }

    fun onImportPicked(uri: Uri) {
        _uiState.update { it.copy(backup = it.backup.copy(pendingImport = uri)) }
    }

    fun cancelImport() {
        _uiState.update { it.copy(backup = it.backup.copy(pendingImport = null)) }
    }

    fun confirmImport(mode: ImportMode) {
        val uri = _uiState.value.backup.pendingImport ?: return
        if (_uiState.value.backup.progress != null) return
        _uiState.update { it.copy(backup = it.backup.copy(pendingImport = null, progress = BackupProgress("準備しています"))) }
        viewModelScope.launch {
            try {
                val r = backupManager.importZip(uri, mode) { setProgress(it) }
                showResult(
                    "インポート完了",
                    "日記 ${r.diaryEntries}件、写真 ${r.diaryPhotos}件、レシート ${r.receipts}件、支出 ${r.expenses}件を取り込みました。" +
                        "重複などでスキップ ${r.skipped}件、画像 ${r.files}枚。",
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                val msg = describe(e)
                val note = if (msg.startsWith("データは取り込みました")) "" else "\n(データは変更されていません)"
                showResult("インポート失敗", msg + note)
            }
        }
    }

    fun dismissBackupResult() {
        _uiState.update { it.copy(backup = it.backup.copy(resultTitle = null, resultMessage = null)) }
    }
}
