package com.example.businesscard.domain.usecase

import com.example.businesscard.domain.repository.BusinessCardRepository
import javax.inject.Inject

/** 名刺を削除する。 */
class DeleteBusinessCardUseCase @Inject constructor(
    private val repository: BusinessCardRepository,
) {
    suspend operator fun invoke(id: Long) = repository.delete(id)
}
