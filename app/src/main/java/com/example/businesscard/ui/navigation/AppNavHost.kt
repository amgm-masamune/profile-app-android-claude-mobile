package com.example.businesscard.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.businesscard.ui.detail.DetailRoute
import com.example.businesscard.ui.edit.EditRoute
import com.example.businesscard.ui.list.ListRoute

@Composable
fun AppNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = CardList) {
        composable<CardList> {
            ListRoute(
                onAddClick = { navController.navigate(CardEdit()) },
                onCardClick = { id -> navController.navigate(CardDetail(id)) },
            )
        }
        composable<CardDetail> {
            DetailRoute(
                onBack = { navController.popBackStack() },
                onEdit = { id -> navController.navigate(CardEdit(id)) },
            )
        }
        composable<CardEdit> {
            EditRoute(
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() },
                // 削除した名刺の表示画面には戻れないので、一覧まで戻る
                onDeleted = { navController.popBackStack<CardList>(inclusive = false) },
            )
        }
    }
}
