package com.example.businesscard.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.businesscard.domain.model.ThemeStyle
import com.example.businesscard.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject

/**
 * 設定を Jetpack DataStore(Preferences)に保存する [UserPreferencesRepository] の実装。
 *
 * 値は enum の名前(文字列)で保存する。知らない名前(将来消した見た目など)が入っていたら既定に戻す。
 */
class DataStoreUserPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : UserPreferencesRepository {

    override val themeStyle: Flow<ThemeStyle> = dataStore.data
        // 読み込みに失敗しても(ファイルが壊れたなど)、既定の見た目で動き続ける
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs -> prefs[Keys.THEME_STYLE].toThemeStyle() }

    override suspend fun setThemeStyle(style: ThemeStyle) {
        dataStore.edit { prefs -> prefs[Keys.THEME_STYLE] = style.name }
    }

    private object Keys {
        val THEME_STYLE = stringPreferencesKey("theme_style")
    }
}

internal fun String?.toThemeStyle(): ThemeStyle =
    ThemeStyle.entries.firstOrNull { it.name == this } ?: ThemeStyle.DEFAULT
