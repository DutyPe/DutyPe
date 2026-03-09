package com.example.dutype.employer.screens.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.dutype.components.CommonHeader
import com.example.dutype.ui.theme.AppTypography
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployerAddressManagementScreen(
    navController: NavController,
    onStatusBarColorChange: (Color) -> Unit
) {
    // Set white status bar
    onStatusBarColorChange(Color.White)
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // LocationService accessed via FirestoreJobViewModel (proper DI pattern)
    val jobViewModel: com.example.dutype.viewmodels.FirestoreJobViewModel = hiltViewModel()
    val locationService = jobViewModel.locationService
    
    // WorkLocationManager for saving work locations to database
    val workerViewModel: com.example.dutype.viewmodels.WorkerHomeViewModel = hiltViewModel()
    val workLocationManager = workerViewModel.workLocationManager
    
    // Employer theme color
    val employerBlue = Color(0xFF3B82F6)
    
    // Office addresses state - Load from database
    var officeAddresses by remember { mutableStateOf<List<OfficeAddress>>(emptyList()) }
    var isLoadingAddresses by remember { mutableStateOf(true) }
    
    // Load saved work locations from database
    androidx.compose.runtime.LaunchedEffect(Unit) {
        scope.launch {
            val result = workLocationManager.getWorkLocations()
            result.onSuccess { locations ->
                officeAddresses = locations.map { workLocation ->
                    OfficeAddress(
                        id = workLocation.id,
                        name = workLocation.label,
                        address = workLocation.address,
                        isDefault = officeAddresses.isEmpty(), // First one is default
                        isActive = true,
                        latitude = workLocation.latitude,
                        longitude = workLocation.longitude
                    )
                }
                isLoadingAddresses = false
                timber.log.Timber.d("📍 AddressManagement: Loaded ${locations.size} saved addresses")
            }.onFailure { error ->
                timber.log.Timber.e(error, "❌ AddressManagement: Failed to load addresses")
                isLoadingAddresses = false
            }
        }
    }
    
    // Add address form state
    var officeName by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var fullAddress by remember { mutableStateOf("") }
    var locationLatitude by remember { mutableStateOf(0.0) }
    var locationLongitude by remember { mutableStateOf(0.0) }
    var isLoadingLocation by remember { mutableStateOf(false) }
    var locationError by remember { mutableStateOf<String?>(null) }
    var isAddingAddress by remember { mutableStateOf(false) }
    
    // Autocomplete state
    var placeSuggestions by remember { mutableStateOf<List<com.example.dutype.models.PlaceSuggestion>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var showSuggestions by remember { mutableStateOf(false) }
    
    // Edit state
    var editingAddressId by remember { mutableStateOf<String?>(null) }
    
    // Location permission launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            isLoadingLocation = true
            locationError = null
            scope.launch {
                try {
                    // Use getHighAccuracyLocation for GPS-level precision (5-10m)
                    val locationInfo = locationService.getHighAccuracyLocation(
                        timeoutMs = 15000L,
                        minAccuracyMeters = 10f
                    )
                    if (locationInfo != null) {
                        // Use detailed full address
                        fullAddress = locationInfo.getFullAddress()
                        searchQuery = locationInfo.getFullAddress()
                        locationLatitude = locationInfo.latitude
                        locationLongitude = locationInfo.longitude
                        timber.log.Timber.d("📍 AddressManagement: Got location - lat: $locationLatitude, lon: $locationLongitude")
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Common Header
        CommonHeader(
            title = "Manage Addresses",
            navController = navController
        )
        
        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Add New Address Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Add New Address",
                        style = AppTypography.sectionHeader.copy(
                            color = Color(0xFF1F2937)
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Office Name Field
                    OutlinedTextField(
                        value = officeName,
                        onValueChange = { officeName = it },
                        label = { Text("Office Name") },
                        placeholder = { Text("e.g., Main Office, Branch") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = employerBlue,
                            focusedLabelColor = employerBlue
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Search/Address Field with location button and autocomplete
                    Column(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { query ->
                                searchQuery = query
                                fullAddress = query
                                
                                // Trigger autocomplete search
                                if (query.length >= 3) {
                                    isSearching = true
                                    showSuggestions = true
                                    scope.launch {
                                        try {
                                            val suggestions = locationService.searchPlaces(query)
                                            placeSuggestions = suggestions
                                            isSearching = false
                                        } catch (e: Exception) {
                                            timber.log.Timber.e(e, "❌ Failed to search places")
                                            isSearching = false
                                            placeSuggestions = emptyList()
                                        }
                                    }
                                } else {
                                    showSuggestions = false
                                    placeSuggestions = emptyList()
                                }
                            },
                            label = { Text("Search or Enter Address") },
                            placeholder = { Text("Search location or enter address") },
                            singleLine = false,
                            maxLines = 3,
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = {
                                if (isSearching) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = employerBlue
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.Search,
                                        contentDescription = "Search",
                                        tint = Color(0xFF6B7280)
                                    )
                                }
                            },
                            trailingIcon = {
                                IconButton(
                                    onClick = {
                                        locationPermissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
                                    },
                                    enabled = !isLoadingLocation
                                ) {
                                    if (isLoadingLocation) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            strokeWidth = 2.dp,
                                            color = employerBlue
                                        )
                                    } else {
                                        Icon(
                                            Icons.Default.MyLocation,
                                            contentDescription = "Use Current Location",
                                            tint = employerBlue
                                        )
                                    }
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = employerBlue,
                                focusedLabelColor = employerBlue
                            )
                        )
                        
                        // Autocomplete suggestions dropdown
                        if (showSuggestions && placeSuggestions.isNotEmpty()) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                            ) {
                                LazyColumn(
                                    modifier = Modifier.height(200.dp)
                                ) {
                                    items(placeSuggestions) { suggestion ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    // User selected a suggestion
                                                    searchQuery = suggestion.description
                                                    fullAddress = suggestion.description
                                                    locationLatitude = suggestion.latitude
                                                    locationLongitude = suggestion.longitude
                                                    showSuggestions = false
                                                    placeSuggestions = emptyList()
                                                    timber.log.Timber.d("📍 Selected place: ${suggestion.description}")
                                                }
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Default.LocationOn,
                                                contentDescription = null,
                                                tint = Color(0xFF6B7280),
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                text = suggestion.description,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = Color(0xFF1F2937)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    // Location error
                    locationError?.let { error ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Add Button
                    Button(
                        onClick = {
                            if (officeName.isNotBlank() && fullAddress.isNotBlank()) {
                                isAddingAddress = true
                                scope.launch {
                                    // Save to database using WorkLocationManager
                                    val result = workLocationManager.saveWorkLocation(
                                        label = officeName.trim(),
                                        address = fullAddress.trim(),
                                        latitude = locationLatitude,
                                        longitude = locationLongitude
                                    )
                                    
                                    result.onSuccess { savedLocation ->
                                        // Reload addresses from database
                                        val locationsResult = workLocationManager.getWorkLocations()
                                        locationsResult.onSuccess { locations ->
                                            officeAddresses = locations.map { workLocation ->
                                                OfficeAddress(
                                                    id = workLocation.id,
                                                    name = workLocation.label,
                                                    address = workLocation.address,
                                                    isDefault = officeAddresses.isEmpty(),
                                                    isActive = true,
                                                    latitude = workLocation.latitude,
                                                    longitude = workLocation.longitude
                                                )
                                            }
                                        }
                                        
                                        // Clear form
                                        officeName = ""
                                        searchQuery = ""
                                        fullAddress = ""
                                        locationLatitude = 0.0
                                        locationLongitude = 0.0
                                        
                                        android.widget.Toast.makeText(
                                            context,
                                            "Address saved successfully",
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                        
                                        timber.log.Timber.d("📍 AddressManagement: Address saved successfully")
                                    }.onFailure { error ->
                                        android.widget.Toast.makeText(
                                            context,
                                            "Failed to save address: ${error.message}",
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                        timber.log.Timber.e(error, "❌ AddressManagement: Failed to save address")
                                    }
                                    
                                    isAddingAddress = false
                                }
                            }
                        },
                        enabled = officeName.isNotBlank() && fullAddress.isNotBlank() && !isAddingAddress,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = employerBlue
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isAddingAddress) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Saving...")
                        } else {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add Address")
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Saved Addresses Section
            if (officeAddresses.isNotEmpty()) {
                Text(
                    text = "Saved Addresses",
                    style = AppTypography.sectionHeader.copy(
                        color = Color(0xFF1F2937)
                    ),
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                officeAddresses.forEach { address ->
                    AddressCard(
                        address = address,
                        onEdit = {
                            // Set editing mode
                            editingAddressId = address.id
                            officeName = address.name
                            searchQuery = address.address
                            fullAddress = address.address
                            locationLatitude = address.latitude
                            locationLongitude = address.longitude
                        },
                        onDelete = {
                            scope.launch {
                                val result = workLocationManager.removeWorkLocation(address.id)
                                result.onSuccess {
                                    // Reload addresses from database
                                    val locationsResult = workLocationManager.getWorkLocations()
                                    locationsResult.onSuccess { locations ->
                                        officeAddresses = locations.map { workLocation ->
                                            OfficeAddress(
                                                id = workLocation.id,
                                                name = workLocation.label,
                                                address = workLocation.address,
                                                isDefault = officeAddresses.isEmpty(),
                                                isActive = true,
                                                latitude = workLocation.latitude,
                                                longitude = workLocation.longitude
                                            )
                                        }
                                    }
                                    android.widget.Toast.makeText(
                                        context,
                                        "Address deleted successfully",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                }.onFailure { error ->
                                    android.widget.Toast.makeText(
                                        context,
                                        "Failed to delete address: ${error.message}",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        },
                        onSetDefault = {
                            officeAddresses = officeAddresses.map { 
                                it.copy(isDefault = it.id == address.id)
                            }
                        },
                        onToggleActive = {
                            officeAddresses = officeAddresses.map { 
                                if (it.id == address.id) it.copy(isActive = !it.isActive) else it
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            } else {
                // Empty state
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOff,
                            contentDescription = null,
                            tint = Color(0xFF9CA3AF),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Saved Addresses",
                            style = AppTypography.emptyStateTitle.copy(
                                color = Color(0xFF6B7280)
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Add your office locations above",
                            style = AppTypography.emptyStateSubtitle.copy(
                                color = Color(0xFF9CA3AF)
                            ),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun AddressCard(
    address: OfficeAddress,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSetDefault: () -> Unit,
    onToggleActive: () -> Unit
) {
    val employerBlue = Color(0xFF3B82F6)
    
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (address.isActive) Color.White else Color(0xFFF9FAFB)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = if (address.isActive) employerBlue else Color(0xFF9CA3AF),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = address.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (address.isActive) Color(0xFF1F2937) else Color(0xFF9CA3AF)
                    )
                    if (address.isDefault) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF10B981).copy(alpha = 0.1f)
                            ),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "DEFAULT",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10B981),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                
                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = Color(0xFF6B7280),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = address.address,
                fontSize = 13.sp,
                color = if (address.isActive) Color(0xFF6B7280) else Color(0xFF9CA3AF),
                lineHeight = 18.sp
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Switch(
                        checked = address.isActive,
                        onCheckedChange = { onToggleActive() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = employerBlue,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Color(0xFFE5E7EB)
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (address.isActive) "Active" else "Inactive",
                        fontSize = 12.sp,
                        color = if (address.isActive) Color(0xFF10B981) else Color(0xFF6B7280)
                    )
                }
                
                if (!address.isDefault) {
                    TextButton(onClick = onSetDefault) {
                        Text(
                            text = "Set as Default",
                            fontSize = 12.sp,
                            color = employerBlue
                        )
                    }
                }
            }
        }
    }
}

data class OfficeAddress(
    val id: String,
    val name: String,
    val address: String,
    val isDefault: Boolean,
    val isActive: Boolean,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)
