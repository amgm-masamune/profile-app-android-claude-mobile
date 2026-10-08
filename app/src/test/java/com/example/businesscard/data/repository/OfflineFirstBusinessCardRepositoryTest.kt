package com.example.businesscard.data.repository

import com.example.businesscard.domain.model.BusinessCard
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineFirstBusinessCardRepositoryTest {

    private val repository = OfflineFirstBusinessCardRepository(FakeBusinessCardDao())

    @Test
    fun save_newCard_isEmittedWithGeneratedId() = runTest {
        repository.save(BusinessCard(name = "山田太郎", company = "ACME"))

        val cards = repository.cards.first()

        assertEquals(1, cards.size)
        assertEquals("山田太郎", cards[0].name)
        assertEquals("ACME", cards[0].company)
        assertTrue(cards[0].id != 0L)
    }

    @Test
    fun save_existingCard_updatesInPlace() = runTest {
        repository.save(BusinessCard(name = "山田太郎"))
        val saved = repository.cards.first().single()

        repository.save(saved.copy(name = "山田次郎"))

        val cards = repository.cards.first()
        assertEquals(1, cards.size)
        assertEquals("山田次郎", repository.observeCard(saved.id).first()?.name)
    }

    @Test
    fun delete_removesCard() = runTest {
        repository.save(BusinessCard(name = "山田太郎"))
        val saved = repository.cards.first().single()

        repository.delete(saved.id)

        assertTrue(repository.cards.first().isEmpty())
        assertNull(repository.observeCard(saved.id).first())
    }
}
