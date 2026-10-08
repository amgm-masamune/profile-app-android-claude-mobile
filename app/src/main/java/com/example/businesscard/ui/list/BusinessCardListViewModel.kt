package com.example.businesscard.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.businesscard.domain.model.BusinessCard
import com.example.businesscard.domain.usecase.ObserveBusinessCardsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class ListUiState(
    val cards: List<BusinessCard> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class BusinessCardListViewModel @Inject constructor(
    observeBusinessCards: ObserveBusinessCardsUseCase,
) : ViewModel() {

    val uiState: StateFlow<ListUiState> = observeBusinessCards()
        .map { ListUiState(cards = it, isLoading = false) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ListUiState(),
        )
}
