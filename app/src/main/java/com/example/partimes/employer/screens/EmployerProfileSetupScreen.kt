package com.example.partimes.employer.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.partimes.employer.models.*
import com.example.partimes.viewmodels.ApplicationViewModel
import com.example.partimes.viewmodels.EmployerProfileSetupViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployerProfileSetupScreen(
    navController: NavController,
    applicationViewModel: ApplicationViewModel = hiltViewModel()
) {
    val viewModel: EmployerProfileSetupViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()
    
    var currentStep by remember { mutableStateOf(0) }
    
    // Update current step in ViewModel
    LaunchedEffect(currentStep) {
        viewModel.updateCurrentStep(currentStep)
    }
    
    val isFormValid by viewModel.isFormValid.collectAsState(initial = false)
    val totalSteps = 3
    
    // Step content
    val stepTitles = listOf(
        "Company Information",
        "Business Details",
        "Verification Details"
    )

    val stepIcons = listOf(
        Icons.Filled.Business,
        Icons.Filled.ContactPhone,
        Icons.Filled.VerifiedUser
    )

    // Professional gradient background
    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF1E3A8A), // Deep professional blue
            Color(0xFF3B82F6), // Bright blue
            Color(0xFFE0F2FE), // Light blue
            Color.White
        ),
        startY = 0f,
        endY = 1200f
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundGradient)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Professional Header with proper back navigation
            ProfessionalHeader(
                navController = navController,
                currentStep = currentStep,
                totalSteps = totalSteps,
                onBackClick = {
                    if (currentStep > 0) {
                        currentStep--
                    } else {
                        navController.popBackStack()
                    }
                }
            )

            // Enhanced Progress Section
            ProfessionalProgressSection(
                currentStep = currentStep,
                totalSteps = totalSteps,
                stepTitles = stepTitles,
                stepIcons = stepIcons
            )

            // Main Content with proper scrolling
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp)
                    .shadow(12.dp, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Content area with proper weight distribution
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(24.dp)
                    ) {
                        // Step content with smooth animations
                        AnimatedVisibility(
                            visible = currentStep == 0,
                            enter = slideInHorizontally(
                                animationSpec = tween(500),
                                initialOffsetX = { it }
                            ) + fadeIn(tween(500)),
                            exit = slideOutHorizontally(
                                animationSpec = tween(500),
                                targetOffsetX = { -it }
                            ) + fadeOut(tween(500))
                        ) {
                            ProfessionalCompanyInfoStep(
                                companyInfo = uiState.companyInfo,
                                onUpdate = viewModel::updateCompanyInfo
                            )
                        }

                        AnimatedVisibility(
                            visible = currentStep == 1,
                            enter = slideInHorizontally(
                                animationSpec = tween(500),
                                initialOffsetX = { it }
                            ) + fadeIn(tween(500)),
                            exit = slideOutHorizontally(
                                animationSpec = tween(500),
                                targetOffsetX = { -it }
                            ) + fadeOut(tween(500))
                        ) {
                            ProfessionalBusinessDetailsStep(
                                businessDetails = uiState.businessDetails,
                                onUpdate = viewModel::updateBusinessDetails
                            )
                        }

                        AnimatedVisibility(
                            visible = currentStep == 2,
                            enter = slideInHorizontally(
                                animationSpec = tween(500),
                                initialOffsetX = { it }
                            ) + fadeIn(tween(500)),
                            exit = slideOutHorizontally(
                                animationSpec = tween(500),
                                targetOffsetX = { -it }
                            ) + fadeOut(tween(500))
                        ) {
                            ProfessionalVerificationDetailsStep(
                                verificationDetails = uiState.verificationDetails,
                                onUpdate = viewModel::updateVerificationDetails
                            )
                        }
                    }

                    // Fixed Navigation Section at bottom
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color.White,
                        shadowElevation = 8.dp
                    ) {
                        ProfessionalNavigationButtons(
                            currentStep = currentStep,
                            totalSteps = totalSteps,
                            isFormValid = isFormValid,
                            isSubmitting = uiState.isSubmitting,
                            onPrevious = { currentStep-- },
                            onNext = { currentStep++ },
                            onComplete = {
                                viewModel.submitProfile(applicationViewModel)
                                navController.navigate("employer_home")
                            }
                        )
                    }

                    // Error message section
                    uiState.error?.let { error ->
                        ProfessionalErrorMessage(error = error)
                    }
                }
            }
        }
    }
}

@Composable
fun ProfessionalHeader(
    navController: NavController,
    currentStep: Int,
    totalSteps: Int,
    onBackClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .statusBarsPadding(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Professional back button
            Card(
                onClick = onBackClick,
                modifier = Modifier.size(48.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.9f)
                ),
                shape = CircleShape,
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFF1E3A8A),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Professional title section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Business Setup",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
                Text(
                    text = "Step ${currentStep + 1} of $totalSteps",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color.White.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Medium
                    )
                )
            }

            // Professional skip button
            Card(
                onClick = {
                    navController.navigate("employer_home") {
                        popUpTo("employer_profile_setup") { inclusive = true }
                    }
                },
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    Color.White.copy(alpha = 0.3f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Text(
                    text = "Skip",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }
    }
}

@Composable
fun ProfessionalProgressSection(
    currentStep: Int,
    totalSteps: Int,
    stepTitles: List<String>,
    stepIcons: List<ImageVector>
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .shadow(8.dp, RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.95f)
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            // Progress header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Setup Progress",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E3A8A)
                    )
                )

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF3B82F6).copy(alpha = 0.1f)
                ) {
                    Text(
                        text = "${((currentStep + 1) * 100 / totalSteps)}% Complete",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF3B82F6)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Professional progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(Color(0xFFE5E7EB), RoundedCornerShape(3.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth((currentStep + 1).toFloat() / totalSteps.toFloat())
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF3B82F6),
                                    Color(0xFF1D4ED8),
                                    Color(0xFF6366F1)
                                )
                            ),
                            RoundedCornerShape(3.dp)
                        )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Professional step indicators
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                stepTitles.forEachIndexed { index, title ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        // Step indicator circle
                        Card(
                            modifier = Modifier.size(56.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = when {
                                    index < currentStep -> Color(0xFF10B981)
                                    index == currentStep -> Color(0xFF3B82F6)
                                    else -> Color(0xFFE5E7EB)
                                }
                            ),
                            shape = CircleShape,
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = if (index <= currentStep) 6.dp else 2.dp
                            )
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                if (index < currentStep) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(28.dp)
                                    )
                                } else {
                                    Icon(
                                        stepIcons[index],
                                        contentDescription = null,
                                        tint = if (index == currentStep) Color.White else Color(0xFF9CA3AF),
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = if (index == currentStep) FontWeight.Bold else FontWeight.Medium,
                                color = when {
                                    index < currentStep -> Color(0xFF10B981)
                                    index == currentStep -> Color(0xFF3B82F6)
                                    else -> Color(0xFF6B7280)
                                }
                            ),
                            textAlign = TextAlign.Center,
                            maxLines = 2
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProfessionalCompanyInfoStep(
    companyInfo: CompanyInfo,
    onUpdate: (CompanyInfo) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        // Step header with description
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        Color(0xFF2196F3).copy(alpha = 0.1f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Business,
                    contentDescription = null,
                    tint = Color(0xFF2196F3),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = "Company Information",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1A1A)
                    )
                )
                Text(
                    text = "Tell us about your company",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFF666666)
                    )
                )
            }
        }

        // Company Name Field
        EnhancedTextField(
            value = companyInfo.companyName,
            onValueChange = { onUpdate(companyInfo.copy(companyName = it)) },
            label = "Company Name *",
            placeholder = "Enter your company name",
            leadingIcon = Icons.Default.Business,
            isRequired = true
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Industry Field
        EnhancedTextField(
            value = companyInfo.industry,
            onValueChange = { onUpdate(companyInfo.copy(industry = it)) },
            label = "Industry *",
            placeholder = "e.g., Technology, Healthcare, Finance",
            leadingIcon = Icons.Default.Category,
            isRequired = true
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Company Size Field
        EnhancedTextField(
            value = companyInfo.companySize,
            onValueChange = { onUpdate(companyInfo.copy(companySize = it)) },
            label = "Company Size",
            placeholder = "e.g., 1-10, 11-50, 51-200, 500+",
            leadingIcon = Icons.Default.People
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Website Field
        EnhancedTextField(
            value = companyInfo.website,
            onValueChange = { onUpdate(companyInfo.copy(website = it)) },
            label = "Website",
            placeholder = "https://www.yourcompany.com",
            leadingIcon = Icons.Default.Language
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Description Field
        EnhancedTextField(
            value = companyInfo.description,
            onValueChange = { onUpdate(companyInfo.copy(description = it)) },
            label = "Company Description",
            placeholder = "Describe what your company does...",
            leadingIcon = Icons.Default.Description,
            minLines = 3,
            maxLines = 5
        )
    }
}

@Composable
fun ProfessionalBusinessDetailsStep(
    businessDetails: BusinessDetails,
    onUpdate: (BusinessDetails) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        // Step header with description
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        Color(0xFF2196F3).copy(alpha = 0.1f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.ContactPhone,
                    contentDescription = null,
                    tint = Color(0xFF2196F3),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = "Business Details",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1A1A)
                    )
                )
                Text(
                    text = "Contact and business information",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFF666666)
                    )
                )
            }
        }

        EnhancedTextField(
            value = businessDetails.contactPersonName,
            onValueChange = { onUpdate(businessDetails.copy(contactPersonName = it)) },
            label = "Contact Person Name *",
            placeholder = "Enter HR manager or contact person name",
            leadingIcon = Icons.Default.Person,
            isRequired = true
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        EnhancedTextField(
            value = businessDetails.contactEmail,
            onValueChange = { onUpdate(businessDetails.copy(contactEmail = it)) },
            label = "Contact Email *",
            placeholder = "hr@yourcompany.com",
            leadingIcon = Icons.Default.Email,
            keyboardType = KeyboardType.Email,
            isRequired = true
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        EnhancedTextField(
            value = businessDetails.contactPhone,
            onValueChange = { onUpdate(businessDetails.copy(contactPhone = it)) },
            label = "Contact Phone *",
            placeholder = "+91 98765 43210",
            leadingIcon = Icons.Default.Phone,
            keyboardType = KeyboardType.Phone,
            isRequired = true
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        EnhancedTextField(
            value = businessDetails.businessAddress,
            onValueChange = { onUpdate(businessDetails.copy(businessAddress = it)) },
            label = "Business Address *",
            placeholder = "Enter your office address",
            leadingIcon = Icons.Default.LocationOn,
            minLines = 2,
            maxLines = 3,
            isRequired = true
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        EnhancedTextField(
            value = businessDetails.yearsInBusiness,
            onValueChange = { onUpdate(businessDetails.copy(yearsInBusiness = it)) },
            label = "Years in Business",
            placeholder = "e.g., 5",
            leadingIcon = Icons.Default.DateRange,
            keyboardType = KeyboardType.Number
        )
    }
}

@Composable
fun ProfessionalVerificationDetailsStep(
    verificationDetails: VerificationDetails,
    onUpdate: (VerificationDetails) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        // Step header with description
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        Color(0xFF2196F3).copy(alpha = 0.1f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.VerifiedUser,
                    contentDescription = null,
                    tint = Color(0xFF2196F3),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = "Verification Details",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1A1A)
                    )
                )
                Text(
                    text = "Legal and business verification",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFF666666)
                    )
                )
            }
        }

        // Info card about verification
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = Color(0xFF2196F3),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "These details help verify your business authenticity and build trust with workers.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF666666)
                    )
                )
            }
        }

        EnhancedTextField(
            value = verificationDetails.businessRegistrationNumber,
            onValueChange = { onUpdate(verificationDetails.copy(businessRegistrationNumber = it)) },
            label = "Business Registration Number",
            placeholder = "Enter your business registration number",
            leadingIcon = Icons.Default.Badge
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        EnhancedTextField(
            value = verificationDetails.gstNumber,
            onValueChange = { onUpdate(verificationDetails.copy(gstNumber = it)) },
            label = "GST Number",
            placeholder = "Enter your GST number",
            leadingIcon = Icons.Default.Receipt
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        EnhancedTextField(
            value = verificationDetails.panNumber,
            onValueChange = { onUpdate(verificationDetails.copy(panNumber = it)) },
            label = "PAN Number",
            placeholder = "Enter your PAN number",
            leadingIcon = Icons.Default.CreditCard
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        EnhancedTextField(
            value = verificationDetails.businessType,
            onValueChange = { onUpdate(verificationDetails.copy(businessType = it)) },
            label = "Business Type",
            placeholder = "e.g., Private Limited, Partnership, Sole Proprietorship",
            leadingIcon = Icons.Default.AccountBalance
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        EnhancedTextField(
            value = verificationDetails.additionalNotes,
            onValueChange = { onUpdate(verificationDetails.copy(additionalNotes = it)) },
            label = "Additional Notes",
            placeholder = "Any additional information about your business...",
            leadingIcon = Icons.Default.Notes,
            minLines = 2,
            maxLines = 4
        )
    }
}

@Composable
fun EnhancedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    leadingIcon: ImageVector? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    minLines: Int = 1,
    maxLines: Int = 1,
    isRequired: Boolean = false
) {
    val isError = isRequired && value.isEmpty()
    val borderColor = when {
        isError -> Color(0xFFEF4444)
        value.isNotEmpty() -> Color(0xFF10B981)
        else -> Color(0xFFD1D5DB)
    }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = label,
                        color = when {
                            isError -> Color(0xFFEF4444)
                            value.isNotEmpty() -> Color(0xFF10B981)
                            else -> Color(0xFF6B7280)
                        }
                    )
                    if (isRequired) {
                        Text(
                            text = " *",
                            color = Color(0xFFEF4444),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            },
            placeholder = {
                Text(
                    text = placeholder,
                    color = Color(0xFF9CA3AF),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            leadingIcon = leadingIcon?.let { icon ->
                {
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape,
                        color = when {
                            isError -> Color(0xFFEF4444).copy(alpha = 0.1f)
                            value.isNotEmpty() -> Color(0xFF10B981).copy(alpha = 0.1f)
                            else -> Color(0xFF6B7280).copy(alpha = 0.1f)
                        }
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = when {
                                isError -> Color(0xFFEF4444)
                                value.isNotEmpty() -> Color(0xFF10B981)
                                else -> Color(0xFF6B7280)
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp)
                        )
                    }
                }
            },
            trailingIcon = if (value.isNotEmpty() && !isError) {
                {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Valid",
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else null,
            modifier = Modifier.fillMaxWidth(),
            singleLine = minLines == 1 && maxLines == 1,
            minLines = minLines,
            maxLines = maxLines,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF3B82F6),
                unfocusedBorderColor = borderColor,
                focusedLabelColor = Color(0xFF3B82F6),
                cursorColor = Color(0xFF3B82F6),
                focusedLeadingIconColor = Color(0xFF3B82F6),
                unfocusedLeadingIconColor = Color(0xFF6B7280)
            )
        )

        // Helper text for required fields
        if (isRequired && value.isEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "This field is required",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFFEF4444)
                    )
                )
            }
        }
    }
}

@Composable
fun ProfessionalNavigationButtons(
    currentStep: Int,
    totalSteps: Int,
    isFormValid: Boolean,
    isSubmitting: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onComplete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Previous Button
        if (currentStep > 0) {
            OutlinedButton(
                onClick = onPrevious,
                modifier = Modifier
                    .height(52.dp)
                    .weight(1f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(2.dp, Color(0xFF3B82F6)),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFF3B82F6)
                )
            ) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Previous",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }

        // Next/Complete Button
        Button(
            onClick = if (currentStep < totalSteps - 1) onNext else onComplete,
            modifier = Modifier
                .height(52.dp)
                .weight(1f),
            enabled = isFormValid && !isSubmitting,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF3B82F6),
                disabledContainerColor = Color(0xFFE5E7EB)
            ),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 4.dp,
                pressedElevation = 8.dp
            )
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Setting up...")
            } else {
                Text(
                    if (currentStep < totalSteps - 1) "Continue" else "Complete Setup",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    if (currentStep < totalSteps - 1) Icons.Default.ArrowForward else Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun ProfessionalErrorMessage(error: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFEE2E2)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = Color(0xFFDC2626),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color(0xFFDC2626),
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }
}
