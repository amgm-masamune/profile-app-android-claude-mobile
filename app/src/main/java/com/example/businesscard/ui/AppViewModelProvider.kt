package com.example.businesscard.ui

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.businesscard.BusinessCardApplication
import com.example.businesscard.ui.detail.DetailViewModel
import com.example.businesscard.ui.edit.EditViewModel
import com.example.businesscard.ui.list.BusinessCardListViewModel

/** 全ViewModelの生成方法をここに集約する。 */
object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer {
            BusinessCardListViewModel(businessCardApplication().container.businessCardRepository)
        }
        initializer {
            DetailViewModel(
                savedStateHandle = createSavedStateHandle(),
                repository = businessCardApplication().container.businessCardRepository,
            )
        }
        initializer {
            EditViewModel(
                savedStateHandle = createSavedStateHandle(),
                repository = businessCardApplication().container.businessCardRepository,
            )
        }
    }
}

private fun CreationExtras.businessCardApplication(): BusinessCardApplication =
    this[APPLICATION_KEY] as BusinessCardApplication
