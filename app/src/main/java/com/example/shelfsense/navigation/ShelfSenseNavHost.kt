package com.example.shelfsense.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.example.shelfsense.domain.PantryFilter
import com.example.shelfsense.screens.addfood.AddFoodScreen
import com.example.shelfsense.screens.addfood.EnterBarcodeScreen
import com.example.shelfsense.screens.addfood.FoodFormScreen
import com.example.shelfsense.screens.addfood.ScanBarcodeScreen
import com.example.shelfsense.screens.auth.ForgotPasswordScreen
import com.example.shelfsense.screens.auth.LoginScreen
import com.example.shelfsense.screens.auth.SignUpScreen
import com.example.shelfsense.screens.detail.FoodDetailScreen
import com.example.shelfsense.screens.home.HomeScreen
import com.example.shelfsense.screens.insights.InsightsScreen
import com.example.shelfsense.screens.pantry.PantryScreen
import com.example.shelfsense.screens.profile.ProfileScreen
import com.example.shelfsense.ui.components.AppMessenger
import com.example.shelfsense.ui.components.LocalMessenger
import com.example.shelfsense.ui.theme.ShelfTheme

@Composable
fun ShelfSenseApp(startSignedIn: Boolean, isSignedIn: () -> Boolean) {
    val c = ShelfTheme.colors
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val messenger = remember(snackbarHostState, scope) { AppMessenger(snackbarHostState, scope) }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    val route = destination?.route
    val showBottomBar = route in topLevelRoutes
    val showFab = route == Routes.HOME || route == Routes.PANTRY

    // a signed out user can still arrive through an old notification, so send them to log in
    LaunchedEffect(destination) {
        val inMainGraph = destination?.hierarchy?.any { it.route == Routes.MAIN_GRAPH } == true
        if (inMainGraph && !isSignedIn()) {
            navController.navigate(Routes.AUTH_GRAPH) {
                popUpTo(navController.graph.id) { inclusive = true }
            }
        }
    }

    CompositionLocalProvider(LocalMessenger provides messenger) {
        Scaffold(
            containerColor = c.background,
            bottomBar = {
                if (showBottomBar) ShelfBottomBar(navController, destination)
            },
            floatingActionButton = {
                AnimatedVisibility(visible = showFab, enter = scaleIn(), exit = scaleOut()) {
                    FloatingActionButton(
                        onClick = { navController.navigate(Routes.ADD_FOOD) },
                        containerColor = c.primary,
                        contentColor = c.onPrimary
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Add food")
                    }
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { innerPadding ->
            // this scaffold owns the system bar insets, so each screen's own scaffold uses zero insets
            NavHost(
                navController = navController,
                startDestination = if (startSignedIn) Routes.MAIN_GRAPH else Routes.AUTH_GRAPH,
                modifier = Modifier.padding(innerPadding).consumeWindowInsets(innerPadding)
            ) {
                authGraph(navController)
                mainGraph(navController)
            }
        }
    }
}

private fun NavGraphBuilder.authGraph(navController: NavHostController) {
    navigation(startDestination = Routes.LOGIN, route = Routes.AUTH_GRAPH) {
        composable(Routes.LOGIN) {
            LoginScreen(
                onLoggedIn = { navController.enterApp() },
                onCreateAccount = { navController.navigate(Routes.SIGN_UP) },
                onForgotPassword = { email -> navController.navigate(Routes.forgotPassword(email)) }
            )
        }
        composable(Routes.SIGN_UP) {
            SignUpScreen(
                onBack = { navController.popBackStack() },
                onSignedUp = { navController.enterApp() }
            )
        }
        composable(
            Routes.FORGOT_PASSWORD,
            arguments = listOf(
                navArgument(Routes.ARG_EMAIL) {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) {
            ForgotPasswordScreen(onBack = { navController.popBackStack() })
        }
    }
}

private fun NavGraphBuilder.mainGraph(navController: NavHostController) {
    navigation(startDestination = Routes.HOME, route = Routes.MAIN_GRAPH) {
        composable(Routes.HOME) {
            HomeScreen(
                onOpenItem = { id -> navController.navigate(Routes.detail(id)) },
                onOpenPantry = { filter -> navController.openTab(Routes.pantry(filter)) },
                onOpenInsights = { navController.openTab(Routes.INSIGHTS) },
                onAddFood = { navController.navigate(Routes.ADD_FOOD) }
            )
        }
        composable(
            Routes.PANTRY,
            arguments = listOf(
                navArgument(Routes.ARG_FILTER) {
                    type = NavType.StringType
                    defaultValue = PantryFilter.ALL.name
                }
            ),
            deepLinks = listOf(navDeepLink { uriPattern = Routes.DEEP_LINK_PANTRY })
        ) {
            PantryScreen(
                onOpenItem = { id -> navController.navigate(Routes.detail(id)) },
                onAddFood = { navController.navigate(Routes.ADD_FOOD) }
            )
        }
        composable(Routes.INSIGHTS) {
            InsightsScreen()
        }
        composable(Routes.PROFILE) {
            ProfileScreen(
                onSignedOut = {
                    navController.navigate(Routes.AUTH_GRAPH) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.ADD_FOOD) {
            AddFoodScreen(
                onBack = { navController.popBackStack() },
                onScan = { navController.navigate(Routes.SCAN) },
                onEnterBarcode = { navController.navigate(Routes.ENTER_BARCODE) },
                onManual = { navController.navigate(Routes.foodForm()) }
            )
        }
        composable(Routes.SCAN) {
            ScanBarcodeScreen(
                onBack = { navController.popBackStack() },
                onScanned = { code -> navController.navigate(Routes.foodForm(barcode = code)) },
                onTypeInstead = {
                    navController.navigate(Routes.ENTER_BARCODE) {
                        popUpTo(Routes.SCAN) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.ENTER_BARCODE) {
            EnterBarcodeScreen(
                onBack = { navController.popBackStack() },
                onLookUp = { code -> navController.navigate(Routes.foodForm(barcode = code)) }
            )
        }
        composable(
            Routes.FOOD_FORM,
            arguments = listOf(
                navArgument(Routes.ARG_BARCODE) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument(Routes.ARG_ITEM_ID) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) {
            FoodFormScreen(
                onCancel = { navController.popBackStack() },
                onSaved = {
                    // a new item returns to wherever Add food was opened from, an edit returns to its detail screen
                    if (!navController.popBackStack(Routes.ADD_FOOD, inclusive = true)) {
                        navController.popBackStack()
                    }
                }
            )
        }
        composable(
            Routes.DETAIL,
            arguments = listOf(navArgument(Routes.ARG_ITEM_ID) { type = NavType.StringType }),
            deepLinks = listOf(navDeepLink { uriPattern = Routes.DEEP_LINK_ITEM })
        ) {
            FoodDetailScreen(
                onBack = { navController.popBackStack() },
                onEdit = { id -> navController.navigate(Routes.foodForm(itemId = id)) }
            )
        }
    }
}

private fun NavHostController.enterApp() {
    navigate(Routes.MAIN_GRAPH) {
        popUpTo(Routes.AUTH_GRAPH) { inclusive = true }
    }
}

// Home's shortcuts behave like tapping the tab, so the bottom bar stays in step
private fun NavHostController.openTab(route: String) {
    navigate(route) {
        popUpTo(Routes.HOME) { saveState = true }
        launchSingleTop = true
    }
}
