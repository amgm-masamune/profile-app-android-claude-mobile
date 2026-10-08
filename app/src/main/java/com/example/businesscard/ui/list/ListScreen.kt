package com.example.businesscard.ui.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.businesscard.R
import com.example.businesscard.ui.component.BusinessCardView
import com.example.businesscard.ui.component.GlassButton
import com.example.businesscard.ui.component.SoftGlassScaffold
import com.example.businesscard.ui.component.SoftGlassTopBar
import com.example.businesscard.ui.component.glassSurface
import com.example.businesscard.ui.preview.SampleCards
import com.example.businesscard.ui.theme.BusinessCardTheme
import com.example.businesscard.ui.theme.SoftGlassShapes
import com.example.businesscard.ui.theme.SoftGlassTheme
import com.example.businesscard.ui.theme.SoftGlassType

/** ViewModelと接続するStatefulなエントリ。 */
@Composable
fun ListRoute(
    onAddClick: () -> Unit,
    onCardClick: (Long) -> Unit,
    viewModel: BusinessCardListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ListScreen(uiState = uiState, onAddClick = onAddClick, onCardClick = onCardClick)
}

/**
 * 一覧画面。追加ボタンは画面下に横いっぱいに置く(親指が届き、左右どちらの手でも押せる)。
 * すりガラスどうしが重なると濁って見えるので、ボタンは名刺の上に浮かせず、リストの下に並べる。
 */
@Composable
fun ListScreen(
    uiState: ListUiState,
    onAddClick: () -> Unit,
    onCardClick: (Long) -> Unit,
) {
    SoftGlassScaffold(
        topBar = { SoftGlassTopBar(title = stringResource(R.string.list_title)) },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(start = 24.dp, top = 8.dp, end = 24.dp, bottom = 24.dp),
            ) {
                GlassButton(
                    text = stringResource(R.string.add_card),
                    onClick = onAddClick,
                    icon = Icons.Outlined.Add,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
    ) { innerPadding ->
        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize())
            uiState.cards.isEmpty() -> Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.list_empty),
                    style = SoftGlassType.body,
                    color = SoftGlassTheme.colors.ink,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .glassSurface(SoftGlassShapes.card)
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                )
            }
            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                // 右下に落ちる影と下端の光が切れないよう、下と右は多めに空ける
                contentPadding = PaddingValues(start = 24.dp, top = 8.dp, end = 24.dp, bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(36.dp),
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

@Preview(widthDp = 412, heightDp = 892)
@Composable
private fun ListScreenPreview() {
    BusinessCardTheme {
        ListScreen(uiState = ListUiState(cards = SampleCards.all, isLoading = false), onAddClick = {}, onCardClick = {})
    }
}
