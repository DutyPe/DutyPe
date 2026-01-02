package com.example.dutype.employer.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dutype.employer.models.JobCategory
import com.example.dutype.employer.models.JobPerk
import com.example.dutype.employer.models.JobUrgency
import com.example.dutype.employer.models.PayType
import com.example.dutype.employer.models.ShiftTiming

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PayTypeDropdown(
    selectedType: PayType,
    onTypeSelected: (PayType) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedType.displayName,
            onValueChange = { },
            readOnly = true,
            label = { Text("Pay Type") },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            PayType.values().forEach { payType ->
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(text = getPayTypeEmoji(payType))
                            Text(
                                text = payType.displayName,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    },
                    onClick = {
                        onTypeSelected(payType)
                        expanded = false
                    }
                )
            }
        }
    }
}

// Helper function to get emoji for PayType
private fun getPayTypeEmoji(payType: PayType): String = when (payType) {
    PayType.HOURLY -> "⏰"
    PayType.DAILY -> "📅"
    PayType.MONTHLY -> "💼"
    PayType.TASK -> "🛠️"
}

@Composable
fun CategorySelectionGrid(
    selectedCategory: JobCategory,
    onCategorySelected: (JobCategory) -> Unit,
    customCategory: String = "",
    onCustomCategoryChange: ((String) -> Unit)? = null
) {
    val primaryBlue = Color(0xFF3B82F6)
    
    Column {
        Text(
            text = "Select Job Category",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.height(280.dp)
        ) {
            items(JobCategory.values().toList()) { category ->
                CategoryChip(
                    category = category,
                    isSelected = selectedCategory == category,
                    onClick = { onCategorySelected(category) }
                )
            }
        }
        
        // Show custom category input when "Other" is selected
        if (selectedCategory == JobCategory.OTHER && onCustomCategoryChange != null) {
            Spacer(modifier = Modifier.height(12.dp))
            androidx.compose.material3.OutlinedTextField(
                value = customCategory,
                onValueChange = onCustomCategoryChange,
                label = { Text("Enter Job Type") },
                placeholder = { Text("e.g., Tailor, Mechanic, Tutor, Beautician") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = primaryBlue,
                    focusedLabelColor = primaryBlue,
                    unfocusedBorderColor = Color(0xFFE2E8F0),
                    cursorColor = primaryBlue
                )
            )
        }
    }
}

@Composable
private fun CategoryChip(
    category: JobCategory,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF3B82F6) else Color(0xFFF8FAFC)
        ),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 1.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = category.icon,
                fontSize = 20.sp
            )
            Text(
                text = category.displayName,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = if (isSelected) Color.White else Color(0xFF374151),
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                ),
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

@Composable
fun PerksSelectionGrid(
    selectedPerks: Set<JobPerk>,
    onPerksChanged: (Set<JobPerk>) -> Unit
) {
    Column {
        Text(
            text = "Select Perks & Benefits",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.height(300.dp)
        ) {
            items(JobPerk.values().toList()) { perk ->
                PerkChip(
                    perk = perk,
                    isSelected = selectedPerks.contains(perk),
                    onClick = {
                        onPerksChanged(
                            if (selectedPerks.contains(perk)) {
                                selectedPerks - perk
                            } else {
                                selectedPerks + perk
                            }
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun PerkChip(
    perk: JobPerk,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF10B981).copy(alpha = 0.1f) else Color.White
        ),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(
            1.dp,
            if (isSelected) Color(0xFF10B981) else Color(0xFFE5E7EB)
        )
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = perk.icon,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = perk.displayName,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = if (isSelected) Color(0xFF10B981) else Color(0xFF374151),
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                ),
                maxLines = 2
            )
        }
    }
}

@Composable
fun WorkScheduleSection(
    selectedShift: ShiftTiming,
    onShiftSelected: (ShiftTiming) -> Unit,
    selectedUrgency: JobUrgency,
    onUrgencySelected: (JobUrgency) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Shift Timing
        Text(
            text = "Shift Timing",
            style = MaterialTheme.typography.labelLarge
        )

        androidx.compose.foundation.lazy.LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(ShiftTiming.values().toList()) { shift ->
                ShiftChip(
                    shift = shift,
                    isSelected = selectedShift == shift,
                    onClick = { onShiftSelected(shift) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Urgency
        Text(
            text = "Hiring Urgency",
            style = MaterialTheme.typography.labelLarge
        )

        androidx.compose.foundation.lazy.LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(JobUrgency.values().toList()) { urgency ->
                UrgencyChip(
                    urgency = urgency,
                    isSelected = selectedUrgency == urgency,
                    onClick = { onUrgencySelected(urgency) }
                )
            }
        }
    }
}

@Composable
private fun ShiftChip(
    shift: ShiftTiming,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF3B82F6) else Color.White
        ),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            1.dp,
            if (isSelected) Color(0xFF3B82F6) else Color(0xFFE5E7EB)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = shift.icon,
                fontSize = 18.sp
            )
            Text(
                text = shift.displayName,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = if (isSelected) Color.White else Color(0xFF374151),
                    fontWeight = FontWeight.Medium
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun UrgencyChip(
    urgency: JobUrgency,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = when {
        isSelected -> when (urgency) {
            JobUrgency.IMMEDIATE -> Color(0xFFDC2626)
            JobUrgency.URGENT -> Color(0xFFEA580C)
            JobUrgency.NORMAL -> Color(0xFF3B82F6)
            JobUrgency.FLEXIBLE -> Color(0xFF10B981)
        }
        else -> Color.White
    }

    val textColor = if (isSelected) Color.White else when (urgency) {
        JobUrgency.IMMEDIATE -> Color(0xFFDC2626)
        JobUrgency.URGENT -> Color(0xFFEA580C)
        JobUrgency.NORMAL -> Color(0xFF3B82F6)
        JobUrgency.FLEXIBLE -> Color(0xFF10B981)
    }

    Card(
        modifier = Modifier.clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, textColor)
    ) {
        Text(
            text = urgency.displayName,
            style = MaterialTheme.typography.bodySmall.copy(
                color = textColor,
                fontWeight = FontWeight.Medium
            ),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun PostJobBottomBar(
    onPreviewClick: () -> Unit,
    onPostClick: () -> Unit,
    isLoading: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onPreviewClick,
                modifier = Modifier.weight(1f),
                enabled = !isLoading
            ) {
                Icon(
                    imageVector = Icons.Default.Preview,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Preview")
            }

            Button(
                onClick = onPostClick,
                modifier = Modifier.weight(2f),
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF3B82F6)
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("Post Job")
            }
        }
    }
}


// =============================================================================
// FORM COMPONENTS (Merged from PostJobFormComponents.kt)
// =============================================================================

@Composable
fun StepHeader(title: String, subtitle: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = Color(0xFF1E293B)
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF6B7280),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
fun JobTitleSection(
    title: String,
    onTitleChange: (String) -> Unit,
    category: JobCategory,
    onCategoryChange: (JobCategory) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Job Title",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            OutlinedTextField(
                value = title,
                onValueChange = onTitleChange,
                placeholder = { Text("e.g., Cook, Driver, Cleaner") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = {
                    Icon(Icons.Default.Work, contentDescription = null)
                }
            )

            Text(
                text = "Job Category",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            CategorySelectionGrid(
                selectedCategory = category,
                onCategorySelected = onCategoryChange
            )
        }
    }
}

@Composable
fun JobDescriptionSection(
    description: String,
    onDescriptionChange: (String) -> Unit
) {
    val primaryBlue = Color(0xFF2563EB)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Job Description",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1E293B)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "*",
                    color = Color(0xFFEF4444),
                    fontWeight = FontWeight.Bold
                )
            }
            OutlinedTextField(
                value = description,
                onValueChange = onDescriptionChange,
                placeholder = { 
                    Text(
                        "Describe responsibilities, requirements, and what you're looking for...",
                        color = Color(0xFF94A3B8)
                    ) 
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = primaryBlue,
                    focusedLabelColor = primaryBlue,
                    unfocusedBorderColor = Color(0xFFE2E8F0),
                    cursorColor = primaryBlue
                )
            )
        }
    }
}

@Composable
fun PaymentSection(
    payAmount: String,
    onPayAmountChange: (String) -> Unit,
    payType: PayType,
    onPayTypeChange: (PayType) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Payment Details",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = payAmount,
                    onValueChange = onPayAmountChange,
                    label = { Text("Amount") },
                    placeholder = { Text("e.g., 500") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(2f),
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.CurrencyRupee, contentDescription = null)
                    }
                )

                PayTypeDropdown(
                    selectedType = payType,
                    onTypeSelected = onPayTypeChange,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun LocationSection(
    location: String,
    onLocationChange: (String) -> Unit,
    isLoadingLocation: Boolean,
    locationError: String?,
    onLocationButtonClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Job Location",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = location,
                onValueChange = onLocationChange,
                placeholder = { Text("Enter location or use GPS") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {
                    Icon(Icons.Default.LocationOn, contentDescription = null)
                },
                trailingIcon = {
                    IconButton(onClick = onLocationButtonClick) {
                        if (isLoadingLocation) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        } else {
                            Icon(Icons.Default.MyLocation, contentDescription = "Use GPS")
                        }
                    }
                }
            )

            locationError?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun VacanciesSection(
    vacancies: String,
    onVacanciesChange: (String) -> Unit
) {
    val primaryBlue = Color(0xFF2563EB)
    val isError = vacancies.isNotEmpty() && (vacancies.toIntOrNull() ?: 0) > 50
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Number of Positions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1E293B)
            )
            OutlinedTextField(
                value = vacancies,
                onValueChange = { newValue ->
                    // Only allow numeric input and max 50 vacancies
                    if (newValue.isEmpty() || (newValue.all { it.isDigit() } && newValue.length <= 2)) {
                        val numValue = newValue.toIntOrNull() ?: 0
                        if (numValue <= 50) {
                            onVacanciesChange(newValue)
                        } else if (newValue.length == 1) {
                            // Allow single digit even if it could lead to >50
                            onVacanciesChange(newValue)
                        }
                    }
                },
                label = { Text("Vacancies (Max 50)") },
                placeholder = { Text("Enter number of positions") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = isError,
                supportingText = if (isError) {
                    { Text("Maximum 50 vacancies allowed for hyper-local jobs", color = Color(0xFFDC2626)) }
                } else null,
                shape = RoundedCornerShape(12.dp),
                leadingIcon = {
                    Icon(
                        Icons.Default.People, 
                        contentDescription = null,
                        tint = if (isError) Color(0xFFDC2626) else Color(0xFF6B7280)
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (isError) Color(0xFFDC2626) else primaryBlue,
                    focusedLabelColor = if (isError) Color(0xFFDC2626) else primaryBlue,
                    unfocusedBorderColor = if (isError) Color(0xFFDC2626) else Color(0xFFE2E8F0),
                    cursorColor = primaryBlue,
                    errorBorderColor = Color(0xFFDC2626)
                )
            )
        }
    }
}

@Composable
fun ContactSection(
    contactNumber: String,
    onContactNumberChange: (String) -> Unit,
    employerName: String,
    onEmployerNameChange: (String) -> Unit
) {
    val primaryBlue = Color(0xFF2563EB)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Contact Information",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1E293B)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "*",
                    color = Color(0xFFEF4444),
                    fontWeight = FontWeight.Bold
                )
            }

            OutlinedTextField(
                value = contactNumber,
                onValueChange = onContactNumberChange,
                label = { Text("Contact Number") },
                placeholder = { Text("+91 9876543210") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                leadingIcon = {
                    Icon(
                        Icons.Default.Phone, 
                        contentDescription = null,
                        tint = Color(0xFF6B7280)
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = primaryBlue,
                    focusedLabelColor = primaryBlue,
                    unfocusedBorderColor = Color(0xFFE2E8F0),
                    cursorColor = primaryBlue
                )
            )

            OutlinedTextField(
                value = employerName,
                onValueChange = onEmployerNameChange,
                label = { Text("Your Name") },
                placeholder = { Text("Enter your name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                leadingIcon = {
                    Icon(
                        Icons.Default.Person, 
                        contentDescription = null,
                        tint = Color(0xFF6B7280)
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = primaryBlue,
                    focusedLabelColor = primaryBlue,
                    unfocusedBorderColor = Color(0xFFE2E8F0),
                    cursorColor = primaryBlue
                )
            )
        }
    }
}
