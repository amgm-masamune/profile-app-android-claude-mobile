package com.amgm.personallog.ui.diary

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation

object DiaryRoutes {
    const val ROOT = "diary"
    const val HOME = "diary/home"
    // 機能内の画面遷移はここにルートを足し、下の navigation { } に composable を追加する
}

/** 日記機能のナビゲーショングラフ。AppNavHost から呼ばれるので、機能内の遷移はここで完結させる。 */
fun NavGraphBuilder.diaryGraph(navController: NavController) {
    navigation(route = DiaryRoutes.ROOT, startDestination = DiaryRoutes.HOME) {
        composable(DiaryRoutes.HOME) { DiaryScreen() }
    }
}
