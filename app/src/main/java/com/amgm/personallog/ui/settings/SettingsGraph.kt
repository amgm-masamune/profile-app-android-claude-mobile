package com.amgm.personallog.ui.settings

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation

object SettingsRoutes {
    const val ROOT = "settings"
    const val HOME = "settings/home"
}

fun NavGraphBuilder.settingsGraph(navController: NavController) {
    navigation(route = SettingsRoutes.ROOT, startDestination = SettingsRoutes.HOME) {
        composable(SettingsRoutes.HOME) { SettingsScreen() }
    }
}
