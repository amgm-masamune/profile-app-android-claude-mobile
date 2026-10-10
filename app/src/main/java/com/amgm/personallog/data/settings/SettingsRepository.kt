package com.amgm.personallog.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class AppSettings(
    val anthropicApiKey: String = "",
    val claudeModelId: String = SettingsRepository.DEFAULT_MODEL_ID,
)

@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    private val keyApi = stringPreferencesKey("anthropicApiKey")
    private val keyModel = stringPreferencesKey("claudeModelId")

    val settings: Flow<AppSettings> = dataStore.data.map { p ->
        AppSettings(
            anthropicApiKey = p[keyApi].orEmpty(),
            claudeModelId = p[keyModel]?.takeIf { it.isNotBlank() } ?: DEFAULT_MODEL_ID,
        )
    }

    suspend fun save(apiKey: String, modelId: String) {
        dataStore.edit { p ->
            p[keyApi] = apiKey
            p[keyModel] = modelId.ifBlank { DEFAULT_MODEL_ID }
        }
    }

    companion object {
        const val DEFAULT_MODEL_ID = "claude-haiku-5-5"
    }
}
