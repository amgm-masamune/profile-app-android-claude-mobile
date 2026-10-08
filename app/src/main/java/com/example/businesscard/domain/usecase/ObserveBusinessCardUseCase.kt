package com.example.businesscard.domain.usecase

import com.example.businesscard.domain.model.BusinessCard
import com.example.businesscard.domain.repository.BusinessCardRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** 指定した名刺を監視する。存在しなければnullを流す。 */
class ObserveBusinessCardUseCase @Inject constructor(
    private val repository: BusinessCardRepository,
) {
    operator fun invoke(id: Long): Flow<BusinessCard?> = repository.observeCard(id)
}
