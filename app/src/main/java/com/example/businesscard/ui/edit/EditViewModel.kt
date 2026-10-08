package com.example.businesscard.ui.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.businesscard.data.BusinessCardRepository
import com.example.businesscard.data.model.BusinessCard
import com.example.businesscard.ui.navigation.CARD_ID_ARG
import com.example.businesscard.ui.navigation.NEW_CARD_ID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EditUiState(
    val name: String = "",
    val company: String = "",
    val title: String = "",
    val phone: String = "",
    val email: String = "",
    val isNew: Boolean = true,
    val nameError: Boolean = false,
    val isSaved: Boolean = false,
    val isDeleted: Boolean = false,
)

class EditViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: BusinessCardRepository,
) : ViewModel() {

    private val cardId: Long = savedStateHandle.get<Long>(CARD_ID_ARG) ?: NEW_CARD_ID

    private val _uiState = MutableStateFlow(EditUiState(isNew = cardId == NEW_CARD_ID))
    val uiState: StateFlow<EditUiState> = _uiState.asStateFlow()

    init {
        if (cardId != NEW_CARD_ID) {
            viewModelScope.launch {
                repository.observeCard(cardId).firstOrNull()?.let { card ->
                    _uiState.update {
                        it.copy(
                            name = card.name,
                            company = card.company,
                            title = card.title,
                            phone = card.phone,
                            email = card.email,
                        )
                    }
                }
            }
        }
    }

    fun onNameChange(value: String) = _uiState.update { it.copy(name = value, nameError = false) }
    fun onCompanyChange(value: String) = _uiState.update { it.copy(company = value) }
    fun onTitleChange(value: String) = _uiState.update { it.copy(title = value) }
    fun onPhoneChange(value: String) = _uiState.update { it.copy(phone = value) }
    fun onEmailChange(value: String) = _uiState.update { it.copy(email = value) }

    fun save() {
        val state = _uiState.value
        if (state.name.isBlank()) {
            _uiState.update { it.copy(nameError = true) }
            return
        }
        viewModelScope.launch {
            repository.save(
                BusinessCard(
                    id = if (state.isNew) 0 else cardId,
                    name = state.name.trim(),
                    company = state.company.trim(),
                    title = state.title.trim(),
                    phone = state.phone.trim(),
                    email = state.email.trim(),
                ),
            )
            _uiState.update { it.copy(isSaved = true) }
        }
    }

    fun delete() {
        if (_uiState.value.isNew) return
        viewModelScope.launch {
            repository.delete(cardId)
            _uiState.update { it.copy(isDeleted = true) }
        }
    }
}
