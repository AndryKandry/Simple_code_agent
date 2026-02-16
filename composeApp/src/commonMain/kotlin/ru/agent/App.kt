package ru.agent

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import org.jetbrains.compose.ui.tooling.preview.Preview
import ru.agent.features.main.presentation.MainScreen
import ru.agent.navigation.AppScreens
import ru.agent.navigation.LocalNavHost
import ru.agent.theme.AppTheme

@Preview
@Composable
internal fun App() = AppTheme {
    DefaultApp()
}

@Composable
internal fun DefaultApp(
    navController: NavHostController = rememberNavController()
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentScreen = backStackEntry?.destination?.route ?: AppScreens.Main

    CompositionLocalProvider(
        LocalNavHost provides navController
    ) {
        NavHost(
            navController = navController,
            startDestination = AppScreens.Main.title
        ) {
            composable(route = AppScreens.Main.title) {
                MainScreen()
            }
        }
    }
}
