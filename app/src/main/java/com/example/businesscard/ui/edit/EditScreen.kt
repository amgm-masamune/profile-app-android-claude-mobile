package com.example.businesscard.ui.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.businesscard.R
import com.example.businesscard.ui.component.GlassButton
import com.example.businesscard.ui.component.GlassConfirmDialog
import com.example.businesscard.ui.component.GlassIconButton
import com.example.businesscard.ui.component.GlassTextField
import com.example.businesscard.ui.component.SoftGlassScaffold
import com.example.businesscard.ui.component.SoftGlassTopBar
import com.example.businesscard.ui.theme.SoftGlassTheme

@Composable
fun EditRoute(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    onDeleted: () -> Unit,
    viewModel: EditViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isSaved) { if (uiState.isSaved) onSaved() }
    LaunchedEffect(uiState.isDeleted) { if (uiState.isDeleted) onDeleted() }

    EditScreen(
        uiState = uiState,
        onBack = onBack,
        onNameChange = viewModel::onNameChange,
        onCompanyChange = viewModel::onCompanyChange,
        onTitleChange = viewModel::onTitleChange,
        onPhoneChange = viewModel::onPhoneChange,
        onEmailChange = viewModel::onEmailChange,
        onSave = viewModel::save,
        onDelete = viewModel::delete,
    )
}

/**
 * 編集画面。操作は画面下にまとめる(キーボードの真上に追従する)。
 *  - 左: 削除(編集時のみ。危険色の小さな丸ボタン。押すと確認が出る)
 *  - 右: 保存(主操作。広く光らせる)
 * 以前は保存・削除が上部に並んでいて、押し間違いと親指の届きにくさが問題だった。
 */
@Composable
fun EditScreen(
    uiState: EditUiState,
    onBack: () -> Unit,
    onNameChange: (String) -> Unit,
    onCompanyChange: (String) -> Unit,
    onTitleChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
) {
    var showDeleteConfirm by rememberSaveable { mutableStateOf(false) }

    SoftGlassScaffold(
        topBar = {
            SoftGlassTopBar(
                title = stringResource(
                    if (uiState.isNew) R.string.edit_title_new else R.string.edit_title_edit,
                ),
                onBack = onBack,
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (!uiState.isNew) {
                    GlassIconButton(
                        onClick = { showDeleteConfirm = true },
                        icon = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.delete),
                        tint = SoftGlassTheme.colors.danger,
                    )
                }
                GlassButton(
                    text = stringResource(R.string.save),
                    onClick = onSave,
                    modifier = Modifier.weight(1f),
                )
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GlassTextField(
                value = uiState.name,
                onValueChange = onNameChange,
                label = stringResource(R.string.label_name),
                isError = uiState.nameError,
                errorText = if (uiState.nameError) stringResource(R.string.error_name_required) else null,
                modifier = Modifier.fillMaxWidth(),
            )
            GlassTextField(
                value = uiState.company,
                onValueChange = onCompanyChange,
                label = stringResource(R.string.label_company),
                modifier = Modifier.fillMaxWidth(),
            )
            GlassTextField(
                value = uiState.title,
                onValueChange = onTitleChange,
                label = stringResource(R.string.label_title),
                modifier = Modifier.fillMaxWidth(),
            )
            GlassTextField(
                value = uiState.phone,
                onValueChange = onPhoneChange,
                label = stringResource(R.string.label_phone),
                modifier = Modifier.fillMaxWidth(),
            )
            GlassTextField(
                value = uiState.email,
                onValueChange = onEmailChange,
                label = stringResource(R.string.label_email),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    if (showDeleteConfirm) {
        GlassConfirmDialog(
            title = stringResource(R.string.delete_confirm_title),
            message = stringResource(R.string.delete_confirm_message),
            confirmLabel = stringResource(R.string.delete),
            dismissLabel = stringResource(R.string.cancel),
            onConfirm = {
                showDeleteConfirm = false
                onDelete()
            },
            onDismiss = { showDeleteConfirm = false },
        )
    }
}
