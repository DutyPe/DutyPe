package com.example.partimes.employer.screens.postjob

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.partimes.api.employer.JobPostingApiClient
import com.example.partimes.employer.components.ContactSection
import com.example.partimes.employer.components.JobDescriptionSection
import com.example.partimes.employer.components.JobPreviewDialog
import com.example.partimes.employer.components.JobSummaryCard
import com.example.partimes.employer.components.JobTitleSection
import com.example.partimes.employer.components.LocationSection
import com.example.partimes.employer.components.PaymentSection
import com.example.partimes.employer.components.PerksSelectionGrid
import com.example.partimes.employer.components.StepHeader
import com.example.partimes.employer.components.VacanciesSection
import com.example.partimes.employer.components.WorkScheduleSection
import com.example.partimes.employer.models.JobPostingModel
import com.example.partimes.employer.models.enums.JobCategory
import com.example.partimes.employer.models.enums.JobPerk
import com.example.partimes.employer.models.enums.JobUrgency
import com.example.partimes.employer.models.enums.PayType
import com.example.partimes.employer.models.enums.ShiftTiming
import com.example.partimes.utils.LocationService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostJobScreen(
    navController: NavController,
    employerId: String? = "emp_001",
    onJobPosted: (() -> Unit)? = null // Add callback for when job is posted
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val locationService = remember { LocationService(context) }

    // Step management
    var currentStep by remember { mutableStateOf(1) }
    val totalSteps = 4

    // Form state matching JobPostingModel
    var title by remember { mutableStateOf("") }
    var payAmount by remember { mutableStateOf("") }
    var payType by remember { mutableStateOf(PayType.DAILY) }
    var location by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var contactNumber by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(JobCategory.COOK) }
    var shiftTiming by remember { mutableStateOf(ShiftTiming.FLEXIBLE) }
    var urgency by remember { mutableStateOf(JobUrgency.FLEXIBLE) }
    var selectedPerks by remember { mutableStateOf<Set<JobPerk>>(emptySet()) }
    var vacancies by remember { mutableStateOf("1") }
    var employerName by remember { mutableStateOf("") }

    // UI state
    var isLoading by remember { mutableStateOf(false) }
    var isLoadingLocation by remember { mutableStateOf(false) }
    var locationError by remember { mutableStateOf<String?>(null) }
    var showPreview by remember { mutableStateOf(false) }

    // Location permission launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            isLoadingLocation = true
            locationError = null
            scope.launch {
                try {
                    val locationInfo = locationService.getCurrentLocation()
                    if (locationInfo != null) {
                        location = locationInfo.address
                    } else {
                        locationError = "Unable to get current location"
                    }
                } catch (e: Exception) {
                    locationError = "Error getting location: ${e.message}"
                } finally {
                    isLoadingLocation = false
                }
            }
        } else {
            locationError = "Location permission denied"
        }
    }

    // Validation functions for each step
    fun validateStep(step: Int): Boolean {
        return when (step) {
            1 -> title.isNotBlank() && description.isNotBlank()
            2 -> payAmount.isNotBlank() && location.isNotBlank()
            3 -> contactNumber.isNotBlank()
            4 -> true
            else -> false
        }
    }

    // Create job posting function
    fun createJobPosting(): JobPostingModel {
        return JobPostingModel(
            title = title,
            payAmount = payAmount,
            payType = payType,
            location = location,
            description = description,
            contactNumber = contactNumber,
            category = category,
            shiftTiming = shiftTiming,
            urgency = urgency,
            perks = selectedPerks.toList(),
            vacancies = vacancies.toIntOrNull() ?: 1,
            employerId = employerId ?: "emp_001",
            employerName = employerName,
            postedTime = System.currentTimeMillis()
        )
    }

    // Submit job function
    fun submitJob() {
        if (!validateStep(4)) return

        isLoading = true
        scope.launch {
            try {
                val jobPosting = createJobPosting()
                withContext(Dispatchers.IO) {
                    JobPostingApiClient.api.postJob(jobPosting)
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Job posted successfully!", Toast.LENGTH_SHORT).show()
                    // Call the callback if provided (for tabbed interface)
                    onJobPosted?.invoke()
                    // Navigate back if no callback provided (standalone screen)
                    if (onJobPosted == null) {
                        navController.popBackStack()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error posting job: ${e.message}", Toast.LENGTH_LONG).show()
                }
            } finally {
                isLoading = false
            }
        }
    }

    // Job Preview Dialog
    if (showPreview) {
        JobPreviewDialog(
            jobPosting = createJobPosting(),
            onDismiss = { showPreview = false },
            onConfirmPost = {
                showPreview = false
                submitJob()
            }
        )
    }

    Scaffold(

        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (currentStep > 1) {
                        OutlinedButton(
                            onClick = { currentStep-- },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Previous")
                        }
                    } else {
                        OutlinedButton(
                            onClick = { navController.popBackStack() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }
                    }

                    if (currentStep < totalSteps) {
                        Button(
                            onClick = { currentStep++ },
                            modifier = Modifier.weight(2f),
                            enabled = validateStep(currentStep)
                        ) {
                            Text("Next")
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                        }
                    } else {
                        // On last step, show Preview and Post buttons
                        OutlinedButton(
                            onClick = { showPreview = true },
                            modifier = Modifier.weight(1f),
                            enabled = validateStep(1) && validateStep(2) && validateStep(3)
                        ) {
                            Icon(Icons.Default.Preview, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Preview")
                        }

                        Button(
                            onClick = { submitJob() },
                            modifier = Modifier.weight(1f),
                            enabled = !isLoading && validateStep(1) && validateStep(2) && validateStep(3)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.White
                                )
                            } else {
                                Icon(Icons.Default.Check, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Post")
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF2193b0), // Clean sky blue  
                            Color(0xFF6dd5ed), // Soft light blue
                            Color(0xFFFFFFFF)  // Pure white     
                        ),
                        startY = 0f,
                        endY = 900f
                    )
                )
                .padding(paddingValues)
        ) {
            // Progress indicator
            LinearProgressIndicator(
                progress = { currentStep.toFloat() / totalSteps },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.primary
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = 16.dp,
                    start = 16.dp,
                    end = 16.dp,
                    bottom = 0.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (currentStep) {
                    1 -> {
                        item {
                            StepHeader("Basic Information", "Tell us about the job position")
                        }

                        item {
                            JobTitleSection(
                                title = title,
                                onTitleChange = { title = it },
                                category = category,
                                onCategoryChange = { category = it }
                            )
                        }

                        item {
                            JobDescriptionSection(
                                description = description,
                                onDescriptionChange = { description = it }
                            )
                        }
                    }

                    2 -> {
                        item {
                            StepHeader("Payment & Location", "Set compensation and work location")
                        }

                        item {
                            PaymentSection(
                                payAmount = payAmount,
                                onPayAmountChange = { payAmount = it },
                                payType = payType,
                                onPayTypeChange = { payType = it }
                            )
                        }

                        item {
                            LocationSection(
                                location = location,
                                onLocationChange = { location = it },
                                isLoadingLocation = isLoadingLocation,
                                locationError = locationError,
                                onLocationButtonClick = {
                                    if (locationService.hasLocationPermission()) {
                                        isLoadingLocation = true
                                        locationError = null
                                        scope.launch {
                                            try {
                                                val locationInfo = locationService.getCurrentLocation()
                                                if (locationInfo != null) {
                                                    location = locationInfo.address
                                                } else {
                                                    locationError = "Unable to get current location"
                                                }
                                            } catch (_: Exception) {
                                                locationError = "Error getting location"
                                            } finally {
                                                isLoadingLocation = false
                                            }
                                        }
                                    } else {
                                        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                                    }
                                }
                            )
                        }
                        item {
                            VacanciesSection(
                                vacancies = vacancies,
                                onVacanciesChange = { vacancies = it }
                            )
                        }
                    }

                    3 -> {
                        item {
                            StepHeader("Contact Information", "How candidates can reach you")
                        }

                        item {
                            ContactSection(
                                contactNumber = contactNumber,
                                onContactNumberChange = { contactNumber = it },
                                employerName = employerName,
                                onEmployerNameChange = { employerName = it }
                            )
                        }
                    }

                    4 -> {
                        item {
                            StepHeader("Additional Details", "Optional settings and preferences")
                        }

                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color.White)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Text(
                                        text = "Work Schedule & Urgency",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))

                                    WorkScheduleSection(
                                        selectedShift = shiftTiming,
                                        onShiftSelected = { shiftTiming = it },
                                        selectedUrgency = urgency,
                                        onUrgencySelected = { urgency = it }
                                    )
                                }
                            }
                        }

                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color.White)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Text(
                                        text = "Perks & Benefits",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))

                                    PerksSelectionGrid(
                                        selectedPerks = selectedPerks,
                                        onPerksChanged = { selectedPerks = it }
                                    )
                                }
                            }
                        }

                        item {
                            JobSummaryCard(
                                title = title,
                                category = category,
                                payAmount = payAmount,
                                payType = payType,
                                location = location,
                                vacancies = vacancies,
                                urgency = urgency,
                                selectedPerks = selectedPerks,
                                shiftTiming = shiftTiming,
                                description = description
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }
    }
}