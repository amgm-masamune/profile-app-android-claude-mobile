package com.amgm.personallog.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.amgm.personallog.ui.diary.DiaryRoutes
import com.amgm.personallog.ui.diary.diaryGraph
import com.amgm.personallog.ui.ledger.ledgerGraph
import com.amgm.personallog.ui.receipt.receiptGraph
import com.amgm.personallog.ui.settings.settingsGraph

/**
 * 全体のルーティング(Wave1で完成。Wave2は編集しない)。
 * 各機能の内部遷移は ui/<機能>/<機能>Graph.kt の拡張関数で完結させる。
 */
@Composable
fun AppNavHost(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(navController = navController, startDestination = DiaryRoutes.ROOT, modifier = modifier) {
        diaryGraph(navController)
        receiptGraph(navController)
        ledgerGraph(navController)
        settingsGraph(navController)
    }
}
