package com.example.partimes.profile.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.partimes.profile.viewmodels.ResumeUploadViewModel
import com.example.partimes.profile.models.ParsedResumeData
import com.example.partimes.ui.components.*

/**
 * Parsed Data Preview Composable
 */
@Composable
fun ParsedDataPreview(
    parsedData: ParsedResumeData,
    onApplyToProfile: () -> Unit,
    onEditData: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Parsed Resume Data",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Personal Info
            if (parsedData.personalInfo?.fullName?.isNotEmpty() == true) {
                Text(
                    text = "Name: ${parsedData.personalInfo.fullName}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            // Skills
            if (parsedData.skills.isNotEmpty()) {
                Text(
                    text = "Skills: ${parsedData.skills.take(3).joinToString(", ") { it.name }}${if (parsedData.skills.size > 3) "..." else ""}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            // Work Experience
            if (parsedData.workExperience.isNotEmpty()) {
                Text(
                    text = "Experience: ${parsedData.workExperience.size} positions",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onEditData,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Edit Data")
                }
                
                Button(
                    onClick = onApplyToProfile,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Apply to Profile")
                }
            }
        }
    }
}

/**
 * Resume Upload Screen
 * Upload and parse resume to auto-fill profile information
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResumeUploadScreen(
    navController: NavController,
    onStatusBarColorChange: (Color) -> Unit = {}
) {
    val viewModel: ResumeUploadViewModel = hiltViewModel()
    val uploadState by viewModel.uploadState.collectAsState()
    val parsedData by viewModel.parsedData.collectAsState()
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
                        text = "Resume Upload",
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
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFF90D5FF)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Processing your resume...",
                            color = Color(0xFF7F8C8D)
                        )
                    }
                }
            } else if (error != null) {
                ErrorScreen(
                    errorState = ErrorState(
                        type = ErrorType.UNKNOWN_ERROR,
                        title = "Error",
                        message = error!!,
                        icon = Icons.Default.Error,
                        canRetry = true,
                        retryAction = { viewModel.retryUpload() }
                    ),
                    onRetry = { viewModel.retryUpload() }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Upload Section
                    item {
                        UploadSection(
                            uploadState = uploadState,
                            onUploadClick = { viewModel.selectFile() }
                        )
                    }

                    // Parsed Data Preview
                    if (parsedData != null) {
                        item {
                            ParsedDataPreview(
                                parsedData = parsedData!!,
                                onApplyToProfile = { viewModel.applyToProfile() },
                                onEditData = { /* Navigate to edit screen */ }
                            )
                        }
                    }

                    // Tips Section
                    item {
                        TipsSection()
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
fun UploadSection(
    uploadState: com.example.partimes.profile.models.ResumeUploadState,
    onUploadClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (uploadState) {
                is com.example.partimes.profile.models.ResumeUploadState.NoFile -> {
                    Icon(
                        Icons.Outlined.Upload,
                        contentDescription = "Upload Resume",
                        modifier = Modifier.size(64.dp),
                        tint = Color(0xFF90D5FF)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Upload Your Resume",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2C3E50)
                    )
                    
                    Text(
                        text = "Upload your resume to automatically fill your profile information",
                        fontSize = 14.sp,
                        color = Color(0xFF7F8C8D),
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    Button(
                        onClick = onUploadClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF90D5FF)
                        )
                    ) {
                        Icon(Icons.Default.Upload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Choose File", color = Color.White)
                    }
                }
                
                is com.example.partimes.profile.models.ResumeUploadState.Uploading -> {
                    CircularProgressIndicator(
                        color = Color(0xFF90D5FF)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Uploading...",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF2C3E50)
                    )
                    
                    Text(
                        text = "${uploadState.progress}% complete",
                        fontSize = 14.sp,
                        color = Color(0xFF7F8C8D)
                    )
                }
                
                is com.example.partimes.profile.models.ResumeUploadState.Uploaded -> {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Upload Complete",
                        modifier = Modifier.size(64.dp),
                        tint = Color(0xFF4CAF50)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Resume Uploaded Successfully",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2C3E50)
                    )
                    
                    Text(
                        text = uploadState.fileName,
                        fontSize = 14.sp,
                        color = Color(0xFF7F8C8D)
                    )
                }
                
                is com.example.partimes.profile.models.ResumeUploadState.Error -> {
                    Icon(
                        Icons.Default.Error,
                        contentDescription = "Upload Error",
                        modifier = Modifier.size(64.dp),
                        tint = Color(0xFFE74C3C)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Upload Failed",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2C3E50)
                    )
                    
                    Text(
                        text = uploadState.error,
                        fontSize = 14.sp,
                        color = Color(0xFFE74C3C),
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = onUploadClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF90D5FF)
                        )
                    ) {
                        Text("Try Again", color = Color.White)
                    }
                }
            }
        }
    }
}


@Composable
fun TipsSection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Tips for Better Results",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2C3E50)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            TipItem(
                icon = Icons.Outlined.Description,
                text = "Use a well-formatted PDF or Word document"
            )
            
            TipItem(
                icon = Icons.Outlined.Info,
                text = "Include clear section headers (Experience, Education, Skills)"
            )
            
            TipItem(
                icon = Icons.Outlined.Check,
                text = "Make sure all dates and contact information are accurate"
            )
            
            TipItem(
                icon = Icons.Outlined.Edit,
                text = "Review and edit the parsed information before applying"
            )
        }
    }
}

@Composable
fun TipItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = Color(0xFF90D5FF),
            modifier = Modifier.size(16.dp)
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Text(
            text = text,
            fontSize = 12.sp,
            color = Color(0xFF7F8C8D)
        )
    }
}
