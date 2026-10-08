package com.example.businesscard.domain.usecase

import com.example.businesscard.domain.model.BusinessCard
import com.example.businesscard.testing.FakeBusinessCardRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SaveBusinessCardUseCaseTest {

    private val repository = FakeBusinessCardRepository()
    private val saveBusinessCard = SaveBusinessCardUseCase(repository)

    @Test
    fun blankName_returnsNameRequiredAndDoesNotSave() = runTest {
        val result = saveBusinessCard(BusinessCard(name = "   "))

        assertEquals(SaveBusinessCardResult.NameRequired, result)
        assertTrue(repository.cards.first().isEmpty())
    }

    @Test
    fun validCard_isTrimmedAndSaved() = runTest {
        val result = saveBusinessCard(BusinessCard(name = " 山田太郎 ", company = " ACME "))

        assertEquals(SaveBusinessCardResult.Success, result)
        val saved = repository.cards.first().single()
        assertEquals("山田太郎", saved.name)
        assertEquals("ACME", saved.company)
    }
}
