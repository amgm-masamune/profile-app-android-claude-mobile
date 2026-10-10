package com.amgm.personallog.ui.receipt

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation

object ReceiptRoutes {
    const val ROOT = "receipt"
    const val HOME = "receipt/home"
    // 機能内の画面遷移はここにルートを足し、下の navigation { } に composable を追加する
}

/** レシート機能のナビゲーショングラフ。AppNavHost から呼ばれるので、機能内の遷移はここで完結させる。 */
fun NavGraphBuilder.receiptGraph(navController: NavController) {
    navigation(route = ReceiptRoutes.ROOT, startDestination = ReceiptRoutes.HOME) {
        composable(ReceiptRoutes.HOME) { ReceiptScreen() }
    }
}
