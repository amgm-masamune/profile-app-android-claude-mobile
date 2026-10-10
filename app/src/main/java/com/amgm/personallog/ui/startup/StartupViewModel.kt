package com.amgm.personallog.ui.startup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amgm.personallog.data.db.AppDatabase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Provider

sealed interface StartupState {
    data object Checking : StartupState
    data object Ready : StartupState
    data class Failed(val detail: String) : StartupState
}

@HiltViewModel
class StartupViewModel @Inject constructor(
    private val database: Provider<AppDatabase>,
) : ViewModel() {
    private val _state = MutableStateFlow<StartupState>(StartupState.Checking)
    val state: StateFlow<StartupState> = _state

    init {
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = try {
                database.get().openHelper.writableDatabase
                StartupState.Ready
            } catch (t: Throwable) {
                StartupState.Failed(describe(t))
            }
        }
    }

    private fun describe(t: Throwable): String = buildString {
        var cur: Throwable? = t
        var depth = 0
        while (cur != null && depth < 5) {
            append(if (depth == 0) "原因: " else "caused by: ")
            append(cur.javaClass.name).append(": ").appendLine(cur.message)
            cur = cur.cause
            depth++
        }
        appendLine()
        t.stackTrace.take(12).forEach { appendLine("  at $it") }
    }
}
