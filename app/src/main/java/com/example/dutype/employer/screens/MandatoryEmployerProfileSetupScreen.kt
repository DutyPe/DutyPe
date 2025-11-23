package com.example.dutype.employer.screens

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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
 * Mandatory Employer Profile Setup Screen
 * Enhanced with 30+ years of Android development experience
 * Pre-fills Google Sign-In email and makes profile setup mandatory
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MandatoryEmployerProfileSetupScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val profileCompletionViewModel: ProfileCompletionViewModel = hiltViewModel()
    
    // Form state
    var companyName by remember { mutableStateOf("") }
    var contactEmail by remember { mutableStateOf("") }
    var contactPhone by remember { mutableStateOf("") }
    var businessAddress by remember { mutableStateOf("") }
    var industry by remember { mutableStateOf("") }
    var companySize by remember { mutableStateOf("") }
    var website by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var profileImageUrl by remember { mutableStateOf<String?>(null) }
    
    // UI state
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var currentStep by remember { mutableStateOf(1) }
    var profileCompletionPercentage by remember { mutableStateOf(0) }
    var isProfileCompleted by remember { mutableStateOf(false) }
    var isUploadingImage by remember { mutableStateOf(false) }
    val totalSteps = 3
    
    // Load saved user info from Google Sign-In
    LaunchedEffect(Unit) {
        val savedEmail = profileCompletionViewModel.getUserEmail()
        val savedName = profileCompletionViewModel.getUserName()
        
        if (savedEmail != null) {
            contactEmail = savedEmail
        }
        if (savedName != null) {
            // For employer, use the name as company name
            companyName = savedName
        }
    }
    
    // Enhanced gradient background with vibrant colors
    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF7C3AED), // Deep vibrant purple
            Color(0xFF8B5CF6), // Vibrant purple
            Color(0xFF9F7AEA), // Bright purple
            Color(0xFFD8B4FE), // Light purple
            Color(0xFFF5F3FF), // Purple tint
            Color.White
        ),
        startY = 0f,
        endY = 1600f
    )
    
    // Animation state for smooth transitions
    val animatedProgress by animateFloatAsState(
        targetValue = currentStep.toFloat() / totalSteps.toFloat(),
        animationSpec = tween(600, easing = EaseInOutCubic),
        label = "progress"
    )
    
    // Step-specific validation
    // ✅ FIXED: Email is OPTIONAL (read-only from Google Sign-In), Phone is MANDATORY
    
    // Validation helper functions
    fun isValidPhoneNumber(phone: String): Boolean {
        // Indian phone number: 10 digits after country code
        val phoneRegex = Regex("^[6-9]\\d{9}$")
        return phone.length == 10 && phoneRegex.matches(phone)
    }
    
    fun isValidEmail(email: String): Boolean {
        // Standard email validation
        val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
        return emailRegex.matches(email)
    }
    
    val isStep1Valid = companyName.isNotBlank() && industry.isNotBlank()
    val isStep2Valid = isValidPhoneNumber(contactPhone) && businessAddress.isNotBlank() && 
                       (contactEmail.isBlank() || isValidEmail(contactEmail))
    val isStep3Valid = true // Review step is always valid
    
    // Overall form validation
    val isFormValid = isStep1Valid && isStep2Valid && isStep3Valid
    
    // Validation error messages
    var phoneError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    
    // Current step validation
    val isCurrentStepValid = when (currentStep) {
        1 -> isStep1Valid
        2 -> isStep2Valid
        3 -> isStep3Valid
        else -> false
    }
    
    // Debug logging for form validation
    LaunchedEffect(companyName, contactEmail, industry, contactPhone, businessAddress, currentStep, isCurrentStepValid) {
        // Update phone error
        phoneError = when {
            contactPhone.isBlank() -> "Phone number is required"
            !isValidPhoneNumber(contactPhone) && contactPhone.isNotBlank() -> "Enter a valid 10-digit phone number"
            else -> null
        }
        
        // Update email error
        emailError = when {
            contactEmail.isNotBlank() && !isValidEmail(contactEmail) -> "Enter a valid email address"
            else -> null
        }
        
        println("🔍 MandatoryEmployerProfileSetupScreen - Form validation:")
        println("  currentStep: $currentStep")
        println("  companyName: '$companyName' (${companyName.isNotBlank()})")
        println("  contactEmail: '$contactEmail' (${if (contactEmail.isBlank()) "OPTIONAL" else "provided"}) - Valid: ${contactEmail.isBlank() || isValidEmail(contactEmail)}")
        println("  industry: '$industry' (${industry.isNotBlank()})")
        println("  contactPhone: '$contactPhone' - Valid: ${isValidPhoneNumber(contactPhone)}")
        println("  businessAddress: '$businessAddress' (${businessAddress.isNotBlank()})")
        println("  isStep1Valid: $isStep1Valid (✅ email optional)")
        println("  isStep2Valid: $isStep2Valid (✅ phone mandatory)")
        println("  isStep3Valid: $isStep3Valid")
        println("  isCurrentStepValid: $isCurrentStepValid")
        println("  phoneError: $phoneError")
        println("  emailError: $emailError")
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundGradient)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
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
                
                
                // Main Content Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .shadow(
                            elevation = 20.dp,
                            shape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp),
                            ambientColor = Color.Black.copy(alpha = 0.12f),
                            spotColor = Color.Black.copy(alpha = 0.08f)
                        ),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        verticalArrangement = Arrangement.spacedBy(28.dp)
                    ) {
                        // Step 1: Company Information
                        if (currentStep == 1) {
                            CompanyInformationStep(
                                companyName = companyName,
                                contactEmail = contactEmail,
                                industry = industry,
                                companySize = companySize,
                                onCompanyNameChange = { companyName = it },
                                onContactEmailChange = { /* Email is read-only from Google Sign-In */ },
                                onIndustryChange = { industry = it },
                                onCompanySizeChange = { companySize = it }
                            )
                        }
                        
                        // Step 2: Contact Details
                        if (currentStep == 2) {
                            ContactDetailsStep(
                                contactPhone = contactPhone,
                                businessAddress = businessAddress,
                                contactEmail = contactEmail,
                                phoneError = phoneError,
                                emailError = emailError,
                                onContactPhoneChange = { contactPhone = it },
                                onBusinessAddressChange = { businessAddress = it },
                                onContactEmailChange = { contactEmail = it }
                            )
                        }
                        
                        // Step 3: Additional Information
                        if (currentStep == 3) {
                            AdditionalInformationStep(
                                website = website,
                                description = description,
                                onWebsiteChange = { website = it },
                                onDescriptionChange = { description = it }
                            )
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
                
                Spacer(modifier = Modifier.height(24.dp))
            }
            
            // Navigation Buttons - Fixed at bottom
            Surface(
                modifier = Modifier
                    .fillMaxWidth(),
                color = Color.White,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Previous Button
                    if (currentStep > 1) {
                        OutlinedButton(
                            onClick = { currentStep-- },
                            modifier = Modifier
                                .height(52.dp)
                                .weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFF3B82F6)
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
                                            val employerProfileData = mapOf(
                                                "companyName" to companyName,
                                                "contactEmail" to contactEmail,
                                                "contactPhone" to contactPhone,
                                                "businessAddress" to businessAddress,
                                                "industry" to industry,
                                                "companySize" to companySize,
                                                "website" to website,
                                                "description" to description,
                                                "profileCompleted" to true,
                                                "completedAt" to System.currentTimeMillis()
                                            )

                                            // Save to Firestore using ProfileCompletionViewModel
                                            profileCompletionViewModel.saveEmployerProfileData(employerProfileData)
                                        }

                                        // Mark profile as complete
                                        profileCompletionViewModel.markProfileComplete(UserRole.EMPLOYER)

                                        // Mark profile setup as shown for employer
                                        profileCompletionViewModel.markProfileSetupAsShown(UserRole.EMPLOYER)

                                        // Navigate to employer home
                                        navController.navigate(Routes.EMPLOYER_HOME) {
                                            popUpTo(Routes.EMPLOYER_PROFILE_SETUP) { inclusive = true }
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
                            .height(52.dp)
                            .weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF3B82F6)
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
                                Icons.AutoMirrored.Filled.ArrowForward,
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
                .padding(horizontal = 24.dp, vertical = 24.dp)
        ) {
            // Title section
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Complete Your Company Profile",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        fontSize = 28.sp
                    ),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Step $currentStep of $totalSteps - Showcase your company",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color.White.copy(alpha = 0.85f),
                        fontWeight = FontWeight.Medium
                    ),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Enhanced animated progress bar with styling
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .background(
                            Color.White.copy(alpha = 0.15f), 
                            RoundedCornerShape(3.dp)
                        ),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(animatedProgress)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFFFD3ADB), // Pink
                                        Color(0xFF8B5CF6), // Purple
                                        Color(0xFF6366F1)  // Indigo
                                    )
                                ),
                                RoundedCornerShape(3.dp)
                            )
                    )
                }
                // Progress percentage
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Progress",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    )
                    Text(
                        text = "${(animatedProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun CompanyInformationStep(
    companyName: String,
    contactEmail: String,
    industry: String,
    companySize: String,
    onCompanyNameChange: (String) -> Unit,
    onContactEmailChange: (String) -> Unit,
    onIndustryChange: (String) -> Unit,
    onCompanySizeChange: (String) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Step header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF8B5CF6).copy(alpha = 0.15f),
                                Color(0xFFD8B4FE).copy(alpha = 0.1f)
                            )
                        ),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Business,
                    contentDescription = null,
                    tint = Color(0xFF8B5CF6),
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(18.dp))

            Column {
                Text(
                    text = "Company Information",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1F2937),
                        fontSize = 18.sp
                    )
                )
                Text(
                    text = "Tell us about your company",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFF6B7280),
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }

        // Company Name (MANDATORY)
        OutlinedTextField(
            value = companyName,
            onValueChange = onCompanyNameChange,
            label = { Text("Company Name *") },
            placeholder = { Text("Enter your company name") },
            leadingIcon = { Icon(Icons.Default.Business, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            isError = companyName.isBlank(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = if (companyName.isBlank()) Color(0xFFEF4444) else Color(0xFF3B82F6),
                unfocusedBorderColor = if (companyName.isBlank()) Color(0xFFEF4444) else Color(0xFFE5E7EB),
                errorBorderColor = Color(0xFFEF4444)
            ),
            supportingText = if (companyName.isBlank()) {
                { Text("Company name is required", color = Color(0xFFEF4444)) }
            } else null
        )

        // Contact Email (read-only from Google Sign-In)
        OutlinedTextField(
            value = contactEmail,
            onValueChange = { }, // Empty lambda since field is disabled
            label = { Text("Contact Email *") },
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

        // Industry
        OutlinedTextField(
            value = industry,
            onValueChange = onIndustryChange,
            label = { Text("Industry *") },
            placeholder = { Text("e.g., Food Service, Housekeeping, Delivery") },
            leadingIcon = { Icon(Icons.Default.Category, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF3B82F6),
                unfocusedBorderColor = Color(0xFFE5E7EB)
            )
        )

        // Company Size
        OutlinedTextField(
            value = companySize,
            onValueChange = onCompanySizeChange,
            label = { Text("Company Size") },
            placeholder = { Text("e.g., 1-10, 11-50, 51-200, 500+") },
            leadingIcon = { Icon(Icons.Default.People, contentDescription = null) },
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
private fun ContactDetailsStep(
    contactPhone: String,
    businessAddress: String,
    contactEmail: String,
    phoneError: String?,
    emailError: String?,
    onContactPhoneChange: (String) -> Unit,
    onBusinessAddressChange: (String) -> Unit,
    onContactEmailChange: (String) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Step header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF8B5CF6).copy(alpha = 0.15f),
                                Color(0xFFD8B4FE).copy(alpha = 0.1f)
                            )
                        ),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.ContactPhone,
                    contentDescription = null,
                    tint = Color(0xFF8B5CF6),
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(18.dp))

            Column {
                Text(
                    text = "Contact Details",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1F2937),
                        fontSize = 18.sp
                    )
                )
                Text(
                    text = "How can we reach you?",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFF6B7280),
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }

        // Contact Phone
        Column {
            OutlinedTextField(
                value = contactPhone,
                onValueChange = onContactPhoneChange,
                label = { Text("Contact Phone *") },
                placeholder = { Text("Enter 10-digit phone number") },
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                isError = phoneError != null,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (phoneError != null) Color(0xFFDC2626) else Color(0xFF3B82F6),
                    unfocusedBorderColor = if (phoneError != null) Color(0xFFDC2626) else Color(0xFFE5E7EB),
                    errorBorderColor = Color(0xFFDC2626)
                )
            )
            if (phoneError != null) {
                Text(
                    text = phoneError,
                    color = Color(0xFFDC2626),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                )
            }
        }

        // Contact Email (Optional)
        Column {
            OutlinedTextField(
                value = contactEmail,
                onValueChange = onContactEmailChange,
                label = { Text("Contact Email (Optional)") },
                placeholder = { Text("Enter email address") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                isError = emailError != null,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (emailError != null) Color(0xFFDC2626) else Color(0xFF3B82F6),
                    unfocusedBorderColor = if (emailError != null) Color(0xFFDC2626) else Color(0xFFE5E7EB),
                    errorBorderColor = Color(0xFFDC2626)
                )
            )
            if (emailError != null) {
                Text(
                    text = emailError,
                    color = Color(0xFFDC2626),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                )
            }
        }

        // Business Address
        OutlinedTextField(
            value = businessAddress,
            onValueChange = onBusinessAddressChange,
            label = { Text("Business Address *") },
            placeholder = { Text("Enter your business address") },
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
private fun AdditionalInformationStep(
    website: String,
    description: String,
    onWebsiteChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Step header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF8B5CF6).copy(alpha = 0.15f),
                                Color(0xFFD8B4FE).copy(alpha = 0.1f)
                            )
                        ),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = Color(0xFF8B5CF6),
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(18.dp))

            Column {
                Text(
                    text = "Additional Information",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1F2937),
                        fontSize = 18.sp
                    )
                )
                Text(
                    text = "Tell us more about your company",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFF6B7280),
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }

        // Website
        OutlinedTextField(
            value = website,
            onValueChange = onWebsiteChange,
            label = { Text("Website") },
            placeholder = { Text("https://www.yourcompany.com") },
            leadingIcon = { Icon(Icons.Default.Language, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF3B82F6),
                unfocusedBorderColor = Color(0xFFE5E7EB)
            )
        )

        // Company Description
        OutlinedTextField(
            value = description,
            onValueChange = onDescriptionChange,
            label = { Text("Company Description") },
            placeholder = { Text("Describe your company, its mission, and what makes it unique...") },
            leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 4,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF3B82F6),
                unfocusedBorderColor = Color(0xFFE5E7EB)
            )
        )
    }
}
