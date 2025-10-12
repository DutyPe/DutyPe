package com.example.partimes.worker.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.partimes.worker.models.*
import com.example.partimes.viewmodels.JobApplicationViewModel
import com.example.partimes.viewmodels.SimpleApplicationFormViewModel
import com.example.partimes.viewmodels.ApplicationFormUiState
import com.example.partimes.auth.AuthManager
import com.example.partimes.network.ApiClient
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSetupScreen(
    jobId: String,
    navController: NavController
) {
    val context = LocalContext.current
    val viewModel: SimpleApplicationFormViewModel = hiltViewModel()
    val jobApplicationViewModel: JobApplicationViewModel = hiltViewModel()
    val jobApplicationUiState by jobApplicationViewModel.uiState.collectAsStateWithLifecycle()
    
    val uiState by viewModel.uiState.collectAsState()
    val isFormValid by viewModel.isFormValid.collectAsState()
    
    // Step management
    var currentStep by remember { mutableStateOf(1) }
    val totalSteps = 3
    
    // Initialize ApiClient and ApplicationViewModel
    LaunchedEffect(Unit) {
        val authManager = AuthManager(context)
        ApiClient.initialize(authManager)
        // Load saved data is handled in ViewModel init
    }
    
    // Handle form submission success
    LaunchedEffect(uiState.isSubmitted) {
        if (uiState.isSubmitted) {
            // Navigate to worker home after form completion
            navController.navigate("worker_home") {
                popUpTo("profile_setup") { inclusive = true }
            }
        }
    }
    
    // Professional gradient background matching employer setup
    val backgroundGradient = androidx.compose.ui.graphics.Brush.verticalGradient(
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
            ProfessionalApplicationFormHeader(
                navController = navController,
                currentStep = currentStep,
                totalSteps = totalSteps,
                isFormValid = isFormValid
            )

            when {
                uiState.isSubmitted -> {
                    ProfessionalApplicationSuccessScreen(
                        onContinue = { navController.popBackStack() }
                    )
                }
                uiState.error != null -> {
                    ProfessionalErrorScreen(
                        error = uiState.error ?: "Unknown error",
                        onRetry = { viewModel.clearError() }
                    )
                }
                else -> {
                    // Main Content Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 16.dp)
                            .shadow(12.dp, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                    ) {
                        ProfessionalMultiStepFormContent(
                            currentStep = currentStep,
                            uiState = uiState,
                            isFormValid = isFormValid,
                            onPersonalInfoChange = { viewModel.updatePersonalInfo(it) },
                            onAddExperience = { viewModel.addWorkExperience(it) },
                            onUpdateExperience = { index, exp -> viewModel.updateWorkExperience(index, exp) },
                            onRemoveExperience = { viewModel.removeWorkExperience(it) },
                            onAddSkill = { viewModel.addSkill(it) },
                            onRemoveSkill = { viewModel.removeSkill(it) },
                            onCoverLetterChange = { viewModel.updateCoverLetter(it) },
                            onUploadDocument = { viewModel.uploadDocument(it) },
                            onRemoveDocument = { viewModel.removeDocument(it) },
                            onSubmit = { viewModel.submitApplication(jobId, jobApplicationViewModel) },
                            onNextStep = { if (currentStep < totalSteps) currentStep++ },
                            onPreviousStep = { if (currentStep > 1) currentStep-- }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfessionalApplicationFormHeader(
    navController: NavController,
    currentStep: Int,
    totalSteps: Int,
    isFormValid: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .statusBarsPadding()
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Professional back button
                Card(
                    onClick = { navController.popBackStack() },
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
                        text = "Profile Setup",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Text(
                        text = "Step $currentStep of $totalSteps",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.White.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Medium
                        )
                    )
                }

                // Form validation indicator
                Card(
                    modifier = Modifier.size(48.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isFormValid)
                            Color(0xFF10B981).copy(alpha = 0.2f)
                        else
                            Color.White.copy(alpha = 0.1f)
                    ),
                    shape = CircleShape,
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isFormValid) Icons.Default.CheckCircle else Icons.Default.Circle,
                            contentDescription = "Form Status",
                            tint = if (isFormValid) Color(0xFF10B981) else Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Professional progress section
            ProfessionalApplicationFormProgress(
                currentStep = currentStep,
                totalSteps = totalSteps
            )
        }
    }
}

@Composable
private fun ProfessionalApplicationFormProgress(
    currentStep: Int,
    totalSteps: Int
) {
    val stepTitles = listOf(
        "Personal Info",
        "Experience & Skills",
        "Documents & Review"
    )

    val stepIcons = listOf(
        Icons.Default.Person,
        Icons.Default.Work,
        Icons.Default.Description
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.95f)
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Progress header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Application Progress",
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
                        text = "${(currentStep * 100 / totalSteps)}% Complete",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF3B82F6)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

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
                        .fillMaxWidth(currentStep.toFloat() / totalSteps.toFloat())
                        .background(
                            androidx.compose.ui.graphics.Brush.horizontalGradient(
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

            Spacer(modifier = Modifier.height(20.dp))

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
                            modifier = Modifier.size(50.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = when {
                                    index < currentStep - 1 -> Color(0xFF10B981)
                                    index == currentStep - 1 -> Color(0xFF3B82F6)
                                    else -> Color(0xFFE5E7EB)
                                }
                            ),
                            shape = CircleShape,
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = if (index <= currentStep - 1) 4.dp else 1.dp
                            )
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                if (index < currentStep - 1) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                } else {
                                    Icon(
                                        stepIcons[index],
                                        contentDescription = null,
                                        tint = if (index == currentStep - 1) Color.White else Color(0xFF9CA3AF),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = if (index == currentStep - 1) FontWeight.Bold else FontWeight.Medium,
                                color = when {
                                    index < currentStep - 1 -> Color(0xFF10B981)
                                    index == currentStep - 1 -> Color(0xFF3B82F6)
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

private fun calculateStepProgress(currentStep: Int, totalSteps: Int): Float {
    return currentStep.toFloat() / totalSteps.toFloat()
}

@Composable
private fun ProfessionalMultiStepFormContent(
    currentStep: Int,
    uiState: ApplicationFormUiState,
    isFormValid: Boolean,
    onPersonalInfoChange: (PersonalInfo) -> Unit,
    onAddExperience: (WorkExperience) -> Unit,
    onUpdateExperience: (Int, WorkExperience) -> Unit,
    onRemoveExperience: (Int) -> Unit,
    onAddSkill: (String) -> Unit,
    onRemoveSkill: (String) -> Unit,
    onCoverLetterChange: (String) -> Unit,
    onUploadDocument: (Document) -> Unit,
    onRemoveDocument: (String) -> Unit,
    onSubmit: () -> Unit,
    onNextStep: () -> Unit,
    onPreviousStep: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Content area with proper scrolling
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(24.dp)
        ) {
            when (currentStep) {
                1 -> {
                    ProfessionalStep1PersonalInfo(
                        personalInfo = uiState.personalInfo,
                        validationErrors = uiState.validationErrors,
                        onPersonalInfoChange = onPersonalInfoChange
                    )
                }
                2 -> {
                    ProfessionalStep2ExperienceAndSkills(
                        experiences = uiState.experience,
                        skills = uiState.skills,
                        validationErrors = uiState.validationErrors,
                        onAddExperience = onAddExperience,
                        onUpdateExperience = onUpdateExperience,
                        onRemoveExperience = onRemoveExperience,
                        onAddSkill = onAddSkill,
                        onRemoveSkill = onRemoveSkill
                    )
                }
                3 -> {
                    ProfessionalStep3DocumentsAndCoverLetter(
                        documents = uiState.documents,
                        coverLetter = uiState.coverLetter,
                        validationErrors = uiState.validationErrors,
                        onUploadDocument = onUploadDocument,
                        onRemoveDocument = onRemoveDocument,
                        onCoverLetterChange = onCoverLetterChange
                    )
                }
            }
        }

        // Fixed Navigation Section at bottom
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            shadowElevation = 8.dp
        ) {
            ProfessionalStepNavigationButtons(
                currentStep = currentStep,
                totalSteps = 3,
                isFormValid = isFormValid,
                isSubmitting = uiState.isSubmitting,
                onPreviousStep = onPreviousStep,
                onNextStep = onNextStep,
                onSubmit = onSubmit
            )
        }
    }
}

@Composable
private fun ProfessionalStep1PersonalInfo(
    personalInfo: PersonalInfo,
    validationErrors: Map<String, String>,
    onPersonalInfoChange: (PersonalInfo) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            // Step header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 24.dp)
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
        }

        item {
            PersonalInfoSection(
                personalInfo = personalInfo,
                validationErrors = validationErrors,
                onPersonalInfoChange = onPersonalInfoChange
            )
        }
    }
}

@Composable
private fun ProfessionalStep2ExperienceAndSkills(
    experiences: List<WorkExperience>,
    skills: List<String>,
    validationErrors: Map<String, String>,
    onAddExperience: (WorkExperience) -> Unit,
    onUpdateExperience: (Int, WorkExperience) -> Unit,
    onRemoveExperience: (Int) -> Unit,
    onAddSkill: (String) -> Unit,
    onRemoveSkill: (String) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            // Step header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 24.dp)
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
                        text = "Experience & Skills",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A1A1A)
                        )
                    )
                    Text(
                        text = "Share your professional background",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color(0xFF666666)
                        )
                    )
                }
            }
        }

        item {
            ExperienceSection(
                experiences = experiences,
                validationErrors = validationErrors,
                onAddExperience = onAddExperience,
                onUpdateExperience = onUpdateExperience,
                onRemoveExperience = onRemoveExperience
            )
        }

        item {
            SkillsSection(
                skills = skills,
                validationErrors = validationErrors,
                onAddSkill = onAddSkill,
                onRemoveSkill = onRemoveSkill
            )
        }
    }
}

@Composable
private fun ProfessionalStep3DocumentsAndCoverLetter(
    documents: List<Document>,
    coverLetter: String,
    validationErrors: Map<String, String>,
    onUploadDocument: (Document) -> Unit,
    onRemoveDocument: (String) -> Unit,
    onCoverLetterChange: (String) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            // Step header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 24.dp)
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
                        Icons.Default.Description,
                        contentDescription = null,
                        tint = Color(0xFF3B82F6),
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = "Documents & Cover Letter",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A1A1A)
                        )
                    )
                    Text(
                        text = "Upload documents and write your story",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color(0xFF666666)
                        )
                    )
                }
            }
        }

        item {
            DocumentUploadSection(
                documents = documents,
                uploadProgress = emptyMap(),
                isUploading = false,
                validationErrors = validationErrors,
                onUploadDocument = onUploadDocument,
                onRemoveDocument = onRemoveDocument
            )
        }

        item {
            CoverLetterSection(
                coverLetter = coverLetter,
                validationErrors = validationErrors,
                onCoverLetterChange = onCoverLetterChange
            )
        }
    }
}

@Composable
private fun ProfessionalStepNavigationButtons(
    currentStep: Int,
    totalSteps: Int,
    isFormValid: Boolean,
    isSubmitting: Boolean,
    onPreviousStep: () -> Unit,
    onNextStep: () -> Unit,
    onSubmit: () -> Unit
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
                onClick = onPreviousStep,
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

        // Next/Submit Button
        Button(
            onClick = if (currentStep == totalSteps) onSubmit else onNextStep,
            modifier = Modifier
                .height(52.dp)
                .weight(1f),
            enabled = if (currentStep == totalSteps) isFormValid && !isSubmitting else !isSubmitting,
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
                Text("Submitting...")
            } else {
                Text(
                    if (currentStep == totalSteps) "Complete Profile" else "Continue",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    if (currentStep == totalSteps) Icons.Default.Check else Icons.Default.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun ProfessionalApplicationSuccessScreen(
    onContinue: () -> Unit
) {
    var showAnimation by remember { mutableStateOf(true) }

    // Success animation
    val infiniteTransition = rememberInfiniteTransition(label = "success")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    LaunchedEffect(Unit) {
        delay(3000)
        onContinue()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                androidx.compose.ui.graphics.Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF10B981).copy(alpha = 0.1f),
                        Color.White
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Animated success icon
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = "Success",
                modifier = Modifier
                    .size(120.dp)
                    .scale(pulseScale),
                tint = Color(0xFF10B981)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Profile Complete! 🎉",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937)
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Your profile has been successfully created.\nYou're ready to start your job search!",
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = Color(0xFF6B7280),
                    lineHeight = 24.sp
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ProfessionalErrorScreen(
    error: String,
    onRetry: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.ErrorOutline,
                contentDescription = "Error",
                modifier = Modifier.size(80.dp),
                tint = Color(0xFFEF4444)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Oops! Something went wrong",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937)
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = error,
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = Color(0xFF6B7280)
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onRetry,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF3B82F6)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "Retry",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Try Again",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }
    }
}

// ==================== MULTI-STEP FORM COMPONENTS ====================

@Composable
private fun Step1PersonalInfo(
    personalInfo: PersonalInfo,
    validationErrors: Map<String, String>,
    onPersonalInfoChange: (PersonalInfo) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Step title
        Text(
            text = "Personal Information",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1F2937)
            )
        )
        
        Text(
            text = "Tell us about yourself to get started",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = Color(0xFF6B7280)
            )
        )
        
        // Personal info fields
        PersonalInfoSection(
            personalInfo = personalInfo,
            validationErrors = validationErrors,
            onPersonalInfoChange = onPersonalInfoChange
        )
    }
}

@Composable
private fun Step2ExperienceAndSkills(
    experiences: List<WorkExperience>,
    skills: List<String>,
    validationErrors: Map<String, String>,
    onAddExperience: (WorkExperience) -> Unit,
    onUpdateExperience: (Int, WorkExperience) -> Unit,
    onRemoveExperience: (Int) -> Unit,
    onAddSkill: (String) -> Unit,
    onRemoveSkill: (String) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Step title
        Text(
            text = "Experience & Skills",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1F2937)
            )
        )
        
        Text(
            text = "Share your professional background and expertise",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = Color(0xFF6B7280)
            )
        )
        
        // Experience section
        ExperienceSection(
            experiences = experiences,
            validationErrors = validationErrors,
            onAddExperience = onAddExperience,
            onUpdateExperience = onUpdateExperience,
            onRemoveExperience = onRemoveExperience
        )
        
        // Skills section
        SkillsSection(
            skills = skills,
            validationErrors = validationErrors,
            onAddSkill = onAddSkill,
            onRemoveSkill = onRemoveSkill
        )
    }
}

@Composable
private fun Step3DocumentsAndCoverLetter(
    documents: List<Document>,
    coverLetter: String,
    validationErrors: Map<String, String>,
    onUploadDocument: (Document) -> Unit,
    onRemoveDocument: (String) -> Unit,
    onCoverLetterChange: (String) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Step title
        Text(
            text = "Documents & Cover Letter",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1F2937)
            )
        )
        
        Text(
            text = "Upload your documents and write a compelling cover letter",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = Color(0xFF6B7280)
            )
        )
        
        // Document upload section
        DocumentUploadSection(
            documents = documents,
            uploadProgress = emptyMap(), // Simplified - no upload progress for now
            isUploading = false, // This will be handled by the viewModel
            validationErrors = validationErrors,
            onUploadDocument = onUploadDocument,
            onRemoveDocument = onRemoveDocument
        )
        
        // Cover letter section
        CoverLetterSection(
            coverLetter = coverLetter,
            validationErrors = validationErrors,
            onCoverLetterChange = onCoverLetterChange
        )
    }
}

@Composable
private fun StepNavigationButtons(
    currentStep: Int,
    totalSteps: Int,
    isFormValid: Boolean,
    isSubmitting: Boolean,
    onPreviousStep: () -> Unit,
    onNextStep: () -> Unit,
    onSubmit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp), // Add bottom padding for better spacing
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Step info
        Text(
            text = "Step $currentStep of $totalSteps",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium,
                color = Color(0xFF6B7280)
            ),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        
        // Navigation buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Previous button (only show if not on first step)
            if (currentStep > 1) {
                OutlinedButton(
                    onClick = onPreviousStep,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF374151)
                    ),
                    border = BorderStroke(2.dp, Color(0xFFE5E7EB)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Previous",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Previous",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            // Next/Submit button
            Button(
                onClick = if (currentStep == totalSteps) onSubmit else onNextStep,
                enabled = if (currentStep == totalSteps) isFormValid && !isSubmitting else !isSubmitting,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF3B82F6),
                    disabledContainerColor = Color(0xFF9CA3AF)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                
                Text(
                    text = when {
                        isSubmitting -> "Submitting..."
                        currentStep == totalSteps -> "Submit"
                        else -> "Next"
                    },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                
                if (currentStep < totalSteps && !isSubmitting) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "Next",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DateOfBirthField(
    dateOfBirth: String,
    onDateChange: (String) -> Unit,
    isError: Boolean,
    errorMessage: String?,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = dateOfBirth,
        onValueChange = onDateChange,
        label = { Text("Date of Birth") },
        placeholder = { Text("DD/MM/YYYY") },
        modifier = modifier,
        isError = isError,
        supportingText = errorMessage?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFF3B82F6),
            unfocusedBorderColor = Color(0xFFE5E7EB),
            errorBorderColor = Color(0xFFEF4444)
        )
    )
}

@Composable
private fun PersonalInfoSection(
    personalInfo: PersonalInfo,
    validationErrors: Map<String, String>,
    onPersonalInfoChange: (PersonalInfo) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Full Name
        OutlinedTextField(
            value = personalInfo.fullName,
            onValueChange = { onPersonalInfoChange(personalInfo.copy(fullName = it)) },
            label = { Text("Full Name *") },
            placeholder = { Text("Enter your full name") },
            leadingIcon = {
                Icon(Icons.Default.Person, contentDescription = null)
            },
            modifier = Modifier.fillMaxWidth(),
            isError = validationErrors.containsKey("fullName"),
            supportingText = validationErrors["fullName"]?.let { { Text(it, color = Color(0xFFEF4444)) } },
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF3B82F6),
                unfocusedBorderColor = Color(0xFFE5E7EB)
            )
        )

        // Email
        OutlinedTextField(
            value = personalInfo.email,
            onValueChange = { onPersonalInfoChange(personalInfo.copy(email = it)) },
            label = { Text("Email Address *") },
            placeholder = { Text("your.email@example.com") },
            leadingIcon = {
                Icon(Icons.Default.Email, contentDescription = null)
            },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            isError = validationErrors.containsKey("email"),
            supportingText = validationErrors["email"]?.let { { Text(it, color = Color(0xFFEF4444)) } },
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF3B82F6),
                unfocusedBorderColor = Color(0xFFE5E7EB)
            )
        )

        // Phone
        OutlinedTextField(
            value = personalInfo.phone,
            onValueChange = { onPersonalInfoChange(personalInfo.copy(phone = it)) },
            label = { Text("Phone Number *") },
            placeholder = { Text("+91 98765 43210") },
            leadingIcon = {
                Icon(Icons.Default.Phone, contentDescription = null)
            },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            isError = validationErrors.containsKey("phone"),
            supportingText = validationErrors["phone"]?.let { { Text(it, color = Color(0xFFEF4444)) } },
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF3B82F6),
                unfocusedBorderColor = Color(0xFFE5E7EB)
            )
        )

        // Address
        OutlinedTextField(
            value = personalInfo.address,
            onValueChange = { onPersonalInfoChange(personalInfo.copy(address = it)) },
            label = { Text("Address *") },
            placeholder = { Text("Enter your current address") },
            leadingIcon = {
                Icon(Icons.Default.LocationOn, contentDescription = null)
            },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 3,
            isError = validationErrors.containsKey("address"),
            supportingText = validationErrors["address"]?.let { { Text(it, color = Color(0xFFEF4444)) } },
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF3B82F6),
                unfocusedBorderColor = Color(0xFFE5E7EB)
            )
        )

        // Date of Birth
        OutlinedTextField(
            value = personalInfo.dateOfBirth,
            onValueChange = { onPersonalInfoChange(personalInfo.copy(dateOfBirth = it)) },
            label = { Text("Date of Birth *") },
            placeholder = { Text("DD/MM/YYYY") },
            leadingIcon = {
                Icon(Icons.Default.DateRange, contentDescription = null)
            },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            isError = validationErrors.containsKey("dateOfBirth"),
            supportingText = validationErrors["dateOfBirth"]?.let { { Text(it, color = Color(0xFFEF4444)) } },
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF3B82F6),
                unfocusedBorderColor = Color(0xFFE5E7EB)
            )
        )

        // Gender Selection
        GenderRadioGroup(
            selectedGender = personalInfo.gender,
            onGenderSelected = { onPersonalInfoChange(personalInfo.copy(gender = it)) },
            isError = validationErrors.containsKey("gender"),
            errorMessage = validationErrors["gender"],
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun GenderRadioGroup(
    selectedGender: String,
    onGenderSelected: (String) -> Unit,
    isError: Boolean,
    errorMessage: String?,
    modifier: Modifier = Modifier
) {
    val genderOptions = listOf("Male", "Female", "Other")

    Column(modifier = modifier) {
        Text(
            text = "Gender *",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium,
                color = if (isError) Color(0xFFEF4444) else Color(0xFF374151)
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
                    border = if (selectedGender == gender)
                        BorderStroke(2.dp, Color(0xFF3B82F6))
                    else
                        BorderStroke(1.dp, Color(0xFFE5E7EB))
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

        if (isError && errorMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFEF4444)
            )
        }
    }
}

@Composable
private fun ExperienceSection(
    experiences: List<WorkExperience>,
    validationErrors: Map<String, String>,
    onAddExperience: (WorkExperience) -> Unit,
    onUpdateExperience: (Int, WorkExperience) -> Unit,
    onRemoveExperience: (Int) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Work Experience",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937)
                )
            )

            Button(
                onClick = { onAddExperience(WorkExperience()) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF3B82F6)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add Experience",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Experience")
            }
        }

        if (experiences.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFF8FAFC)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.WorkOutline,
                        contentDescription = null,
                        tint = Color(0xFF9CA3AF),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No work experience added yet",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF6B7280)
                        ),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Click 'Add Experience' to get started",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color(0xFF9CA3AF)
                        ),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            experiences.forEachIndexed { index, experience ->
                ExperienceItem(
                    experience = experience,
                    index = index,
                    validationErrors = validationErrors,
                    onUpdate = { onUpdateExperience(index, it) },
                    onRemove = { onRemoveExperience(index) }
                )
            }
        }
    }
}

@Composable
private fun ExperienceItem(
    experience: WorkExperience,
    index: Int,
    validationErrors: Map<String, String>,
    onUpdate: (WorkExperience) -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF8FAFC)
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFE5E7EB))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with remove button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Experience ${index + 1}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937)
                    )
                )

                IconButton(
                    onClick = onRemove,
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            Color(0xFFFEE2E2),
                            CircleShape
                        )
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Remove",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            OutlinedTextField(
                value = experience.company,
                onValueChange = { onUpdate(experience.copy(company = it)) },
                label = { Text("Company *") },
                placeholder = { Text("Enter company name") },
                leadingIcon = {
                    Icon(Icons.Default.Business, contentDescription = null)
                },
                modifier = Modifier.fillMaxWidth(),
                isError = validationErrors.containsKey("experience_${index}_company"),
                supportingText = validationErrors["experience_${index}_company"]?.let { { Text(it, color = Color(0xFFEF4444)) } },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF3B82F6),
                    unfocusedBorderColor = Color(0xFFE5E7EB)
                )
            )

            OutlinedTextField(
                value = experience.position,
                onValueChange = { onUpdate(experience.copy(position = it)) },
                label = { Text("Position *") },
                placeholder = { Text("Enter your job title") },
                leadingIcon = {
                    Icon(Icons.Default.Work, contentDescription = null)
                },
                modifier = Modifier.fillMaxWidth(),
                isError = validationErrors.containsKey("experience_${index}_position"),
                supportingText = validationErrors["experience_${index}_position"]?.let { { Text(it, color = Color(0xFFEF4444)) } },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF3B82F6),
                    unfocusedBorderColor = Color(0xFFE5E7EB)
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = experience.startDate,
                    onValueChange = { onUpdate(experience.copy(startDate = it)) },
                    label = { Text("Start Date *") },
                    placeholder = { Text("MM/YYYY") },
                    leadingIcon = {
                        Icon(Icons.Default.DateRange, contentDescription = null)
                    },
                    modifier = Modifier.weight(1f),
                    isError = validationErrors.containsKey("experience_${index}_startDate"),
                    supportingText = validationErrors["experience_${index}_startDate"]?.let { { Text(it, color = Color(0xFFEF4444)) } },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF3B82F6),
                        unfocusedBorderColor = Color(0xFFE5E7EB)
                    )
                )

                OutlinedTextField(
                    value = experience.endDate ?: "",
                    onValueChange = { onUpdate(experience.copy(endDate = it)) },
                    label = { Text("End Date") },
                    placeholder = { Text("MM/YYYY") },
                    leadingIcon = {
                        Icon(Icons.Default.DateRange, contentDescription = null)
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !experience.isCurrent,
                    isError = validationErrors.containsKey("experience_${index}_endDate"),
                    supportingText = validationErrors["experience_${index}_endDate"]?.let { { Text(it, color = Color(0xFFEF4444)) } },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF3B82F6),
                        unfocusedBorderColor = Color(0xFFE5E7EB)
                    )
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = experience.isCurrent,
                    onCheckedChange = { onUpdate(experience.copy(isCurrent = it)) },
                    colors = CheckboxDefaults.colors(
                        checkedColor = Color(0xFF3B82F6)
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "I currently work here",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFF374151)
                    )
                )
            }

            OutlinedTextField(
                value = experience.description,
                onValueChange = { onUpdate(experience.copy(description = it)) },
                label = { Text("Job Description") },
                placeholder = { Text("Describe your responsibilities and achievements...") },
                leadingIcon = {
                    Icon(Icons.Default.Description, contentDescription = null)
                },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3,
                isError = validationErrors.containsKey("experience_${index}_description"),
                supportingText = validationErrors["experience_${index}_description"]?.let { { Text(it, color = Color(0xFFEF4444)) } },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF3B82F6),
                    unfocusedBorderColor = Color(0xFFE5E7EB)
                )
            )
        }
    }
}

@Composable
private fun SkillsSection(
    skills: List<String>,
    validationErrors: Map<String, String>,
    onAddSkill: (String) -> Unit,
    onRemoveSkill: (String) -> Unit
) {
    var newSkill by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Skills & Expertise",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1F2937)
            )
        )

        // Add skill input
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = newSkill,
                onValueChange = { newSkill = it },
                label = { Text("Add Skill") },
                placeholder = { Text("e.g., JavaScript, Project Management") },
                leadingIcon = {
                    Icon(Icons.Default.Psychology, contentDescription = null)
                },
                modifier = Modifier.weight(1f),
                isError = validationErrors.containsKey("skills"),
                supportingText = validationErrors["skills"]?.let { { Text(it, color = Color(0xFFEF4444)) } },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF3B82F6),
                    unfocusedBorderColor = Color(0xFFE5E7EB)
                )
            )

            Button(
                onClick = {
                    if (newSkill.isNotBlank()) {
                        onAddSkill(newSkill.trim())
                        newSkill = ""
                    }
                },
                enabled = newSkill.isNotBlank(),
                modifier = Modifier.height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF3B82F6)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add",
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Skills display
        if (skills.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFF8FAFC)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Psychology,
                        contentDescription = null,
                        tint = Color(0xFF9CA3AF),
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No skills added yet",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color(0xFF6B7280)
                        )
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.height(150.dp)
            ) {
                items(skills.chunked(2)) { skillRow ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        skillRow.forEach { skill ->
                            SkillChip(
                                skill = skill,
                                onRemove = { onRemoveSkill(skill) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SkillChip(
    skill: String,
    onRemove: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF3B82F6).copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color(0xFF3B82F6).copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = skill,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF3B82F6)
                )
            )

            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(20.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Remove",
                    tint = Color(0xFF3B82F6),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun DocumentUploadSection(
    documents: List<Document>,
    uploadProgress: Map<String, Float>,
    isUploading: Boolean,
    validationErrors: Map<String, String>,
    onUploadDocument: (Document) -> Unit,
    onRemoveDocument: (String) -> Unit
) {
    val context = LocalContext.current
    val documentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val document = Document(
                name = context.contentResolver.getFileName(uri) ?: "document",
                type = DocumentType.RESUME,
                localPath = it.toString()
            )
            onUploadDocument(document)
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Documents",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1F2937)
            )
        )

        // Upload button
        Card(
            onClick = { documentLauncher.launch("*/*") },
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF3B82F6).copy(alpha = 0.1f)
            ),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(
                2.dp,
                Color(0xFF3B82F6).copy(alpha = 0.3f)
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.CloudUpload,
                    contentDescription = "Upload",
                    tint = Color(0xFF3B82F6),
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = if (isUploading) "Uploading..." else "Upload Resume & Documents",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF3B82F6)
                    )
                )
                Text(
                    text = "PDF, DOC, DOCX files supported",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF6B7280)
                    )
                )
            }
        }

        // Documents list
        if (documents.isNotEmpty()) {
            documents.forEach { document ->
                DocumentItem(
                    document = document,
                    progress = uploadProgress[document.id] ?: 0f,
                    onRemove = { onRemoveDocument(document.id) }
                )
            }
        }

        if (validationErrors.containsKey("resume")) {
            Card(
                modifier = Modifier.fillMaxWidth(),
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
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = validationErrors["resume"] ?: "",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color(0xFFEF4444)
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun DocumentItem(
    document: Document,
    progress: Float,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        document.type.getIcon(),
                        contentDescription = "Document",
                        tint = Color(0xFF3B82F6),
                        modifier = Modifier.size(24.dp)
                    )

                    Column {
                        Text(
                            text = document.name,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF1F2937)
                            )
                        )
                        Text(
                            text = document.type.getDisplayName(),
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFF6B7280)
                            )
                        )
                    }
                }

                IconButton(
                    onClick = onRemove,
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            Color(0xFFFEE2E2),
                            CircleShape
                        )
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Remove",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Progress bar
            if (progress > 0f && progress < 1f) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF3B82F6),
                    trackColor = Color(0xFFE5E7EB)
                )
            }
        }
    }
}

@Composable
private fun CoverLetterSection(
    coverLetter: String,
    validationErrors: Map<String, String>,
    onCoverLetterChange: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Cover Letter",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1F2937)
            )
        )

        // Info card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFF0F9FF)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = Color(0xFF3B82F6),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Write a compelling cover letter to stand out from other candidates!",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF1E40AF)
                    )
                )
            }
        }

        // Cover letter field
        OutlinedTextField(
            value = coverLetter,
            onValueChange = onCoverLetterChange,
            label = { Text("Write your cover letter") },
            placeholder = { Text("Dear Hiring Manager,\n\nI am excited to apply for this position because...") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 6,
            maxLines = 10,
            isError = validationErrors.containsKey("coverLetter"),
            supportingText = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (validationErrors.containsKey("coverLetter")) {
                        Text(
                            text = validationErrors["coverLetter"] ?: "",
                            color = Color(0xFFEF4444)
                        )
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }
                    Text(
                        text = "${coverLetter.length} characters",
                        color = Color(0xFF6B7280),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF3B82F6),
                unfocusedBorderColor = Color(0xFFD1D5DB),
                focusedLabelColor = Color(0xFF3B82F6),
                cursorColor = Color(0xFF3B82F6)
            )
        )
    }
}

@Composable
private fun ApplicationSuccessScreen(
    onContinue: () -> Unit
) {
    // Success animation
    val infiniteTransition = rememberInfiniteTransition(label = "success")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    LaunchedEffect(Unit) {
        delay(3000)
        onContinue()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                androidx.compose.ui.graphics.Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF10B981).copy(alpha = 0.1f),
                        Color.White
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Animated success icon
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = "Success",
                modifier = Modifier
                    .size(120.dp)
                    .scale(pulseScale),
                tint = Color(0xFF10B981)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Profile Complete! 🎉",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937)
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Your profile has been successfully created.\nYou're ready to start your job search!",
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = Color(0xFF6B7280),
                    lineHeight = 24.sp
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ErrorScreen(
    error: String,
    onRetry: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.ErrorOutline,
                contentDescription = "Error",
                modifier = Modifier.size(80.dp),
                tint = Color(0xFFEF4444)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Oops! Something went wrong",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937)
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = error,
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = Color(0xFF6B7280)
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onRetry,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF3B82F6)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "Retry",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Try Again",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }
    }
}

// Extension function to get file name from URI
private fun android.content.ContentResolver.getFileName(uri: android.net.Uri): String? {
    var name: String? = null
    val returnCursor = query(uri, null, null, null, null)
    if (returnCursor != null) {
        val nameIndex = returnCursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
        returnCursor.moveToFirst()
        name = returnCursor.getString(nameIndex)
        returnCursor.close()
    }
    return name
}
