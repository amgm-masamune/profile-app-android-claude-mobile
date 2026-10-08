package com.example.businesscard.domain.usecase

import com.example.businesscard.domain.model.BusinessCard
import com.example.businesscard.domain.repository.BusinessCardRepository
import javax.inject.Inject

/** 保存の結果。バリデーションエラーは例外ではなく結果として返す。 */
sealed interface SaveBusinessCardResult {
    data object Success : SaveBusinessCardResult
    data object NameRequired : SaveBusinessCardResult
}

/** 名刺を検証・整形して保存する。業務ルール(氏名必須、前後の空白除去)はここに置く。 */
class SaveBusinessCardUseCase @Inject constructor(
    private val repository: BusinessCardRepository,
) {
    suspend operator fun invoke(card: BusinessCard): SaveBusinessCardResult {
        if (card.name.isBlank()) return SaveBusinessCardResult.NameRequired
        repository.save(
            card.copy(
                name = card.name.trim(),
                company = card.company.trim(),
                title = card.title.trim(),
                phone = card.phone.trim(),
                email = card.email.trim(),
            ),
        )
        return SaveBusinessCardResult.Success
    }
}
