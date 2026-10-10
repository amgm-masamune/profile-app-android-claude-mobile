package com.amgm.personallog.ui.receipt

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.amgm.personallog.data.model.Categories
import com.amgm.personallog.receipt.ReceiptImageCodec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** レシートの確認・修正画面。 */
@Composable
fun ReceiptDetailScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReceiptDetailViewModel = hiltViewModel(),
) {
    val s by viewModel.state.collectAsStateWithLifecycle()
    var showDelete by rememberSaveable { mutableStateOf(false) }
    var showRescan by rememberSaveable { mutableStateOf(false) }
    var bigImage by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(s.finished) { if (s.finished) onBack() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("戻る") }
            Text("レシートの確認", style = MaterialTheme.typography.titleLarge)
        }

        if (s.loading) {
            CircularProgressIndicator()
            return@Column
        }
        if (s.notFound) {
            Text("レシートが見つかりません。")
            return@Column
        }

        if (s.imagePath.isNotBlank()) {
            ReceiptImage(imagePath = s.imagePath, big = bigImage)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { bigImage = !bigImage }) { Text(if (bigImage) "画像を小さく" else "画像を大きく") }
                OutlinedButton(enabled = !s.busy, onClick = { showRescan = true }) { Text("読み取り直す") }
            }
        }

        if (s.scanning) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CircularProgressIndicator()
                Text("読み取り中です…(最大1〜2分)")
            }
        }

        s.error?.let { msg ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Column(Modifier.padding(12.dp)) {
                    Text(msg, color = MaterialTheme.colorScheme.onErrorContainer)
                    TextButton(onClick = viewModel::dismissError) { Text("閉じる") }
                }
            }
        }
        s.info?.let { msg ->
            Card {
                Column(Modifier.padding(12.dp)) {
                    Text(msg)
                    TextButton(onClick = viewModel::dismissInfo) { Text("閉じる") }
                }
            }
        }

        if (s.confirmed) {
            Text("確定済み(編集して「再確定」すると家計簿も更新されます)", color = MaterialTheme.colorScheme.primary)
        } else {
            Text("未確定(下書き)", color = MaterialTheme.colorScheme.tertiary)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = s.date,
                onValueChange = viewModel::setDate,
                label = { Text("日付 yyyy-MM-dd") },
                singleLine = true,
                modifier = Modifier.weight(1.6f),
            )
            OutlinedTextField(
                value = s.time,
                onValueChange = viewModel::setTime,
                label = { Text("時刻 HH:mm") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
        }
        OutlinedTextField(
            value = s.store,
            onValueChange = viewModel::setStore,
            label = { Text("店名") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Text("品目", style = MaterialTheme.typography.titleMedium)
        s.items.forEachIndexed { index, item ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = item.name,
                            onValueChange = { viewModel.setItemName(item.key, it) },
                            label = { Text("品目${index + 1}") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = { viewModel.removeItem(item.key) }) { Text("削除") }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = item.quantity,
                            onValueChange = { viewModel.setItemQuantity(item.key, it) },
                            label = { Text("数量") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = item.amount,
                            onValueChange = { viewModel.setItemAmount(item.key, it) },
                            label = { Text("金額(円)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1.4f),
                        )
                        TextButton(onClick = { viewModel.toggleItemSign(item.key) }) { Text("+/-") }
                    }
                    CategoryPicker(value = item.category, onChange = { viewModel.setItemCategory(item.key, it) })
                }
            }
        }
        OutlinedButton(onClick = viewModel::addItem) { Text("品目を追加") }

        OutlinedTextField(
            value = s.total,
            onValueChange = viewModel::setTotal,
            label = { Text("合計(円)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )
        Text("品目の合計: ${formatYen(s.itemsSum)}", style = MaterialTheme.typography.bodyMedium)
        s.mismatch?.let { diff ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                Column(Modifier.padding(12.dp)) {
                    Text(
                        "品目の合計と合計欄が一致しません(合計欄 - 品目合計 = ${formatYen(diff)})。読み取りミスの可能性があります。",
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                    TextButton(onClick = viewModel::useItemsSumAsTotal) { Text("合計欄を品目合計に合わせる") }
                }
            }
        }

        if (s.rawJson.isNotBlank() && s.items.isEmpty()) {
            Text("Claudeの応答原文(解釈できなかった場合の参考)", style = MaterialTheme.typography.labelMedium)
            SelectionContainer {
                Text(s.rawJson, style = MaterialTheme.typography.bodySmall)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!s.confirmed) {
                OutlinedButton(enabled = !s.busy, onClick = viewModel::saveDraft) { Text("下書き保存") }
            }
            Button(enabled = !s.busy, onClick = viewModel::confirm) { Text(if (s.confirmed) "再確定" else "確定") }
        }
        TextButton(enabled = !s.busy, onClick = { showDelete = true }) {
            Text("このレシートを削除", color = MaterialTheme.colorScheme.error)
        }
    }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("レシートを削除") },
            text = { Text("レシート画像も端末から削除されます。このレシートから作った家計簿の行はどうしますか?") },
            confirmButton = {
                Column(horizontalAlignment = Alignment.End) {
                    TextButton(onClick = { showDelete = false; viewModel.delete(alsoExpenses = true) }) { Text("家計簿の行も消す") }
                    TextButton(onClick = { showDelete = false; viewModel.delete(alsoExpenses = false) }) { Text("家計簿の行は残す") }
                    TextButton(onClick = { showDelete = false }) { Text("キャンセル") }
                }
            },
        )
    }
    if (showRescan) {
        AlertDialog(
            onDismissRequest = { showRescan = false },
            title = { Text("読み取り直す") },
            text = { Text("現在の入力内容は、新しい読み取り結果で置き換わります。画像をClaudeに再送信します。") },
            confirmButton = {
                TextButton(onClick = { showRescan = false; viewModel.rescan() }) { Text("読み取り直す") }
            },
            dismissButton = { TextButton(onClick = { showRescan = false }) { Text("キャンセル") } },
        )
    }
}

@Composable
private fun CategoryPicker(value: String, onChange: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { open = true }) { Text("カテゴリ: $value") }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            val options = if (value in Categories.DEFAULTS) Categories.DEFAULTS else Categories.DEFAULTS + value
            options.forEach { c ->
                DropdownMenuItem(text = { Text(c) }, onClick = { onChange(c); open = false })
            }
        }
    }
}

@Composable
private fun ReceiptImage(imagePath: String, big: Boolean) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val bitmap by produceState<ImageBitmap?>(initialValue = null, imagePath) {
        value = withContext(Dispatchers.IO) {
            try {
                ReceiptImageCodec.decodeScaled(File(context.filesDir, imagePath), 1600)?.asImageBitmap()
            } catch (e: Exception) {
                null
            }
        }
    }
    val bmp = bitmap
    if (bmp == null) {
        Text("(画像を表示できません)", style = MaterialTheme.typography.bodySmall)
    } else {
        Image(
            bitmap = bmp,
            contentDescription = "レシート画像",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxWidth().heightIn(max = if (big) 640.dp else 220.dp),
        )
    }
}
