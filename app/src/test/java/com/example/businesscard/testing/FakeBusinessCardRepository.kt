package com.example.businesscard.testing

import com.example.businesscard.domain.repository.BusinessCardRepository
import com.example.businesscard.domain.model.BusinessCard
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** ViewModelのテスト用の偽Repository。 */
class FakeBusinessCardRepository : BusinessCardRepository {
    private val store = MutableStateFlow<List<BusinessCard>>(emptyList())
    private var nextId = 1L

    override val cards: Flow<List<BusinessCard>> = store

    override fun observeCard(id: Long): Flow<BusinessCard?> =
        store.map { list -> list.firstOrNull { it.id == id } }

    override suspend fun save(card: BusinessCard) {
        store.update { list ->
            if (card.id == 0L) {
                list + card.copy(id = nextId++)
            } else {
                list.map { if (it.id == card.id) card else it }
            }
        }
    }

    override suspend fun delete(id: Long) {
        store.update { list -> list.filterNot { it.id == id } }
    }
}
