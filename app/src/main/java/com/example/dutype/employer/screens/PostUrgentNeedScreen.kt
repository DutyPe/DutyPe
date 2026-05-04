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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.dutype.firestore.FirestoreCollections
import com.example.dutype.models.InstantHelpDefaults
import com.example.dutype.models.QuickUrgentNeedInput
import com.example.dutype.navigation.Routes
import com.example.dutype.ui.theme.EmployerColors
import com.example.dutype.utils.JobCategoryResolver
import com.example.dutype.viewmodels.InstantHelpViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

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
    var needType by rememberSaveable { mutableStateOf("urgent_now") }
    var budgetText by rememberSaveable { mutableStateOf("") }
    var radiusKm by rememberSaveable { mutableStateOf(5.0) }
    var notes by rememberSaveable { mutableStateOf("") }
    var contactNumber by rememberSaveable { mutableStateOf("") }
    var whatsappNumber by rememberSaveable { mutableStateOf("") }
    var whatsappSameAsContact by rememberSaveable { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val authPhone = FirebaseAuth.getInstance().currentUser?.phoneNumber.orEmpty()
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        val profilePhone = if (userId.isNullOrBlank()) {
            authPhone
        } else {
            runCatching {
                FirebaseFirestore.getInstance()
                    .collection(FirestoreCollections.EMPLOYER_PROFILES)
                    .document(userId)
                    .get()
                    .await()
            }.getOrNull()?.let { snapshot ->
                snapshot.getString("phone")
                    ?: snapshot.getString("phoneNumber")
                    ?: snapshot.getString("contactNumber")
            }.orEmpty().ifBlank { authPhone }
        }
        if (contactNumber.isBlank()) contactNumber = profilePhone
        if (whatsappNumber.isBlank()) whatsappNumber = profilePhone
    }

    val inferredCategory = JobCategoryResolver.inferCategory(title, notes)
    val effectiveCategory = inferredCategory?.displayName ?: "Helper"
    val scheduleLabel = if (needType == "scheduled") {
        buildTomorrowScheduleLabel()
    } else {
        ""
    }
    val canPost = title.trim().length >= 3 &&
        contactNumber.trim().isNotBlank() &&
        (whatsappSameAsContact || whatsappNumber.trim().isNotBlank())

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
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Urgent jobs expire automatically",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = Color(0xFF92400E),
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = "Now and today posts stay open for 24 hours. Tomorrow posts stay open for 48 hours.",
                            style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF78350F))
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

                AutoPickedUrgentCategory(
                    category = effectiveCategory,
                    hasTitle = title.trim().length >= 3
                )

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
                            "scheduled" to "Tomorrow"
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

                Text(
                    text = if (needType == "scheduled") {
                        "$scheduleLabel posts expire in 48 hours."
                    } else {
                        "This urgent job will expire in 24 hours."
                    },
                    style = MaterialTheme.typography.bodySmall.copy(color = EmployerColors.TextSecondary)
                )

                if (needType == "scheduled") {
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

        item {
            UrgentNeedSectionCard(title = "Contact numbers") {
                OutlinedTextField(
                    value = contactNumber,
                    onValueChange = { value ->
                        contactNumber = value
                        if (whatsappSameAsContact) whatsappNumber = value
                    },
                    label = { Text("Contact number") },
                    placeholder = { Text("+91 phone number") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Checkbox(
                        checked = whatsappSameAsContact,
                        onCheckedChange = { checked ->
                            whatsappSameAsContact = checked
                            if (checked) whatsappNumber = contactNumber
                        }
                    )
                    Text(
                        text = "WhatsApp number is same as contact number",
                        style = MaterialTheme.typography.bodyMedium.copy(color = EmployerColors.TextPrimary)
                    )
                }

                if (!whatsappSameAsContact) {
                    OutlinedTextField(
                        value = whatsappNumber,
                        onValueChange = { whatsappNumber = it },
                        label = { Text("WhatsApp number") },
                        placeholder = { Text("+91 WhatsApp number") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
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
                            category = effectiveCategory,
                            needType = needType,
                            contactNumber = contactNumber,
                            whatsappNumber = if (whatsappSameAsContact) contactNumber else whatsappNumber,
                            budgetText = budgetText,
                            radiusKm = radiusKm,
                            scheduledAtMillis = if (needType == "scheduled") {
                                buildTomorrowScheduleMillis()
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
private fun AutoPickedUrgentCategory(category: String, hasTitle: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = "Auto-picked category",
                style = MaterialTheme.typography.labelMedium.copy(
                    color = Color(0xFF1D4ED8),
                    fontWeight = FontWeight.SemiBold
                )
            )
            Text(
                text = if (hasTitle) category else "Type the work title to detect category",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color(0xFF1E3A8A),
                    fontWeight = FontWeight.Bold
                )
            )
        }
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

private fun buildTomorrowScheduleMillis(): Long {
    val zone = ZoneId.systemDefault()
    return LocalDate.now(zone)
        .plusDays(1)
        .atTime(9, 0)
        .atZone(zone)
        .toInstant()
        .toEpochMilli()
}

private fun buildTomorrowScheduleLabel(): String {
    val date = LocalDate.now(ZoneId.systemDefault()).plusDays(1)
    val dateText = date.format(DateTimeFormatter.ofPattern("EEE, dd MMM"))
    return "Tomorrow, $dateText"
}
