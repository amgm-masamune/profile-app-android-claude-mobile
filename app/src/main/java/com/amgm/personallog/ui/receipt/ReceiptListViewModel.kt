package com.amgm.personallog.ui.receipt

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amgm.personallog.data.model.Receipt
import com.amgm.personallog.data.repository.ReceiptRepository
import com.amgm.personallog.receipt.ReceiptImageStore
import com.amgm.personallog.receipt.ReceiptMapper
import com.amgm.personallog.receipt.ReceiptScanner
import com.amgm.personallog.receipt.ScanOutcome
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReceiptListUiState(
    val scanning: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class ReceiptListViewModel @Inject constructor(
    private val repository: ReceiptRepository,
    private val store: ReceiptImageStore,
    private val scanner: ReceiptScanner,
) : ViewModel() {

    val receipts: StateFlow<List<Receipt>> = repository.observeReceipts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _state = MutableStateFlow(ReceiptListUiState())
    val state: StateFlow<ReceiptListUiState> = _state.asStateFlow()

    private val _openEvents = Channel<Long>(Channel.BUFFERED)

    /** 読み取り完了後に開くべきレシートID。 */
    val openEvents = _openEvents.receiveAsFlow()

    fun newCameraPath(): String = store.newRelativePath()

    fun cameraUri(relativePath: String): Uri = store.uriFor(relativePath)

    fun discardCapture(relativePath: String) = store.delete(relativePath)

    fun dismissError() = _state.update { it.copy(error = null) }

    fun showError(message: String) = _state.update { it.copy(error = message) }

    /** 撮影済みの画像(filesDir相対パス)を読み取る。 */
    fun onCaptured(relativePath: String) {
        if (!beginScan()) {
            store.delete(relativePath)
            return
        }
        viewModelScope.launch { process(relativePath) }
    }

    /** ギャラリーから選んだ画像を保存して読み取る。 */
    fun onPicked(uri: Uri) {
        if (!beginScan()) return
        viewModelScope.launch {
            val rel = try {
                store.importFrom(uri)
            } catch (e: CancellationException) {
                throw e
            } catch (e: OutOfMemoryError) {
                _state.update { it.copy(scanning = false, error = "画像が大きすぎて読み込めませんでした") }
                return@launch
            } catch (e: Exception) {
                _state.update { it.copy(scanning = false, error = "画像を読み込めませんでした: ${e.message.orEmpty()}") }
                return@launch
            }
            process(rel)
        }
    }

    /** 二重送信防止: すでに処理中なら false。 */
    private fun beginScan(): Boolean {
        if (_state.value.scanning) return false
        _state.update { it.copy(scanning = true, error = null) }
        return true
    }

    private suspend fun process(relativePath: String) {
        try {
            val outcome = scanner.scan(relativePath)
            val (receipt, items) = ReceiptMapper.draftReceipt(outcome, relativePath, System.currentTimeMillis())
            val id = repository.save(receipt, items)
            when (outcome) {
                is ScanOutcome.Parsed -> {
                    _state.update { it.copy(scanning = false) }
                    _openEvents.send(id)
                }
                is ScanOutcome.Unparsable -> {
                    _state.update { it.copy(scanning = false, error = "読み取り結果を解釈できませんでした。手入力で修正してください。") }
                    _openEvents.send(id)
                }
                is ScanOutcome.Failed -> {
                    _state.update {
                        it.copy(scanning = false, error = outcome.message + "\n画像は下書きとして保存しました。一覧から開いて「読み取り直す」ができます。")
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _state.update { it.copy(scanning = false, error = "保存中にエラーが発生しました: ${e.message.orEmpty()}") }
        }
    }
}
