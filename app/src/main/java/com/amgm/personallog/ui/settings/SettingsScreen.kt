package com.amgm.personallog.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun SettingsScreen(modifier: Modifier = Modifier, viewModel: SettingsViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val info = remember { AppInfo.read(context) }
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("設定", style = MaterialTheme.typography.headlineSmall)

        OutlinedTextField(
            value = viewModel.apiKey,
            onValueChange = { viewModel.apiKey = it; viewModel.saved = false },
            label = { Text("Anthropic APIキー") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = viewModel.modelId,
            onValueChange = { viewModel.modelId = it; viewModel.saved = false },
            label = { Text("Claudeモデル ID") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Button(onClick = { viewModel.save() }) { Text("保存") }
        if (viewModel.saved) Text("保存しました", color = MaterialTheme.colorScheme.primary)

        Text("アプリ情報", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
        Text("versionName: ${info.versionName}")
        Text("versionCode: ${info.versionCode}")
        Text("署名 SHA-256(ビルド間で同じなら上書き更新できます):")
        SelectionContainer { Text(info.signatureSha256, style = MaterialTheme.typography.bodySmall) }
    }
}
