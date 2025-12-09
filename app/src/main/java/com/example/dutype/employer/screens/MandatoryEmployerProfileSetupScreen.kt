package com.example.dutype.employer.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.dutype.models.UserRole
import com.example.dutype.navigation.Routes
import com.example.dutype.viewmodels.ProfileCompletionViewModel
import com.example.dutype.utils.ValidationUtils
import kotlinx.coroutines.launch

@Composable
fun MandatoryEmployerProfileSetupScreen(
    navController: NavController,
    viewModel: ProfileCompletionViewModel = hiltViewModel()
) {
    val scope = rememberCoroutineScope()

    // Form state
    var companyName by remember { mutableStateOf("") }
    var contactEmail by remember { mutableStateOf("") }
    var contactPhone by remember { mutableStateOf("") }
    var businessAddress by remember { mutableStateOf("") }
    var industry by remember { mutableStateOf("") }
    var companySize by remember { mutableStateOf("") }
    var website by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    // UI state
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var currentStep by remember { mutableStateOf(1) }
    val totalSteps = 3

    // Load saved user info from Google Sign-In
    LaunchedEffect(Unit) {
        val savedEmail = viewModel.getUserEmail()
        val savedName = viewModel.getUserName()
        if (savedEmail != null) {
            contactEmail = savedEmail
        }
        if (savedName != null) {
            companyName = savedName
        }
    }

    // Validation logic
    val isStep1Valid = companyName.isNotBlank() && industry.isNotBlank()
    val isStep2Valid = ValidationUtils.isValidIndianPhoneNumber(contactPhone) && businessAddress.isNotBlank() &&
            (contactEmail.isBlank() || ValidationUtils.isValidEmail(contactEmail))
    val isStep3Valid = true // Additional info is optional

    var phoneError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(contactPhone, contactEmail) {
        phoneError = when {
            contactPhone.isNotBlank() && !ValidationUtils.isValidIndianPhoneNumber(contactPhone) -> "Enter a valid 10-digit phone number"
            else -> null
        }
        emailError = when {
            contactEmail.isNotBlank() && !ValidationUtils.isValidEmail(contactEmail) -> "Enter a valid email address"
            else -> null
        }
    }

    val isCurrentStepValid = when (currentStep) {
        1 -> isStep1Valid
        2 -> isStep2Valid
        3 -> isStep3Valid
        else -> false
    }

    fun handleCompletion() {
        scope.launch {
            isLoading = true
            errorMessage = null
            try {
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
                    viewModel.saveEmployerProfileData(employerProfileData)
                }
                viewModel.markProfileComplete(UserRole.EMPLOYER)
                viewModel.markProfileSetupAsShown(UserRole.EMPLOYER)

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

    MandatoryEmployerProfileSetupContent(
        companyName = companyName,
        contactEmail = contactEmail,
        contactPhone = contactPhone,
        businessAddress = businessAddress,
        industry = industry,
        companySize = companySize,
        website = website,
        description = description,
        isLoading = isLoading,
        errorMessage = errorMessage,
        currentStep = currentStep,
        totalSteps = totalSteps,
        isCurrentStepValid = isCurrentStepValid,
        phoneError = phoneError,
        emailError = emailError,
        onCompanyNameChange = { companyName = it },
        onContactEmailChange = { contactEmail = it },
        onContactPhoneChange = { contactPhone = it },
        onBusinessAddressChange = { businessAddress = it },
        onIndustryChange = { industry = it },
        onCompanySizeChange = { companySize = it },
        onWebsiteChange = { website = it },
        onDescriptionChange = { description = it },
        onPreviousClick = { currentStep-- },
        onNextClick = { currentStep++ },
        onCompleteClick = { handleCompletion() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MandatoryEmployerProfileSetupContent(
    companyName: String,
    contactEmail: String,
    contactPhone: String,
    businessAddress: String,
    industry: String,
    companySize: String,
    website: String,
    description: String,
    isLoading: Boolean,
    errorMessage: String?,
    currentStep: Int,
    totalSteps: Int,
    isCurrentStepValid: Boolean,
    phoneError: String?,
    emailError: String?,
    onCompanyNameChange: (String) -> Unit,
    onContactEmailChange: (String) -> Unit,
    onContactPhoneChange: (String) -> Unit,
    onBusinessAddressChange: (String) -> Unit,
    onIndustryChange: (String) -> Unit,
    onCompanySizeChange: (String) -> Unit,
    onWebsiteChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onCompleteClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
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
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 28.dp,
                            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                            ambientColor = Color.Black.copy(alpha = 0.12f),
                            spotColor = Color.Black.copy(alpha = 0.08f)
                        ),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        if (currentStep == 1) {
                            CompanyInformationStep(
                                companyName = companyName,
                                contactEmail = contactEmail,
                                industry = industry,
                                companySize = companySize,
                                onCompanyNameChange = onCompanyNameChange,
                                onContactEmailChange = {},
                                onIndustryChange = onIndustryChange,
                                onCompanySizeChange = onCompanySizeChange
                            )
                        }

                        if (currentStep == 2) {
                            ContactDetailsStep(
                                contactPhone = contactPhone,
                                businessAddress = businessAddress,
                                contactEmail = contactEmail,
                                phoneError = phoneError,
                                emailError = emailError,
                                onContactPhoneChange = onContactPhoneChange,
                                onBusinessAddressChange = onBusinessAddressChange,
                                onContactEmailChange = onContactEmailChange
                            )
                        }

                        if (currentStep == 3) {
                            AdditionalInformationStep(
                                website = website,
                                description = description,
                                onWebsiteChange = onWebsiteChange,
                                onDescriptionChange = onDescriptionChange
                            )
                        }

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
                                        text = errorMessage,
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

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (currentStep > 1) {
                        OutlinedButton(
                            onClick = onPreviousClick,
                            modifier = Modifier
                                .height(52.dp)
                                .weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFF3B82F6)
                            )
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Previous")
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }

                    Button(
                        onClick = {
                            if (currentStep < totalSteps) {
                                onNextClick()
                            } else {
                                onCompleteClick()
                            }
                        },
                        enabled = isCurrentStepValid && !isLoading,
                        modifier = Modifier
                            .height(52.dp)
                            .weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text(
                                if (currentStep == totalSteps) "Complete Profile" else "Next",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                        if (currentStep < totalSteps) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
        modifier = Modifier.padding(top = 20.dp),
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

        OutlinedTextField(
            value = contactEmail,
            onValueChange = { },
            label = { Text("Contact Email *") },
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            enabled = false,
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

        Column {
            Text(
                text = "Industry * (Select all that apply)",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF374151)
                ),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            val industriesWithIcons = listOf(
                "Food Service" to Icons.Default.Restaurant,
                "Housekeeping" to Icons.Default.HomeWork,
                "Delivery" to Icons.Default.LocalShipping,
                "Warehouse" to Icons.Default.Warehouse,
                "Construction" to Icons.Default.Construction,
                "Healthcare" to Icons.Default.LocalHospital,
                "Retail" to Icons.Default.Store,
                "Manufacturing" to Icons.Default.PrecisionManufacturing,
                "Security" to Icons.Default.Security,
                "Hospitality" to Icons.Default.Hotel,
                "Transportation" to Icons.Default.DirectionsBus,
                "Agriculture" to Icons.Default.Agriculture,
                "IT Services" to Icons.Default.Computer,
                "Education" to Icons.Default.School,
                "Real Estate" to Icons.Default.Home,
                "Others" to Icons.Default.MoreHoriz
            )

            var selectedIndustries by remember {
                mutableStateOf(
                    industry.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                industriesWithIcons.chunked(2).forEach { rowIndustries ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowIndustries.forEach { (ind, icon) ->
                            FilterChip(
                                selected = selectedIndustries.contains(ind),
                                onClick = {
                                    selectedIndustries = if (selectedIndustries.contains(ind)) {
                                        selectedIndustries - ind
                                    } else {
                                        selectedIndustries + ind
                                    }
                                    onIndustryChange(selectedIndustries.joinToString(", "))
                                },
                                label = {
                                    Text(
                                        ind,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (selectedIndustries.contains(ind))
                                                FontWeight.SemiBold
                                            else
                                                FontWeight.Normal
                                        )
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                leadingIcon = {
                                    Icon(
                                        icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = selectedIndustries.contains(ind),
                                    borderWidth = if (selectedIndustries.contains(ind)) 1.5.dp else 1.dp,
                                    borderColor = if (selectedIndustries.contains(ind))
                                        Color(0xFF8B5CF6)
                                    else
                                        Color(0xFFE5E7EB)
                                ),
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = Color.White,
                                    selectedContainerColor = Color(0xFF8B5CF6).copy(alpha = 0.12f),
                                    labelColor = Color(0xFF6B7280),
                                    selectedLabelColor = Color(0xFF8B5CF6),
                                    iconColor = Color(0xFF9CA3AF),
                                    selectedLeadingIconColor = Color(0xFF8B5CF6)
                                )
                            )
                        }
                        if (rowIndustries.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            if (selectedIndustries.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF8B5CF6).copy(alpha = 0.05f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF8B5CF6),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            "${selectedIndustries.size} industr${if (selectedIndustries.size > 1) "ies" else "y"} selected",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFF8B5CF6),
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
            }
        }

        Column {
            var companySizeExpanded by remember { mutableStateOf(false) }
            val companySizes = listOf(
                "1-10 employees",
                "11-50 employees",
                "51-200 employees",
                "201-500 employees",
                "500+ employees"
            )

            ExposedDropdownMenuBox(
                expanded = companySizeExpanded,
                onExpandedChange = { companySizeExpanded = it }
            ) {
                OutlinedTextField(
                    value = companySize,
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("Company Size") },
                    placeholder = { Text("Select company size") },
                    leadingIcon = { Icon(Icons.Default.People, contentDescription = null) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = companySizeExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF8B5CF6),
                        unfocusedBorderColor = Color(0xFFE5E7EB)
                    )
                )

                ExposedDropdownMenu(
                    expanded = companySizeExpanded,
                    onDismissRequest = { companySizeExpanded = false }
                ) {
                    companySizes.forEach { size ->
                        DropdownMenuItem(
                            text = { Text(size) },
                            onClick = {
                                onCompanySizeChange(size)
                                companySizeExpanded = false
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.People,
                                    contentDescription = null,
                                    tint = Color(0xFF8B5CF6)
                                )
                            }
                        )
                    }
                }
            }
        }
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
        modifier = Modifier.padding(top = 20.dp),
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
        modifier = Modifier.padding(top = 20.dp),
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

// Previews
@Preview(showBackground = true, name = "Step 1: Company Info")
@Composable
private fun Step1CompanyInfoPreview() {
    MandatoryEmployerProfileSetupContent(
        companyName = "Awesome Inc.",
        contactEmail = "contact@awesome.com",
        contactPhone = "",
        businessAddress = "",
        industry = "IT Services",
        companySize = "11-50 employees",
        website = "",
        description = "",
        isLoading = false,
        errorMessage = null,
        currentStep = 1,
        totalSteps = 3,
        isCurrentStepValid = true,
        phoneError = null,
        emailError = null,
        onCompanyNameChange = {},
        onContactEmailChange = {},
        onContactPhoneChange = {},
        onBusinessAddressChange = {},
        onIndustryChange = {},
        onCompanySizeChange = {},
        onWebsiteChange = {},
        onDescriptionChange = {},
        onPreviousClick = {},
        onNextClick = {},
        onCompleteClick = {}
    )
}

@Preview(showBackground = true, name = "Step 2: Contact Details")
@Composable
private fun Step2ContactDetailsPreview() {
    MandatoryEmployerProfileSetupContent(
        companyName = "Awesome Inc.",
        contactEmail = "contact@awesome.com",
        contactPhone = "1234567890",
        businessAddress = "123 Main St, Anytown",
        industry = "IT Services",
        companySize = "11-50 employees",
        website = "",
        description = "",
        isLoading = false,
        errorMessage = null,
        currentStep = 2,
        totalSteps = 3,
        isCurrentStepValid = true,
        phoneError = null,
        emailError = null,
        onCompanyNameChange = {},
        onContactEmailChange = {},
        onContactPhoneChange = {},
        onBusinessAddressChange = {},
        onIndustryChange = {},
        onCompanySizeChange = {},
        onWebsiteChange = {},
        onDescriptionChange = {},
        onPreviousClick = {},
        onNextClick = {},
        onCompleteClick = {}
    )
}

@Preview(showBackground = true, name = "Step 3: Additional Info")
@Composable
private fun Step3AdditionalInfoPreview() {
    MandatoryEmployerProfileSetupContent(
        companyName = "Awesome Inc.",
        contactEmail = "contact@awesome.com",
        contactPhone = "1234567890",
        businessAddress = "123 Main St, Anytown",
        industry = "IT Services",
        companySize = "11-50 employees",
        website = "https://awesome.com",
        description = "We make awesome things!",
        isLoading = false,
        errorMessage = null,
        currentStep = 3,
        totalSteps = 3,
        isCurrentStepValid = true,
        phoneError = null,
        emailError = null,
        onCompanyNameChange = {},
        onContactEmailChange = {},
        onContactPhoneChange = {},
        onBusinessAddressChange = {},
        onIndustryChange = {},
        onCompanySizeChange = {},
        onWebsiteChange = {},
        onDescriptionChange = {},
        onPreviousClick = {},
        onNextClick = {},
        onCompleteClick = {}
    )
}
