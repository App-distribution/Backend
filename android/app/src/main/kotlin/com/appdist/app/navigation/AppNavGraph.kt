package com.appdist.app.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.appdist.feature.auth.ui.login.LoginScreen
import com.appdist.feature.auth.ui.otp.OtpScreen
import com.appdist.feature.browse.ui.builds.BuildsScreen
import com.appdist.feature.browse.ui.projects.ProjectsScreen
import com.appdist.feature.builddetail.ui.BuildDetailScreen
import com.appdist.feature.home.ui.HomeScreen
import com.appdist.feature.mine.ui.MineScreen
import com.appdist.feature.settings.ui.PermissionsScreen
import com.appdist.feature.settings.ui.SettingsScreen
import com.appdist.feature.upload.ui.UploadScreen

private val bottomNavRoutes: Set<String> = bottomNavItems.map { it.route }.toSet()

@Composable
fun AppNavGraph(viewModel: AppNavViewModel = hiltViewModel()) {
    val authState by viewModel.authState.collectAsStateWithLifecycle()

    if (authState == AuthState.Loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
            if (currentRoute in bottomNavRoutes) BottomNavBar(navController)
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = if (authState == AuthState.Authenticated) "home" else "auth/login",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("auth/login") {
                LoginScreen(onNavigateToOtp = { email ->
                    navController.navigate("auth/otp/$email")
                })
            }
            composable(
                "auth/otp/{email}",
                arguments = listOf(navArgument("email") { type = NavType.StringType })
            ) { backStack ->
                OtpScreen(
                    email = backStack.arguments?.getString("email") ?: "",
                    onSuccess = {
                        navController.navigate("home") { popUpTo(0) { inclusive = true } }
                    }
                )
            }

            composable("home") {
                HomeScreen(onBuildClick = { navController.navigate("builds/detail/$it") })
            }

            composable("browse") {
                ProjectsScreen(
                    onProjectClick = { navController.navigate("browse/projects/$it/builds") },
                    onUploadClick = { navController.navigate("upload") }
                )
            }
            composable(
                "browse/projects/{projectId}/builds",
                arguments = listOf(navArgument("projectId") { type = NavType.StringType })
            ) { backStack ->
                BuildsScreen(
                    projectId = backStack.arguments?.getString("projectId") ?: "",
                    onBuildClick = { navController.navigate("builds/detail/$it") },
                    onUploadClick = { navController.navigate("upload") }
                )
            }

            composable(
                "builds/detail/{buildId}",
                arguments = listOf(navArgument("buildId") { type = NavType.StringType }),
                deepLinks = listOf(navDeepLink { uriPattern = "appdist://builds/{buildId}" })
            ) { backStack ->
                BuildDetailScreen(
                    buildId = backStack.arguments?.getString("buildId") ?: "",
                    onBack = { navController.popBackStack() }
                )
            }

            composable("upload") {
                UploadScreen(onSuccess = { navController.popBackStack() })
            }
            composable("mine") {
                MineScreen(onBuildClick = { navController.navigate("builds/detail/$it") })
            }
            composable("profile") {
                SettingsScreen(
                    onPermissionsClick = { navController.navigate("profile/permissions") },
                    onLogout = {
                        navController.navigate("auth/login") { popUpTo(0) { inclusive = true } }
                    }
                )
            }
            composable("profile/permissions") { PermissionsScreen() }
        }
    }
}
