package com.example.businesscard.ui.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBox
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.businesscard.R
import com.example.businesscard.ui.component.GlassButton
import com.example.businesscard.ui.component.GlassButtonStyle
import com.example.businesscard.ui.component.GlassConfirmDialog
import com.example.businesscard.ui.component.GlassIconButton
import com.example.businesscard.ui.component.GlassTextField
import com.example.businesscard.ui.component.SoftGlassScaffold
import com.example.businesscard.ui.component.SoftGlassTopBar
import com.example.businesscard.ui.preview.SampleCards
import com.example.businesscard.ui.theme.BusinessCardTheme

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
 *  - 左: 削除(編集時のみ。危険色の四角いアイコンボタン。押すと確認が出る)
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
                    .padding(start = 24.dp, top = 12.dp, end = 24.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (!uiState.isNew) {
                    GlassIconButton(
                        onClick = { showDeleteConfirm = true },
                        icon = Icons.Outlined.Delete,
                        contentDescription = stringResource(R.string.delete),
                        style = GlassButtonStyle.Danger,
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
                .verticalScroll(rememberScrollState())
                // 下端の光と右下の影が次の欄にかからないよう、欄どうしの間を広めに取る
                .padding(start = 24.dp, top = 8.dp, end = 24.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            GlassTextField(
                value = uiState.name,
                onValueChange = onNameChange,
                label = stringResource(R.string.label_name),
                leadingIcon = Icons.Outlined.Person,
                isError = uiState.nameError,
                errorText = if (uiState.nameError) stringResource(R.string.error_name_required) else null,
                keyboardOptions = NextField,
                modifier = Modifier.fillMaxWidth(),
            )
            GlassTextField(
                value = uiState.company,
                onValueChange = onCompanyChange,
                label = stringResource(R.string.label_company),
                leadingIcon = Icons.Outlined.Home,
                keyboardOptions = NextField,
                modifier = Modifier.fillMaxWidth(),
            )
            GlassTextField(
                value = uiState.title,
                onValueChange = onTitleChange,
                label = stringResource(R.string.label_title),
                leadingIcon = Icons.Outlined.AccountBox,
                keyboardOptions = NextField,
                modifier = Modifier.fillMaxWidth(),
            )
            GlassTextField(
                value = uiState.phone,
                onValueChange = onPhoneChange,
                label = stringResource(R.string.label_phone),
                leadingIcon = Icons.Outlined.Phone,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
            GlassTextField(
                value = uiState.email,
                onValueChange = onEmailChange,
                label = stringResource(R.string.label_email),
                leadingIcon = Icons.Outlined.Email,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
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

private val NextField = KeyboardOptions(imeAction = ImeAction.Next)

@Preview(widthDp = 412, heightDp = 892)
@Composable
private fun EditScreenPreview() {
    val card = SampleCards.taro
    BusinessCardTheme {
        EditScreen(
            uiState = EditUiState(
                name = card.name,
                company = card.company,
                title = card.title,
                phone = card.phone,
                email = card.email,
                isNew = false,
            ),
            onBack = {},
            onNameChange = {},
            onCompanyChange = {},
            onTitleChange = {},
            onPhoneChange = {},
            onEmailChange = {},
            onSave = {},
            onDelete = {},
        )
    }
}
