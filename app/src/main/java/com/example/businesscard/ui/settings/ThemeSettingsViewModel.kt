package com.example.businesscard.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.businesscard.domain.model.ThemeStyle
import com.example.businesscard.domain.usecase.ObserveThemeStyleUseCase
import com.example.businesscard.domain.usecase.SetThemeStyleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ThemeSettingsUiState(
    val selected: ThemeStyle? = null,
    val options: List<ThemeStyle> = ThemeStyle.entries,
)

/** 見た目の切り替え。選んだ見た目は保存され、アプリ全体(MainActivity)がそれを見て描き直す。 */
@HiltViewModel
class ThemeSettingsViewModel @Inject constructor(
    observeThemeStyle: ObserveThemeStyleUseCase,
    private val setThemeStyle: SetThemeStyleUseCase,
) : ViewModel() {

    val uiState: StateFlow<ThemeSettingsUiState> = observeThemeStyle()
        .map { ThemeSettingsUiState(selected = it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ThemeSettingsUiState(),
        )

    fun select(style: ThemeStyle) {
        viewModelScope.launch { setThemeStyle(style) }
    }
}
