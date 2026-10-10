package com.amgm.personallog.ui.diary

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.io.File

/** 日記タブのトップ(一覧)。onOpenDate は "yyyy-MM-dd" の編集画面を開く。 */
@Composable
fun DiaryScreen(
    modifier: Modifier = Modifier,
    onOpenDate: (String) -> Unit = {},
) {
    val viewModel: DiaryListViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val today = remember { DiaryLogic.today() }
    val hasToday = state.items.any { it.entry.date == today }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onOpenDate(today) },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(if (hasToday) "今日の日記を開く" else "今日の日記を書く") },
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                state.error != null -> Text(
                    state.error.orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp),
                )
                state.loading -> Unit
                state.items.isEmpty() -> Text(
                    "まだ日記がありません。\n右下のボタンから今日の日記を書いてみましょう。",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.align(Alignment.Center).padding(16.dp),
                )
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = 12.dp, end = 12.dp, top = 12.dp, bottom = 96.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(state.items, key = { it.entry.id }) { item ->
                        DiaryCard(item = item, onClick = { onOpenDate(item.entry.date) })
                    }
                }
            }
        }
    }
}

@Composable
private fun DiaryCard(item: DiaryListItem, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                DiaryLogic.formatDateLabel(item.entry.date),
                style = MaterialTheme.typography.titleMedium,
            )
            val preview = DiaryLogic.previewText(item.entry.text)
            if (preview.isNotEmpty()) {
                Text(preview, style = MaterialTheme.typography.bodyMedium, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
            if (item.photos.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    item.photos.take(4).forEach { p ->
                        PhotoThumb(relativePath = p.filePath, reqPx = 200, modifier = Modifier.size(64.dp))
                    }
                    Text("${item.photos.size}枚", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

/** filesDir 相対パスの写真をサムネとして表示(正方形に中央クロップ)。 */
@Composable
fun PhotoThumb(relativePath: String, reqPx: Int, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val file = remember(relativePath) { File(context.filesDir, relativePath) }
    val bitmap by rememberPhotoBitmap(file, reqPx)
    Box(
        modifier = modifier.clip(RoundedCornerShape(6.dp)),
    ) {
        val bmp = bitmap
        if (bmp != null) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Box(modifier = Modifier.fillMaxSize())
        }
    }
}
