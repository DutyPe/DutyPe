package com.example.dutype.worker.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.dutype.models.UserRole
import com.example.dutype.navigation.Routes
import com.example.dutype.viewmodels.ProfileCompletionViewModel
import com.example.dutype.services.ProfileCompletionService
import kotlinx.coroutines.launch

/**
 * Mandatory Worker Profile Setup Screen
 * Enhanced with 30+ years of Android development experience
 * Pre-fills Google Sign-In email and makes profile setup mandatory
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MandatoryWorkerProfileSetupScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val profileCompletionViewModel: ProfileCompletionViewModel = hiltViewModel()
    // Form state
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var dateOfBirth by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var skills by remember { mutableStateOf("") }
    var experience by remember { mutableStateOf("") }
    
    // UI state
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var currentStep by remember { mutableStateOf(1) }
    var profileCompletionPercentage by remember { mutableStateOf(0) }
    var isProfileCompleted by remember { mutableStateOf(false) }
    var isUploadingImage by remember { mutableStateOf(false) }
    var isEmailLoaded by remember { mutableStateOf(false) }
    val totalSteps = 3
    
    // Load saved user info from Google Sign-In
    LaunchedEffect(Unit) {
        val savedEmail = profileCompletionViewModel.getUserEmail()
        val savedName = profileCompletionViewModel.getUserName()
        
        println("🔍 MandatoryWorkerProfileSetupScreen - Loading user info:")
        println("  savedEmail: $savedEmail")
        println("  savedName: $savedName")
        
        if (savedEmail != null) {
            email = savedEmail
            isEmailLoaded = true
        }
        if (savedName != null) {
            fullName = savedName
        }
        
        // If no saved email, we still mark as loaded to allow validation
        if (savedEmail == null) {
            isEmailLoaded = true
        }
        
        println("  After loading - email: $email, fullName: $fullName, isEmailLoaded: $isEmailLoaded")
    }
    
    // Email is locked and cannot be changed
    val isEmailLocked = email.isNotBlank()
    
    // Enhanced gradient background with better color transition
    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0F172A), // Deep navy
            Color(0xFF1E293B), // Slate
            Color(0xFF3B82F6), // Bright blue
            Color(0xFFE0F2FE), // Light blue
            Color(0xFFF8FAFC), // Very light
            Color.White
        ),
        startY = 0f,
        endY = 1400f
    )
    
    // Animation state for smooth transitions
    val animatedProgress by animateFloatAsState(
        targetValue = currentStep.toFloat() / totalSteps.toFloat(),
        animationSpec = tween(600, easing = EaseInOutCubic),
        label = "progress"
    )
    
    // Step-specific validation - email field is auto-populated from Google Sign-In
    val isStep1Valid = fullName.isNotBlank() && email.isNotBlank() && phoneNumber.isNotBlank() && address.isNotBlank()
    val isStep2Valid = dateOfBirth.isNotBlank() && gender.isNotBlank()
    val isStep3Valid = skills.isNotBlank() && experience.isNotBlank()
    
    // Overall form validation
    val isFormValid = isStep1Valid && isStep2Valid && isStep3Valid
    
    
    // Current step validation
    val isCurrentStepValid = when (currentStep) {
        1 -> isStep1Valid
        2 -> isStep2Valid
        3 -> isStep3Valid
        else -> false
    }
    
    // Debug logging for form validation
    LaunchedEffect(fullName, email, phoneNumber, address, dateOfBirth, gender, currentStep, isCurrentStepValid) {
        println("🔍 MandatoryWorkerProfileSetupScreen - Form validation:")
        println("  currentStep: $currentStep")
        println("  fullName: '$fullName' (${fullName.isNotBlank()})")
        println("  email: '$email' (${email.isNotBlank()})")
        println("  phoneNumber: '$phoneNumber' (${phoneNumber.isNotBlank()})")
        println("  address: '$address' (${address.isNotBlank()})")
        println("  dateOfBirth: '$dateOfBirth' (${dateOfBirth.isNotBlank()})")
        println("  gender: '$gender' (${gender.isNotBlank()})")
        println("  isStep1Valid: $isStep1Valid")
        println("  isStep2Valid: $isStep2Valid")
        println("  isStep3Valid: $isStep3Valid")
        println("  isCurrentStepValid: $isCurrentStepValid")
        println("  isFormValid: $isFormValid")
        println("  isLoading: $isLoading")
        println("  Button should be enabled: ${isCurrentStepValid && !isLoading}")
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundGradient)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Professional Header
            ProfessionalHeader(
                navController = navController,
                currentStep = currentStep,
                totalSteps = totalSteps,
                isFormValid = isFormValid,
                animatedProgress = animatedProgress
            )
            
            
            // Enhanced Main Content Card with better styling
            AnimatedVisibility(
                visible = true,
                enter = slideInVertically(
                    animationSpec = tween(600, easing = EaseOutCubic),
                    initialOffsetY = { it / 2 }
                ) + fadeIn(animationSpec = tween(600))
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .shadow(
                            elevation = 16.dp,
                            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                            ambientColor = Color.Black.copy(alpha = 0.1f),
                            spotColor = Color.Black.copy(alpha = 0.05f)
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        // Step 1: Personal Information
                        if (currentStep == 1) {
                            AnimatedVisibility(
                                visible = true,
                                enter = slideInVertically() + fadeIn(),
                                exit = slideOutVertically() + fadeOut()
                            ) {
                                PersonalInformationStep(
                                    fullName = fullName,
                                    email = email,
                                    phoneNumber = phoneNumber,
                                    address = address,
                                    onFullNameChange = { fullName = it },
                                    onEmailChange = { /* Email is read-only from Google Sign-In */ },
                                    onPhoneChange = { phoneNumber = it },
                                    onAddressChange = { address = it }
                                )
                            }
                        }
                        
                        // Step 2: Additional Details
                        if (currentStep == 2) {
                            AnimatedVisibility(
                                visible = true,
                                enter = slideInVertically() + fadeIn(),
                                exit = slideOutVertically() + fadeOut()
                            ) {
                                AdditionalDetailsStep(
                                    dateOfBirth = dateOfBirth,
                                    gender = gender,
                                    onDateOfBirthChange = { dateOfBirth = it },
                                    onGenderChange = { gender = it }
                                )
                            }
                        }
                        
                        // Step 3: Professional Information
                        if (currentStep == 3) {
                            AnimatedVisibility(
                                visible = true,
                                enter = slideInVertically() + fadeIn(),
                                exit = slideOutVertically() + fadeOut()
                            ) {
                                ProfessionalInformationStep(
                                    skills = skills,
                                    experience = experience,
                                    onSkillsChange = { skills = it },
                                    onExperienceChange = { experience = it }
                                )
                            }
                        }
                    
                        // Error Message
                        if (errorMessage != null) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE2E2)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = errorMessage ?: "",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = Color(0xFFEF4444)
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // Enhanced Navigation Buttons
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                color = Color.White,
                shadowElevation = 12.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Previous Button
                    if (currentStep > 1) {
                        OutlinedButton(
                            onClick = { currentStep-- },
                            modifier = Modifier
                                .height(56.dp)
                                .weight(1f),
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFF3B82F6)
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.5.dp, 
                                Color(0xFF3B82F6).copy(alpha = 0.3f)
                            )
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Previous")
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    
                    // Next/Complete Button
                    Button(
                        onClick = {
                            if (currentStep < totalSteps) {
                                currentStep++
                            } else {
                                // Complete profile setup
                                scope.launch {
                                    isLoading = true
                                    errorMessage = null
                                    
                                    try {
                                        // Save profile data to Firestore
                                        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                                        if (currentUser != null) {
                                            val workerProfileData = mapOf(
                                                "fullName" to fullName,
                                                "email" to email,
                                                "phone" to phoneNumber,  // Changed from phoneNumber to phone to match Firebase
                                                "address" to address,
                                                "dateOfBirth" to dateOfBirth,
                                                "gender" to gender,
                                                "skills" to skills,
                                                "experience" to experience,
                                                "profileCompleted" to true,
                                                "completedAt" to System.currentTimeMillis()
                                            )
                                            
                                            // Save to Firestore using ProfileCompletionViewModel
                                            profileCompletionViewModel.saveWorkerProfileData(workerProfileData)
                                        }

                                        // Mark profile as complete
                                        profileCompletionViewModel.markProfileComplete(UserRole.WORKER)

                                        // Mark profile setup as shown for worker
                                        profileCompletionViewModel.markProfileSetupAsShown(UserRole.WORKER)
                                        
                                        // Navigate to worker home
                                        navController.navigate(Routes.WORKER_HOME) {
                                            popUpTo(Routes.PROFILE_SETUP) { inclusive = true }
                                        }
                                    } catch (e: Exception) {
                                        errorMessage = e.message ?: "Failed to complete profile setup"
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            }
                        },
                        enabled = isCurrentStepValid && !isLoading,
                        modifier = Modifier
                            .height(56.dp)
                            .weight(1f),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isCurrentStepValid) Color(0xFF3B82F6) else Color(0xFF9CA3AF)
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                if (currentStep == totalSteps) "Complete Profile" else "Next",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                        if (currentStep < totalSteps) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                Icons.Default.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfessionalHeader(
    navController: NavController,
    currentStep: Int,
    totalSteps: Int,
    isFormValid: Boolean,
    animatedProgress: Float = currentStep.toFloat() / totalSteps.toFloat()
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // Title section
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Complete Your Profile",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                    ),
                    textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Step $currentStep of $totalSteps",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.White.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Medium
                    ),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Enhanced animated progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .background(
                        Color.White.copy(alpha = 0.2f), 
                        RoundedCornerShape(4.dp)
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animatedProgress)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF06B6D4), // Cyan
                                    Color(0xFF3B82F6), // Blue
                                    Color(0xFF8B5CF6)  // Purple
                                )
                            ),
                            RoundedCornerShape(4.dp)
                        )
                )
            }
        }
    }
}

@Composable
private fun PersonalInformationStep(
    fullName: String,
    email: String,
    phoneNumber: String,
    address: String,
    onFullNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onAddressChange: (String) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Step header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        Color(0xFF3B82F6).copy(alpha = 0.1f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = Color(0xFF3B82F6),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = "Personal Information",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1A1A)
                    )
                )
                Text(
                    text = "Tell us about yourself",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFF666666)
                    )
                )
            }
        }

        // Full Name
        OutlinedTextField(
            value = fullName,
            onValueChange = onFullNameChange,
            label = { Text("Full Name *") },
            placeholder = { Text("Enter your full name") },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF3B82F6),
                unfocusedBorderColor = Color(0xFFE5E7EB)
            )
        )

        // Email (read-only from Google Sign-In)
        OutlinedTextField(
            value = email,
            onValueChange = { }, // Empty lambda since field is disabled
            label = { Text("Email Address *") },
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            enabled = false, // Read-only from Google Sign-In
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                disabledBorderColor = Color(0xFFE5E7EB),
                disabledTextColor = Color(0xFF6B7280)
            ),
            trailingIcon = {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Verified by Google",
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(20.dp)
                )
            }
        )

        // Phone Number
        OutlinedTextField(
            value = phoneNumber,
            onValueChange = onPhoneChange,
            label = { Text("Phone Number *") },
            placeholder = { Text("+91 98765 43210") },
            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF3B82F6),
                unfocusedBorderColor = Color(0xFFE5E7EB)
            )
        )

        // Address
        OutlinedTextField(
            value = address,
            onValueChange = onAddressChange,
            label = { Text("Address *") },
            placeholder = { Text("Enter your current address") },
            leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 3,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF3B82F6),
                unfocusedBorderColor = Color(0xFFE5E7EB)
            )
        )
    }
}

@Composable
private fun AdditionalDetailsStep(
    dateOfBirth: String,
    gender: String,
    onDateOfBirthChange: (String) -> Unit,
    onGenderChange: (String) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Step header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        Color(0xFF3B82F6).copy(alpha = 0.1f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.DateRange,
                    contentDescription = null,
                    tint = Color(0xFF3B82F6),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = "Additional Details",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1A1A)
                    )
                )
                Text(
                    text = "Complete your profile information",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFF666666)
                    )
                )
            }
        }

        // Date of Birth
        OutlinedTextField(
            value = dateOfBirth,
            onValueChange = onDateOfBirthChange,
            label = { Text("Date of Birth *") },
            placeholder = { Text("DD/MM/YYYY") },
            leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF3B82F6),
                unfocusedBorderColor = Color(0xFFE5E7EB)
            )
        )

        // Gender Selection
        GenderSelectionField(
            selectedGender = gender,
            onGenderSelected = onGenderChange
        )
    }
}

@Composable
private fun ProfessionalInformationStep(
    skills: String,
    experience: String,
    onSkillsChange: (String) -> Unit,
    onExperienceChange: (String) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Step header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        Color(0xFF3B82F6).copy(alpha = 0.1f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Work,
                    contentDescription = null,
                    tint = Color(0xFF3B82F6),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = "Professional Information",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1A1A)
                    )
                )
                Text(
                    text = "Share your skills and experience",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFF666666)
                    )
                )
            }
        }

        // Skills
        OutlinedTextField(
            value = skills,
            onValueChange = onSkillsChange,
            label = { Text("Skills *") },
            placeholder = { Text("e.g., Cooking, Customer Service, Cleaning, Driving") },
            leadingIcon = { Icon(Icons.Default.Psychology, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF3B82F6),
                unfocusedBorderColor = Color(0xFFE5E7EB)
            )
        )

        // Experience
        OutlinedTextField(
            value = experience,
            onValueChange = onExperienceChange,
            label = { Text("Experience Level *") },
            placeholder = { Text("e.g., Fresher, 1-2 years, 3-5 years, 5+ years") },
            leadingIcon = { Icon(Icons.Default.TrendingUp, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF3B82F6),
                unfocusedBorderColor = Color(0xFFE5E7EB)
            )
        )
    }
}

@Composable
private fun GenderSelectionField(
    selectedGender: String,
    onGenderSelected: (String) -> Unit
) {
    val genderOptions = listOf("Male", "Female", "Other")

    Column {
        Text(
            text = "Gender *",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium,
                color = Color(0xFF374151)
            ),
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            genderOptions.forEach { gender ->
                Card(
                    onClick = { onGenderSelected(gender) },
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedGender == gender)
                            Color(0xFF3B82F6).copy(alpha = 0.1f)
                        else
                            Color(0xFFF8FAFC)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        if (selectedGender == gender) 2.dp else 1.dp,
                        if (selectedGender == gender) Color(0xFF3B82F6) else Color(0xFFE5E7EB)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = gender,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (selectedGender == gender) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedGender == gender) Color(0xFF3B82F6) else Color(0xFF6B7280)
                            )
                        )
                    }
                }
            }
        }
    }
}
