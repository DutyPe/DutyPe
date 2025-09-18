package com.example.partimes.employer.screens.about

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavController
import com.example.partimes.common.chat.info.AboutUsScreen

@Composable
fun EmployerAboutScreen(
    navController: NavController,
    onStatusBarColorChange: (Color) -> Unit
) {
    AboutUsScreen(
        navController = navController,
        onStatusBarColorChange = onStatusBarColorChange
    )
}
