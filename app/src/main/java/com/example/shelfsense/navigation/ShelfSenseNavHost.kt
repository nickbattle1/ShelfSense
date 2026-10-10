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
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder
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
import com.example.shelfsense.screens.auth.VerifyEmailScreen
import com.example.shelfsense.screens.detail.FoodDetailScreen
import com.example.shelfsense.screens.home.HomeScreen
import com.example.shelfsense.screens.insights.InsightsScreen
import com.example.shelfsense.screens.pantry.PantryScreen
import com.example.shelfsense.screens.profile.ProfileScreen
import com.example.shelfsense.ui.components.AppMessenger
import com.example.shelfsense.ui.components.LocalMessenger
import com.example.shelfsense.ui.theme.ShelfTheme

@Composable
fun ShelfSenseApp(startRoute: String, hasAccess: () -> Boolean) {
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

    // an old notification can still open the app for someone signed out or unverified, so send them to log in
    LaunchedEffect(destination) {
        val inMainGraph = destination?.hierarchy?.any { it.route == Routes.MAIN_GRAPH } == true
        if (inMainGraph && !hasAccess()) {
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
                        onClick = { navController.goTo(Routes.ADD_FOOD) },
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
                startDestination = startRoute,
                modifier = Modifier.padding(innerPadding).consumeWindowInsets(innerPadding)
            ) {
                authGraph(navController, hasAccess)
                composable(Routes.VERIFY_EMAIL) {
                    VerifyEmailScreen(
                        onVerified = {
                            navController.navigate(Routes.MAIN_GRAPH) {
                                popUpTo(navController.graph.id) { inclusive = true }
                            }
                        },
                        onSignedOut = {
                            navController.navigate(Routes.AUTH_GRAPH) {
                                popUpTo(navController.graph.id) { inclusive = true }
                            }
                        }
                    )
                }
                mainGraph(navController)
            }
        }
    }
}

private fun NavGraphBuilder.authGraph(navController: NavHostController, hasAccess: () -> Boolean) {
    navigation(startDestination = Routes.LOGIN, route = Routes.AUTH_GRAPH) {
        composable(Routes.LOGIN) {
            LoginScreen(
                onLoggedIn = { navController.enterApp(hasAccess) },
                onCreateAccount = { navController.goTo(Routes.SIGN_UP) },
                onForgotPassword = { email -> navController.goTo(Routes.forgotPassword(email)) }
            )
        }
        composable(Routes.SIGN_UP) {
            SignUpScreen(
                onBack = { navController.goBack() },
                onSignedUp = { navController.enterApp(hasAccess) }
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
            ForgotPasswordScreen(onBack = { navController.goBack() })
        }
    }
}

private fun NavGraphBuilder.mainGraph(navController: NavHostController) {
    navigation(startDestination = Routes.HOME, route = Routes.MAIN_GRAPH) {
        composable(Routes.HOME) {
            HomeScreen(
                onOpenItem = { id -> navController.goTo(Routes.detail(id)) },
                onOpenPantry = { filter -> navController.openTab(Routes.pantry(filter)) },
                onOpenInsights = { navController.openTab(Routes.INSIGHTS) },
                onAddFood = { navController.goTo(Routes.ADD_FOOD) }
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
                onOpenItem = { id -> navController.goTo(Routes.detail(id)) },
                onAddFood = { navController.goTo(Routes.ADD_FOOD) }
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
                onBack = { navController.goBack() },
                onScan = { navController.goTo(Routes.SCAN) },
                onEnterBarcode = { navController.goTo(Routes.ENTER_BARCODE) },
                onManual = { navController.goTo(Routes.foodForm()) }
            )
        }
        composable(Routes.SCAN) {
            ScanBarcodeScreen(
                onBack = { navController.goBack() },
                // not guarded, the scan result can arrive a moment before this screen is resumed again
                onScanned = { code -> navController.navigate(Routes.foodForm(barcode = code)) },
                onTypeInstead = {
                    navController.goTo(Routes.ENTER_BARCODE) {
                        popUpTo(Routes.SCAN) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.ENTER_BARCODE) {
            EnterBarcodeScreen(
                onBack = { navController.goBack() },
                onLookUp = { code -> navController.goTo(Routes.foodForm(barcode = code)) }
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
                onCancel = { navController.goBack() },
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
                onBack = { navController.goBack() },
                onEdit = { id -> navController.goTo(Routes.foodForm(itemId = id)) }
            )
        }
    }
}

// an unconfirmed email goes to the verify screen first, everyone else straight into the app
private fun NavHostController.enterApp(hasAccess: () -> Boolean) {
    navigate(if (hasAccess()) Routes.MAIN_GRAPH else Routes.VERIFY_EMAIL) {
        popUpTo(Routes.AUTH_GRAPH) { inclusive = true }
    }
}

// Home's shortcuts behave like tapping the tab, so the bottom bar stays in step
private fun NavHostController.openTab(route: String) {
    if (!settled()) return
    navigate(route) {
        popUpTo(Routes.HOME) { saveState = true }
        launchSingleTop = true
    }
}

// a second tap can land while the first screen change is still animating, before the new top screen is resumed.
// without this a double tap on Back pops Home as well and leaves a blank screen, or a double tap opens a screen twice
private fun NavHostController.settled(): Boolean =
    currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED

private fun NavHostController.goBack() {
    if (settled()) popBackStack()
}

private fun NavHostController.goTo(route: String, builder: NavOptionsBuilder.() -> Unit = {}) {
    if (settled()) navigate(route, builder)
}
