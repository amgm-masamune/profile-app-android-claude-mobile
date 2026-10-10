package com.amgm.personallog.ui.receipt

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amgm.personallog.data.db.ExpenseDao
import com.amgm.personallog.data.model.Receipt
import com.amgm.personallog.data.model.ReceiptItem
import com.amgm.personallog.data.model.ReceiptStatus
import com.amgm.personallog.data.repository.ReceiptRepository
import com.amgm.personallog.receipt.ParsedItem
import com.amgm.personallog.receipt.ReceiptCalc
import com.amgm.personallog.receipt.ReceiptImageStore
import com.amgm.personallog.receipt.ReceiptMapper
import com.amgm.personallog.receipt.ReceiptScanner
import com.amgm.personallog.receipt.ScanOutcome
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class ItemForm(
    val key: Long,
    val name: String,
    val quantity: String,
    val amount: String,
    val category: String,
    val unitPrice: Int = 0,
)

data class DetailUiState(
    val loading: Boolean = true,
    val notFound: Boolean = false,
    val imagePath: String = "",
    val rawJson: String = "",
    val createdAt: Long = 0,
    val confirmed: Boolean = false,
    val date: String = "",
    val time: String = "",
    val store: String = "",
    val total: String = "",
    val items: List<ItemForm> = emptyList(),
    val busy: Boolean = false,
    val scanning: Boolean = false,
    val error: String? = null,
    val info: String? = null,
    val finished: Boolean = false,
) {
    /** total - 品目合計。一致、または total 未入力なら null。 */
    val mismatch: Long?
        get() = ReceiptCalc.mismatch(items.map { it.amount.toIntOrNull() ?: 0 }, total.toIntOrNull())

    val itemsSum: Long get() = ReceiptCalc.itemsSum(items.map { it.amount.toIntOrNull() ?: 0 })
}

@HiltViewModel
class ReceiptDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ReceiptRepository,
    private val expenseDao: ExpenseDao,
    private val store: ReceiptImageStore,
    private val scanner: ReceiptScanner,
) : ViewModel() {

    private val receiptId: Long = savedStateHandle.get<Long>("id") ?: 0L
    private var nextKey = 1L

    private val _state = MutableStateFlow(DetailUiState())
    val state: StateFlow<DetailUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val r = repository.getReceipt(receiptId)
            if (r == null) {
                _state.update { it.copy(loading = false, notFound = true) }
                return@launch
            }
            val items = repository.getItems(receiptId)
            val (d, t) = splitPurchasedAt(r.purchasedAt)
            _state.value = DetailUiState(
                loading = false,
                imagePath = r.imagePath,
                rawJson = r.rawJson,
                createdAt = r.createdAt,
                confirmed = r.status == ReceiptStatus.CONFIRMED,
                date = d,
                time = t,
                store = r.storeName,
                total = r.totalAmount.toString(),
                items = items.map {
                    ItemForm(newKey(), it.name, formatQuantity(it.quantity), it.amount.toString(), it.category, it.unitPrice)
                },
            )
        }
    }

    private fun newKey(): Long = nextKey++

    private fun splitPurchasedAt(s: String): Pair<String, String> =
        s.take(10) to s.drop(10).trim()

    private fun formatQuantity(q: Double): String =
        if (q == Math.floor(q) && q < 1_000_000) q.toLong().toString() else q.toString()

    fun setDate(v: String) = _state.update { it.copy(date = v) }
    fun setTime(v: String) = _state.update { it.copy(time = v) }
    fun setStore(v: String) = _state.update { it.copy(store = v) }
    fun setTotal(v: String) = _state.update { it.copy(total = v.filter { c -> c.isDigit() || c == '-' }) }

    private fun updateItem(key: Long, f: (ItemForm) -> ItemForm) =
        _state.update { s -> s.copy(items = s.items.map { if (it.key == key) f(it) else it }) }

    fun setItemName(key: Long, v: String) = updateItem(key) { it.copy(name = v) }
    fun setItemQuantity(key: Long, v: String) = updateItem(key) { it.copy(quantity = v.filter { c -> c.isDigit() || c == '.' }) }
    fun setItemAmount(key: Long, v: String) = updateItem(key) { it.copy(amount = v.filter { c -> c.isDigit() || c == '-' }) }
    fun setItemCategory(key: Long, v: String) = updateItem(key) { it.copy(category = v) }

    fun toggleItemSign(key: Long) = updateItem(key) {
        val a = it.amount
        it.copy(amount = if (a.startsWith("-")) a.drop(1) else if (a.isEmpty()) "-" else "-$a")
    }

    fun addItem() = _state.update {
        it.copy(items = it.items + ItemForm(newKey(), "", "1", "", com.amgm.personallog.data.model.Categories.UNCATEGORIZED))
    }

    fun removeItem(key: Long) = _state.update { s -> s.copy(items = s.items.filterNot { it.key == key }) }

    /** 品目合計を total に反映する。 */
    fun useItemsSumAsTotal() = _state.update { it.copy(total = it.itemsSum.toString()) }

    fun dismissError() = _state.update { it.copy(error = null) }
    fun dismissInfo() = _state.update { it.copy(info = null) }

    /** フォームを検証して Receipt と品目に変換。不正なら error をセットして null。 */
    private fun buildFromForm(strict: Boolean): Pair<Receipt, List<ReceiptItem>>? {
        val s = _state.value
        val date = s.date.trim()
        if (!ReceiptCalc.isValidDate(date)) {
            _state.update { it.copy(error = "日付は yyyy-MM-dd 形式(例 2026-10-10)で入力してください。") }
            return null
        }
        val time = s.time.trim()
        if (time.isNotEmpty() && !ReceiptCalc.isValidTime(time)) {
            _state.update { it.copy(error = "時刻は HH:mm 形式(例 14:05)で入力するか、空にしてください。") }
            return null
        }
        if (strict) {
            val bad = s.items.indexOfFirst { it.amount.toIntOrNull() == null }
            if (bad >= 0) {
                _state.update { it.copy(error = "品目${bad + 1}の金額が入力されていません。") }
                return null
            }
        }
        val items = s.items.map {
            val qty = it.quantity.toDoubleOrNull()?.takeIf { q -> q > 0 } ?: 1.0
            val amount = it.amount.toIntOrNull() ?: 0
            ReceiptItem(
                receiptId = receiptId,
                name = it.name.trim(),
                quantity = qty,
                unitPrice = if (it.unitPrice != 0) it.unitPrice else Math.round(amount / qty).toInt(),
                amount = amount,
                category = it.category,
            )
        }
        val total = s.total.toIntOrNull() ?: ReceiptCalc.itemsSum(items.map { it.amount }).toInt()
        val receipt = Receipt(
            id = receiptId,
            purchasedAt = ReceiptMapper.purchasedAt(date, time),
            storeName = s.store.trim(),
            totalAmount = total,
            imagePath = s.imagePath,
            rawJson = s.rawJson,
            status = if (s.confirmed) ReceiptStatus.CONFIRMED else ReceiptStatus.DRAFT,
            createdAt = s.createdAt,
        )
        return receipt to items
    }

    /** 下書きとして保存(確定済みレシートでは使わない)。 */
    fun saveDraft() {
        if (_state.value.busy) return
        val built = buildFromForm(strict = false) ?: return
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            try {
                repository.save(built.first, built.second)
                _state.update { it.copy(busy = false, info = "下書きを保存しました。") }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(busy = false, error = "保存に失敗しました: ${e.message.orEmpty()}") }
            }
        }
    }

    /** 確定: レシート・品目を保存し、家計簿(Expense)を品目ごとに作り直す。 */
    fun confirm() {
        if (_state.value.busy) return
        val built = buildFromForm(strict = true) ?: return
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            try {
                repository.save(built.first, built.second)
                repository.confirm(receiptId, perItem = true)
                _state.update { it.copy(busy = false, confirmed = true, finished = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(busy = false, error = "確定に失敗しました: ${e.message.orEmpty()}") }
            }
        }
    }

    /** 画像をもう一度読み取り、フォームを結果で置き換える。 */
    fun rescan() {
        val s = _state.value
        if (s.busy || s.imagePath.isBlank()) return
        _state.update { it.copy(busy = true, scanning = true, error = null, info = null) }
        viewModelScope.launch {
            val outcome = scanner.scan(s.imagePath)
            _state.update { cur ->
                when (outcome) {
                    is ScanOutcome.Parsed -> {
                        val p = outcome.receipt
                        cur.copy(
                            busy = false,
                            scanning = false,
                            rawJson = outcome.raw,
                            date = p.date ?: cur.date.ifBlank { LocalDate.now().toString() },
                            time = p.time.orEmpty(),
                            store = p.store,
                            total = (p.total ?: ReceiptCalc.itemsSum(p.items.map { it.amount }).toInt()).toString(),
                            items = p.items.map { it.toForm() },
                            info = "読み取りました。内容を確認してください。",
                        )
                    }
                    is ScanOutcome.Unparsable -> cur.copy(
                        busy = false,
                        scanning = false,
                        rawJson = outcome.raw,
                        error = "読み取り結果を解釈できませんでした。手入力で修正してください。",
                    )
                    is ScanOutcome.Failed -> cur.copy(busy = false, scanning = false, error = outcome.message)
                }
            }
        }
    }

    private fun ParsedItem.toForm() = ItemForm(
        key = newKey(),
        name = name,
        quantity = formatQuantity(quantity),
        amount = amount.toString(),
        category = category,
        unitPrice = unitPrice,
    )

    /** レシート削除。画像ファイルも消す。alsoExpenses=true なら家計簿の行も消す(false なら残る)。 */
    fun delete(alsoExpenses: Boolean) {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            try {
                val r = repository.getReceipt(receiptId)
                if (r != null) {
                    if (alsoExpenses) expenseDao.deleteByReceiptId(receiptId)
                    repository.delete(r)
                    store.delete(r.imagePath)
                }
                _state.update { it.copy(busy = false, finished = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(busy = false, error = "削除に失敗しました: ${e.message.orEmpty()}") }
            }
        }
    }
}
