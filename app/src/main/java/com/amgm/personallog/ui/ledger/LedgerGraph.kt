package com.amgm.personallog.ui.ledger

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation

object LedgerRoutes {
    const val ROOT = "ledger"
    const val HOME = "ledger/home"
    // 機能内の画面遷移はここにルートを足し、下の navigation { } に composable を追加する
}

/** 家計簿機能のナビゲーショングラフ。AppNavHost から呼ばれるので、機能内の遷移はここで完結させる。 */
fun NavGraphBuilder.ledgerGraph(navController: NavController) {
    navigation(route = LedgerRoutes.ROOT, startDestination = LedgerRoutes.HOME) {
        composable(LedgerRoutes.HOME) { LedgerScreen() }
    }
}
