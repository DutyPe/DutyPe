package com.example.partimes.jobseeker.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.partimes.viewmodels.ApplicationFormViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplicationFormScreen(
    jobId: String,
    navController: NavController,
    viewModel: ApplicationFormViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val uploadProgress by viewModel.uploadProgress.collectAsState()
    val isFormValid by viewModel.isFormValid.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.loadUserProfile()
    }
    
    // Handle form submission success
    LaunchedEffect(uiState.isSubmitted) {
        if (uiState.isSubmitted) {
            // Navigate back or show success screen
            navController.popBackStack()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Apply for Job",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
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
                    containerColor = Color(0xFF0D47A1)
                )
            )
        }
    ) { paddingValues ->
        JobseekerGradientBackground {
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
                    ApplicationFormContent(
                        uiState = uiState,
                        uploadProgress = uploadProgress,
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
                        onSubmit = { viewModel.submitApplication(jobId) },
                        modifier = Modifier.padding(paddingValues)
                    )
                }
            }
        }
    }
}

@Composable
private fun ApplicationFormContent(
    uiState: ApplicationFormUiState,
    uploadProgress: Map<String, Float>,
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
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            PersonalInfoSection(
                personalInfo = uiState.personalInfo,
                validationErrors = uiState.validationErrors,
                onPersonalInfoChange = onPersonalInfoChange
            )
        }
        
        item {
            ExperienceSection(
                experiences = uiState.experience,
                validationErrors = uiState.validationErrors,
                onAddExperience = onAddExperience,
                onUpdateExperience = onUpdateExperience,
                onRemoveExperience = onRemoveExperience
            )
        }
        
        item {
            SkillsSection(
                skills = uiState.skills,
                validationErrors = uiState.validationErrors,
                onAddSkill = onAddSkill,
                onRemoveSkill = onRemoveSkill
            )
        }
        
        item {
            DocumentUploadSection(
                documents = uiState.documents,
                uploadProgress = uploadProgress,
                isUploading = uiState.isUploading,
                validationErrors = uiState.validationErrors,
                onUploadDocument = onUploadDocument,
                onRemoveDocument = onRemoveDocument
            )
        }
        
        item {
            CoverLetterSection(
                coverLetter = uiState.coverLetter,
                validationErrors = uiState.validationErrors,
                onCoverLetterChange = onCoverLetterChange
            )
        }
        
        item {
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = onSubmit,
                enabled = isFormValid && !uiState.isSubmitting,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF0D47A1)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (uiState.isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }
                Text(
                    text = if (uiState.isSubmitting) "Submitting..." else "Submit Application",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun PersonalInfoSection(
    personalInfo: PersonalInfo,
    validationErrors: Map<String, String>,
    onPersonalInfoChange: (PersonalInfo) -> Unit
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
                text = "Personal Information",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = Color(0xFF0D47A1)
            )
            
            // Full Name
            OutlinedTextField(
                value = personalInfo.fullName,
                onValueChange = { onPersonalInfoChange(personalInfo.copy(fullName = it)) },
                label = { Text("Full Name") },
                modifier = Modifier.fillMaxWidth(),
                isError = validationErrors.containsKey("fullName"),
                supportingText = validationErrors["fullName"]?.let { { Text(it) } }
            )
            
            // Email
            OutlinedTextField(
                value = personalInfo.email,
                onValueChange = { onPersonalInfoChange(personalInfo.copy(email = it)) },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                isError = validationErrors.containsKey("email"),
                supportingText = validationErrors["email"]?.let { { Text(it) } }
            )
            
            // Phone
            OutlinedTextField(
                value = personalInfo.phone,
                onValueChange = { onPersonalInfoChange(personalInfo.copy(phone = it)) },
                label = { Text("Phone Number") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                isError = validationErrors.containsKey("phone"),
                supportingText = validationErrors["phone"]?.let { { Text(it) } }
            )
            
            // Address
            OutlinedTextField(
                value = personalInfo.address,
                onValueChange = { onPersonalInfoChange(personalInfo.copy(address = it)) },
                label = { Text("Address") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3,
                isError = validationErrors.containsKey("address"),
                supportingText = validationErrors["address"]?.let { { Text(it) } }
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Date of Birth
                OutlinedTextField(
                    value = personalInfo.dateOfBirth,
                    onValueChange = { onPersonalInfoChange(personalInfo.copy(dateOfBirth = it)) },
                    label = { Text("Date of Birth") },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("DD/MM/YYYY") },
                    isError = validationErrors.containsKey("dateOfBirth"),
                    supportingText = validationErrors["dateOfBirth"]?.let { { Text(it) } }
                )
                
                // Gender
                OutlinedTextField(
                    value = personalInfo.gender,
                    onValueChange = { onPersonalInfoChange(personalInfo.copy(gender = it)) },
                    label = { Text("Gender") },
                    modifier = Modifier.weight(1f),
                    isError = validationErrors.containsKey("gender"),
                    supportingText = validationErrors["gender"]?.let { { Text(it) } }
                )
            }
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Work Experience",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color(0xFF0D47A1)
                )
                
                IconButton(
                    onClick = { onAddExperience(WorkExperience()) }
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add Experience",
                        tint = Color(0xFF0D47A1)
                    )
                }
            }
            
            if (experiences.isEmpty()) {
                Text(
                    text = "No work experience added yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
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
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Experience ${index + 1}",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
                
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
            
            OutlinedTextField(
                value = experience.company,
                onValueChange = { onUpdate(experience.copy(company = it)) },
                label = { Text("Company") },
                modifier = Modifier.fillMaxWidth(),
                isError = validationErrors.containsKey("experience_${index}_company"),
                supportingText = validationErrors["experience_${index}_company"]?.let { { Text(it) } }
            )
            
            OutlinedTextField(
                value = experience.position,
                onValueChange = { onUpdate(experience.copy(position = it)) },
                label = { Text("Position") },
                modifier = Modifier.fillMaxWidth(),
                isError = validationErrors.containsKey("experience_${index}_position"),
                supportingText = validationErrors["experience_${index}_position"]?.let { { Text(it) } }
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
                    supportingText = validationErrors["experience_${index}_startDate"]?.let { { Text(it) } }
                )
                
                OutlinedTextField(
                    value = experience.endDate ?: "",
                    onValueChange = { onUpdate(experience.copy(endDate = it)) },
                    label = { Text("End Date") },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("MM/YYYY") },
                    enabled = !experience.isCurrent,
                    isError = validationErrors.containsKey("experience_${index}_endDate"),
                    supportingText = validationErrors["experience_${index}_endDate"]?.let { { Text(it) } }
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = experience.isCurrent,
                    onCheckedChange = { onUpdate(experience.copy(isCurrent = it)) }
                )
                Text(
                    text = "Currently working here",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            OutlinedTextField(
                value = experience.description,
                onValueChange = { onUpdate(experience.copy(description = it)) },
                label = { Text("Job Description") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3,
                isError = validationErrors.containsKey("experience_${index}_description"),
                supportingText = validationErrors["experience_${index}_description"]?.let { { Text(it) } }
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = "Success",
            modifier = Modifier.size(80.dp),
            tint = Color(0xFF4CAF50)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Application Submitted!",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Your application has been successfully submitted. The employer will review it and get back to you soon.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = Color.Gray
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF0D47A1)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "Continue",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
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
