package com.example.businesscard.ui.settings

import com.example.businesscard.domain.model.ThemeStyle
import com.example.businesscard.domain.usecase.ObserveThemeStyleUseCase
import com.example.businesscard.domain.usecase.SetThemeStyleUseCase
import com.example.businesscard.testing.FakeUserPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ThemeSettingsViewModelTest {

    private val repository = FakeUserPreferencesRepository(initial = ThemeStyle.PORCELAIN)

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = ThemeSettingsViewModel(
        observeThemeStyle = ObserveThemeStyleUseCase(repository),
        setThemeStyle = SetThemeStyleUseCase(repository),
    )

    @Test
    fun uiState_showsSavedStyleAndAllOptions() = runTest {
        val viewModel = createViewModel()
        // WhileSubscribed なので、購読している間だけ値が流れる
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        assertEquals(ThemeStyle.PORCELAIN, viewModel.uiState.value.selected)
        assertEquals(ThemeStyle.entries, viewModel.uiState.value.options)
    }

    @Test
    fun select_savesStyleAndUpdatesState() = runTest {
        val viewModel = createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        viewModel.select(ThemeStyle.EDGELIT)

        assertEquals(ThemeStyle.EDGELIT, repository.themeStyle.first())
        assertEquals(ThemeStyle.EDGELIT, viewModel.uiState.value.selected)
    }
}
