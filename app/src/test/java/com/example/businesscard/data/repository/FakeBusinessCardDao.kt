package com.example.businesscard.data.repository

import com.example.businesscard.data.local.BusinessCardDao
import com.example.businesscard.data.local.BusinessCardEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** Roomを使わずにRepositoryをテストするための偽DAO。 */
class FakeBusinessCardDao : BusinessCardDao {
    private val rows = MutableStateFlow<List<BusinessCardEntity>>(emptyList())
    private var nextId = 1L

    override fun observeAll(): Flow<List<BusinessCardEntity>> = rows.map { it.sortedByDescending { e -> e.id } }

    override fun observeById(id: Long): Flow<BusinessCardEntity?> =
        rows.map { list -> list.firstOrNull { it.id == id } }

    override suspend fun upsert(entity: BusinessCardEntity) {
        rows.update { list ->
            if (entity.id == 0L) {
                list + entity.copy(id = nextId++)
            } else {
                list.map { if (it.id == entity.id) entity else it }
            }
        }
    }

    override suspend fun deleteById(id: Long) {
        rows.update { list -> list.filterNot { it.id == id } }
    }
}
