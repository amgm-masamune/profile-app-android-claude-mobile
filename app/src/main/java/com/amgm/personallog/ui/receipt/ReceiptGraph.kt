package com.amgm.personallog.ui.receipt

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument

object ReceiptRoutes {
    const val ROOT = "receipt"
    const val HOME = "receipt/home"
    const val DETAIL = "receipt/detail/{id}"

    fun detail(id: Long) = "receipt/detail/$id"
}

/** レシート機能のナビゲーショングラフ。AppNavHost から呼ばれるので、機能内の遷移はここで完結させる。 */
fun NavGraphBuilder.receiptGraph(navController: NavController) {
    navigation(route = ReceiptRoutes.ROOT, startDestination = ReceiptRoutes.HOME) {
        composable(ReceiptRoutes.HOME) {
            ReceiptScreen(onOpenReceipt = { id -> navController.navigate(ReceiptRoutes.detail(id)) })
        }
        composable(
            route = ReceiptRoutes.DETAIL,
            arguments = listOf(navArgument("id") { type = NavType.LongType }),
        ) {
            ReceiptDetailScreen(onBack = { navController.popBackStack() })
        }
    }
}
