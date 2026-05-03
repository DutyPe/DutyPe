package com.example.dutype.employer.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private const val OWN_CATEGORY_LABEL = "Own category"

@Composable
fun PostUrgentNeedScreen(
    navController: NavController,
    viewModel: InstantHelpViewModel = hiltViewModel()
) {
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

        PostUrgentNeedContent(
            viewModel = viewModel,
            onPosted = { requestId ->
                navController.navigate(Routes.employerUrgentNeedDetailRoute(requestId)) {
                    popUpTo(Routes.EMPLOYER_DASHBOARD) { inclusive = true }
                    launchSingleTop = true
                }
            }
        )
    }
}

@Composable
internal fun PostUrgentNeedContent(
    viewModel: InstantHelpViewModel,
    onPosted: (String) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    showIntroCard: Boolean = true
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var title by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("Helper") }
    var ownCategory by rememberSaveable { mutableStateOf("") }
    var needType by rememberSaveable { mutableStateOf("urgent_now") }
    var selectedDayOffset by rememberSaveable { mutableStateOf(1) }
    var selectedHour by rememberSaveable { mutableStateOf(9) }
    var budgetText by rememberSaveable { mutableStateOf("") }
    var radiusKm by rememberSaveable { mutableStateOf(5.0) }
    var notes by rememberSaveable { mutableStateOf("") }

    val effectiveCategory = if (category == OWN_CATEGORY_LABEL) {
        ownCategory.trim().ifBlank { "Other" }
    } else {
        category
    }
    val scheduleLabel = if (needType == "scheduled") {
        buildScheduleLabel(selectedDayOffset, selectedHour)
    } else {
        ""
    }
    val canPost = title.trim().length >= 3 &&
        (category != OWN_CATEGORY_LABEL || ownCategory.trim().length >= 2)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (showIntroCard) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Instant hiring request",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = "Reach nearby available workers within 10 km for urgent local work.",
                            style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFFE2E8F0))
                        )
                    }
                }
            }
        }

        item {
            UrgentNeedSectionCard(title = "Work details") {
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

                SectionLabel("Category")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(InstantHelpDefaults.categories + OWN_CATEGORY_LABEL) { option ->
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

                if (category == OWN_CATEGORY_LABEL) {
                    OutlinedTextField(
                        value = ownCategory,
                        onValueChange = { ownCategory = it },
                        label = { Text("Your category") },
                        placeholder = { Text("Event helper, packer, delivery help") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp)
                    )
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

        item {
            UrgentNeedSectionCard(title = "Timing") {
                SectionLabel("When")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(
                        listOf(
                            "urgent_now" to "Now",
                            "today" to "Today",
                            "scheduled" to "Schedule"
                        )
                    ) { option ->
                        FilterChip(
                            selected = needType == option.first,
                            onClick = { needType = option.first },
                            label = { Text(option.second) },
                            leadingIcon = if (needType == option.first) {
                                { Icon(Icons.Default.Schedule, contentDescription = null) }
                            } else {
                                null
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFFFEDD5),
                                selectedLabelColor = Color(0xFF9A3412)
                            )
                        )
                    }
                }

                if (needType == "scheduled") {
                    Text(
                        text = "Pick any day in the next 7 days",
                        style = MaterialTheme.typography.bodySmall.copy(color = EmployerColors.TextSecondary)
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items((1..7).toList()) { offset ->
                            FilterChip(
                                selected = selectedDayOffset == offset,
                                onClick = { selectedDayOffset = offset },
                                label = { Text(buildDayChipLabel(offset)) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFDCFCE7),
                                    selectedLabelColor = Color(0xFF166534)
                                )
                            )
                        }
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(
                            listOf(
                                9 to "9:00 AM",
                                13 to "1:00 PM",
                                17 to "5:00 PM",
                                20 to "8:00 PM"
                            )
                        ) { option ->
                            FilterChip(
                                selected = selectedHour == option.first,
                                onClick = { selectedHour = option.first },
                                label = { Text(option.second) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFE0E7FF),
                                    selectedLabelColor = Color(0xFF3730A3)
                                )
                            )
                        }
                    }
                    Text(
                        text = scheduleLabel,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color(0xFF166534),
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }
        }

        item {
            UrgentNeedSectionCard(title = "Pay and reach") {
                OutlinedTextField(
                    value = budgetText,
                    onValueChange = { budgetText = it },
                    label = { Text("Budget") },
                    placeholder = { Text("Rs 500, hourly, negotiable") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp)
                )

                SectionLabel("Search radius")
                Text(
                    text = "Instant works are limited to 10 km.",
                    style = MaterialTheme.typography.bodySmall.copy(color = EmployerColors.TextSecondary)
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
                            category = effectiveCategory,
                            needType = needType,
                            budgetText = budgetText,
                            radiusKm = radiusKm,
                            scheduledAtMillis = if (needType == "scheduled") {
                                buildScheduleMillis(selectedDayOffset, selectedHour)
                            } else {
                                0L
                            },
                            scheduleLabel = scheduleLabel
                        )
                    ) { requestId ->
                        Toast.makeText(context, "Urgent need posted", Toast.LENGTH_SHORT).show()
                        onPosted(requestId)
                    }
                },
                enabled = !state.isPostingUrgentNeed && canPost,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EmployerColors.Primary)
            ) {
                if (state.isPostingUrgentNeed) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(end = 8.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                }
                Text("Post urgent need")
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
private fun UrgentNeedSectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
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
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    color = EmployerColors.TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            )
            content()
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge.copy(
            color = EmployerColors.TextPrimary,
            fontWeight = FontWeight.SemiBold
        )
    )
}

private fun buildScheduleMillis(dayOffset: Int, hour: Int): Long {
    val zone = ZoneId.systemDefault()
    return LocalDate.now(zone)
        .plusDays(dayOffset.coerceIn(1, 7).toLong())
        .atTime(hour, 0)
        .atZone(zone)
        .toInstant()
        .toEpochMilli()
}

private fun buildScheduleLabel(dayOffset: Int, hour: Int): String {
    val date = LocalDate.now(ZoneId.systemDefault()).plusDays(dayOffset.coerceIn(1, 7).toLong())
    val dateText = date.format(DateTimeFormatter.ofPattern("EEE, dd MMM"))
    val hourText = when (hour) {
        9 -> "9:00 AM"
        13 -> "1:00 PM"
        17 -> "5:00 PM"
        20 -> "8:00 PM"
        else -> "$hour:00"
    }
    return "$dateText at $hourText"
}

private fun buildDayChipLabel(dayOffset: Int): String {
    val date = LocalDate.now(ZoneId.systemDefault()).plusDays(dayOffset.coerceIn(1, 7).toLong())
    return when (dayOffset) {
        1 -> "Tomorrow"
        2 -> "After tomorrow"
        else -> date.format(DateTimeFormatter.ofPattern("EEE, dd MMM"))
    }
}
