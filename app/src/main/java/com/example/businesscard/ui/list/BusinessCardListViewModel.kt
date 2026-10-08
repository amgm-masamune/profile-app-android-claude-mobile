package com.example.businesscard.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.businesscard.data.BusinessCardRepository
import com.example.businesscard.data.model.BusinessCard
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class ListUiState(
    val cards: List<BusinessCard> = emptyList(),
    val isLoading: Boolean = true,
)

class BusinessCardListViewModel(
    repository: BusinessCardRepository,
) : ViewModel() {

    val uiState: StateFlow<ListUiState> = repository.cards
        .map { ListUiState(cards = it, isLoading = false) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ListUiState(),
        )
}
