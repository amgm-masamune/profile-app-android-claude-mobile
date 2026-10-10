package com.amgm.personallog.ui.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amgm.personallog.data.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository,
) : ViewModel() {
    var apiKey by mutableStateOf("")
    var modelId by mutableStateOf(SettingsRepository.DEFAULT_MODEL_ID)
    var saved by mutableStateOf(false)

    init {
        viewModelScope.launch {
            val s = repository.settings.first()
            apiKey = s.anthropicApiKey
            modelId = s.claudeModelId
        }
    }

    fun save() {
        viewModelScope.launch {
            repository.save(apiKey.trim(), modelId.trim())
            saved = true
        }
    }
}
