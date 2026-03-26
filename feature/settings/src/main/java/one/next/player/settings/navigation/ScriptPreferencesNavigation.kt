package one.next.player.settings.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import one.next.player.settings.screens.scripts.ManageScriptsScreen
import one.next.player.settings.screens.scripts.ScriptPreferencesScreen

const val scriptPreferencesNavigationRoute = "script_preferences_route"
const val manageScriptsNavigationRoute = "manage_scripts_route"

fun NavController.navigateToScriptPreferences(navOptions: NavOptions? = null) {
    this.navigate(scriptPreferencesNavigationRoute, navOptions)
}

fun NavController.navigateToManageScripts(navOptions: NavOptions? = null) {
    this.navigate(manageScriptsNavigationRoute, navOptions)
}

fun NavGraphBuilder.scriptPreferencesScreen(
    onNavigateUp: () -> Unit,
    onManageScriptsClick: () -> Unit
) {
    composable(route = scriptPreferencesNavigationRoute) {
        ScriptPreferencesScreen(
            onNavigateUp = onNavigateUp,
            onManageScriptsClick = onManageScriptsClick
        )
    }
}

fun NavGraphBuilder.manageScriptsScreen(
    onNavigateUp: () -> Unit
) {
    composable(route = manageScriptsNavigationRoute) {
        ManageScriptsScreen(
            onNavigateUp = onNavigateUp
        )
    }
}
