package com.prismgrade.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.prismgrade.di.ServiceLocator
import com.prismgrade.ui.capture.CaptureScreen
import com.prismgrade.ui.capture.CaptureViewModel
import com.prismgrade.ui.history.HistoryScreen
import com.prismgrade.ui.history.HistoryViewModel
import com.prismgrade.ui.result.ResultScreen
import com.prismgrade.ui.result.ResultViewModel
import com.prismgrade.ui.settings.SettingsScreen
import com.prismgrade.ui.settings.SettingsViewModel
import com.prismgrade.ui.theme.PrismColors

/** Every place the app can be. */
sealed class Destination(val route: String) {
    data object Capture : Destination("capture")
    data object History : Destination("history")
    data object Settings : Destination("settings")

    data object Result : Destination("result/{inspectionId}") {
        const val ARG = "inspectionId"
        fun routeFor(id: Long) = "result/$id"
    }
}

private data class TabItem(
    val destination: Destination,
    val label: String,
    val icon: ImageVector,
)

private val TABS = listOf(
    TabItem(Destination.Capture, "Inspect", Icons.Default.CameraAlt),
    TabItem(Destination.History, "History", Icons.Default.History),
    TabItem(Destination.Settings, "Settings", Icons.Default.Settings),
)

@Composable
fun PrismGradeNavHost(
    services: ServiceLocator,
    navController: NavHostController = rememberNavController(),
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination

    Scaffold(
        containerColor = PrismColors.Void,
        bottomBar = {
            NavigationBar(containerColor = PrismColors.Panel) {
                TABS.forEach { tab ->
                    val selected = currentRoute?.hierarchy?.any {
                        it.route == tab.destination.route
                    } == true

                    NavigationBarItem(
                        selected = selected,
                        onClick = { navController.navigateToTab(tab.destination.route) },
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(tab.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PrismColors.Scan,
                            selectedTextColor = PrismColors.Scan,
                            unselectedIconColor = PrismColors.InkFaint,
                            unselectedTextColor = PrismColors.InkFaint,
                            indicatorColor = PrismColors.PanelRaised,
                        ),
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Destination.Capture.route,
            modifier = Modifier.padding(padding),
        ) {
            composable(Destination.Capture.route) {
                val viewModel: CaptureViewModel = viewModel(
                    factory = CaptureViewModel.Factory(
                        services.inspectionRepository,
                        services.imageProcessor,
                    ),
                )
                val state by viewModel.uiState.collectAsStateWithLifecycle()

                // A finished inspection opens its readout, then the flag is cleared
                // so returning to this tab doesn't bounce forward again.
                LaunchedEffect(state.completedInspectionId) {
                    state.completedInspectionId?.let { id ->
                        viewModel.onNavigatedToResult()
                        viewModel.reset()
                        navController.navigate(Destination.Result.routeFor(id))
                    }
                }

                CaptureScreen(
                    state = state,
                    onPickImage = viewModel::onImagePicked,
                    onClearImage = viewModel::clearImage,
                    onModeChange = viewModel::setMode,
                    onCardDescriptionChange = viewModel::onCardDescriptionChange,
                    onConditionDescriptionChange = viewModel::onConditionDescriptionChange,
                    onRunInspection = viewModel::runInspection,
                    onOpenSettings = { navController.navigateToTab(Destination.Settings.route) },
                    newCaptureFile = viewModel::newCaptureFile,
                )
            }

            composable(Destination.History.route) {
                val viewModel: HistoryViewModel = viewModel(
                    factory = HistoryViewModel.Factory(services.inspectionRepository),
                )
                val inspections by viewModel.inspections.collectAsStateWithLifecycle()

                HistoryScreen(
                    inspections = inspections,
                    onOpen = { id -> navController.navigate(Destination.Result.routeFor(id)) },
                    onDelete = viewModel::delete,
                )
            }

            composable(Destination.Settings.route) {
                val viewModel: SettingsViewModel = viewModel(
                    factory = SettingsViewModel.Factory(services.settingsStore),
                )
                val state by viewModel.uiState.collectAsStateWithLifecycle()

                SettingsScreen(
                    state = state,
                    onApiKeyChange = viewModel::onApiKeyChange,
                    onSave = viewModel::save,
                    onClear = viewModel::clear,
                )
            }

            composable(
                route = Destination.Result.route,
                arguments = listOf(navArgument(Destination.Result.ARG) { type = NavType.LongType }),
            ) { entry ->
                val id = entry.arguments?.getLong(Destination.Result.ARG) ?: return@composable
                val viewModel: ResultViewModel = viewModel(
                    factory = ResultViewModel.Factory(services.inspectionRepository),
                )
                val inspection by viewModel.inspection.collectAsStateWithLifecycle()

                LaunchedEffect(id) { viewModel.load(id) }

                ResultScreen(inspection = inspection)
            }
        }
    }
}

/** Tab switching keeps one entry per tab and restores where the user was. */
private fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
