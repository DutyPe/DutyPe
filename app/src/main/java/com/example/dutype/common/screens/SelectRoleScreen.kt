package com.example.dutype.common.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.Business
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.dutype.app.R
import com.example.dutype.viewmodels.FirestoreJobViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectRoleScreen(
    navController: NavHostController,
    onRoleSelected: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val jobViewModel: FirestoreJobViewModel = hiltViewModel()
    val locationPreferences = jobViewModel.locationPreferences
    val locationService = jobViewModel.locationService

    // Notification permission launcher (Android 13+)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Notification permission result - proceed regardless */ }

    // Location permission launcher - fetch location immediately on grant
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            Timber.d("📍 SelectRoleScreen: Location permission granted, fetching location...")
            locationPreferences.setPermissionGranted(true)
            scope.launch(Dispatchers.IO) {
                try {
                    locationService.getLocationFast(locationPreferences) { locationData ->
                        if (locationData != null) {
                            Timber.d("📍 SelectRoleScreen: Location fetched: ${locationData.getShortAddress()}")
                        }
                    }
                } catch (e: Exception) {
                    Timber.e(e, "📍 SelectRoleScreen: Failed to fetch location")
                }
            }
        }
    }

    // Request permissions on first composition
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .windowInsetsPadding(WindowInsets.statusBars)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))
        
        // App Logo
        Icon(
            painter = painterResource(id = R.drawable.ic_dutype_logo),
            contentDescription = "DutyPe",
            modifier = Modifier.size(64.dp),
            tint = Color.Unspecified
        )
        
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            "Welcome to DutyPe",
            style = com.example.dutype.ui.theme.AppTypography.pageTitle.copy(
                fontSize = 26.sp,
                color = Color(0xFF1F2937)
            ),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            "Choose how you'd like to get started",
            style = com.example.dutype.ui.theme.AppTypography.bodyMedium.copy(
                color = Color(0xFF6B7280)
            ),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Worker Card
        RoleCard(
            icon = Icons.Default.Engineering,
            iconBackground = Color(0xFF10B981),
            title = "I'm looking for work",
            subtitle = "Find jobs near you, apply instantly, and get hired fast",
            features = listOf("Browse thousands of jobs", "Apply with one tap", "Get hired in 24 hours"),
            borderColor = Color(0xFF10B981),
            onClick = { onRoleSelected("WORKER") }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Employer Card
        RoleCard(
            icon = Icons.Default.Business,
            iconBackground = Color(0xFF3B82F6),
            title = "I'm hiring workers",
            subtitle = "Post jobs, review applications, and hire the best talent",
            features = listOf("Post jobs for free", "Get instant applications", "Hire verified workers"),
            borderColor = Color(0xFF3B82F6),
            onClick = { onRoleSelected("EMPLOYER") }
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Bottom text
        Text(
            "You can switch roles anytime from your profile",
            style = com.example.dutype.ui.theme.AppTypography.bodySmall.copy(
                color = Color(0xFF9CA3AF)
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
        
        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
private fun RoleCard(
    icon: ImageVector,
    iconBackground: Color,
    title: String,
    subtitle: String,
    features: List<String>,
    borderColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, borderColor.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Icon circle
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(iconBackground.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconBackground,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = com.example.dutype.ui.theme.AppTypography.cardTitle.copy(
                            color = Color(0xFF1F2937),
                            fontSize = 17.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = com.example.dutype.ui.theme.AppTypography.bodySmall.copy(
                            color = Color(0xFF6B7280),
                            lineHeight = 18.sp
                        )
                    )
                }

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = borderColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Feature bullets
            features.forEach { feature ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(iconBackground)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = feature,
                        style = com.example.dutype.ui.theme.AppTypography.bodySmall.copy(
                            color = Color(0xFF4B5563)
                        )
                    )
                }
            }
        }
    }
}
