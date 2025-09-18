package com.example.partimes.profile.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.partimes.profile.viewmodels.AdvancedProfileViewModel
import com.example.partimes.ui.components.*

/**
 * Advanced Profile Management Screen
 * Comprehensive profile management with skills, experience, and verification
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedProfileScreen(
    navController: NavController,
    onStatusBarColorChange: (Color) -> Unit = {},
    viewModel: AdvancedProfileViewModel = hiltViewModel()
) {
    val profile by viewModel.profile.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    
    // Set status bar color
    LaunchedEffect(Unit) {
        onStatusBarColorChange(Color.White)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top App Bar
            TopAppBar(
                title = {
                    Text(
                        text = "Advanced Profile",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF90D5FF)
                )
            )

            // Content
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFF90D5FF)
                    )
                }
            } else if (error != null) {
                ErrorScreen(
                    errorState = ErrorState(
                        type = ErrorType.UNKNOWN_ERROR,
                        title = "Error",
                        message = error!!,
                        icon = Icons.Default.Error,
                        canRetry = true,
                        retryAction = { viewModel.loadProfile() }
                    ),
                    onRetry = { viewModel.loadProfile() }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Profile Completion Card
                    item {
                        ProfileCompletionCard(
                            completionPercentage = profile.profileCompletionPercentage,
                            onCompleteProfile = { /* Navigate to profile completion */ }
                        )
                    }

                    // Quick Actions
                    item {
                        QuickActionsSection(
                            onSkillsClick = { navController.navigate("skills_management") },
                            onResumeClick = { navController.navigate("resume_upload") },
                            onVerificationClick = { navController.navigate("verification") }
                        )
                    }

                    // Profile Sections
                    item {
                        ProfileSection(
                            title = "Personal Information",
                            icon = Icons.Outlined.Person,
                            completionStatus = getPersonalInfoCompletion(profile),
                            onClick = { navController.navigate("personal_info") }
                        )
                    }

                    item {
                        ProfileSection(
                            title = "Skills & Expertise",
                            icon = Icons.Outlined.Psychology,
                            completionStatus = getSkillsCompletion(profile),
                            onClick = { navController.navigate("skills_management") }
                        )
                    }

                    item {
                        ProfileSection(
                            title = "Work Experience",
                            icon = Icons.Outlined.Work,
                            completionStatus = getExperienceCompletion(profile),
                            onClick = { navController.navigate("work_experience") }
                        )
                    }

                    item {
                        ProfileSection(
                            title = "Education",
                            icon = Icons.Outlined.School,
                            completionStatus = getEducationCompletion(profile),
                            onClick = { navController.navigate("education") }
                        )
                    }

                    item {
                        ProfileSection(
                            title = "Verification",
                            icon = Icons.Outlined.Verified,
                            completionStatus = getVerificationCompletion(profile),
                            onClick = { navController.navigate("verification") }
                        )
                    }

                    item {
                        ProfileSection(
                            title = "Work Preferences",
                            icon = Icons.Outlined.Settings,
                            completionStatus = getPreferencesCompletion(profile),
                            onClick = { navController.navigate("work_preferences") }
                        )
                    }

                    // Spacer for bottom padding
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileCompletionCard(
    completionPercentage: Int,
    onCompleteProfile: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Profile Completion",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2C3E50)
                    )
                    Text(
                        text = "$completionPercentage% Complete",
                        fontSize = 14.sp,
                        color = Color(0xFF7F8C8D)
                    )
                }
                
                if (completionPercentage < 100) {
                    Button(
                        onClick = onCompleteProfile,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF90D5FF)
                        )
                    ) {
                        Text("Complete Profile", color = Color.White)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            LinearProgressIndicator(
                progress = completionPercentage / 100f,
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF90D5FF),
                trackColor = Color(0xFFE8F4FD)
            )
        }
    }
}

@Composable
fun QuickActionsSection(
    onSkillsClick: () -> Unit,
    onResumeClick: () -> Unit,
    onVerificationClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Quick Actions",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2C3E50)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                QuickActionButton(
                    icon = Icons.Outlined.Psychology,
                    label = "Skills",
                    onClick = onSkillsClick
                )
                
                QuickActionButton(
                    icon = Icons.Outlined.Upload,
                    label = "Resume",
                    onClick = onResumeClick
                )
                
                QuickActionButton(
                    icon = Icons.Outlined.Verified,
                    label = "Verify",
                    onClick = onVerificationClick
                )
            }
        }
    }
}

@Composable
fun QuickActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFFE8F4FD))
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = Color(0xFF90D5FF),
                modifier = Modifier.size(24.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color(0xFF2C3E50),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun ProfileSection(
    title: String,
    icon: ImageVector,
    completionStatus: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFE8F4FD)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = title,
                    tint = Color(0xFF90D5FF),
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF2C3E50)
                )
                Text(
                    text = completionStatus,
                    fontSize = 14.sp,
                    color = Color(0xFF7F8C8D)
                )
            }
            
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "Navigate",
                tint = Color(0xFF7F8C8D)
            )
        }
    }
}

// Helper functions for completion status
private fun getPersonalInfoCompletion(profile: com.example.partimes.profile.models.AdvancedProfile): String {
    val personalInfo = profile.personalInfo
    val completed = listOf(
        personalInfo.fullName.isNotEmpty(),
        personalInfo.email.isNotEmpty(),
        personalInfo.phone.isNotEmpty(),
        personalInfo.dateOfBirth.isNotEmpty()
    ).count { it }
    return "$completed/4 completed"
}

private fun getSkillsCompletion(profile: com.example.partimes.profile.models.AdvancedProfile): String {
    return "${profile.skills.size} skills added"
}

private fun getExperienceCompletion(profile: com.example.partimes.profile.models.AdvancedProfile): String {
    return "${profile.workExperience.size} experiences added"
}

private fun getEducationCompletion(profile: com.example.partimes.profile.models.AdvancedProfile): String {
    return "${profile.education.size} education entries"
}

private fun getVerificationCompletion(profile: com.example.partimes.profile.models.AdvancedProfile): String {
    val verified = profile.verifications.count { it.status == com.example.partimes.profile.models.VerificationStatus.VERIFIED }
    return "$verified/${profile.verifications.size} verified"
}

private fun getPreferencesCompletion(profile: com.example.partimes.profile.models.AdvancedProfile): String {
    val preferences = profile.workPreferences
    val completed = listOf(
        preferences.preferredWorkType != com.example.partimes.profile.models.WorkPreference.ON_SITE,
        preferences.preferredEmploymentType != com.example.partimes.profile.models.EmploymentType.FULL_TIME,
        preferences.preferredSalary != null
    ).count { it }
    return "$completed/3 preferences set"
}
