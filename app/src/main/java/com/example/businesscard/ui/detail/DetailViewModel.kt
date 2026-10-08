package com.example.businesscard.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.businesscard.domain.model.BusinessCard
import com.example.businesscard.domain.usecase.ObserveBusinessCardUseCase
import com.example.businesscard.ui.navigation.CardDetail
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class DetailUiState(
    val card: BusinessCard? = null,
    val isLoading: Boolean = true,
)

@HiltViewModel
class DetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeBusinessCard: ObserveBusinessCardUseCase,
) : ViewModel() {

    val cardId: Long = savedStateHandle.toRoute<CardDetail>().cardId

    val uiState: StateFlow<DetailUiState> = observeBusinessCard(cardId)
        .map { DetailUiState(card = it, isLoading = false) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DetailUiState(),
        )
}
