package com.example.businesscard.data.repository

import com.example.businesscard.data.local.BusinessCardDao
import com.example.businesscard.data.mapper.toDomain
import com.example.businesscard.data.mapper.toEntity
import com.example.businesscard.domain.model.BusinessCard
import com.example.businesscard.domain.repository.BusinessCardRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** ローカルDB(Room)だけを情報源とするRepository実装。 */
@Singleton
class OfflineFirstBusinessCardRepository @Inject constructor(
    private val dao: BusinessCardDao,
) : BusinessCardRepository {

    override val cards: Flow<List<BusinessCard>> =
        dao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override fun observeCard(id: Long): Flow<BusinessCard?> =
        dao.observeById(id).map { it?.toDomain() }

    override suspend fun save(card: BusinessCard) = dao.upsert(card.toEntity())

    override suspend fun delete(id: Long) = dao.deleteById(id)
}
