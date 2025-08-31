package com.example.partimes.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Text
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.partimes.auth.LoginBottomSheet
import com.example.partimes.navigation.employer.EmployerMainScreen
import com.example.partimes.navigation.jobSeekerNavGraph.JobSeekerMainScreen
import com.example.partimes.common.chat.SelectRoleScreen
import com.example.partimes.common.chat.SplashScreen
import com.example.partimes.location.LocationServiceScreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun MainNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = "login_bottom_sheet"
    ) {
        composable("splash") {
            SplashScreen(navController)
        }

        composable("login_bottom_sheet") {
            LoginBottomSheetScreen(navController = navController)
        }

        composable(Routes.LOCATION_SERVICE_SCREEN_ROUTE) {
            LocationServiceScreen(navController = navController)
        }

        composable("select_role") {
            SelectRoleScreen(navController)
        }

        // 5. Jobseeker Home
        composable("jobseeker_home") {
            JobSeekerMainScreen() 
        }

        // 6. Employer Home
        composable("employer_home") {
            EmployerMainScreen()
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun LoginBottomSheetScreen(navController: NavHostController) {
    val bottomSheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Expanded,
        skipHalfExpanded = true
    )
    val scope = rememberCoroutineScope()

    // Handle back press and sheet state changes
    LaunchedEffect(bottomSheetState.targetValue) {
        if (bottomSheetState.targetValue == ModalBottomSheetValue.Hidden) {
            navController.navigate(Routes.LOCATION_SERVICE_SCREEN_ROUTE) {
                popUpTo("login_bottom_sheet") { inclusive = true }
            }
        }
    }

    ModalBottomSheetLayout(
        sheetState = bottomSheetState,
        sheetContent = {
            LoginBottomSheet(
                onLoginSuccess = {
                    scope.launch {
                        bottomSheetState.hide()
                    }
                    navController.navigate(Routes.LOCATION_SERVICE_SCREEN_ROUTE) {
                        popUpTo("login_bottom_sheet") { inclusive = true }
                    }
                },
                onDismiss = {
                    scope.launch {
                        bottomSheetState.hide()
                    }
                }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) {
        // Background content with Skip button
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
        ) {
            // Skip button in top-right corner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        navController.navigate(Routes.LOCATION_SERVICE_SCREEN_ROUTE) {
                            popUpTo("login_bottom_sheet") { inclusive = true }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color.White.copy(alpha = 0.9f),
                        contentColor = Color.Black
                    ),
                    elevation = ButtonDefaults.elevation(4.dp)
                ) {
                    Text(
                        "Skip",
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp
                    )
                }
            }

            // Optional: Add some branding or background content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 100.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                Text(
                    text = "ParTimes",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Your Gateway to Flexible Employment",
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }
    }
}
