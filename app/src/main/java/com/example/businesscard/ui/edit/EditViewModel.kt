package com.example.businesscard.ui.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.businesscard.domain.model.BusinessCard
import com.example.businesscard.domain.usecase.DeleteBusinessCardUseCase
import com.example.businesscard.domain.usecase.ObserveBusinessCardUseCase
import com.example.businesscard.domain.usecase.SaveBusinessCardResult
import com.example.businesscard.domain.usecase.SaveBusinessCardUseCase
import com.example.businesscard.ui.navigation.CardEdit
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

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

@HiltViewModel
class EditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val observeBusinessCard: ObserveBusinessCardUseCase,
    private val saveBusinessCard: SaveBusinessCardUseCase,
    private val deleteBusinessCard: DeleteBusinessCardUseCase,
) : ViewModel() {

    private val cardId: Long = savedStateHandle.toRoute<CardEdit>().cardId
    private val isNew: Boolean = cardId == BusinessCard.NEW_ID

    private val _uiState = MutableStateFlow(EditUiState(isNew = isNew))
    val uiState: StateFlow<EditUiState> = _uiState.asStateFlow()

    init {
        if (!isNew) {
            viewModelScope.launch {
                observeBusinessCard(cardId).firstOrNull()?.let { card ->
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
        viewModelScope.launch {
            val result = saveBusinessCard(
                BusinessCard(
                    id = cardId,
                    name = state.name,
                    company = state.company,
                    title = state.title,
                    phone = state.phone,
                    email = state.email,
                ),
            )
            _uiState.update {
                when (result) {
                    SaveBusinessCardResult.Success -> it.copy(isSaved = true)
                    SaveBusinessCardResult.NameRequired -> it.copy(nameError = true)
                }
            }
        }
    }

    fun delete() {
        if (isNew) return
        viewModelScope.launch {
            deleteBusinessCard(cardId)
            _uiState.update { it.copy(isDeleted = true) }
        }
    }
}
