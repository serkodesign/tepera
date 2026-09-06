package com.serkodesign.tepera.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.serkodesign.tepera.data.repository.ActivityRepository
import com.serkodesign.tepera.data.repository.CategoryRepository
import com.serkodesign.tepera.ui.addentry.AddEntryScreen
import com.serkodesign.tepera.ui.category.CategoriesScreen
import com.serkodesign.tepera.ui.home.HomeScreen

private object Routes {
    const val HOME = "home"
    const val ADD_ENTRY = "add_entry"
    const val CATEGORIES = "categories"
}

@Composable
fun TeperaNavHost(
    categoryRepository: CategoryRepository,
    activityRepository: ActivityRepository,
    navController: NavHostController = rememberNavController()
) {
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                categoryRepository = categoryRepository,
                activityRepository = activityRepository,
                onAddEntry = { navController.navigate(Routes.ADD_ENTRY) },
                onOpenCategories = { navController.navigate(Routes.CATEGORIES) }
            )
        }
        composable(Routes.ADD_ENTRY) {
            AddEntryScreen(
                categoryRepository = categoryRepository,
                activityRepository = activityRepository,
                onSaved = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.CATEGORIES) {
            CategoriesScreen(
                repository = categoryRepository,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
