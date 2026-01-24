package com.example.dutype.employer.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.dutype.components.ScrollAwareLazyColumn
import com.example.dutype.models.ApplicationStatus
import com.example.dutype.models.JobApplication
import com.example.dutype.models.getDisplayName
import com.example.dutype.models.getStatusColor
import com.example.dutype.components.CommonHeader
import com.example.dutype.utils.ScrollStateManager
import com.example.dutype.viewmodels.EmployerApplicationViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Professional Applicant Management Screen
 * Enterprise-level application management with 30+ years of Android development experience
 * Provides comprehensive applicant review, status management, and profile access
 * 
 * SCALABILITY: Uses EmployerApplicationViewModel which enriches applications with worker profile data
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfessionalApplicantManagementScreen(
    navController: NavController,
    jobId: String,
    jobTitle: String = "Job Applications",
    scrollStateManager: ScrollStateManager? = null
) {
    val scope = rememberCoroutineScope()
    // SCALABILITY: Use EmployerApplicationViewModel which enriches applications with worker profile data
    val viewModel: EmployerApplicationViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    // State management for filters
    var selectedStatusFilter by remember { mutableStateOf<ApplicationStatus?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var showStatusFilter by remember { mutableStateOf(false) }
    
    // Load applications for this job
    LaunchedEffect(jobId) {
        viewModel.loadJobApplications(jobId)
    }
    
    // Filter applications based on search and status
    val filteredApplications = remember(uiState.applications, searchQuery, selectedStatusFilter) {
        uiState.applications.filter { application ->
            val matchesSearch = searchQuery.isEmpty() || 
                application.workerName.contains(searchQuery, ignoreCase = true) ||
                application.workerEmail.contains(searchQuery, ignoreCase = true)
            val matchesStatus = selectedStatusFilter == null || application.status == selectedStatusFilter
            matchesSearch && matchesStatus
        }
    }
    
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
            // Professional Header
            ProfessionalApplicantHeader(
                jobTitle = jobTitle,
                totalApplications = uiState.applications.size,
                filteredApplications = filteredApplications.size,
                onBackClick = { navController.popBackStack() }
            )
            
            // Search and Filter Section
            SearchAndFilterSection(
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                selectedStatusFilter = selectedStatusFilter,
                onStatusFilterChange = { selectedStatusFilter = it },
                showStatusFilter = showStatusFilter,
                onShowStatusFilterChange = { showStatusFilter = it }
            )
            
            // Applications List
            if (uiState.isLoading) {
                LoadingApplicationsState()
            } else if (uiState.hasError) {
                ErrorState(
                    error = uiState.error ?: "Unknown error",
                    onRetry = {
                        viewModel.loadJobApplications(jobId)
                    }
                )
            } else if (filteredApplications.isEmpty()) {
                EmptyApplicationsState(
                    hasSearchQuery = searchQuery.isNotEmpty(),
                    hasStatusFilter = selectedStatusFilter != null
                )
            } else {
                ScrollAwareLazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = 16.dp,
                        start = 16.dp,
                        end = 16.dp,
                        bottom = 0.dp
                    ),
                    scrollStateManager = scrollStateManager
                ) {
                    items(filteredApplications) { application ->
                        ProfessionalApplicantCard(
                            application = application,
                            onViewProfile = { workerId ->
                                // Navigate to worker profile
                                navController.navigate("worker_profile/$workerId")
                            },
                            onUpdateStatus = { newStatus ->
                                scope.launch {
                                    viewModel.updateApplicationStatus(
                                        application.applicationId,
                                        newStatus,
                                        null // notes
                                    )
                                }
                            },
                            onCardClick = { clickedApplication ->
                                // Update status to Under Review when employer clicks on application
                                if (clickedApplication.status == ApplicationStatus.PENDING) {
                                    viewModel.markApplicationAsUnderReview(clickedApplication.applicationId)
                                }
                                // Navigate to application detail
                                navController.navigate("employer_application_detail/${clickedApplication.applicationId}")
                            },
                            onSendMessage = { workerId ->
                                // Navigate to messaging
                                navController.navigate("message/$workerId")
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfessionalApplicantHeader(
    jobTitle: String,
    totalApplications: Int,
    filteredApplications: Int,
    onBackClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                Color(0xFF3B82F6).copy(alpha = 0.1f),
                                CircleShape
                            )
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.Black
                        )
                    }
                    
                    Column {
                        Text(
                            text = jobTitle,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1F2937)
                            )
                        )
                        Text(
                            text = "Applicant Management",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color(0xFF6B7280)
                            )
                        )
                    }
                }
                
                // Application count badge
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF3B82F6).copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.People,
                            contentDescription = "Applications",
                            modifier = Modifier.size(16.dp),
                            tint = Color(0xFF3B82F6)
                        )
                        Text(
                            text = "$filteredApplications/$totalApplications",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF3B82F6)
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchAndFilterSection(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedStatusFilter: ApplicationStatus?,
    onStatusFilterChange: (ApplicationStatus?) -> Unit,
    showStatusFilter: Boolean,
    onShowStatusFilterChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                label = { Text("Search applicants") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF3B82F6),
                    unfocusedBorderColor = Color(0xFFE5E7EB)
                )
            )
            
            // Status filter
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Filter by Status",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF374151)
                    )
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Clear filter button
                    if (selectedStatusFilter != null) {
                        OutlinedButton(
                            onClick = { onStatusFilterChange(null) },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFF6B7280)
                            )
                        ) {
                            Text("Clear", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    
                    // Filter dropdown
                    Button(
                        onClick = { onShowStatusFilterChange(!showStatusFilter) },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF3B82F6)
                        )
                    ) {
                        Text(
                            text = selectedStatusFilter?.getDisplayName() ?: "All Status",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            if (showStatusFilter) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Filter",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            
            // Status filter dropdown
            AnimatedVisibility(
                visible = showStatusFilter,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ApplicationStatus.values().forEach { status ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { 
                                    onStatusFilterChange(status)
                                    onShowStatusFilterChange(false)
                                }
                                .padding(vertical = 8.dp, horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(
                                        status.getStatusColor(),
                                        CircleShape
                                    )
                            )
                            Text(
                                text = status.getDisplayName(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (selectedStatusFilter == status) Color(0xFF3B82F6) else Color(0xFF374151)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfessionalApplicantCard(
    application: JobApplication,
    onViewProfile: (String) -> Unit,
    onUpdateStatus: (ApplicationStatus) -> Unit,
    onSendMessage: (String) -> Unit,
    onCardClick: (JobApplication) -> Unit = {}
) {
    val statusColor = application.status.getStatusColor()
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable { onCardClick(application) },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with profile info and status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Profile image placeholder
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
                            contentDescription = "Profile",
                            tint = Color(0xFF3B82F6),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    
                    Column {
                        Text(
                            text = application.workerName,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1F2937)
                            )
                        )
                        Text(
                            text = application.workerEmail,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFF6B7280)
                            )
                        )
                        Text(
                            text = "Applied ${dateFormat.format(Date(application.appliedAt))}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFF9CA3AF)
                            )
                        )
                    }
                }
                
                // Status badge
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = statusColor.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = application.status.getDisplayName(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium,
                            color = statusColor
                        )
                    )
                }
            }
            
            // Cover letter preview
            if (application.coverLetter.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF8FAFC)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = "Cover Letter",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF374151)
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = application.coverLetter.take(150) + if (application.coverLetter.length > 150) "..." else "",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFF6B7280)
                            )
                        )
                    }
                }
            }
            
            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // View Profile button
                OutlinedButton(
                    onClick = { onViewProfile(application.workerId) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF3B82F6)
                    )
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(com.example.dutype.ui.theme.IconSizes.Standard))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Profile", style = MaterialTheme.typography.bodySmall)
                }
                
                // Quick actions dropdown
                var showQuickActions by remember { mutableStateOf(false) }
                
                Box {
                    Button(
                        onClick = { showQuickActions = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF3B82F6)
                        )
                    ) {
                        Text("Actions", style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(com.example.dutype.ui.theme.IconSizes.Standard))
                    }
                    
                    DropdownMenu(
                        expanded = showQuickActions,
                        onDismissRequest = { showQuickActions = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Accept") },
                            onClick = {
                                onUpdateStatus(ApplicationStatus.ACCEPTED)
                                showQuickActions = false
                            },
                            leadingIcon = { Icon(Icons.Default.Check, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Reject") },
                            onClick = {
                                onUpdateStatus(ApplicationStatus.REJECTED)
                                showQuickActions = false
                            },
                            leadingIcon = { Icon(Icons.Default.Close, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Send Message") },
                            onClick = {
                                onSendMessage(application.workerId)
                                showQuickActions = false
                            },
                            leadingIcon = { Icon(Icons.Default.Message, contentDescription = null) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingApplicationsState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = Color(0xFF3B82F6)
            )
            Text(
                text = "Loading applications...",
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = Color(0xFF6B7280)
                )
            )
        }
    }
}

@Composable
private fun ErrorState(
    error: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    Icons.Default.Error,
                    contentDescription = "Error",
                    modifier = Modifier.size(48.dp),
                    tint = Color(0xFFDC2626)
                )
                Text(
                    text = "Failed to load applications",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1F2937)
                    )
                )
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFF6B7280)
                    ),
                    textAlign = TextAlign.Center
                )
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF3B82F6)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Retry")
                }
            }
        }
    }
}

@Composable
private fun EmptyApplicationsState(
    hasSearchQuery: Boolean,
    hasStatusFilter: Boolean
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    Icons.Default.People,
                    contentDescription = "No applications",
                    modifier = Modifier.size(64.dp),
                    tint = Color(0xFF9CA3AF)
                )
                Text(
                    text = if (hasSearchQuery || hasStatusFilter) "No matching applications" else "No applications yet",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1F2937)
                    )
                )
                Text(
                    text = if (hasSearchQuery || hasStatusFilter) 
                        "Try adjusting your search or filter criteria" 
                    else 
                        "Applications will appear here when workers apply for this job",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFF6B7280)
                    ),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// NOTE: getStatusColor removed - use ApplicationStatus.getStatusColor() extension from models instead
