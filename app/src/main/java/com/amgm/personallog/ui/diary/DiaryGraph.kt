package com.amgm.personallog.ui.diary

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument

object DiaryRoutes {
    const val ROOT = "diary"
    const val HOME = "diary/home"
    const val EDIT = "diary/edit/{date}"

    fun edit(date: String) = "diary/edit/$date"
}

/** 日記機能のナビゲーショングラフ。AppNavHost から呼ばれるので、機能内の遷移はここで完結させる。 */
fun NavGraphBuilder.diaryGraph(navController: NavController) {
    navigation(route = DiaryRoutes.ROOT, startDestination = DiaryRoutes.HOME) {
        composable(DiaryRoutes.HOME) {
            DiaryScreen(onOpenDate = { date -> navController.navigate(DiaryRoutes.edit(date)) })
        }
        composable(
            route = DiaryRoutes.EDIT,
            arguments = listOf(navArgument("date") { type = NavType.StringType }),
        ) {
            DiaryEditScreen(onClose = { navController.popBackStack() })
        }
    }
}
