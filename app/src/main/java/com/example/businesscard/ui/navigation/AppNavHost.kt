package com.example.businesscard.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.businesscard.ui.detail.DetailRoute
import com.example.businesscard.ui.edit.EditRoute
import com.example.businesscard.ui.list.ListRoute

@Composable
fun AppNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Routes.LIST) {
        composable(Routes.LIST) {
            ListRoute(
                onAddClick = { navController.navigate(Routes.edit()) },
                onCardClick = { id -> navController.navigate(Routes.detail(id)) },
            )
        }
        composable(
            route = Routes.DETAIL,
            arguments = listOf(navArgument(CARD_ID_ARG) { type = NavType.LongType }),
        ) {
            DetailRoute(
                onBack = { navController.popBackStack() },
                onEdit = { id -> navController.navigate(Routes.edit(id)) },
            )
        }
        composable(
            route = Routes.EDIT,
            arguments = listOf(
                navArgument(CARD_ID_ARG) {
                    type = NavType.LongType
                    defaultValue = NEW_CARD_ID
                },
            ),
        ) {
            EditRoute(
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() },
                // 削除した名刺の表示画面には戻れないので、一覧まで戻る
                onDeleted = { navController.popBackStack(Routes.LIST, inclusive = false) },
            )
        }
    }
}
