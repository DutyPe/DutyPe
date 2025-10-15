package com.example.dutype.employer.screens.about

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.dutype.ui.components.ReusableAboutUs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployerAboutScreen(
    navController: NavController,
    onStatusBarColorChange: (Color) -> Unit
) {
    onStatusBarColorChange(Color(0xFF2193b0))
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "About Us",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            fontSize = 18.sp
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBackIosNew,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        ReusableAboutUs(
            modifier = Modifier.padding(top = innerPadding.calculateTopPadding()),
            showHeader = false,
            backgroundColor = Color(0xFF2193b0),
            gradientColors = listOf(
                Color(0xFF2193b0),
                Color(0xFF6dd5ed),
                Color(0xFFFFFFFF)
            ),
            cardBackgroundColor = Color.White,
            primaryTextColor = Color(0xFF374151),
            secondaryTextColor = Color(0xFF6B7280),
            accentColor = Color(0xFF3B82F6)
        )
    }
}
