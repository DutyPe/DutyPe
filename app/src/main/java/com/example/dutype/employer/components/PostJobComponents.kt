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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material.icons.filled.Send
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dutype.employer.models.enums.JobCategory
import com.example.dutype.employer.models.enums.JobPerk
import com.example.dutype.employer.models.enums.JobUrgency
import com.example.dutype.employer.models.enums.PayType
import com.example.dutype.employer.models.enums.ShiftTiming

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
    onCategorySelected: (JobCategory) -> Unit
) {
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
            modifier = Modifier.height(200.dp)
        ) {
            items(JobCategory.values().toList()) { category ->
                CategoryChip(
                    category = category,
                    isSelected = selectedCategory == category,
                    onClick = { onCategorySelected(category) }
                )
            }
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
            containerColor = if (isSelected) Color(0xFF6366F1) else Color(0xFFF8FAFC)
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
            text = "Select Perks & Benefits (Optional)",
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

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.height(120.dp)
        ) {
            items(ShiftTiming.values().toList()) { shift ->
                ShiftChip(
                    shift = shift,
                    isSelected = selectedShift == shift,
                    onClick = { onShiftSelected(shift) }
                )
            }
        }

        // Urgency
        Text(
            text = "Hiring Urgency",
            style = MaterialTheme.typography.labelLarge
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            JobUrgency.values().forEach { urgency ->
                UrgencyChip(
                    urgency = urgency,
                    isSelected = selectedUrgency == urgency,
                    onClick = { onUrgencySelected(urgency) },
                    modifier = Modifier.weight(1f)
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
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF6366F1) else Color.White
        ),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(
            1.dp,
            if (isSelected) Color(0xFF6366F1) else Color(0xFFE5E7EB)
        )
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = shift.icon,
                fontSize = 20.sp
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
    onClick: () -> Unit,
    modifier: Modifier = Modifier
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
        modifier = modifier.clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, textColor)
    ) {
        Text(
            text = urgency.displayName,
            style = MaterialTheme.typography.bodySmall.copy(
                color = textColor,
                fontWeight = FontWeight.Medium
            ),
            modifier = Modifier.padding(8.dp),
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
                    containerColor = Color(0xFF6366F1)
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
