package com.example.dutype.employer.screens

import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.dutype.components.CommonHeader
import com.example.dutype.models.InstantHelpDefaults
import com.example.dutype.models.QuickUrgentNeedInput
import com.example.dutype.navigation.Routes
import com.example.dutype.ui.theme.EmployerColors
import com.example.dutype.viewmodels.InstantHelpViewModel

@Composable
fun PostUrgentNeedScreen(
    navController: NavController,
    viewModel: InstantHelpViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var title by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("Helper") }
    var needType by rememberSaveable { mutableStateOf("urgent_now") }
    var budgetText by rememberSaveable { mutableStateOf("") }
    var radiusKm by rememberSaveable { mutableStateOf(5.0) }
    var notes by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EmployerColors.ScreenBackground)
    ) {
        CommonHeader(
            title = "Post urgent need",
            navController = navController,
            subtitle = "For same-day local help",
            backgroundColor = EmployerColors.ScreenBackground,
            titleColor = EmployerColors.TextPrimary,
            subtitleColor = EmployerColors.TextSecondary
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Quick request details",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = EmployerColors.TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        )

                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Work needed") },
                            placeholder = { Text("Cook needed for 2 hours") },
                            leadingIcon = { Icon(Icons.Default.Work, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp)
                        )

                        Text(
                            text = "Category",
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = EmployerColors.TextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(InstantHelpDefaults.categories) { option ->
                                FilterChip(
                                    selected = category == option,
                                    onClick = { category = option },
                                    label = { Text(option) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFFEFF6FF),
                                        selectedLabelColor = Color(0xFF1D4ED8)
                                    )
                                )
                            }
                        }

                        Text(
                            text = "When",
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = EmployerColors.TextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(
                                listOf(
                                    "urgent_now" to "Now",
                                    "today" to "Today",
                                    "scheduled" to "Tomorrow"
                                )
                            ) { option ->
                                FilterChip(
                                    selected = needType == option.first,
                                    onClick = { needType = option.first },
                                    label = { Text(option.second) },
                                    leadingIcon = if (needType == option.first) {
                                        { Icon(Icons.Default.Schedule, contentDescription = null) }
                                    } else null,
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFFFFEDD5),
                                        selectedLabelColor = Color(0xFF9A3412)
                                    )
                                )
                            }
                        }

                        OutlinedTextField(
                            value = budgetText,
                            onValueChange = { budgetText = it },
                            label = { Text("Budget") },
                            placeholder = { Text("Rs 500, hourly, negotiable") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp)
                        )

                        Text(
                            text = "Search radius",
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = EmployerColors.TextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(InstantHelpDefaults.radiusOptionsKm) { radius ->
                                FilterChip(
                                    selected = radiusKm == radius,
                                    onClick = { radiusKm = radius },
                                    label = { Text("${radius.toInt()} km") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFFDCFCE7),
                                        selectedLabelColor = Color(0xFF166534)
                                    )
                                )
                            }
                        }

                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text("Work details") },
                            placeholder = { Text("Timing, exact work, landmark") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            shape = RoundedCornerShape(14.dp)
                        )
                    }
                }
            }

            if (!state.error.isNullOrBlank()) {
                item {
                    Text(
                        text = state.error ?: "",
                        style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFFDC2626)),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }

            item {
                Button(
                    onClick = {
                        viewModel.createUrgentNeed(
                            QuickUrgentNeedInput(
                                title = title,
                                description = notes,
                                category = category,
                                needType = needType,
                                budgetText = budgetText,
                                radiusKm = radiusKm
                            )
                        ) {
                            Toast.makeText(context, "Urgent need posted", Toast.LENGTH_SHORT).show()
                            navController.navigate(Routes.employerUrgentNeedDetailRoute(it)) {
                                popUpTo(Routes.EMPLOYER_DASHBOARD) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    },
                    enabled = !state.isPostingUrgentNeed && title.trim().length >= 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmployerColors.Primary)
                ) {
                    if (state.isPostingUrgentNeed) {
                        CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp), strokeWidth = 2.dp, color = Color.White)
                    }
                    Text("Post urgent need")
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}