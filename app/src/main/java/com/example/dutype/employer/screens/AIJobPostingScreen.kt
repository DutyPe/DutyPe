package com.example.dutype.employer.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.dutype.employer.viewmodels.AIJobPostingViewModel
import com.example.dutype.services.ai.FieldValidation
import com.dutype.app.R

/**
 * AI-Enhanced Job Posting Screen
 * 
 * Features:
 * - Real-time AI validation of title and description
 * - Salary validation against market rates
 * - 3-strike blocking system warning
 * - Risk score indicator
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIJobPostingScreen(
    navController: NavController,
    onJobPosted: () -> Unit,
    onStatusBarColorChange: (Color) -> Unit = {},
    viewModel: AIJobPostingViewModel = hiltViewModel(),
    locationService: com.example.dutype.utils.LocationService = com.example.dutype.utils.LocationService(androidx.compose.ui.platform.LocalContext.current)
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    
    // Set status bar color
    LaunchedEffect(Unit) {
        onStatusBarColorChange(Color.White)
    }
    
    // Handle success
    LaunchedEffect(uiState.submitSuccess) {
        if (uiState.submitSuccess) {
            onJobPosted()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(stringResource(R.string.post_a_job), fontWeight = FontWeight.Bold)
                        Text(
                            stringResource(R.string.ai_protected_posting),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1A237E),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        
        // Check if suspended
        if (uiState.isSuspended) {
            SuspendedScreen(
                reason = uiState.blockReason ?: "Account suspended",
                onContactSupport = { /* Navigate to support */ }
            )
            return@Scaffold
        }
        
        // Check eligibility
        if (uiState.isCheckingEligibility) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Checking posting eligibility...")
                }
            }
            return@Scaffold
        }
        
        if (!uiState.canPost) {
            BlockedScreen(
                reason = uiState.blockReason ?: "Cannot post jobs",
                blockedAttempts = uiState.blockedAttempts,
                onContactSupport = { /* Navigate to support */ }
            )
            return@Scaffold
        }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Warning banner if blocked attempts > 0
            if (uiState.blockedAttempts > 0) {
                WarningBanner(blockedAttempts = uiState.blockedAttempts)
            }
            
            // Risk Score Indicator
            if (uiState.overallRiskScore > 0) {
                RiskScoreIndicator(score = uiState.overallRiskScore)
            }
            
            // Job Title
            ValidatedTextField(
                value = uiState.title,
                onValueChange = { viewModel.onTitleChanged(it) },
                label = "Job Title",
                placeholder = "e.g., Delivery Boy, Security Guard",
                validation = uiState.titleValidation,
                isValidating = uiState.isValidatingTitle,
                leadingIcon = Icons.Default.Work
            )
            
            // Job Description
            ValidatedTextField(
                value = uiState.description,
                onValueChange = { viewModel.onDescriptionChanged(it) },
                label = "Job Description",
                placeholder = "Describe the job responsibilities, requirements...",
                validation = uiState.descriptionValidation,
                isValidating = uiState.isValidatingDescription,
                leadingIcon = Icons.Default.Description,
                minLines = 4,
                maxLines = 8
            )
            
            // Category Dropdown
            CategoryDropdown(
                selectedCategory = uiState.category,
                onCategorySelected = { viewModel.onCategoryChanged(it) }
            )
            
            // Salary Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Pay Amount
                OutlinedTextField(
                    value = uiState.payAmount,
                    onValueChange = { 
                        viewModel.onSalaryChanged(it, uiState.payType)
                    },
                    label = { Text("Salary (₹)") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    leadingIcon = { Text("₹", fontWeight = FontWeight.Bold) },
                    isError = !uiState.salaryValidation.isValid,
                    supportingText = if (!uiState.salaryValidation.isValid) {
                        { Text(uiState.salaryValidation.warnings.firstOrNull() ?: "") }
                    } else null
                )
                
                // Pay Type Dropdown
                PayTypeDropdown(
                    selectedType = uiState.payType,
                    onTypeSelected = { viewModel.onSalaryChanged(uiState.payAmount, it) }
                )
            }
            
            // Location with Autocomplete
            com.example.dutype.components.LocationAutocompleteField(
                value = uiState.location,
                onValueChange = { viewModel.onLocationChanged(it) },
                onLocationSelected = { address, _, _ ->
                    viewModel.onLocationChanged(address)
                },
                locationService = locationService,
                label = "Location",
                placeholder = "Search location (e.g., Hyderabad, Telangana)"
            )
            
            // Vacancies
            OutlinedTextField(
                value = uiState.vacancies,
                onValueChange = { viewModel.onVacanciesChanged(it) },
                label = { Text("Number of Vacancies") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                leadingIcon = { Icon(Icons.Default.People, contentDescription = null) }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Submit Button
            Button(
                onClick = { viewModel.submitJob() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !uiState.isSubmitting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1A237E)
                )
            ) {
                if (uiState.isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Analyzing & Posting...")
                } else {
                    Icon(Icons.Default.Send, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.post_job), fontSize = 16.sp)
                }
            }
            
            // Error message
            uiState.submitError?.let { error ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ValidatedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    validation: FieldValidation,
    isValidating: Boolean,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    minLines: Int = 1,
    maxLines: Int = 1
) {
    Column {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(leadingIcon, contentDescription = null) },
            trailingIcon = {
                when {
                    isValidating -> CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    validation.errors.isNotEmpty() -> Icon(
                        Icons.Default.Error,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    validation.warnings.isNotEmpty() -> Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFFFA000)
                    )
                    value.isNotBlank() && validation.isValid -> Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50)
                    )
                }
            },
            isError = validation.errors.isNotEmpty(),
            minLines = minLines,
            maxLines = maxLines
        )
        
        // Show errors
        validation.errors.forEach { error ->
            Text(
                text = "❌ $error",
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
        
        // Show warnings
        validation.warnings.forEach { warning ->
            Text(
                text = "⚠️ $warning",
                color = Color(0xFFFFA000),
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryDropdown(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val categories = listOf(
        "Delivery", "Security", "Housekeeping", "Driver", 
        "Waiter", "Cook", "Helper", "Packing", "Other"
    )
    
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selectedCategory.ifBlank { "Select Category" },
            onValueChange = {},
            readOnly = true,
            label = { Text("Job Category") },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            leadingIcon = { Icon(Icons.Default.Category, contentDescription = null) }
        )
        
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            categories.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category) },
                    onClick = {
                        onCategorySelected(category.lowercase())
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PayTypeDropdown(
    selectedType: String,
    onTypeSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val payTypes = listOf("DAILY", "HOURLY", "MONTHLY", "FIXED")
    
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.width(140.dp)
    ) {
        OutlinedTextField(
            value = selectedType,
            onValueChange = {},
            readOnly = true,
            label = { Text("Per") },
            modifier = Modifier.menuAnchor(),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
        )
        
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            payTypes.forEach { type ->
                DropdownMenuItem(
                    text = { Text(type) },
                    onClick = {
                        onTypeSelected(type)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun RiskScoreIndicator(score: Int) {
    val color = when {
        score >= 70 -> MaterialTheme.colorScheme.error
        score >= 40 -> Color(0xFFFFA000)
        else -> Color(0xFF4CAF50)
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when {
                    score >= 70 -> Icons.Default.Warning
                    score >= 40 -> Icons.Default.Info
                    else -> Icons.Default.CheckCircle
                },
                contentDescription = null,
                tint = color
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Risk Score: $score/100",
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Text(
                    text = when {
                        score >= 70 -> "High risk - Review your content"
                        score >= 40 -> "Medium risk - Some concerns detected"
                        else -> "Low risk - Looking good!"
                    },
                    fontSize = 12.sp,
                    color = color.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun WarningBanner(blockedAttempts: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFF3E0)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = Color(0xFFE65100)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "⚠️ Warning: $blockedAttempts/3 blocked attempts",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE65100)
                )
                Text(
                    text = "3 blocked attempts will suspend your account",
                    fontSize = 12.sp,
                    color = Color(0xFFE65100).copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun BlockedScreen(
    reason: String,
    blockedAttempts: Int,
    onContactSupport: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.Block,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Cannot Post Jobs",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = reason,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onContactSupport) {
                    Text("Contact Support")
                }
            }
        }
    }
}

@Composable
private fun SuspendedScreen(
    reason: String,
    onContactSupport: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFFFEBEE)
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.Gavel,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = Color(0xFFB71C1C)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Account Suspended",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFB71C1C)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = reason,
                    color = Color(0xFFB71C1C).copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(onClick = onContactSupport) {
                    Text("Appeal Suspension")
                }
            }
        }
    }
}
