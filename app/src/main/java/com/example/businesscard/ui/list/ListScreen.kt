package com.example.businesscard.ui.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.businesscard.R
import com.example.businesscard.ui.AppViewModelProvider
import com.example.businesscard.ui.component.BusinessCardView

/** ViewModelと接続するStatefulなエントリ。 */
@Composable
fun ListRoute(
    onAddClick: () -> Unit,
    onCardClick: (Long) -> Unit,
    viewModel: BusinessCardListViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ListScreen(uiState = uiState, onAddClick = onAddClick, onCardClick = onCardClick)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListScreen(
    uiState: ListUiState,
    onAddClick: () -> Unit,
    onCardClick: (Long) -> Unit,
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.list_title)) }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_card))
            }
        },
    ) { innerPadding ->
        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize().padding(innerPadding))
            uiState.cards.isEmpty() -> Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(stringResource(R.string.list_empty))
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(items = uiState.cards, key = { it.id }) { card ->
                    BusinessCardView(
                        card = card,
                        modifier = Modifier.clickable { onCardClick(card.id) },
                    )
                }
            }
        }
    }
}
