package com.example.partimes.employer.screens.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.partimes.utils.LocationService
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployerAddressManagementScreen(
    navController: NavController,
    onStatusBarColorChange: (Color) -> Unit
) {
    onStatusBarColorChange(Color(0xFF2193b0)) // Clean sky blue theme color
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val locationService = remember { LocationService(context) }
    
    // Office addresses state - starts empty, will be populated from real data
    var officeAddresses by remember { mutableStateOf<List<OfficeAddress>>(emptyList()) }
    
    var showAddAddressDialog by remember { mutableStateOf(false) }
    var showEditAddressDialog by remember { mutableStateOf(false) }
    var selectedAddress by remember { mutableStateOf<OfficeAddress?>(null) }
    var isLoadingLocation by remember { mutableStateOf(false) }
    var locationError by remember { mutableStateOf<String?>(null) }
    
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
                        // Auto-fill the address field with current location
                        // This will be handled in the AddEditAddressDialog
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

    Scaffold(
        topBar = {
            // Custom transparent top bar that blends with gradient
            androidx.compose.material3.TopAppBar(
                title = {
                    Text(
                        text = "Manage Addresses",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            fontSize = 18.sp
                        )
                    )
                },
                navigationIcon = {
                    androidx.compose.material3.IconButton(onClick = { navController.popBackStack() }) {
                        androidx.compose.material3.Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.ArrowBackIosNew,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddAddressDialog = true },
                containerColor = Color(0xFF2193b0),
                contentColor = Color.White
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Address"
                )
            }
        }
    ) { innerPadding ->
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
                .padding(
                    top = innerPadding.calculateTopPadding(),
                    start = innerPadding.calculateStartPadding(LocalLayoutDirection.current),
                    end = innerPadding.calculateEndPadding(LocalLayoutDirection.current)
                )
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            // Header Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                // elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Office Locations",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF374151)
                        )
                    }
                    Text(
                        text = "Manage your office locations where you hire workers. Add multiple addresses for different branches or departments.",
                        fontSize = 14.sp,
                        color = Color(0xFF374151).copy(alpha = 0.8f),
                        lineHeight = 20.sp
                    )
                }
            }

            // Addresses List
            if (officeAddresses.isEmpty()) {
                EmptyAddressesCard(
                    onAddAddress = { showAddAddressDialog = true }
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(officeAddresses) { address ->
                        AddressCard(
                            address = address,
                            onEdit = {
                                selectedAddress = address
                                showEditAddressDialog = true
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
                    }
                }
            }
        }
    }

    // Add Address Dialog
    if (showAddAddressDialog) {
        AddEditAddressDialog(
            title = "Add New Office Location",
            onDismiss = { showAddAddressDialog = false },
            onSave = { officeName, fullAddress ->
                val newAddress = OfficeAddress(
                    id = (officeAddresses.size + 1).toString(),
                    name = officeName.trim(),
                    address = fullAddress.trim(),
                    isDefault = officeAddresses.isEmpty(),
                    isActive = true
                )
                officeAddresses = officeAddresses + newAddress
                showAddAddressDialog = false
            }
        )
    }

    // Edit Address Dialog
    if (showEditAddressDialog && selectedAddress != null) {
        AddEditAddressDialog(
            title = "Edit Office Location",
            initialName = selectedAddress!!.name,
            initialAddress = selectedAddress!!.address,
            onDismiss = { 
                showEditAddressDialog = false
                selectedAddress = null
            },
            onSave = { officeName, fullAddress ->
                officeAddresses = officeAddresses.map { 
                    if (it.id == selectedAddress!!.id) {
                        it.copy(name = officeName.trim(), address = fullAddress.trim())
                    } else it
                }
                showEditAddressDialog = false
                selectedAddress = null
            }
        )
    }
}

@Composable
private fun EmptyAddressesCard(
    onAddAddress: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth()
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
                tint = Color(0xFF6B7280),
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No Office Addresses",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF374151)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Add your office locations to help workers find work near you.",
                fontSize = 14.sp,
                color = Color(0xFF6B7280),
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
            Spacer(modifier = Modifier.height(24.dp))
//            Button(
//                onClick = onAddAddress,
//                colors = ButtonDefaults.buttonColors(
//                    containerColor = Color(0xFF2193b0)
//                ),
//                shape = RoundedCornerShape(12.dp)
//            ) {
//                Icon(
//                    imageVector = Icons.Default.Add,
//                    contentDescription = null,
//                    modifier = Modifier.size(18.dp)
//                )
//                Spacer(modifier = Modifier.width(8.dp))
//                Text("Add First Address")
//            }
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
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (address.isActive) 
                MaterialTheme.colorScheme.surface 
            else 
                Color(0xFFF9FAFB)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = if (address.isActive) Color(0xFF2193b0) else Color(0xFF9CA3AF),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = address.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (address.isActive) Color(0xFF2193b0) else Color(0xFF9CA3AF)
                    )
                    if (address.isDefault) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF10B981).copy(alpha = 0.1f)
                            ),
                            shape = RoundedCornerShape(8.dp)
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
                    IconButton(onClick = onEdit) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = Color(0xFF6B7280)
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = Color(0xFFEF4444)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = address.address,
                fontSize = 14.sp,
                color = if (address.isActive) Color(0xFF6B7280) else Color(0xFF9CA3AF),
                lineHeight = 18.sp
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Switch(
                        checked = address.isActive,
                        onCheckedChange = { onToggleActive() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF1A237E),
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
                            color = Color(0xFF1A237E)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AddEditAddressDialog(
    title: String,
    initialName: String = "",
    initialAddress: String = "",
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val locationService = remember { LocationService(context) }
    
    var name by remember { mutableStateOf(initialName) }
    var address by remember { mutableStateOf(initialAddress) }
    var isLoadingLocation by remember { mutableStateOf(false) }
    var locationError by remember { mutableStateOf<String?>(null) }
    
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
                        address = locationInfo.address
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2193b0)
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Office Name Field
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Office Name *") },
                    placeholder = { Text("e.g., Main Office, Branch Office, Warehouse") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF2193b0),
                        focusedLabelColor = Color(0xFF2193b0)
                    )
                )
                
                // Full Address Field with Location Button
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Full Address *") },
                    placeholder = { Text("Street, City, State, ZIP Code, Country") },
                    minLines = 3,
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = { if (name.isNotBlank() && address.isNotBlank()) onSave(name, address) }
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF2193b0),
                        focusedLabelColor = Color(0xFF2193b0)
                    ),
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                if (locationService.hasLocationPermission()) {
                                    locationPermissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
                                } else {
                                    locationPermissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
                                }
                            },
                            enabled = !isLoadingLocation
                        ) {
                            if (isLoadingLocation) {
                                androidx.compose.material3.CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = Color(0xFF2193b0)
                                )
                            } else {
                                Icon(
                                    Icons.Default.LocationOn,
                                    contentDescription = "Use Current Location",
                                    tint = Color(0xFF2193b0)
                                )
                            }
                        }
                    }
                )
                
                // Show location error if any
                locationError?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, address) },
                enabled = name.isNotBlank() && address.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2193b0)
                )
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

data class OfficeAddress(
    val id: String,
    val name: String,
    val address: String,
    val isDefault: Boolean,
    val isActive: Boolean
)
