package com.amgm.personallog.ui.receipt

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.amgm.personallog.data.model.Receipt
import com.amgm.personallog.data.model.ReceiptStatus

/** レシートタブのトップ画面: 一覧と「撮影」「写真から選ぶ」。 */
@Composable
fun ReceiptScreen(
    modifier: Modifier = Modifier,
    onOpenReceipt: (Long) -> Unit = {},
    viewModel: ReceiptListViewModel = hiltViewModel(),
) {
    val receipts by viewModel.receipts.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()

    // 撮影中の保存先(画面回転・プロセス再生成後も結果を受け取れるよう保存する)
    var pendingPath by rememberSaveable { mutableStateOf<String?>(null) }

    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val path = pendingPath
        pendingPath = null
        if (path != null) {
            if (ok) viewModel.onCaptured(path) else viewModel.discardCapture(path)
        }
    }
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.onPicked(uri)
    }

    LaunchedEffect(Unit) {
        viewModel.openEvents.collect { onOpenReceipt(it) }
    }

    Column(modifier = modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("レシート", style = MaterialTheme.typography.headlineSmall)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                enabled = !state.scanning,
                onClick = {
                    try {
                        val path = viewModel.newCameraPath()
                        pendingPath = path
                        takePicture.launch(viewModel.cameraUri(path))
                    } catch (e: Exception) {
                        pendingPath = null
                        viewModel.showError("カメラを起動できませんでした。「写真から選ぶ」をお使いください。")
                    }
                },
            ) { Text("撮影") }
            Button(
                enabled = !state.scanning,
                onClick = {
                    pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
            ) { Text("写真から選ぶ") }
        }

        if (state.scanning) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CircularProgressIndicator()
                Text("レシートを読み取り中です…(最大1〜2分かかることがあります)")
            }
        }

        state.error?.let { msg ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Column(Modifier.padding(12.dp)) {
                    Text(msg, color = MaterialTheme.colorScheme.onErrorContainer)
                    TextButton(onClick = viewModel::dismissError) { Text("閉じる") }
                }
            }
        }

        if (receipts.isEmpty()) {
            Text("まだレシートがありません。「撮影」または「写真から選ぶ」から追加してください。")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                items(receipts, key = { it.id }) { r ->
                    ReceiptRow(r, onClick = { onOpenReceipt(r.id) })
                }
            }
        }
    }
}

@Composable
private fun ReceiptRow(r: Receipt, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.weight(1f)) {
                Text(r.purchasedAt, style = MaterialTheme.typography.bodySmall)
                Text(r.storeName.ifBlank { "(店名未入力)" }, style = MaterialTheme.typography.titleMedium)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(formatYen(r.totalAmount.toLong()), style = MaterialTheme.typography.titleMedium)
                if (r.status == ReceiptStatus.DRAFT) {
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = RoundedCornerShape(4.dp),
                    ) {
                        Text(
                            "未確定",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
            }
        }
    }
}

internal fun formatYen(v: Long): String = String.format(java.util.Locale.JAPAN, "¥%,d", v)
