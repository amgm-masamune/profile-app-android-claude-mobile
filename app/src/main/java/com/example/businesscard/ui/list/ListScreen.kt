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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.businesscard.R
import com.example.businesscard.domain.model.ThemeStyle
import com.example.businesscard.ui.designsystem.AppBusinessCard
import com.example.businesscard.ui.designsystem.AppButton
import com.example.businesscard.ui.designsystem.AppIconButton
import com.example.businesscard.ui.designsystem.AppIcons
import com.example.businesscard.ui.designsystem.AppMessage
import com.example.businesscard.ui.designsystem.AppScaffold
import com.example.businesscard.ui.designsystem.AppTheme
import com.example.businesscard.ui.designsystem.AppTopBar
import com.example.businesscard.ui.designsystem.BusinessCardAppTheme
import com.example.businesscard.ui.preview.SampleCards
import com.example.businesscard.ui.settings.ThemeSettingsRoute

/** ViewModelと接続するStatefulなエントリ。見た目の切り替えダイアログもここで開く。 */
@Composable
fun ListRoute(
    onAddClick: () -> Unit,
    onCardClick: (Long) -> Unit,
    viewModel: BusinessCardListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showThemeSettings by rememberSaveable { mutableStateOf(false) }
    ListScreen(
        uiState = uiState,
        onAddClick = onAddClick,
        onCardClick = onCardClick,
        onThemeClick = { showThemeSettings = true },
    )
    if (showThemeSettings) {
        ThemeSettingsRoute(onDismiss = { showThemeSettings = false })
    }
}

/**
 * 一覧画面。追加ボタンは画面下に横いっぱいに置く(親指が届き、左右どちらの手でも押せる)。
 * ボタンは名刺の上に浮かせず、リストの下に並べる(すりガラスどうしが重なると濁るため。どの見た目でも同じ配置)。
 * 見た目の切り替えは、右上の丸いボタンから。
 */
@Composable
fun ListScreen(
    uiState: ListUiState,
    onAddClick: () -> Unit,
    onCardClick: (Long) -> Unit,
    onThemeClick: () -> Unit = {},
) {
    val dimens = AppTheme.dimens
    AppScaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.list_title),
                actions = {
                    AppIconButton(
                        onClick = onThemeClick,
                        icon = AppIcons.Theme,
                        contentDescription = stringResource(R.string.theme_settings),
                    )
                },
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(
                        start = dimens.screenHorizontal,
                        top = dimens.actionsTop,
                        end = dimens.screenHorizontal,
                        bottom = dimens.actionsBottom,
                    ),
            ) {
                AppButton(
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
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(dimens.screenHorizontal),
                contentAlignment = Alignment.Center,
            ) {
                AppMessage(text = stringResource(R.string.list_empty))
            }
            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                // 右下に落ちる影と下端の光が切れないよう、下は多めに空ける
                contentPadding = PaddingValues(
                    start = dimens.screenHorizontal,
                    top = dimens.contentTop,
                    end = dimens.screenHorizontal,
                    bottom = dimens.contentBottom,
                ),
                verticalArrangement = Arrangement.spacedBy(dimens.listSpacing),
            ) {
                items(items = uiState.cards, key = { it.id }) { card ->
                    AppBusinessCard(
                        card = card,
                        onClick = { onCardClick(card.id) },
                    )
                }
            }
        }
    }
}

@Preview(name = "Edgelit", widthDp = 412, heightDp = 892)
@Composable
private fun ListScreenEdgelitPreview() {
    BusinessCardAppTheme(ThemeStyle.EDGELIT) {
        ListScreen(uiState = ListUiState(cards = SampleCards.all, isLoading = false), onAddClick = {}, onCardClick = {})
    }
}

@Preview(name = "Porcelain", widthDp = 412, heightDp = 892)
@Composable
private fun ListScreenPorcelainPreview() {
    BusinessCardAppTheme(ThemeStyle.PORCELAIN) {
        ListScreen(uiState = ListUiState(cards = SampleCards.all, isLoading = false), onAddClick = {}, onCardClick = {})
    }
}
