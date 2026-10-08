package com.example.businesscard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.businesscard.domain.model.ThemeStyle
import com.example.businesscard.domain.usecase.ObserveThemeStyleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed interface MainActivityUiState {
    /** 保存された設定を読み込み中(最初の一瞬だけ) */
    data object Loading : MainActivityUiState

    data class Ready(val themeStyle: ThemeStyle) : MainActivityUiState
}

/** アプリ全体の状態(いまは見た目だけ)。 */
@HiltViewModel
class MainActivityViewModel @Inject constructor(
    observeThemeStyle: ObserveThemeStyleUseCase,
) : ViewModel() {

    val uiState: StateFlow<MainActivityUiState> = observeThemeStyle()
        .map<ThemeStyle, MainActivityUiState> { MainActivityUiState.Ready(it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MainActivityUiState.Loading,
        )
}
