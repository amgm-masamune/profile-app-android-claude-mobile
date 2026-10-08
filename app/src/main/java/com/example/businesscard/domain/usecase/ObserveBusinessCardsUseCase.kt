package com.example.businesscard.domain.usecase

import com.example.businesscard.domain.model.BusinessCard
import com.example.businesscard.domain.repository.BusinessCardRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** 全名刺(新しい順)を監視する。 */
class ObserveBusinessCardsUseCase @Inject constructor(
    private val repository: BusinessCardRepository,
) {
    operator fun invoke(): Flow<List<BusinessCard>> = repository.cards
}
