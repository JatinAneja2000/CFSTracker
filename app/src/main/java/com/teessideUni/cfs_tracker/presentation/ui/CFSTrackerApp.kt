package com.teessideUni.cfs_tracker.presentation.ui

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.teessideUni.cfs_tracker.data.remote.RespiratoryRateDataSourceImpl
import com.teessideUni.cfs_tracker.data.repository.RespiratoryRateRepositoryImpl
import com.teessideUni.cfs_tracker.domain.CFSTrackerNavigationActions
import com.teessideUni.cfs_tracker.domain.CFSTrackerRoute
import com.teessideUni.cfs_tracker.domain.CFSTrackerTopLevelDestination
import com.teessideUni.cfs_tracker.domain.use_case.RecordRespiratoryRateUseCase
import com.teessideUni.cfs_tracker.presentation.ui.RespiratoryRate.RespiratoryRateScreen
import com.teessideUni.cfs_tracker.presentation.ui.RespiratoryRate.RespiratoryRateViewModel
import com.teessideUni.cfs_tracker.presentation.ui.home.EmptyComingSoon
import com.teessideUni.cfs_tracker.presentation.ui.home.HomeScreen
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CFSTrackerApp(context: Context) {

    CFSTrackerNavigationWrapper(context)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CFSTrackerNavigationWrapper(context: Context) {
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val navController = rememberNavController()
    val navigationActions = remember(navController) {
        CFSTrackerNavigationActions(navController)
    }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val selectedDestination =
        navBackStackEntry?.destination?.route ?: CFSTrackerRoute.HOME

    ModalNavigationDrawer(
        drawerContent = {
            ModalNavigationDrawerContent(
                selectedDestination = selectedDestination,
                navigateToTopLevelDestination = navigationActions::navigateTo,
                onDrawerClicked = {
                    scope.launch {
                        drawerState.close()
                    }
                }
            )
        },
        drawerState = drawerState
    ) {
        CFSTrackerAppContent(
            context,
            navController = navController,
            selectedDestination = selectedDestination,
            navigateToTopLevelDestination = navigationActions::navigateTo,
        ) {
            scope.launch {
                drawerState.open()
            }
        }
    }
}

@Composable
fun CFSTrackerAppContent(
    context: Context,
    modifier: Modifier = Modifier,
    navController: NavHostController,
    selectedDestination: String,
    navigateToTopLevelDestination: (CFSTrackerTopLevelDestination) -> Unit,
    onDrawerClicked: () -> Unit = {}
) {
    Row(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.inverseOnSurface)
        ) {
            CFSTrackerNavHost(
                navController = navController,
                modifier = Modifier.weight(1f),
                context
            )
            AnimatedVisibility(visible = true) {
                CFSTrackerBottomNavigationBar(
                    selectedDestination = selectedDestination,
                    navigateToTopLevelDestination = navigateToTopLevelDestination
                )
            }
        }
    }
}

@Composable
private fun CFSTrackerNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    context: Context
) {
    NavHost(
        modifier = modifier,
        navController = navController,
        startDestination = CFSTrackerRoute.HOME,
    ) {
        composable(CFSTrackerRoute.HOME) {
            HomeScreen(navController)
        }
        composable(CFSTrackerRoute.REPORTS) {
            EmptyComingSoon()
        }
        composable(CFSTrackerRoute.SETTINGS) {
            EmptyComingSoon()
        }
        composable(CFSTrackerRoute.RESPIRATORY_RATE_RECORDER){
            val dataSrc = RespiratoryRateDataSourceImpl()
            val repo = RespiratoryRateRepositoryImpl(context, dataSrc)
            val useCase = RecordRespiratoryRateUseCase(repo)
            val viewModel = RespiratoryRateViewModel(useCase)
            RespiratoryRateScreen(viewModel)
        }
    }
}
