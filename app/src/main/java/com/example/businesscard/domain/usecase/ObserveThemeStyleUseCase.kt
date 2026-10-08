package com.example.businesscard.domain.usecase

import com.example.businesscard.domain.model.ThemeStyle
import com.example.businesscard.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject

/** 選んでいる見た目を監視する。同じ値が続けて流れても画面を組み立て直さないよう、変わったときだけ流す。 */
class ObserveThemeStyleUseCase @Inject constructor(
    private val repository: UserPreferencesRepository,
) {
    operator fun invoke(): Flow<ThemeStyle> = repository.themeStyle.distinctUntilChanged()
}
