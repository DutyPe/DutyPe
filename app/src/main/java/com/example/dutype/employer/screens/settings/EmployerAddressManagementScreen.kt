package com.example.dutype.employer.screens.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import com.example.dutype.utils.LocationService
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
    val locationService = remember { LocationService(context) }
    
    // Employer theme color
    val employerBlue = Color(0xFF3B82F6)
    
    // Office addresses state
    var officeAddresses by remember { mutableStateOf<List<OfficeAddress>>(emptyList()) }
    
    // Add address form state
    var officeName by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var fullAddress by remember { mutableStateOf("") }
    var isLoadingLocation by remember { mutableStateOf(false) }
    var locationError by remember { mutableStateOf<String?>(null) }
    var isAddingAddress by remember { mutableStateOf(false) }
    
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
                    val locationInfo = locationService.getCurrentLocation()
                    if (locationInfo != null) {
                        fullAddress = locationInfo.address
                        searchQuery = locationInfo.address
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
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1F2937)
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
                    
                    // Search/Address Field with location button
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { 
                            searchQuery = it
                            fullAddress = it
                        },
                        label = { Text("Search or Enter Address") },
                        placeholder = { Text("Search location or enter address") },
                        singleLine = false,
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "Search",
                                tint = Color(0xFF6B7280)
                            )
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
                                val newAddress = OfficeAddress(
                                    id = System.currentTimeMillis().toString(),
                                    name = officeName.trim(),
                                    address = fullAddress.trim(),
                                    isDefault = officeAddresses.isEmpty(),
                                    isActive = true
                                )
                                officeAddresses = officeAddresses + newAddress
                                officeName = ""
                                searchQuery = ""
                                fullAddress = ""
                            }
                        },
                        enabled = officeName.isNotBlank() && fullAddress.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = employerBlue
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
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
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Saved Addresses Section
            if (officeAddresses.isNotEmpty()) {
                Text(
                    text = "Saved Addresses",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1F2937),
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
                        },
                        onDelete = {
                            officeAddresses = officeAddresses.filter { it.id != address.id }
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
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF6B7280)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Add your office locations above",
                            fontSize = 14.sp,
                            color = Color(0xFF9CA3AF),
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
    val isActive: Boolean
)
