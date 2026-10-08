package com.example.businesscard.ui.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FabPosition
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.businesscard.R
import com.example.businesscard.ui.AppViewModelProvider
import com.example.businesscard.ui.component.BusinessCardView
import com.example.businesscard.ui.component.GlassButton
import com.example.businesscard.ui.component.SoftGlassScaffold
import com.example.businesscard.ui.component.SoftGlassTopBar
import com.example.businesscard.ui.component.glassSurface
import com.example.businesscard.ui.theme.SoftGlassShapes
import com.example.businesscard.ui.theme.SoftGlassTheme

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

/**
 * 一覧画面。追加ボタンは画面下の中央に置く(親指が届き、左右どちらの手でも押せる)。
 * 最後の名刺が隠れないよう、リストの下に余白を足している。
 */
@Composable
fun ListScreen(
    uiState: ListUiState,
    onAddClick: () -> Unit,
    onCardClick: (Long) -> Unit,
) {
    SoftGlassScaffold(
        topBar = { SoftGlassTopBar(title = stringResource(R.string.list_title)) },
        floatingActionButton = {
            GlassButton(
                text = stringResource(R.string.add_card),
                onClick = onAddClick,
                icon = Icons.Default.Add,
            )
        },
        floatingActionButtonPosition = FabPosition.Center,
    ) { innerPadding ->
        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize())
            uiState.cards.isEmpty() -> Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.list_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = SoftGlassTheme.colors.inkMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .glassSurface(SoftGlassShapes.card)
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                )
            }
            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = innerPadding.calculateTopPadding()),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    top = 8.dp,
                    end = 16.dp,
                    bottom = innerPadding.calculateBottomPadding() + LIST_BOTTOM_SPACE,
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(items = uiState.cards, key = { it.id }) { card ->
                    BusinessCardView(
                        card = card,
                        onClick = { onCardClick(card.id) },
                    )
                }
            }
        }
    }
}

/** 浮かせた追加ボタン(高さ56dp + 余白)に最後の名刺が隠れないための下余白 */
private val LIST_BOTTOM_SPACE = 112.dp
