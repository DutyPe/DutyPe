package com.example.dutype.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class SearchFilter(
    val key: String,
    val label: String,
    val value: String,
    val isSelected: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedSearchComponent(
    onSearch: (Map<String, Any>) -> Unit,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedLocation by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("") }
    var selectedPayType by remember { mutableStateOf("") }
    var selectedJobType by remember { mutableStateOf("") }
    var selectedUrgency by remember { mutableStateOf("") }
    var minSalary by remember { mutableStateOf("") }
    var maxSalary by remember { mutableStateOf("") }
    var isRemote by remember { mutableStateOf(false) }
    var isVerified by remember { mutableStateOf(false) }
    var showFilters by remember { mutableStateOf(false) }
    
    val locations = listOf("Hyderabad", "Mumbai", "Delhi", "Bangalore", "Chennai", "Pune", "Kolkata")
    val categories = listOf("Cook", "Driver", "Security", "Housekeeping", "Delivery", "Sales", "Customer Service")
    val payTypes = listOf("HOURLY", "DAILY", "WEEKLY", "MONTHLY")
    val jobTypes = listOf("Part-time", "Full-time", "Contract", "Freelance")
    val urgencyLevels = listOf("URGENT", "NORMAL", "FLEXIBLE")
    
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search jobs...") },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = "Search")
            },
            trailingIcon = {
                Row {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                    IconButton(onClick = { showFilters = !showFilters }) {
                        Icon(
                            Icons.Default.FilterList,
                            contentDescription = "Filters",
                            tint = if (showFilters) Color(0xFF3B82F6) else Color(0xFF6B7280)
                        )
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
        
        // Quick Filters
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(listOf("Remote", "Verified", "Urgent", "High Pay")) { filter ->
                FilterChip(
                    onClick = {
                        when (filter) {
                            "Remote" -> isRemote = !isRemote
                            "Verified" -> isVerified = !isVerified
                            "Urgent" -> selectedUrgency = if (selectedUrgency == "URGENT") "" else "URGENT"
                            "High Pay" -> {
                                minSalary = "500"
                                maxSalary = "2000"
                            }
                        }
                    },
                    label = { Text(filter) },
                    selected = when (filter) {
                        "Remote" -> isRemote
                        "Verified" -> isVerified
                        "Urgent" -> selectedUrgency == "URGENT"
                        "High Pay" -> minSalary.isNotEmpty()
                        else -> false
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF3B82F6),
                        selectedLabelColor = Color.White
                    )
                )
            }
        }
        
        // Advanced Filters
        if (showFilters) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAFB)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Advanced Filters",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color(0xFF1F2937)
                    )
                    
                    // Location Filter
                    FilterDropdown(
                        label = "Location",
                        options = locations,
                        selectedValue = selectedLocation,
                        onValueChange = { selectedLocation = it }
                    )
                    
                    // Category Filter
                    FilterDropdown(
                        label = "Category",
                        options = categories,
                        selectedValue = selectedCategory,
                        onValueChange = { selectedCategory = it }
                    )
                    
                    // Pay Type Filter
                    FilterDropdown(
                        label = "Pay Type",
                        options = payTypes,
                        selectedValue = selectedPayType,
                        onValueChange = { selectedPayType = it }
                    )
                    
                    // Job Type Filter
                    FilterDropdown(
                        label = "Job Type",
                        options = jobTypes,
                        selectedValue = selectedJobType,
                        onValueChange = { selectedJobType = it }
                    )
                    
                    // Salary Range
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = minSalary,
                            onValueChange = { minSalary = it },
                            label = { Text("Min Salary") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        
                        OutlinedTextField(
                            value = maxSalary,
                            onValueChange = { maxSalary = it },
                            label = { Text("Max Salary") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                    
                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onClearFilters,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Clear All")
                        }
                        
                        Button(
                            onClick = {
                                val filters: Map<String, Any> = buildMap {
                                    if (searchQuery.isNotEmpty()) put("query", searchQuery)
                                    if (selectedLocation.isNotEmpty()) put("location", selectedLocation)
                                    if (selectedCategory.isNotEmpty()) put("category", selectedCategory)
                                    if (selectedPayType.isNotEmpty()) put("payType", selectedPayType)
                                    if (selectedJobType.isNotEmpty()) put("jobType", selectedJobType)
                                    if (selectedUrgency.isNotEmpty()) put("urgency", selectedUrgency)
                                    if (minSalary.isNotEmpty()) put("minSalary", minSalary.toDoubleOrNull() ?: 0.0)
                                    if (maxSalary.isNotEmpty()) put("maxSalary", maxSalary.toDoubleOrNull() ?: 0.0)
                                    if (isRemote) put("isRemote", true)
                                    if (isVerified) put("isVerified", true)
                                }
                                onSearch(filters)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Apply Filters")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterDropdown(
    label: String,
    options: List<String>,
    selectedValue: String,
    onValueChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedValue,
            onValueChange = { },
            readOnly = true,
            label = { Text(label) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            shape = RoundedCornerShape(8.dp)
        )
        
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onValueChange(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
