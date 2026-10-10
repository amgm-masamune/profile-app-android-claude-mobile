package com.amgm.personallog.ui.startup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.amgm.personallog.ui.AppRoot

/** 起動時にDBを開いて確認し、失敗したらクラッシュせず原因を画面に表示する。 */
@Composable
fun StartupGate() {
    val vm: StartupViewModel = hiltViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when (val s = state) {
            StartupState.Checking -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            StartupState.Ready -> AppRoot()
            is StartupState.Failed -> DbErrorScreen(s.detail)
        }
    }
}

@Composable
private fun DbErrorScreen(detail: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("データベースを開けませんでした", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.error)
        Text(
            "アプリは終了せずにこの画面を表示しています。下の内容を開発担当に伝えてください。" +
                "データを消したくない場合は、アンインストールせずに最新版を上書きインストールしてください。",
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(detail, style = MaterialTheme.typography.bodySmall)
    }
}
