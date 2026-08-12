package com.worddeck.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.worddeck.feature.auth.LoginScreen
import com.worddeck.feature.auth.RegisterScreen
import com.worddeck.feature.home.HomeScreen

object AppGraph {
    const val AUTH = "auth"
    const val MAIN = "main"
}

object AppDestination {
    const val LOGIN = "auth/login"
    const val REGISTER = "auth/register"
    const val HOME = "main/home"
}

@Composable
fun AppNavigation(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = AppGraph.AUTH,
        modifier = modifier,
    ) {
        navigation(
            route = AppGraph.AUTH,
            startDestination = AppDestination.LOGIN,
        ) {
            composable(AppDestination.LOGIN) {
                LoginScreen()
            }
            composable(AppDestination.REGISTER) {
                RegisterScreen()
            }
        }

        navigation(
            route = AppGraph.MAIN,
            startDestination = AppDestination.HOME,
        ) {
            composable(AppDestination.HOME) {
                HomeScreen()
            }
        }
    }
}
