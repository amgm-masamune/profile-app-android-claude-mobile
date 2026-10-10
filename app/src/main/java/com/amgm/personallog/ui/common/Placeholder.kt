package com.amgm.personallog.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** 未実装タブ用の空画面。実装担当は自分の Screen から使わなくなってよい。 */
@Composable
fun PlaceholderScreen(title: String, description: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall)
        Text(description, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp))
    }
}
