@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.amgm.personallog.ui.diary

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.amgm.personallog.data.model.DiaryPhoto
import java.io.File

@Composable
fun DiaryEditScreen(onClose: () -> Unit, modifier: Modifier = Modifier) {
    val viewModel: DiaryEditViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showDeleteEntry by rememberSaveable { mutableStateOf(false) }
    var viewerIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    var pendingDeleteId by rememberSaveable { mutableStateOf<Long?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris ->
        viewModel.addPhotos(uris)
    }

    LaunchedEffect(state.closed) {
        if (state.closed) onClose()
    }
    BackHandler { viewModel.saveAndClose() }

    val importing = state.importing != null

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TextButton(onClick = { viewModel.saveAndClose() }) { Text("戻る") }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { showDeleteEntry = true }, enabled = !state.loading) { Text("この日記を削除") }
                    Button(onClick = { viewModel.saveAndClose() }, enabled = !state.loading) { Text("保存") }
                }
            }
        },
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize().padding(padding).imePadding().padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    enabled = !importing && !state.loading,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(DiaryLogic.formatDateLabel(state.date)) }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                OutlinedTextField(
                    value = viewModel.text,
                    onValueChange = viewModel::onTextChange,
                    enabled = !state.loading,
                    label = { Text("本文") },
                    minLines = 6,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }
            state.error?.let { message ->
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = { viewModel.clearError() }) { Text("閉じる") }
                    }
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Button(
                        onClick = {
                            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        enabled = !importing && !state.loading,
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Text("写真を追加")
                    }
                    Text("${state.photos.size}枚", style = MaterialTheme.typography.labelLarge)
                }
            }
            state.importing?.let { p ->
                item(span = { GridItemSpan(maxLineSpan) }) {
                    androidx.compose.foundation.layout.Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Text("写真を取り込み中… ${p.done} / ${p.total}", style = MaterialTheme.typography.bodySmall)
                        LinearProgressIndicator(
                            progress = { if (p.total == 0) 0f else p.done.toFloat() / p.total },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
            items(state.photos, key = { it.id }) { photo ->
                val index = state.photos.indexOf(photo)
                PhotoThumb(
                    relativePath = photo.filePath,
                    reqPx = 360,
                    modifier = Modifier
                        .aspectRatio(1f)
                        .combinedClickable(
                            onClick = { viewerIndex = index },
                            onLongClick = { pendingDeleteId = photo.id },
                        ),
                )
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                if (state.photos.isNotEmpty()) {
                    Text(
                        "写真をタップで拡大、長押しで削除",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
            }
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = DiaryLogic.dateToUtcMillis(state.date))
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { viewModel.changeDate(DiaryLogic.utcMillisToDate(it)) }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("キャンセル") } },
        ) { DatePicker(state = pickerState) }
    }

    if (showDeleteEntry) {
        ConfirmDialog(
            message = "この日の日記と写真(${state.photos.size}枚)をすべて削除します。元に戻せません。",
            confirmLabel = "削除",
            onConfirm = {
                showDeleteEntry = false
                viewModel.deleteEntry()
            },
            onDismiss = { showDeleteEntry = false },
        )
    }

    val pendingPhoto = state.photos.firstOrNull { it.id == pendingDeleteId }
    if (pendingDeleteId != null) {
        if (pendingPhoto == null) {
            pendingDeleteId = null
        } else {
            ConfirmDialog(
                message = "この写真を削除しますか?",
                confirmLabel = "削除",
                onConfirm = {
                    viewModel.deletePhoto(pendingPhoto)
                    pendingDeleteId = null
                },
                onDismiss = { pendingDeleteId = null },
            )
        }
    }

    viewerIndex?.let { start ->
        if (state.photos.isEmpty()) {
            viewerIndex = null
        } else {
            PhotoViewerDialog(
                photos = state.photos,
                startIndex = start.coerceIn(0, state.photos.size - 1),
                onDelete = { pendingDeleteId = it.id },
                onDismiss = { viewerIndex = null },
            )
        }
    }
}

@Composable
private fun ConfirmDialog(message: String, confirmLabel: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("キャンセル") } },
    )
}

@Composable
private fun PhotoViewerDialog(
    photos: List<DiaryPhoto>,
    startIndex: Int,
    onDelete: (DiaryPhoto) -> Unit,
    onDismiss: () -> Unit,
) {
    val photosState = rememberUpdatedState(photos)
    val pagerState = rememberPagerState(initialPage = startIndex, pageCount = { photosState.value.size })
    val context = LocalContext.current
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                val photo = photosState.value.getOrNull(page)
                if (photo != null) {
                    val file = remember(photo.filePath) { File(context.filesDir, photo.filePath) }
                    val bitmap by rememberPhotoBitmap(file, 2048)
                    val bmp = bitmap
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        if (bmp != null) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = null,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            Text("読み込み中…", color = Color.White)
                        }
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter).padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "${pagerState.currentPage + 1} / ${photos.size}",
                    color = Color.White,
                    modifier = Modifier.padding(start = 8.dp),
                )
                Row {
                    TextButton(onClick = {
                        photos.getOrNull(pagerState.currentPage)?.let(onDelete)
                    }) { Text("削除", color = Color.White) }
                    TextButton(onClick = onDismiss) { Text("閉じる", color = Color.White) }
                }
            }
        }
    }
}
