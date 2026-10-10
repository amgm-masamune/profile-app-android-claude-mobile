package com.amgm.personallog.ui.diary

import android.content.Context
import android.media.ExifInterface
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amgm.personallog.data.db.DiaryDao
import com.amgm.personallog.data.model.DiaryEntry
import com.amgm.personallog.data.model.DiaryPhoto
import com.amgm.personallog.data.repository.DiaryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

data class DiaryListItem(val entry: DiaryEntry, val photos: List<DiaryPhoto>)

data class DiaryListUiState(
    val loading: Boolean = true,
    val items: List<DiaryListItem> = emptyList(),
    val error: String? = null,
)

@HiltViewModel
class DiaryListViewModel @Inject constructor(
    repository: DiaryRepository,
) : ViewModel() {
    val uiState: StateFlow<DiaryListUiState> =
        combine(repository.observeEntries(), repository.observeAllPhotos()) { entries, photos ->
            val byEntry = photos.groupBy { it.entryId }
            entries
                .map { e -> DiaryListItem(e, byEntry[e.id].orEmpty().sortedWith(compareBy({ it.sortOrder }, { it.id }))) }
                .filterNot { DiaryLogic.isEmptyEntry(it.entry.text, it.photos.size) }
        }
            .map { DiaryListUiState(loading = false, items = it) }
            .catch { emit(DiaryListUiState(loading = false, error = "日記の読み込みに失敗しました: ${it.message}")) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DiaryListUiState())
}

data class ImportProgress(val done: Int, val total: Int)

data class DiaryEditUiState(
    val loading: Boolean = true,
    val date: String,
    val entryId: Long? = null,
    val photos: List<DiaryPhoto> = emptyList(),
    val importing: ImportProgress? = null,
    val error: String? = null,
    val closed: Boolean = false,
)

@HiltViewModel
class DiaryEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
    private val repository: DiaryRepository,
    private val dao: DiaryDao,
) : ViewModel() {
    private val initialDate: String =
        savedStateHandle.get<String>("date")?.takeIf { DiaryLogic.isValidDate(it) } ?: DiaryLogic.today()

    private val _state = MutableStateFlow(DiaryEditUiState(date = initialDate))
    val uiState: StateFlow<DiaryEditUiState> = _state.asStateFlow()

    var text by mutableStateOf("")
        private set

    private val mutex = Mutex()
    private var saveJob: Job? = null
    private var photosJob: Job? = null

    init {
        viewModelScope.launch {
            try {
                val entry = repository.observeEntry(initialDate).first()
                if (entry != null) {
                    text = entry.text
                    bindEntry(entry.id)
                }
                _state.update { it.copy(loading = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = "読み込みに失敗しました: ${e.message}") }
            }
        }
    }

    private fun bindEntry(id: Long) {
        if (_state.value.entryId == id && photosJob?.isActive == true) return
        _state.update { it.copy(entryId = id) }
        photosJob?.cancel()
        photosJob = viewModelScope.launch {
            repository.observePhotos(id)
                .catch { e -> setError("写真の読み込みに失敗しました: ${e.message}") }
                .collect { list -> _state.update { it.copy(photos = list) } }
        }
    }

    private fun setError(message: String) {
        _state.update { it.copy(error = message) }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    fun onTextChange(value: String) {
        text = value
        scheduleSave()
    }

    private fun scheduleSave() {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(800)
            persist()
        }
    }

    /** 本文を保存。空(本文も写真も無い)なら作らない/既存なら削除。成功なら true。 */
    private suspend fun persist(): Boolean = withContext(NonCancellable) {
        mutex.withLock {
            try {
                val date = _state.value.date
                val t = text
                val id = _state.value.entryId
                if (id == null) {
                    if (!t.isBlank()) bindEntry(repository.saveText(date, t))
                } else {
                    val photoCount = repository.observePhotos(id).first().size
                    if (DiaryLogic.isEmptyEntry(t, photoCount)) {
                        repository.observeEntry(date).first()?.let { repository.deleteEntry(it) }
                        photosJob?.cancel()
                        _state.update { it.copy(entryId = null, photos = emptyList()) }
                    } else {
                        repository.saveText(date, t)
                    }
                }
                true
            } catch (e: Exception) {
                setError("保存に失敗しました: ${e.message}")
                false
            }
        }
    }

    /** 保存して画面を閉じる(戻る/保存ボタン)。 */
    fun saveAndClose() {
        if (_state.value.importing != null) {
            setError("写真の取り込み中です。完了までお待ちください")
            return
        }
        saveJob?.cancel()
        viewModelScope.launch {
            if (persist()) _state.update { it.copy(closed = true) }
        }
    }

    fun changeDate(newDate: String) {
        if (newDate == _state.value.date || _state.value.importing != null) return
        viewModelScope.launch {
            withContext(NonCancellable) {
                mutex.withLock {
                    try {
                        val oldDate = _state.value.date
                        if (repository.observeEntry(newDate).first() != null) {
                            setError("${DiaryLogic.formatDateLabel(newDate)}には既に日記があります")
                        } else {
                            if (_state.value.entryId != null) {
                                val e = repository.observeEntry(oldDate).first()
                                if (e != null) dao.update(e.copy(date = newDate, updatedAt = System.currentTimeMillis()))
                            }
                            _state.update { it.copy(date = newDate) }
                        }
                    } catch (e: Exception) {
                        setError("日付の変更に失敗しました: ${e.message}")
                    }
                }
            }
        }
    }

    fun addPhotos(uris: List<Uri>) {
        if (uris.isEmpty() || _state.value.importing != null) return
        val date = _state.value.date
        viewModelScope.launch {
            _state.update { it.copy(importing = ImportProgress(0, uris.size)) }
            var failed = 0
            val base = System.currentTimeMillis()
            uris.forEachIndexed { index, uri ->
                var copied: File? = null
                try {
                    val (file, takenAt) = withContext(Dispatchers.IO) { copyPhoto(uri, date, base, index) }
                    copied = file
                    val rel = DiaryLogic.photoRelativePath(date, file.name)
                    withContext(NonCancellable) {
                        mutex.withLock {
                            val id = repository.getOrCreateEntryId(date)
                            repository.addPhoto(id, rel, takenAt)
                            bindEntry(id)
                        }
                    }
                } catch (e: CancellationException) {
                    copied?.delete()
                    throw e
                } catch (e: Exception) {
                    copied?.delete()
                    failed++
                }
                _state.update { it.copy(importing = ImportProgress(index + 1, uris.size)) }
            }
            _state.update { it.copy(importing = null) }
            if (failed > 0) setError("${failed}枚の写真を取り込めませんでした")
            scheduleSave()
        }
    }

    /** ストリームコピーのみ(再エンコード無し)。撮影日時はEXIFから読めれば返す。 */
    private fun copyPhoto(uri: Uri, date: String, base: Long, index: Int): Pair<File, Long?> {
        val dir = File(context.filesDir, "diary/$date")
        if (!dir.exists() && !dir.mkdirs()) throw java.io.IOException("保存先を作成できません")
        var attempt = 0
        var dest = File(dir, DiaryLogic.photoFileName(base, index, attempt))
        while (dest.exists()) {
            attempt++
            dest = File(dir, DiaryLogic.photoFileName(base, index, attempt))
        }
        val tmp = File(dir, dest.name + ".tmp")
        try {
            val input = context.contentResolver.openInputStream(uri) ?: throw java.io.IOException("読み込めません")
            input.use { src -> FileOutputStream(tmp).use { out -> src.copyTo(out) } }
            if (!tmp.renameTo(dest)) throw java.io.IOException("ファイル名の確定に失敗")
        } catch (e: Exception) {
            tmp.delete()
            throw e
        }
        val takenAt = try {
            val exif = ExifInterface(dest.absolutePath)
            DiaryLogic.parseExifDateTime(
                exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL) ?: exif.getAttribute(ExifInterface.TAG_DATETIME),
            )
        } catch (e: Exception) {
            null
        }
        return dest to takenAt
    }

    fun deletePhoto(photo: DiaryPhoto) {
        viewModelScope.launch {
            withContext(NonCancellable) {
                mutex.withLock {
                    try {
                        repository.deletePhoto(photo)
                        withContext(Dispatchers.IO) { File(context.filesDir, photo.filePath).delete() }
                    } catch (e: Exception) {
                        setError("写真の削除に失敗しました: ${e.message}")
                    }
                }
            }
            scheduleSave()
        }
    }

    fun deleteEntry() {
        if (_state.value.importing != null) {
            setError("写真の取り込み中です。完了までお待ちください")
            return
        }
        saveJob?.cancel()
        viewModelScope.launch {
            withContext(NonCancellable) {
                mutex.withLock {
                    try {
                        val id = _state.value.entryId
                        if (id != null) {
                            val photos = repository.observePhotos(id).first()
                            repository.observeEntry(_state.value.date).first()?.let { repository.deleteEntry(it) }
                            withContext(Dispatchers.IO) {
                                photos.forEach { File(context.filesDir, it.filePath).delete() }
                                File(context.filesDir, "diary/${_state.value.date}").delete() // 空の時だけ消える
                            }
                        }
                        photosJob?.cancel()
                        text = ""
                        _state.update { it.copy(entryId = null, photos = emptyList(), closed = true) }
                    } catch (e: Exception) {
                        setError("削除に失敗しました: ${e.message}")
                    }
                }
            }
        }
    }
}
