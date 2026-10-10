@file:OptIn(ExperimentalMaterial3Api::class)

package com.amgm.personallog.ui.ledger

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Canvas
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.amgm.personallog.backup.ImportMode
import com.amgm.personallog.data.model.Categories
import com.amgm.personallog.data.model.Expense
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** 家計簿タブのトップ画面。 */
@Composable
fun LedgerScreen(modifier: Modifier = Modifier, viewModel: LedgerViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<Expense?>(null) }
    var creating by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) viewModel.exportTo(uri)
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.onImportPicked(uri)
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        ) {
            item(key = "header") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = viewModel::previousMonth) { Text("◀") }
                    Text(
                        LedgerLogic.monthLabel(state.month),
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center,
                    )
                    TextButton(onClick = viewModel::nextMonth) { Text("▶") }
                    Spacer(Modifier.weight(1f))
                    Box {
                        TextButton(onClick = { menuOpen = true }) { Text("バックアップ ▾") }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text("エクスポート(ZIPに保存)") },
                                onClick = {
                                    menuOpen = false
                                    exportLauncher.launch("personal-log-${LocalDate.now()}.zip")
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("インポート(ZIPから復元)") },
                                onClick = {
                                    menuOpen = false
                                    importLauncher.launch(arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream"))
                                },
                            )
                        }
                    }
                }
            }
            state.error?.let { err ->
                item(key = "error") {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    ) {
                        Row(Modifier.padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(err, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.weight(1f))
                            TextButton(onClick = viewModel::dismissError) { Text("閉じる") }
                        }
                    }
                }
            }
            item(key = "total") {
                Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Text("支出合計", style = MaterialTheme.typography.labelLarge)
                    Text(
                        LedgerLogic.formatYen(state.total),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            item(key = "cat-title") { SectionTitle("カテゴリ別") }
            if (state.shares.isEmpty()) {
                item(key = "cat-empty") {
                    Text(
                        if (state.loading) "読み込み中…" else "この月の支出はありません",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
            } else {
                val max = state.shares.maxOf { it.total }.coerceAtLeast(1L)
                items(state.shares, key = { "cat-" + it.category }) { share ->
                    CategoryBar(share, fraction = share.total.toFloat() / max)
                }
            }
            item(key = "trend-title") { SectionTitle("月別推移(直近6か月)") }
            item(key = "trend") { TrendChart(state.trend, selected = state.month) }
            item(key = "list-title") { SectionTitle("支出一覧") }
            if (state.days.isEmpty()) {
                item(key = "list-empty") {
                    Text("右下の + から支出を追加できます", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 8.dp))
                }
            }
            state.days.forEach { group ->
                item(key = "day-" + group.date) {
                    Row(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(LedgerLogic.dayLabel(group.date), style = MaterialTheme.typography.titleSmall)
                        Text(LedgerLogic.formatYen(group.total), style = MaterialTheme.typography.titleSmall)
                    }
                    HorizontalDivider()
                }
                items(group.items, key = { "exp-" + it.id }) { expense ->
                    ExpenseRow(expense, onClick = { editing = expense })
                }
            }
        }

        FloatingActionButton(
            onClick = { creating = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        ) { Icon(Icons.Filled.Add, contentDescription = "支出を追加") }
    }

    if (creating) {
        ExpenseEditorDialog(
            original = null,
            initialDate = LedgerLogic.defaultDate(state.month, LocalDate.now()),
            onDismiss = { creating = false },
            onSave = { date, amount, category, memo, done -> viewModel.saveExpense(null, date, amount, category, memo, done) },
            onDelete = null,
        )
    }
    editing?.let { target ->
        ExpenseEditorDialog(
            original = target,
            initialDate = target.date,
            onDismiss = { editing = null },
            onSave = { date, amount, category, memo, done -> viewModel.saveExpense(target, date, amount, category, memo, done) },
            onDelete = {
                viewModel.deleteExpense(target)
                editing = null
            },
        )
    }

    BackupDialogs(
        state = state.backup,
        onPickMode = viewModel::confirmImport,
        onCancelImport = viewModel::cancelImport,
        onDismissResult = viewModel::dismissBackupResult,
    )
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 20.dp, bottom = 6.dp),
    )
}

@Composable
private fun CategoryBar(share: CategoryShare, fraction: Float) {
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(share.category, style = MaterialTheme.typography.bodyMedium)
            Text("${LedgerLogic.formatYen(share.total)}(${share.percent}%)", style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.height(2.dp))
        Box(
            Modifier.fillMaxWidth().height(10.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(5.dp)),
        ) {
            Box(
                Modifier.fillMaxWidth(fraction.coerceIn(0.02f, 1f)).height(10.dp)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(5.dp)),
            )
        }
    }
}

@Composable
private fun TrendChart(trend: List<MonthTotal>, selected: String) {
    if (trend.isEmpty()) return
    val max = trend.maxOf { it.total }.coerceAtLeast(1L)
    val active = MaterialTheme.colorScheme.primary
    val inactive = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
    Column(Modifier.fillMaxWidth()) {
        Canvas(Modifier.fillMaxWidth().height(110.dp)) {
            val n = trend.size
            val slot = size.width / n
            val barWidth = slot * 0.55f
            trend.forEachIndexed { i, m ->
                val h = if (m.total <= 0L) 2f else (size.height * (m.total.toFloat() / max)).coerceAtLeast(4f)
                drawRoundRect(
                    color = if (m.yearMonth == selected) active else inactive,
                    topLeft = Offset(slot * i + (slot - barWidth) / 2f, size.height - h),
                    size = Size(barWidth, h),
                    cornerRadius = CornerRadius(4f, 4f),
                )
            }
        }
        Row(Modifier.fillMaxWidth()) {
            trend.forEach { m ->
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        LedgerLogic.compactYen(m.total),
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Clip,
                    )
                    Text(
                        "${m.yearMonth.takeLast(2).trimStart('0')}月",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (m.yearMonth == selected) FontWeight.Bold else FontWeight.Normal,
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpenseRow(expense: Expense, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(expense.category, style = MaterialTheme.typography.bodyLarge)
                if (expense.receiptId != null) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "レシート由来",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.tertiaryContainer, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
            if (expense.memo.isNotBlank()) {
                Text(
                    expense.memo,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Text(LedgerLogic.formatYen(expense.amount.toLong()), style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun ExpenseEditorDialog(
    original: Expense?,
    initialDate: String,
    onDismiss: () -> Unit,
    onSave: (date: String, amount: Int, category: String, memo: String, done: (String?) -> Unit) -> Unit,
    onDelete: (() -> Unit)?,
) {
    var dateText by remember { mutableStateOf(initialDate) }
    var amountText by remember { mutableStateOf(original?.amount?.toString() ?: "") }
    var category by remember { mutableStateOf(original?.category) }
    var memo by remember { mutableStateOf(original?.memo ?: "") }
    var validation by remember { mutableStateOf<ExpenseValidation?>(null) }
    var saveError by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showCategoryMenu by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    val categories = remember(original) {
        val base = Categories.DEFAULTS.filter { it != Categories.UNCATEGORIZED }
        val extra = original?.category
        if (extra != null && extra !in base) base + extra else base
    }

    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text(if (original == null) "支出を追加" else "支出を編集") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (original?.receiptId != null) {
                    Text("レシート由来の支出です", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.tertiary)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = dateText,
                        onValueChange = { dateText = it },
                        label = { Text("日付 (yyyy-MM-dd)") },
                        singleLine = true,
                        isError = validation?.dateError != null,
                        supportingText = if (validation?.dateError != null) { { Text(validation?.dateError ?: "") } } else null,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { showDatePicker = true }) { Text("選択") }
                }
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("金額(円)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = validation?.amountError != null,
                    supportingText = if (validation?.amountError != null) { { Text(validation?.amountError ?: "") } } else null,
                    modifier = Modifier.fillMaxWidth(),
                )
                Column {
                    Box {
                        OutlinedButton(onClick = { showCategoryMenu = true }, modifier = Modifier.fillMaxWidth()) {
                            Text("カテゴリ: " + (category ?: "選択してください") + " ▾")
                        }
                        DropdownMenu(expanded = showCategoryMenu, onDismissRequest = { showCategoryMenu = false }) {
                            categories.forEach { c ->
                                DropdownMenuItem(
                                    text = { Text(c) },
                                    onClick = {
                                        category = c
                                        showCategoryMenu = false
                                    },
                                )
                            }
                        }
                    }
                    validation?.categoryError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
                OutlinedTextField(
                    value = memo,
                    onValueChange = { memo = it },
                    label = { Text("メモ") },
                    modifier = Modifier.fillMaxWidth(),
                )
                saveError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !saving,
                onClick = {
                    val v = LedgerLogic.validate(dateText, amountText, category)
                    validation = v
                    saveError = null
                    if (v.isValid) {
                        saving = true
                        onSave(v.date!!, v.amount!!, v.category!!, memo) { err ->
                            saving = false
                            if (err == null) onDismiss() else saveError = err
                        }
                    }
                },
            ) { Text("保存") }
        },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    TextButton(enabled = !saving, onClick = { confirmDelete = true }) {
                        Text("削除", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(enabled = !saving, onClick = onDismiss) { Text("キャンセル") }
            }
        },
    )

    if (showDatePicker) {
        val initialMillis = try {
            LocalDate.parse(dateText.trim()).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        } catch (e: Exception) {
            null
        }
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { ms ->
                        dateText = Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate().toString()
                    }
                    showDatePicker = false
                }) { Text("決定") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("キャンセル") } },
        ) { DatePicker(state = pickerState) }
    }

    if (confirmDelete && onDelete != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("削除しますか?") },
            text = { Text("この支出を削除します。元に戻せません。") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete()
                }) { Text("削除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("キャンセル") } },
        )
    }
}

@Composable
private fun BackupDialogs(
    state: BackupUiState,
    onPickMode: (ImportMode) -> Unit,
    onCancelImport: () -> Unit,
    onDismissResult: () -> Unit,
) {
    if (state.pendingImport != null) {
        var mode by remember { mutableStateOf(ImportMode.MERGE) }
        AlertDialog(
            onDismissRequest = onCancelImport,
            title = { Text("バックアップを取り込む") },
            text = {
                Column {
                    Text("既存のデータの扱いを選んでください。")
                    Spacer(Modifier.height(8.dp))
                    ModeRow("追加(重複はスキップ)", "今あるデータは残し、無いものだけ追加します", mode == ImportMode.MERGE) { mode = ImportMode.MERGE }
                    ModeRow("すべて置き換え", "今ある日記・レシート・家計簿を全て削除してから取り込みます", mode == ImportMode.REPLACE) { mode = ImportMode.REPLACE }
                }
            },
            confirmButton = { TextButton(onClick = { onPickMode(mode) }) { Text("取り込む") } },
            dismissButton = { TextButton(onClick = onCancelImport) { Text("キャンセル") } },
        )
    }
    state.progress?.let { p ->
        AlertDialog(
            onDismissRequest = {},
            title = { Text("処理中") },
            text = {
                Column {
                    Text(p.message + if (p.total > 0) "(${p.current}/${p.total})" else if (p.current > 0) "(${p.current})" else "")
                    Spacer(Modifier.height(12.dp))
                    if (p.total > 0) {
                        LinearProgressIndicator(progress = { p.current.toFloat() / p.total }, modifier = Modifier.fillMaxWidth())
                    } else {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
            },
            confirmButton = {},
        )
    }
    if (state.resultTitle != null) {
        AlertDialog(
            onDismissRequest = onDismissResult,
            title = { Text(state.resultTitle) },
            text = { Text(state.resultMessage ?: "") },
            confirmButton = { TextButton(onClick = onDismissResult) { Text("OK") } },
        )
    }
}

@Composable
private fun ModeRow(title: String, sub: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Column {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
