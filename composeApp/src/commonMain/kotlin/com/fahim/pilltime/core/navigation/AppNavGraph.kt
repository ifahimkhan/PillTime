package com.fahim.pilltime.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.savedstate.read
import com.fahim.pilltime.presentation.addedit.AddEditReminderScreen
import com.fahim.pilltime.presentation.reminderlist.ReminderListScreen

/**
 * Centralized routes for the whole app. Destinations are built through the typed [addEdit] helper
 * (which substitutes the optional id argument) rather than concatenating strings at call sites.
 */
object Routes {
    const val REMINDER_LIST = "reminderList"
    const val ARG_ID = "id"
    const val ADD_EDIT = "addEditReminder?$ARG_ID={$ARG_ID}"

    /** null id = add a new reminder; non-null = edit the existing one. */
    fun addEdit(id: Long? = null): String =
        if (id == null) "addEditReminder" else "addEditReminder?$ARG_ID=$id"
}

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.REMINDER_LIST) {
        composable(Routes.REMINDER_LIST) {
            ReminderListScreen(
                onAddReminder = { navController.navigate(Routes.addEdit()) },
                onEditReminder = { id -> navController.navigate(Routes.addEdit(id)) },
            )
        }
        composable(
            route = Routes.ADD_EDIT,
            arguments = listOf(
                navArgument(Routes.ARG_ID) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) { backStackEntry ->
            val id = backStackEntry.arguments
                ?.read { if (contains(Routes.ARG_ID)) getStringOrNull(Routes.ARG_ID) else null }
                ?.toLongOrNull()
            AddEditReminderScreen(
                reminderId = id,
                onDone = { navController.popBackStack() },
            )
        }
    }
}
