package com.example.dutype.employer.screens

import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.dutype.components.SelfieCaptureStep
import com.example.dutype.models.UserRole
import com.example.dutype.navigation.Routes
import com.example.dutype.services.NotificationService
import com.example.dutype.viewmodels.ProfileCompletionViewModel
import com.example.dutype.utils.ValidationUtils
import com.example.dutype.services.FCMTokenManager
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MandatoryEmployerProfileSetupScreen(
    navController: NavController,
    viewModel: ProfileCompletionViewModel = hiltViewModel()
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    // NotificationService for sending profile completion notification
    val notificationService = remember {
        NotificationService(context, FirebaseFirestore.getInstance())
    }
    
    // FCMTokenManager for registering FCM token with role
    val fcmTokenManager = remember { FCMTokenManager() }

    // Form state
    var companyName by remember { mutableStateOf("") }
    var contactEmail by remember { mutableStateOf("") }
    var contactPhone by remember { mutableStateOf("") }
    var businessAddress by remember { mutableStateOf("") }
    var industry by remember { mutableStateOf("") }
    var companySize by remember { mutableStateOf("") }
    var dateOfBirth by remember { mutableStateOf("") }
    
    // Selfie state
    var selfieUri by remember { mutableStateOf<Uri?>(null) }
    var selfieUrl by remember { mutableStateOf<String?>(null) }
    var isUploadingSelfie by remember { mutableStateOf(false) }
    var selfieError by remember { mutableStateOf<String?>(null) }

    // UI state
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var currentStep by remember { mutableStateOf(1) }
    var showValidationErrors by remember { mutableStateOf(false) }
    var gender by remember { mutableStateOf("") }
    val totalSteps = 3  // Removed additional info step (website/description)

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
            (contactEmail.isBlank() || ValidationUtils.isValidEmail(contactEmail)) && gender.isNotBlank() && 
            dateOfBirth.isNotBlank() && ValidationUtils.isValidDateOfBirth(dateOfBirth)
    val isStep3Valid = selfieUri != null  // Selfie is mandatory (now step 3)

    var phoneError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var companyNameError by remember { mutableStateOf<String?>(null) }
    var industryError by remember { mutableStateOf<String?>(null) }
    var addressError by remember { mutableStateOf<String?>(null) }
    var genderError by remember { mutableStateOf<String?>(null) }
    var dateOfBirthError by remember { mutableStateOf<String?>(null) }

    // Update errors only when showValidationErrors is true
    LaunchedEffect(contactPhone, contactEmail, companyName, industry, businessAddress, gender, dateOfBirth, showValidationErrors) {
        if (showValidationErrors) {
            phoneError = when {
                contactPhone.isBlank() -> "Phone number is required"
                !ValidationUtils.isValidIndianPhoneNumber(contactPhone) -> "Enter a valid 10-digit phone number"
                else -> null
            }
            emailError = when {
                contactEmail.isNotBlank() && !ValidationUtils.isValidEmail(contactEmail) -> "Enter a valid email address"
                else -> null
            }
            companyNameError = if (companyName.isBlank()) "Company name is required" else null
            industryError = if (industry.isBlank()) "Please select at least one industry" else null
            addressError = if (businessAddress.isBlank()) "Work location is required" else null
            genderError = if (gender.isBlank()) "Please select your gender" else null
            dateOfBirthError = ValidationUtils.getDateOfBirthError(dateOfBirth)
        } else {
            phoneError = null
            emailError = null
            companyNameError = null
            industryError = null
            addressError = null
            genderError = null
            dateOfBirthError = null
        }
    }

    val isCurrentStepValid = when (currentStep) {
        1 -> isStep1Valid
        2 -> isStep2Valid
        3 -> isStep3Valid  // Selfie step (was step 4)
        else -> false
    }

    fun handleCompletion() {
        scope.launch {
            isLoading = true
            errorMessage = null
            try {
                val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                if (currentUser != null) {
                    // First upload selfie if available
                    var uploadedSelfieUrl: String? = null
                    if (selfieUri != null) {
                        isUploadingSelfie = true
                        val uploadResult = viewModel.uploadProfileImage(
                            selfieUri!!,
                            currentUser.uid,
                            "EMPLOYER"
                        )
                        uploadResult.fold(
                            onSuccess = { url ->
                                uploadedSelfieUrl = url
                                selfieUrl = url
                                Timber.d("📸 Employer selfie uploaded: $url")
                            },
                            onFailure = { e ->
                                Timber.e(e, "📸 Failed to upload employer selfie")
                                // Continue without selfie URL if upload fails
                            }
                        )
                        isUploadingSelfie = false
                    }
                    
                    val employerProfileData = mutableMapOf(
                        "companyName" to companyName,
                        "fullName" to companyName,  // Also save as fullName for profile completion check
                        "contactEmail" to contactEmail,
                        "contactPhone" to contactPhone,
                        "phone" to contactPhone,  // Also save as phone for consistency
                        "businessAddress" to businessAddress,
                        "industry" to industry,
                        "companySize" to companySize,
                        "gender" to gender,
                        "dateOfBirth" to dateOfBirth,
                        "role" to "EMPLOYER",
                        "profileCompleted" to true,
                        "completedAt" to System.currentTimeMillis()
                    )
                    
                    // Add selfie URL if uploaded
                    if (uploadedSelfieUrl != null) {
                        employerProfileData["profileImageUrl"] = uploadedSelfieUrl!!
                    }
                    
                    viewModel.saveEmployerProfileData(employerProfileData)
                }
                
                // Save role to local DataStore so app knows which home to navigate to on reopen
                viewModel.updateUserRole(UserRole.EMPLOYER)
                viewModel.markProfileComplete(UserRole.EMPLOYER)
                viewModel.markProfileSetupAsShown(UserRole.EMPLOYER)
                
                // Send profile completion notification (welcome message)
                val notificationUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                if (notificationUser != null) {
                    try {
                        notificationService.sendProfileCompleteNotification(
                            userName = companyName,
                            userId = notificationUser.uid,
                            userRole = "EMPLOYER"
                        )
                        Timber.d("📬 Profile completion notification sent for employer")
                        
                        // Register FCM token with role for push notifications
                        fcmTokenManager.registerTokenWithRole("EMPLOYER")
                        Timber.d("📬 FCM token registered with EMPLOYER role")
                    } catch (e: Exception) {
                        Timber.e(e, "📬 Failed to send profile completion notification or register FCM")
                    }
                }

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
        gender = gender,
        dateOfBirth = dateOfBirth,
        selfieUri = selfieUri,
        isUploadingSelfie = isUploadingSelfie,
        selfieError = selfieError,
        isLoading = isLoading,
        errorMessage = errorMessage,
        currentStep = currentStep,
        totalSteps = totalSteps,
        isCurrentStepValid = isCurrentStepValid,
        showValidationErrors = showValidationErrors,
        phoneError = if (showValidationErrors) phoneError else null,
        emailError = if (showValidationErrors) emailError else null,
        companyNameError = if (showValidationErrors) companyNameError else null,
        industryError = if (showValidationErrors) industryError else null,
        addressError = if (showValidationErrors) addressError else null,
        genderError = if (showValidationErrors) genderError else null,
        dateOfBirthError = if (showValidationErrors) dateOfBirthError else null,
        onCompanyNameChange = { companyName = it },
        onContactEmailChange = { contactEmail = it },
        onContactPhoneChange = { contactPhone = it },
        onBusinessAddressChange = { businessAddress = it },
        onIndustryChange = { industry = it },
        onCompanySizeChange = { companySize = it },
        onGenderChange = { gender = it },
        onDateOfBirthChange = { dateOfBirth = it },
        onSelfieCapture = { uri ->
            selfieUri = uri
            selfieError = null
            Timber.d("📸 Employer selfie captured: $uri")
        },
        onSelfieRetake = {
            selfieUri = null
            selfieUrl = null
        },
        onPreviousClick = { currentStep-- },
        onNextClick = {
            showValidationErrors = true
            if (isCurrentStepValid) {
                currentStep++
                showValidationErrors = false
            }
        },
        onCompleteClick = {
            showValidationErrors = true
            if (isCurrentStepValid) {
                handleCompletion()
            }
        }
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
    gender: String,
    dateOfBirth: String,
    selfieUri: Uri?,
    isUploadingSelfie: Boolean,
    selfieError: String?,
    isLoading: Boolean,
    errorMessage: String?,
    currentStep: Int,
    totalSteps: Int,
    isCurrentStepValid: Boolean,
    showValidationErrors: Boolean,
    phoneError: String?,
    emailError: String?,
    companyNameError: String?,
    industryError: String?,
    addressError: String?,
    genderError: String?,
    dateOfBirthError: String?,
    onCompanyNameChange: (String) -> Unit,
    onContactEmailChange: (String) -> Unit,
    onContactPhoneChange: (String) -> Unit,
    onBusinessAddressChange: (String) -> Unit,
    onIndustryChange: (String) -> Unit,
    onCompanySizeChange: (String) -> Unit,
    onGenderChange: (String) -> Unit,
    onDateOfBirthChange: (String) -> Unit,
    onSelfieCapture: (Uri) -> Unit,
    onSelfieRetake: () -> Unit,
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
                                industry = industry,
                                companySize = companySize,
                                companyNameError = companyNameError,
                                industryError = industryError,
                                onCompanyNameChange = onCompanyNameChange,
                                onIndustryChange = onIndustryChange,
                                onCompanySizeChange = onCompanySizeChange
                            )
                        }

                        if (currentStep == 2) {
                            ContactDetailsStep(
                                contactPhone = contactPhone,
                                businessAddress = businessAddress,
                                contactEmail = contactEmail,
                                gender = gender,
                                dateOfBirth = dateOfBirth,
                                phoneError = phoneError,
                                emailError = emailError,
                                addressError = addressError,
                                genderError = genderError,
                                dateOfBirthError = dateOfBirthError,
                                onContactPhoneChange = onContactPhoneChange,
                                onBusinessAddressChange = onBusinessAddressChange,
                                onContactEmailChange = onContactEmailChange,
                                onGenderChange = onGenderChange,
                                onDateOfBirthChange = onDateOfBirthChange
                            )
                        }

                        // Step 3: Selfie Capture (Mandatory)
                        if (currentStep == 3) {
                            SelfieCaptureStep(
                                selfieUri = selfieUri,
                                isUploading = isUploadingSelfie,
                                selfieError = if (showValidationErrors && selfieUri == null) "Please take a selfie to continue" else selfieError,
                                isEmployer = true,
                                onSelfieCapture = onSelfieCapture,
                                onRetake = onSelfieRetake
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
                            modifier = Modifier.size(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFF3B82F6)
                            ),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Go back", modifier = Modifier.size(24.dp))
                        }
                        Spacer(modifier = Modifier.weight(1f))
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
    industry: String,
    companySize: String,
    companyNameError: String?,
    industryError: String?,
    onCompanyNameChange: (String) -> Unit,
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

        Column {
            OutlinedTextField(
                value = companyName,
                onValueChange = onCompanyNameChange,
                label = { Text("Company Name *") },
                placeholder = { Text("Enter your company name") },
                leadingIcon = { Icon(Icons.Default.Business, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                isError = companyNameError != null,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (companyNameError != null) Color(0xFFEF4444) else Color(0xFF3B82F6),
                    unfocusedBorderColor = if (companyNameError != null) Color(0xFFEF4444) else Color(0xFFE5E7EB),
                    errorBorderColor = Color(0xFFEF4444)
                )
            )
            if (companyNameError != null) {
                Text(
                    text = companyNameError,
                    color = Color(0xFFEF4444),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                )
            }
        }

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContactDetailsStep(
    contactPhone: String,
    businessAddress: String,
    contactEmail: String,
    gender: String,
    dateOfBirth: String,
    phoneError: String?,
    emailError: String?,
    addressError: String?,
    genderError: String?,
    dateOfBirthError: String?,
    onContactPhoneChange: (String) -> Unit,
    onBusinessAddressChange: (String) -> Unit,
    onContactEmailChange: (String) -> Unit,
    onGenderChange: (String) -> Unit,
    onDateOfBirthChange: (String) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val locationService = remember { com.example.dutype.utils.LocationService(context) }
    var isFetchingLocation by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    
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
                onValueChange = { newValue ->
                    // Only allow digits and limit to 10 characters
                    if (newValue.all { it.isDigit() } && newValue.length <= 10) {
                        onContactPhoneChange(newValue)
                    }
                },
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

        // Work Location with Fetch button
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Work Location *",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1F2937)
                    )
                )
                
                Button(
                    onClick = {
                        isFetchingLocation = true
                        if (locationService.hasLocationPermission()) {
                            coroutineScope.launch {
                                // Use getHighAccuracyLocation for GPS-level precision (5-10m)
                                val locationInfo = locationService.getHighAccuracyLocation(
                                    timeoutMs = 15000L,
                                    minAccuracyMeters = 10f
                                )
                                if (locationInfo != null) {
                                    // Use detailed full address for business profile
                                    onBusinessAddressChange(locationInfo.getFullAddress())
                                }
                                isFetchingLocation = false
                            }
                        } else {
                            isFetchingLocation = false
                        }
                    },
                    modifier = Modifier.height(36.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF111111)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Icon(
                        Icons.Default.MyLocation,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isFetchingLocation) "Fetching..." else "Fetch",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            }
            
            OutlinedTextField(
                value = businessAddress,
                onValueChange = onBusinessAddressChange,
                placeholder = { Text("Enter your work location") },
                leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3,
                isError = addressError != null,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (addressError != null) Color(0xFFDC2626) else Color(0xFF3B82F6),
                    unfocusedBorderColor = if (addressError != null) Color(0xFFDC2626) else Color(0xFFE5E7EB),
                    errorBorderColor = Color(0xFFDC2626)
                )
            )
            if (addressError != null) {
                Text(
                    text = addressError,
                    color = Color(0xFFDC2626),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                )
            }
        }
        
        // Gender Selection
        Column {
            Text(
                text = "Gender *",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1F2937)
                ),
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            val genderOptions = listOf("Male", "Female", "Other")
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                genderOptions.forEach { genderOption ->
                    Row(
                        modifier = Modifier
                            .selectable(
                                selected = (genderOption == gender),
                                onClick = { onGenderChange(genderOption) },
                                role = Role.RadioButton
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (genderOption == gender),
                            onClick = null,
                            colors = RadioButtonDefaults.colors(
                                selectedColor = Color(0xFF8B5CF6),
                                unselectedColor = Color(0xFFD1D5DB)
                            ),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = genderOption,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 8.dp),
                            color = Color(0xFF1F2937)
                        )
                    }
                }
            }
            
            if (genderError != null) {
                Text(
                    text = genderError,
                    color = Color(0xFFDC2626),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                )
            }
        }
        
        // Date of Birth
        Column {
            var showDatePicker by remember { mutableStateOf(false) }
            val datePickerState = rememberDatePickerState()
            
            Text(
                text = "Date of Birth *",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1F2937)
                ),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            OutlinedTextField(
                value = dateOfBirth,
                onValueChange = { },
                readOnly = true,
                placeholder = { Text("Select your date of birth") },
                leadingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.DateRange, contentDescription = "Select date")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true },
                isError = dateOfBirthError != null,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (dateOfBirthError != null) Color(0xFFDC2626) else Color(0xFF3B82F6),
                    unfocusedBorderColor = if (dateOfBirthError != null) Color(0xFFDC2626) else Color(0xFFE5E7EB),
                    errorBorderColor = Color(0xFFDC2626)
                )
            )
            
            if (dateOfBirthError != null) {
                Text(
                    text = dateOfBirthError,
                    color = Color(0xFFDC2626),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                )
            }
            
            if (showDatePicker) {
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                datePickerState.selectedDateMillis?.let { millis ->
                                    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                                    onDateOfBirthChange(dateFormat.format(Date(millis)))
                                }
                                showDatePicker = false
                            }
                        ) {
                            Text("OK")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDatePicker = false }) {
                            Text("Cancel")
                        }
                    }
                ) {
                    DatePicker(state = datePickerState)
                }
            }
        }
    }
}
