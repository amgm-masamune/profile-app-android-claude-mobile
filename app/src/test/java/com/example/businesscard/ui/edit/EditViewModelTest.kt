package com.example.businesscard.ui.edit

import androidx.lifecycle.SavedStateHandle
import com.example.businesscard.data.model.BusinessCard
import com.example.businesscard.ui.navigation.CARD_ID_ARG
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EditViewModelTest {

    private val repository = FakeBusinessCardRepository()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun save_withBlankName_showsErrorAndDoesNotSave() = runTest {
        val viewModel = EditViewModel(SavedStateHandle(), repository)

        viewModel.onNameChange("  ")
        viewModel.save()

        assertTrue(viewModel.uiState.value.nameError)
        assertFalse(viewModel.uiState.value.isSaved)
        assertTrue(repository.cards.first().isEmpty())
    }

    @Test
    fun save_withName_savesTrimmedCardAndFinishes() = runTest {
        val viewModel = EditViewModel(SavedStateHandle(), repository)

        viewModel.onNameChange(" 山田太郎 ")
        viewModel.onCompanyChange("ACME")
        viewModel.save()

        val saved = repository.cards.first().single()
        assertEquals("山田太郎", saved.name)
        assertEquals("ACME", saved.company)
        assertTrue(viewModel.uiState.value.isSaved)
    }

    @Test
    fun existingCard_isLoadedAndUpdatedWithoutCreatingNew() = runTest {
        repository.save(BusinessCard(name = "山田太郎", email = "taro@example.com"))
        val id = repository.cards.first().single().id

        val viewModel = EditViewModel(SavedStateHandle(mapOf(CARD_ID_ARG to id)), repository)
        assertEquals("山田太郎", viewModel.uiState.value.name)
        assertFalse(viewModel.uiState.value.isNew)

        viewModel.onNameChange("山田次郎")
        viewModel.save()

        val cards = repository.cards.first()
        assertEquals(1, cards.size)
        assertEquals("山田次郎", cards.single().name)
        assertEquals("taro@example.com", cards.single().email)
    }
}
