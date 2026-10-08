package com.example.businesscard.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.businesscard.data.BusinessCardRepository
import com.example.businesscard.data.model.BusinessCard
import com.example.businesscard.ui.navigation.CARD_ID_ARG
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class DetailUiState(
    val card: BusinessCard? = null,
    val isLoading: Boolean = true,
)

class DetailViewModel(
    savedStateHandle: SavedStateHandle,
    repository: BusinessCardRepository,
) : ViewModel() {

    val cardId: Long = checkNotNull(savedStateHandle.get<Long>(CARD_ID_ARG))

    val uiState: StateFlow<DetailUiState> = repository.observeCard(cardId)
        .map { DetailUiState(card = it, isLoading = false) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DetailUiState(),
        )
}
