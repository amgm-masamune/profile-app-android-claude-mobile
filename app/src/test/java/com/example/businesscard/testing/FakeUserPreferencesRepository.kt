package com.example.businesscard.testing

import com.example.businesscard.domain.model.ThemeStyle
import com.example.businesscard.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** ViewModelのテスト用の偽Repository。 */
class FakeUserPreferencesRepository(initial: ThemeStyle = ThemeStyle.DEFAULT) : UserPreferencesRepository {
    private val style = MutableStateFlow(initial)

    override val themeStyle: Flow<ThemeStyle> = style

    override suspend fun setThemeStyle(style: ThemeStyle) {
        this.style.value = style
    }
}
