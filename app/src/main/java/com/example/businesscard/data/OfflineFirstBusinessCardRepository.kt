package com.example.businesscard.data

import com.example.businesscard.data.local.BusinessCardDao
import com.example.businesscard.data.local.asEntity
import com.example.businesscard.data.local.asExternalModel
import com.example.businesscard.data.model.BusinessCard
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** ローカルDB(Room)だけを情報源とするRepository実装。 */
class OfflineFirstBusinessCardRepository(
    private val dao: BusinessCardDao,
) : BusinessCardRepository {

    override val cards: Flow<List<BusinessCard>> =
        dao.observeAll().map { entities -> entities.map { it.asExternalModel() } }

    override fun observeCard(id: Long): Flow<BusinessCard?> =
        dao.observeById(id).map { it?.asExternalModel() }

    override suspend fun save(card: BusinessCard) = dao.upsert(card.asEntity())

    override suspend fun delete(id: Long) = dao.deleteById(id)
}
