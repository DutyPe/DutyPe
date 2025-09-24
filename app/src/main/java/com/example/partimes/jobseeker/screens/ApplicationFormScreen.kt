package com.example.partimes.jobseeker.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
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
import com.example.partimes.jobseeker.models.*
import com.example.partimes.ui.theme.JobseekerGradientBackground
import com.example.partimes.viewmodels.SimpleApplicationFormViewModel
import com.example.partimes.viewmodels.ApplicationFormUiState
import com.example.partimes.viewmodels.ApplicationViewModel
import com.example.partimes.auth.AuthManager
import com.example.partimes.network.ApiClient
import com.example.partimes.models.JobApplication
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberDatePickerState
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSetupScreen(
    jobId: String,
    navController: NavController,
    viewModel: SimpleApplicationFormViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val applicationViewModel: ApplicationViewModel = hiltViewModel()
    val applicationUiState by applicationViewModel.uiState.collectAsState()
    
    val uiState by viewModel.uiState.collectAsState()
    val isFormValid by viewModel.isFormValid.collectAsState()
    
    // Step management
    var currentStep by remember { mutableStateOf(1) }
    val totalSteps = 3
    
    // Initialize ApiClient and ApplicationViewModel
    LaunchedEffect(Unit) {
        val authManager = AuthManager(context)
        ApiClient.initialize(authManager)
        applicationViewModel.initialize(authManager)
        // Load saved data is handled in ViewModel init
    }
    
    // Handle form submission success
    LaunchedEffect(uiState.isSubmitted) {
        if (uiState.isSubmitted) {
            // Navigate to jobseeker home after form completion
            navController.navigate("jobseeker_home") {
                popUpTo("profile_setup") { inclusive = true }
            }
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Custom Header with Progress Bar and Step Indicator
        ProfileSetupHeader(
            navController = navController,
            progress = calculateStepProgress(currentStep, totalSteps),
            currentStep = currentStep,
            totalSteps = totalSteps,
            isFormValid = isFormValid
        )
        
        when {
            uiState.isSubmitted -> {
                ApplicationSuccessScreen(
                    onContinue = { navController.popBackStack() }
                )
            }
            uiState.error != null -> {
                ErrorScreen(
                    error = uiState.error ?: "Unknown error",
                    onRetry = { viewModel.clearError() }
                )
            }
            else -> {
                MultiStepFormContent(
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
                    onSubmit = { viewModel.submitApplication(jobId, applicationViewModel) },
                    onNextStep = { if (currentStep < totalSteps) currentStep++ },
                    onPreviousStep = { if (currentStep > 1) currentStep-- }
                )
            }
        }
    }
}

@Composable
private fun ProfileSetupHeader(
    navController: NavController,
    progress: Float,
    currentStep: Int,
    totalSteps: Int,
    isFormValid: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .statusBarsPadding()
    ) {
        // Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back Button and Title
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBackIosNew,
                        contentDescription = "Back",
                        tint = Color.Black,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = "Profile Setup",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        fontSize = 20.sp
                    )
                )
            }
            
//
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Step dots with better visibility
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(totalSteps) { step ->
                val stepNumber = step + 1
                val isCompleted = stepNumber < currentStep
                val isCurrent = stepNumber == currentStep
                
                // Step dot with number
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            color = when {
                                isCompleted -> Color(0xFF3B82F6)
                                isCurrent -> Color(0xFF3B82F6)
                                else -> Color(0xFFE5E7EB)
                            },
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stepNumber.toString(),
                        color = when {
                            isCompleted -> Color.White
                            isCurrent -> Color.White
                            else -> Color(0xFF9CA3AF)
                        },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // Connector line between dots
                if (step < totalSteps - 1) {
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(2.dp)
                            .background(
                                color = if (stepNumber < currentStep) Color(0xFF3B82F6) else Color(0xFFE5E7EB),
                                shape = RoundedCornerShape(1.dp)
                            )
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Progress Section
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Form Completion",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF64748B)
                    )
                )
                
                Text(
                    text = "${(progress * 100).roundToInt()}%",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF3B82F6)
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Progress Bar
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = Color(0xFF3B82F6),
                trackColor = Color(0xFFE5E7EB)
            )
        }
    }
}

private fun calculateStepProgress(currentStep: Int, totalSteps: Int): Float {
    return currentStep.toFloat() / totalSteps.toFloat()
}

@Composable
private fun MultiStepFormContent(
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
    onPreviousStep: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Step content with scrollable column
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                when (currentStep) {
                    1 -> {
                        Step1PersonalInfo(
                            personalInfo = uiState.personalInfo,
                            validationErrors = uiState.validationErrors,
                            onPersonalInfoChange = onPersonalInfoChange
                        )
                    }
                    2 -> {
                        Step2ExperienceAndSkills(
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
                        Step3DocumentsAndCoverLetter(
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
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Navigation buttons - always visible at bottom with proper padding
        StepNavigationButtons(
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

@Composable
private fun PersonalInfoSection(
    personalInfo: PersonalInfo,
    validationErrors: Map<String, String>,
    onPersonalInfoChange: (PersonalInfo) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Section Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 20.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = Color(0xFF3B82F6),
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Personal Information",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )
        }
        
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Full Name
            OutlinedTextField(
                value = personalInfo.fullName,
                onValueChange = { onPersonalInfoChange(personalInfo.copy(fullName = it)) },
                label = { Text("Full Name") },
                modifier = Modifier.fillMaxWidth(),
                isError = validationErrors.containsKey("fullName"),
                supportingText = validationErrors["fullName"]?.let { { Text(it) } },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF3B82F6),
                    unfocusedBorderColor = Color(0xFFE5E7EB)
                )
            )
            
            // Email
            OutlinedTextField(
                value = personalInfo.email,
                onValueChange = { onPersonalInfoChange(personalInfo.copy(email = it)) },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                isError = validationErrors.containsKey("email"),
                supportingText = validationErrors["email"]?.let { { Text(it) } },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF3B82F6),
                    unfocusedBorderColor = Color(0xFFE5E7EB)
                )
            )
            
            // Phone
            OutlinedTextField(
                value = personalInfo.phone,
                onValueChange = { onPersonalInfoChange(personalInfo.copy(phone = it)) },
                label = { Text("Phone Number") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                isError = validationErrors.containsKey("phone"),
                supportingText = validationErrors["phone"]?.let { { Text(it) } },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF3B82F6),
                    unfocusedBorderColor = Color(0xFFE5E7EB)
                )
            )
            
            // Address
            OutlinedTextField(
                value = personalInfo.address,
                onValueChange = { onPersonalInfoChange(personalInfo.copy(address = it)) },
                label = { Text("Address") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3,
                isError = validationErrors.containsKey("address"),
                supportingText = validationErrors["address"]?.let { { Text(it) } },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF3B82F6),
                    unfocusedBorderColor = Color(0xFFE5E7EB)
                )
            )
            
            // Date of Birth - Manual Text Input
            DateOfBirthField(
                dateOfBirth = personalInfo.dateOfBirth,
                onDateChange = { onPersonalInfoChange(personalInfo.copy(dateOfBirth = it)) },
                isError = validationErrors.containsKey("dateOfBirth"),
                errorMessage = validationErrors["dateOfBirth"],
                modifier = Modifier.fillMaxWidth()
            )
            
            // Gender Radio Group
            GenderRadioGroup(
                selectedGender = personalInfo.gender,
                onGenderSelected = { onPersonalInfoChange(personalInfo.copy(gender = it)) },
                isError = validationErrors.containsKey("gender"),
                errorMessage = validationErrors["gender"],
                modifier = Modifier.fillMaxWidth()
            )
        }
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
            text = "Gender",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium,
                color = if (isError) Color(0xFFEF4444) else Color(0xFF374151)
            ),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            genderOptions.forEach { gender ->
                Row(
                    modifier = Modifier
                        .clickable { onGenderSelected(gender) }
                        .padding(vertical = 4.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedGender == gender,
                        onClick = { onGenderSelected(gender) },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = Color(0xFF3B82F6),
                            unselectedColor = Color(0xFF9CA3AF)
                        )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = gender,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF374151)
                    )
                }
            }
        }
        
        if (isError && errorMessage != null) {
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFEF4444),
                modifier = Modifier.padding(top = 4.dp)
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
        modifier = Modifier.fillMaxWidth()
    ) {
        // Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Work,
                    contentDescription = null,
                    tint = Color(0xFF3B82F6),
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Work Experience",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
            }
            
            IconButton(
                onClick = { onAddExperience(WorkExperience()) },
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        Color(0xFF3B82F6).copy(alpha = 0.1f),
                        CircleShape
                    )
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add Experience",
                    tint = Color(0xFF3B82F6),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (experiences.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.WorkOutline,
                        contentDescription = null,
                        tint = Color(0xFF9CA3AF),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No work experience added yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF9CA3AF),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Tap + to add your first experience",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF9CA3AF),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
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
}

@Composable
private fun ExperienceItem(
    experience: WorkExperience,
    index: Int,
    validationErrors: Map<String, String>,
    onUpdate: (WorkExperience) -> Unit,
    onRemove: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Color(0xFFF8FAFC),
                RoundedCornerShape(12.dp)
            )
            .border(
                1.dp,
                Color(0xFFE5E7EB),
                RoundedCornerShape(12.dp)
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Experience ${index + 1}",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
            )
            
            IconButton(
                onClick = onRemove,
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        Color(0xFFFEF2F2),
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
            label = { Text("Company") },
            modifier = Modifier.fillMaxWidth(),
            isError = validationErrors.containsKey("experience_${index}_company"),
            supportingText = validationErrors["experience_${index}_company"]?.let { { Text(it) } },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF3B82F6),
                unfocusedBorderColor = Color(0xFFE5E7EB)
            )
        )
        
        OutlinedTextField(
            value = experience.position,
            onValueChange = { onUpdate(experience.copy(position = it)) },
            label = { Text("Position") },
            modifier = Modifier.fillMaxWidth(),
            isError = validationErrors.containsKey("experience_${index}_position"),
            supportingText = validationErrors["experience_${index}_position"]?.let { { Text(it) } },
            shape = RoundedCornerShape(12.dp),
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
                label = { Text("Start Date") },
                modifier = Modifier.weight(1f),
                placeholder = { Text("MM/YYYY") },
                isError = validationErrors.containsKey("experience_${index}_startDate"),
                supportingText = validationErrors["experience_${index}_startDate"]?.let { { Text(it) } },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF3B82F6),
                    unfocusedBorderColor = Color(0xFFE5E7EB)
                )
            )
            
            OutlinedTextField(
                value = experience.endDate ?: "",
                onValueChange = { onUpdate(experience.copy(endDate = it)) },
                label = { Text("End Date") },
                modifier = Modifier.weight(1f),
                placeholder = { Text("MM/YYYY") },
                enabled = !experience.isCurrent,
                isError = validationErrors.containsKey("experience_${index}_endDate"),
                supportingText = validationErrors["experience_${index}_endDate"]?.let { { Text(it) } },
                shape = RoundedCornerShape(12.dp),
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
            Text(
                text = "Currently working here",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF64748B)
            )
        }
        
        OutlinedTextField(
            value = experience.description,
            onValueChange = { onUpdate(experience.copy(description = it)) },
            label = { Text("Job Description") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 3,
            isError = validationErrors.containsKey("experience_${index}_description"),
            supportingText = validationErrors["experience_${index}_description"]?.let { { Text(it) } },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF3B82F6),
                unfocusedBorderColor = Color(0xFFE5E7EB)
            )
        )
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
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Skills",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = Color(0xFF0D47A1)
            )
            
            // Add skill input
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = newSkill,
                    onValueChange = { newSkill = it },
                    label = { Text("Add Skill") },
                    modifier = Modifier.weight(1f),
                    isError = validationErrors.containsKey("skills"),
                    supportingText = validationErrors["skills"]?.let { { Text(it) } }
                )
                
                Button(
                    onClick = {
                        if (newSkill.isNotBlank()) {
                            onAddSkill(newSkill)
                            newSkill = ""
                        }
                    },
                    enabled = newSkill.isNotBlank()
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            
            // Skills list
            if (skills.isEmpty()) {
                Text(
                    text = "No skills added yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    skills.forEach { skill ->
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

@Composable
private fun SkillChip(
    skill: String,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier
            .border(
                1.dp,
                Color(0xFF0D47A1).copy(alpha = 0.3f),
                RoundedCornerShape(16.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0D47A1).copy(alpha = 0.1f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = skill,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF0D47A1)
            )
            
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(16.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Remove",
                    tint = Color(0xFF0D47A1),
                    modifier = Modifier.size(12.dp)
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
                type = DocumentType.RESUME, // Default to resume, can be changed
                localPath = it.toString()
            )
            onUploadDocument(document)
        }
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Documents",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = Color(0xFF0D47A1)
            )
            
            // Upload button
            OutlinedButton(
                onClick = { documentLauncher.launch("*/*") },
                enabled = !isUploading,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFF0D47A1)
                )
            ) {
                if (isUploading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Icon(
                    Icons.Default.Upload,
                    contentDescription = "Upload",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Upload Document")
            }
            
            // Documents list
            if (documents.isEmpty()) {
                Text(
                    text = "No documents uploaded yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                documents.forEach { document ->
                    DocumentItem(
                        document = document,
                        progress = uploadProgress[document.id] ?: 0f,
                        onRemove = { onRemoveDocument(document.id) }
                    )
                }
            }
            
            if (validationErrors.containsKey("resume")) {
                Text(
                    text = validationErrors["resume"] ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
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
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
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
                    tint = Color(0xFF0D47A1),
                    modifier = Modifier.size(24.dp)
                )
                
                Column {
                    Text(
                        text = document.name,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium
                        )
                    )
                    Text(
                        text = document.type.getDisplayName(),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
            
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Remove",
                    tint = Color.Red,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        
        // Progress bar
        if (progress > 0f && progress < 1f) {
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                color = Color(0xFF0D47A1)
            )
        }
    }
}

@Composable
private fun CoverLetterSection(
    coverLetter: String,
    validationErrors: Map<String, String>,
    onCoverLetterChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Cover Letter",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = Color(0xFF0D47A1)
            )
            
            OutlinedTextField(
                value = coverLetter,
                onValueChange = onCoverLetterChange,
                label = { Text("Write your cover letter") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 8,
                isError = validationErrors.containsKey("coverLetter"),
                supportingText = validationErrors["coverLetter"]?.let { { Text(it) } }
            )
            
            Text(
                text = "${coverLetter.length} characters",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ApplicationSuccessScreen(
    onContinue: () -> Unit
) {
    var showConfetti by remember { mutableStateOf(true) }
    val context = LocalContext.current
    
    // Awesome confetti animation with multiple layers
    val infiniteTransition = rememberInfiniteTransition(label = "confetti")
    
    // Layer 1 - Fast falling confetti
    val confetti1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "confetti1"
    )
    
    // Layer 2 - Medium speed confetti
    val confetti2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "confetti2"
    )
    
    // Layer 3 - Slow floating confetti
    val confetti3 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "confetti3"
    )
    
    // Layer 4 - Spinning confetti
    val confetti4 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "confetti4"
    )
    
    // Success icon animation
    var iconScale by remember { mutableStateOf(0f) }
    var textOpacity by remember { mutableStateOf(0f) }
    
    LaunchedEffect(Unit) {
        // Play success sound (can be added later with proper sound file)
        // For now, we'll focus on the visual confetti animation
        
        // Animate icon scale
        iconScale = 1f
        
        // Animate text opacity
        delay(500)
        textOpacity = 1f
        
        // Show confetti for 5 seconds, then stop and navigate
        delay(5000)
        showConfetti = false
        
        // Navigate to jobseeker home after confetti stops
        delay(1000) // Give time to see confetti stop
        onContinue()
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Awesome multi-layer confetti animation
        if (showConfetti) {
            // Layer 1 - Fast falling circular confetti
            repeat(35) { index ->
                val colors = listOf(
                    Color(0xFFFF6B6B), Color(0xFF4ECDC4), Color(0xFF45B7D1),
                    Color(0xFF96CEB4), Color(0xFFFECA57), Color(0xFFFF9FF3),
                    Color(0xFF54A0FF), Color(0xFF5F27CD), Color(0xFF00D2D3),
                    Color(0xFFFF8A80), Color(0xFF80CBC4), Color(0xFF90CAF9)
                )
                
                val color = colors[index % colors.size]
                val size = (12 + (index % 15)).dp
                val xOffset = (index * 45 + confetti1 * 120).dp
                val yOffset = (index * 35 + confetti1 * 250).dp
                
                Box(
                    modifier = Modifier
                        .offset(x = xOffset, y = yOffset)
                        .size(size)
                        .background(color, CircleShape)
                        .alpha(1.0f)
                )
            }
            
            // Layer 2 - Medium speed square confetti
            repeat(30) { index ->
                val colors = listOf(
                    Color(0xFFFF5722), Color(0xFF00BCD4), Color(0xFF8BC34A),
                    Color(0xFFFFC107), Color(0xFFE91E63), Color(0xFF3F51B5),
                    Color(0xFF009688), Color(0xFFFF9800), Color(0xFF795548)
                )
                
                val color = colors[index % colors.size]
                val size = (14 + (index % 12)).dp
                val xOffset = (index * 55 + confetti2 * 100).dp
                val yOffset = (index * 40 + confetti2 * 200).dp
                
                Box(
                    modifier = Modifier
                        .offset(x = xOffset, y = yOffset)
                        .size(size)
                        .background(color, RoundedCornerShape(2.dp))
                        .alpha(1.0f)
                )
            }
            
            // Layer 3 - Slow floating diamond confetti
            repeat(25) { index ->
                val colors = listOf(
                    Color(0xFFFF1744), Color(0xFF00E5FF), Color(0xFF76FF03),
                    Color(0xFFFFD600), Color(0xFFD500F9), Color(0xFF651FFF),
                    Color(0xFF00E676), Color(0xFFFF6D00), Color(0xFF8D6E63)
                )
                
                val color = colors[index % colors.size]
                val size = (16 + (index % 8)).dp
                val xOffset = (index * 60 + confetti3 * 80).dp
                val yOffset = (index * 45 + confetti3 * 180).dp
                
                Box(
                    modifier = Modifier
                        .offset(x = xOffset, y = yOffset)
                        .size(size)
                        .background(color, RoundedCornerShape(4.dp))
                        .alpha(1.0f)
                )
            }
            
            // Layer 4 - Spinning star confetti
            repeat(20) { index ->
                val colors = listOf(
                    Color(0xFFFFD700), Color(0xFFC0C0C0), Color(0xFFFF69B4),
                    Color(0xFF00CED1), Color(0xFF32CD32), Color(0xFFFF4500)
                )
                
                val color = colors[index % colors.size]
                val size = (18 + (index % 6)).dp
                val xOffset = (index * 70 + confetti4 * 90).dp
                val yOffset = (index * 50 + confetti4 * 160).dp
                
                Box(
                    modifier = Modifier
                        .offset(x = xOffset, y = yOffset)
                        .size(size)
                        .background(color, RoundedCornerShape(6.dp))
                        .alpha(1.0f)
                )
            }
            
            // Layer 5 - Tiny sparkle confetti
            repeat(40) { index ->
                val colors = listOf(
                    Color(0xFFFFFFFF), Color(0xFFFFF8DC), Color(0xFFF0F8FF),
                    Color(0xFFFFE4E1), Color(0xFFF5F5DC), Color(0xFFE6E6FA)
                )
                
                val color = colors[index % colors.size]
                val size = (8 + (index % 6)).dp
                val xOffset = (index * 35 + confetti1 * 70).dp
                val yOffset = (index * 25 + confetti2 * 120).dp
                
                Box(
                    modifier = Modifier
                        .offset(x = xOffset, y = yOffset)
                        .size(size)
                        .background(color, CircleShape)
                        .alpha(1.0f)
                )
            }
        }
        
        // Main content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Animated success icon
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = "Success",
                modifier = Modifier
                    .size(120.dp)
                    .scale(iconScale),
                tint = Color(0xFF4CAF50)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Animated success text
            Text(
                text = "🎉 Profile Complete! 🎉",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp
                ),
                textAlign = TextAlign.Center,
                color = Color(0xFF2E7D32),
                modifier = Modifier.alpha(textOpacity)
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Text(
                text = "Congratulations! Your profile has been successfully set up.",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 18.sp,
                    lineHeight = 24.sp
                ),
                textAlign = TextAlign.Center,
                color = Color(0xFF424242),
                modifier = Modifier.alpha(textOpacity)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "You're all set to start applying for jobs and connecting with employers! 🚀",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 16.sp
                ),
                textAlign = TextAlign.Center,
                color = Color(0xFF757575),
                modifier = Modifier.alpha(textOpacity)
            )
            
            Spacer(modifier = Modifier.height(40.dp))
            
            // Auto-navigation message
            Text(
                text = "Redirecting to dashboard...",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp,
                    color = Color(0xFF9CA3AF)
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(textOpacity)
            )
        }
    }
}

@Composable
private fun ErrorScreen(
    error: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.ErrorOutline,
            contentDescription = "Error",
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.error
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Something went wrong",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = error,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = Color.Gray
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF0D47A1)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                Icons.Default.Refresh,
                contentDescription = "Retry",
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Try Again",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
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
