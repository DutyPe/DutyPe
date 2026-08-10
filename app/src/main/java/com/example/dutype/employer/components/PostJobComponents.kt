package com.example.dutype.employer.components

import com.dutype.app.R
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dutype.employer.helpers.JobPostingHelpers
import com.example.dutype.employer.models.PayType
import com.example.dutype.employer.models.ShiftTiming
import com.example.dutype.ui.theme.EmployerColors

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
            label = { Text(stringResource(R.string.pay_type)) },
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
                            Icon(
                                imageVector = JobPostingHelpers.getPayTypeIcon(payType),
                                contentDescription = null,
                                tint = EmployerColors.Primary,
                                modifier = Modifier.size(16.dp)
                            )
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

@Composable
fun WorkScheduleSection(
    selectedShift: ShiftTiming,
    onShiftSelected: (ShiftTiming) -> Unit,
    customStart: String = "",
    onCustomStartChange: (String) -> Unit = {},
    customEnd: String = "",
    onCustomEndChange: (String) -> Unit = {}
) {
    val shiftOptions = listOf(ShiftTiming.MORNING, ShiftTiming.NIGHT, ShiftTiming.BOTH, ShiftTiming.FLEXIBLE)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.auto_shift),
            style = MaterialTheme.typography.labelLarge
        )

        androidx.compose.foundation.lazy.LazyRow(
            horizontalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            items(shiftOptions) { shift ->
                ShiftChip(
                    shift = shift,
                    isSelected = selectedShift == shift,
                    onClick = { onShiftSelected(shift) }
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
            .padding(end = 4.dp)
            .height(32.dp)
            .defaultMinSize(minWidth = 0.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) EmployerColors.Primary else EmployerColors.CardBackground
        ),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            1.dp,
            if (isSelected) EmployerColors.Primary else EmployerColors.Border
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = shift.icon,
                fontSize = 13.sp
            )
            Text(
                text = shift.displayName,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = if (isSelected) Color.White else EmployerColors.TextPrimary,
                    fontWeight = FontWeight.Medium
                ),
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )
        }
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
                Text(stringResource(R.string.preview_button))
            }

            Button(
                onClick = onPostClick,
                modifier = Modifier.weight(2f),
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmployerColors.Primary
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
                Text(stringResource(R.string.post_job_button))
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
            color = EmployerColors.TextPrimary
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = EmployerColors.TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
fun JobTitleSection(
    title: String,
    onTitleChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = com.example.dutype.ui.theme.EmployerColors.CardBackground)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.job_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            OutlinedTextField(
                value = title,
                onValueChange = onTitleChange,
                placeholder = { Text(stringResource(R.string.edit_job_title_hint)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = {
                    Icon(Icons.Default.Work, contentDescription = null)
                }
            )

        }
    }
}

@Composable
fun JobDescriptionSection(
    description: String,
    onDescriptionChange: (String) -> Unit
) {
    val primaryBlue = EmployerColors.Primary

    SectionContainer {
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.job_description),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = EmployerColors.TextPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "*",
                    color = EmployerColors.Error,
                    fontWeight = FontWeight.Bold
                )
            }
            OutlinedTextField(
                value = description,
                onValueChange = onDescriptionChange,
                placeholder = { 
                    Text(
                        stringResource(R.string.job_description_hint),
                        color = EmployerColors.TextTertiary
                    ) 
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = primaryBlue,
                    focusedLabelColor = primaryBlue,
                    unfocusedBorderColor = EmployerColors.Border,
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
        colors = CardDefaults.cardColors(containerColor = com.example.dutype.ui.theme.EmployerColors.CardBackground)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.payment_details),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = payAmount,
                onValueChange = onPayAmountChange,
                label = { Text(stringResource(R.string.amount)) },
                placeholder = { Text(stringResource(R.string.payment_amount_hint)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = {
                    Icon(Icons.Default.CurrencyRupee, contentDescription = null)
                },
                supportingText = {
                    Text(
                        text = stringResource(R.string.auto_enter_amount_range_10000_15000_or_text_bas),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            )

            Text(
                text = stringResource(R.string.pay_type),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = EmployerColors.TextSecondary
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(PayType.values().toList()) { type ->
                    val selected = payType == type
                    FilterChip(
                        selected = selected,
                        onClick = { onPayTypeChange(type) },
                        modifier = Modifier.height(34.dp),
                        label = {
                            Text(
                                text = type.displayName,
                                fontSize = 12.sp,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                            )
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmployerColors.Primary,
                            selectedLabelColor = Color.White,
                            containerColor = com.example.dutype.ui.theme.EmployerColors.CardBackground,
                            labelColor = EmployerColors.TextPrimary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selected,
                            borderColor = EmployerColors.Border,
                            selectedBorderColor = EmployerColors.Primary
                        )
                    )
                }
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
        colors = CardDefaults.cardColors(containerColor = com.example.dutype.ui.theme.EmployerColors.CardBackground)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.auto_job_location),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = location,
                onValueChange = onLocationChange,
                placeholder = { Text(stringResource(R.string.enter_location_or_gps)) },
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
    val primaryBlue = EmployerColors.Primary
    val isError = vacancies.isNotEmpty() && (vacancies.toIntOrNull() ?: 0) > 50

    SectionContainer {
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = stringResource(R.string.auto_number_of_positions),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = EmployerColors.TextPrimary
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
                label = { Text(stringResource(R.string.vacancies_max_50)) },
                placeholder = { Text(stringResource(R.string.enter_number_of_positions)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = isError,
                supportingText = if (isError) {
                    { Text(stringResource(R.string.max_50_vacancies_hint), color = EmployerColors.Error) }
                } else null,
                shape = RoundedCornerShape(12.dp),
                leadingIcon = {
                    Icon(
                        Icons.Default.People, 
                        contentDescription = null,
                        tint = if (isError) EmployerColors.Error else EmployerColors.IconSecondary
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (isError) EmployerColors.Error else primaryBlue,
                    focusedLabelColor = if (isError) EmployerColors.Error else primaryBlue,
                    unfocusedBorderColor = if (isError) EmployerColors.Error else EmployerColors.Border,
                    cursorColor = primaryBlue,
                    errorBorderColor = EmployerColors.Error
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
    val primaryBlue = EmployerColors.Primary

    SectionContainer {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.auto_contact_information),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = EmployerColors.TextPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "*",
                    color = EmployerColors.Error,
                    fontWeight = FontWeight.Bold
                )
            }

            OutlinedTextField(
                value = contactNumber,
                onValueChange = onContactNumberChange,
                label = { Text(stringResource(R.string.contact_number_label)) },
                placeholder = { Text("+91 9876543210") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                leadingIcon = {
                    Icon(
                        Icons.Default.Phone,
                        contentDescription = null,
                        tint = EmployerColors.IconSecondary
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = primaryBlue,
                    focusedLabelColor = primaryBlue,
                    unfocusedBorderColor = EmployerColors.Border,
                    cursorColor = primaryBlue
                )
            )

            // Batch-p #9: WhatsApp number field removed from job posting.
            // Workers contact employers via the platform-provided phone number.

            OutlinedTextField(
                value = employerName,
                onValueChange = onEmployerNameChange,
                label = { Text(stringResource(R.string.your_name)) },
                placeholder = { Text(stringResource(R.string.enter_your_name)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                leadingIcon = {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = EmployerColors.IconSecondary
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = primaryBlue,
                    focusedLabelColor = primaryBlue,
                    unfocusedBorderColor = EmployerColors.Border,
                    cursorColor = primaryBlue
                )
            )
        }
    }
}

