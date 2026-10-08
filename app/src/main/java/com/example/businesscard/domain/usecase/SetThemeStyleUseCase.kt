package com.example.businesscard.domain.usecase

import com.example.businesscard.domain.model.ThemeStyle
import com.example.businesscard.domain.repository.UserPreferencesRepository
import javax.inject.Inject

/** 見た目を切り替えて保存する(次に起動したときもその見た目で開く)。 */
class SetThemeStyleUseCase @Inject constructor(
    private val repository: UserPreferencesRepository,
) {
    suspend operator fun invoke(style: ThemeStyle) = repository.setThemeStyle(style)
}
