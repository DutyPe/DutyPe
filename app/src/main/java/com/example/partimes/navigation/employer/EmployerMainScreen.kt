package com.example.partimes.navigation.employer

// import com.example.partimes.employer.screens.EmployerScreen
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.zIndex
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.partimes.common.chat.help.CallSupportScreen
import com.example.partimes.common.chat.help.ChatSupportScreen
import com.example.partimes.common.chat.help.ReportProblemScreen
import com.example.partimes.common.chat.help.TutorialScreen
import com.example.partimes.common.chat.info.FaqScreen
import com.example.partimes.common.employer.EmployerProfileScreen
import com.example.partimes.components.ScrollAwareBottomBar
import com.example.partimes.employer.screens.ViewApplicantsScreen
import com.example.partimes.employer.screens.about.EmployerAboutScreen
import com.example.partimes.employer.screens.homeScreen.EmployerHomeScreen
import com.example.partimes.employer.screens.postedJobs.PostedJobsScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.partimes.employer.viewmodels.EmployerViewModel
import com.example.partimes.employer.screens.referral.EmployerReferEarnScreen
import com.example.partimes.employer.screens.postjob.PostJobScreen
import com.example.partimes.employer.screens.reviews.EmployerReviewsScreen
import com.example.partimes.employer.screens.settings.EmployerAddressManagementScreen
import com.example.partimes.employer.screens.settings.EmployerNotificationsScreen
import com.example.partimes.employer.screens.support.EmployerSupportScreen
import com.example.partimes.navigation.Routes
import com.example.partimes.utils.rememberScrollStateManager
import com.example.partimes.utils.rememberWindowSizeClass

@Composable
fun EmployerMainScreen() {
    val navController = rememberNavController()
    val scrollStateManager = rememberScrollStateManager()

    // Create a unified state for the current screen to manage status bar color
    var currentStatusBarColor by remember { mutableStateOf(Color(0xFF1A237E)) }
    val isBottomBarVisible by scrollStateManager.isBottomBarVisible

    // Main container that handles all system bars
    Box(modifier = Modifier.fillMaxSize()) {

        // Status bar overlay - ALWAYS at the top
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsTopHeight(WindowInsets.statusBars)
                .background(currentStatusBarColor)
                .align(Alignment.TopCenter)
                .zIndex(1000f) // Ensure it's always on top
        )

        // Navigation bar overlay - Only show when bottom bar is visible
        if (isBottomBarVisible) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsBottomHeight(WindowInsets.navigationBars)
                    .background(Color.Black)
                    .align(Alignment.BottomCenter)
                    .zIndex(1000f) // Ensure it's always on top
            )
        }

        // Main content area
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                ScrollAwareBottomBar(
                    isVisible = isBottomBarVisible,
                    windowSizeClass = rememberWindowSizeClass()
                ) {
                    EmployerBottomBar(navController)
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),
                        bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                    )
                    .padding(
                        top = paddingValues.calculateTopPadding(),
                        start = paddingValues.calculateStartPadding(LocalLayoutDirection.current),
                        end = paddingValues.calculateEndPadding(LocalLayoutDirection.current)
                        // Removed bottom padding to prevent white space behind bottom bar
                    )
            ) {
                NavHost(
                    navController = navController,
                    startDestination = Routes.EMPLOYER_DASHBOARD
                ) {
                    composable(Routes.EMPLOYER_DASHBOARD) {
                        EmployerHomeScreen(
                            navController = navController,
                            onStatusBarColorChange = { color ->
                                currentStatusBarColor = color
                            },
                            scrollStateManager = scrollStateManager
                        )
                    }
                    composable(Routes.EMPLOYER_POST_JOB) {
                        PostJobScreen(
                            navController = navController,
                            onJobPosted = {
                                // Navigate back to dashboard after job is posted
                                navController.navigate(Routes.EMPLOYER_DASHBOARD) {
                                    popUpTo(Routes.EMPLOYER_POST_JOB) { inclusive = true }
                                }
                            }
                        )
                    }
                    composable(Routes.EMPLOYER_PROFILE) {
                        EmployerProfileScreen(
                            rootNavController = navController
                        )
                    }
                    composable(Routes.EMPLOYER_MY_JOBS) {
                        PostedJobsScreen(
                            navController = navController
                        )
                    }
                    composable(
                        Routes.VIEW_APPLICANTS,
                        arguments = listOf(navArgument("jobId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val jobId = backStackEntry.arguments?.getString("jobId") ?: ""
                        ViewApplicantsScreen(navController, jobId)
                    }
                    composable(Routes.EMPLOYER_ABOUT) {
                        EmployerAboutScreen(
                            navController = navController,
                            onStatusBarColorChange = { color ->
                                currentStatusBarColor = color
                            }
                        )
                    }
                    composable(Routes.EMPLOYER_HELP) {
                        EmployerSupportScreen(
                            navController = navController,
                            onStatusBarColorChange = { color ->
                                currentStatusBarColor = color
                            }
                        )
                    }
                    composable(Routes.EMPLOYER_FAQ) {
                        FaqScreen(
                            navController = navController,
                            onStatusBarColorChange = { color ->
                                currentStatusBarColor = color
                            }
                        )
                    }
                    composable(Routes.EMPLOYER_CHAT_SUPPORT) {
                        ChatSupportScreen(
                            navController = navController,
                            onStatusBarColorChange = { color ->
                                currentStatusBarColor = color
                            }
                        )
                    }
                    composable(Routes.EMPLOYER_CALL_SUPPORT) {
                        CallSupportScreen(
                            navController = navController,
                            onStatusBarColorChange = { color ->
                                currentStatusBarColor = color
                            }
                        )
                    }
                    composable(Routes.EMPLOYER_REPORT) {
                        ReportProblemScreen(
                            navController = navController,
                            onStatusBarColorChange = { color ->
                                currentStatusBarColor = color
                            }
                        )
                    }
                    composable(Routes.EMPLOYER_TUTORIAL) {
                        TutorialScreen(
                            navController = navController,
                            onStatusBarColorChange = { color ->
                                currentStatusBarColor = color
                            }
                        )
                    }
                    composable(Routes.EMPLOYER_NOTIFICATIONS) {
                        EmployerNotificationsScreen(
                            navController = navController,
                            onStatusBarColorChange = { color ->
                                currentStatusBarColor = color
                            }
                        )
                    }
                    composable(Routes.EMPLOYER_MANAGE_ADDRESSES) {
                        EmployerAddressManagementScreen(
                            navController = navController,
                            onStatusBarColorChange = { color ->
                                currentStatusBarColor = color
                            }
                        )
                    }
                    composable(Routes.EMPLOYER_REVIEWS) {
                        EmployerReviewsScreen(
                            navController = navController,
                            onStatusBarColorChange = { color ->
                                currentStatusBarColor = color
                            }
                        )
                    }
                    composable(Routes.EMPLOYER_REFER_EARN) {
                        EmployerReferEarnScreen(
                            navController = navController,
                            onStatusBarColorChange = { color ->
                                currentStatusBarColor = color
                            }
                        )
                    }
                    composable(Routes.EMPLOYER_MY_JOBS) {
                        PostedJobsScreen(
                            navController = navController,
                            viewModel = viewModel()
                        )
                    }
                }
            }
        }
    }
}

