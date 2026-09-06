package com.serkodesign.tepera.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.serkodesign.tepera.data.local.SettingsStore
import com.serkodesign.tepera.data.repository.ActivityRepository
import com.serkodesign.tepera.data.repository.BalanceRepository
import com.serkodesign.tepera.data.repository.CategoryRepository
import com.serkodesign.tepera.data.repository.ExcludedAppRepository
import com.serkodesign.tepera.data.repository.InstalledAppsProvider
import com.serkodesign.tepera.ui.addentry.AddEntryScreen
import com.serkodesign.tepera.ui.category.CategoriesScreen
import com.serkodesign.tepera.ui.home.HomeScreen
import com.serkodesign.tepera.ui.onboarding.OnboardingScreen
import com.serkodesign.tepera.ui.settings.ExclusionListScreen
import com.serkodesign.tepera.ui.settings.SettingsScreen

private object Routes {
    const val HOME = "home"
    const val ADD_ENTRY = "add_entry"
    const val ADD_ENTRY_WITH_CATEGORY = "add_entry?categoryId={categoryId}"
    const val CATEGORIES = "categories"
    const val ONBOARDING = "onboarding"
    const val SETTINGS = "settings"
    const val EXCLUSION_LIST = "exclusion_list"

    fun addEntry(categoryId: String? = null) =
        if (categoryId != null) "add_entry?categoryId=$categoryId" else ADD_ENTRY
}

@Composable
fun TeperaNavHost(
    categoryRepository: CategoryRepository,
    activityRepository: ActivityRepository,
    balanceRepository: BalanceRepository,
    excludedAppRepository: ExcludedAppRepository,
    installedAppsProvider: InstalledAppsProvider,
    settingsStore: SettingsStore,
    navController: NavHostController = rememberNavController(),
    // FR-4.1/4.4: тап по кнопці категорії на віджеті або по Quick Settings tile відкриває
    // MainActivity з цим "натяком" — обробляється один раз при вході, не при кожній рекомпозиції.
    pendingOpenAddEntry: Boolean = false,
    pendingCategoryId: String? = null
) {
    LaunchedEffect(Unit) {
        if (pendingOpenAddEntry) {
            navController.navigate(Routes.addEntry(pendingCategoryId))
        }
    }

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                categoryRepository = categoryRepository,
                activityRepository = activityRepository,
                balanceRepository = balanceRepository,
                settingsStore = settingsStore,
                onAddEntry = { navController.navigate(Routes.ADD_ENTRY) },
                onOpenCategories = { navController.navigate(Routes.CATEGORIES) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onShowOnboarding = { navController.navigate(Routes.ONBOARDING) }
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
        composable(
            Routes.ADD_ENTRY_WITH_CATEGORY,
            arguments = listOf(navArgument("categoryId") { type = NavType.StringType })
        ) { backStackEntry ->
            AddEntryScreen(
                categoryRepository = categoryRepository,
                activityRepository = activityRepository,
                onSaved = { navController.popBackStack() },
                onBack = { navController.popBackStack() },
                initialCategoryId = backStackEntry.arguments?.getString("categoryId")
            )
        }
        composable(Routes.CATEGORIES) {
            CategoriesScreen(
                repository = categoryRepository,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                settingsStore = settingsStore,
                onDone = { navController.popBackStack() }
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                settingsStore = settingsStore,
                onOpenExclusionList = { navController.navigate(Routes.EXCLUSION_LIST) },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.EXCLUSION_LIST) {
            ExclusionListScreen(
                installedAppsProvider = installedAppsProvider,
                excludedAppRepository = excludedAppRepository,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
