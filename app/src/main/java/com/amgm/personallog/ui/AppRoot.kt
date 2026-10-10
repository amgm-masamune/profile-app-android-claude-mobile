package com.amgm.personallog.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.amgm.personallog.ui.diary.DiaryRoutes
import com.amgm.personallog.ui.ledger.LedgerRoutes
import com.amgm.personallog.ui.receipt.ReceiptRoutes
import com.amgm.personallog.ui.settings.SettingsRoutes

enum class TopLevelTab(val route: String, val label: String, val icon: ImageVector) {
    Diary(DiaryRoutes.ROOT, "日記", Icons.Filled.Edit),
    Receipt(ReceiptRoutes.ROOT, "レシート", Icons.Filled.Check),
    Ledger(LedgerRoutes.ROOT, "家計簿", Icons.Filled.Home),
    Settings(SettingsRoutes.ROOT, "設定", Icons.Filled.Settings),
}

@Composable
fun AppRoot() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    Scaffold(
        bottomBar = {
            NavigationBar {
                TopLevelTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = destination?.hierarchy?.any { it.route == tab.route } == true,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        AppNavHost(navController = navController, modifier = Modifier.padding(innerPadding))
    }
}
