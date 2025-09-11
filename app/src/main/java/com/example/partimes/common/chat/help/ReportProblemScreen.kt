package com.example.partimes.common.chat.help

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.partimes.utils.BackNavigationTopBar
import kotlinx.coroutines.delay

data class ProblemCategory(
    val title: String,
    val description: String,
    val icon: ImageVector
)

@Composable
fun ReportProblemScreen(
    navController: NavController,
    onStatusBarColorChange: (Color) -> Unit
) {
    onStatusBarColorChange(Color.Black)
    var isVisible by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf<ProblemCategory?>(null) }
    var problemDescription by remember { mutableStateOf("") }
    var contactEmail by remember { mutableStateOf("") }
    var isUrgent by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    val problemCategories = remember {
        listOf(
            ProblemCategory(
                title = "App Crashes",
                description = "App closes unexpectedly or freezes",
                icon = Icons.Default.BugReport
            ),
            ProblemCategory(
                title = "Login Issues",
                description = "Can't sign in or authentication problems",
                icon = Icons.AutoMirrored.Filled.Login
            ),
            ProblemCategory(
                title = "Job Application Problems",
                description = "Issues with applying for jobs or submissions failing",
                icon = Icons.Default.Work
            ),
            ProblemCategory(
                title = "Payment Issues",
                description = "Problems with payments or earnings",
                icon = Icons.Default.Payment
            ),
            ProblemCategory(
                title = "Notification Problems",
                description = "Not receiving notifications or wrong alerts",
                icon = Icons.Default.Notifications
            ),
            ProblemCategory(
                title = "Profile/Account Issues",
                description = "Problems with profile updates or account settings",
                icon = Icons.Default.Person
            ),
            ProblemCategory(
                title = "Voice Feature Problems",
                description = "Voice commands not working properly",
                icon = Icons.Default.Mic
            ),
            ProblemCategory(
                title = "Other Issues",
                description = "Any other problem not listed above",
                icon = Icons.Default.MoreHoriz
            )
        )
    }

    LaunchedEffect(Unit) {
        delay(100)
        isVisible = true
    }

    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = {
                showSuccessDialog = false
                navController.popBackStack()
            },
            title = { Text("Report Submitted") },
            text = { Text("Thank you for reporting this issue. Our team will investigate and get back to you within 24 hours.") },
            confirmButton = {
                TextButton(onClick = {
                    showSuccessDialog = false
                    navController.popBackStack()
                }) {
                    Text("OK")
                }
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        )
    }

    Scaffold(
        topBar = {
            BackNavigationTopBar(title = "Report a Problem", navController = navController)
        }
    ) { innerPadding ->
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn() + slideInVertically()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(
                        top = innerPadding.calculateTopPadding(),
                        start = innerPadding.calculateStartPadding(LocalLayoutDirection.current),
                        end = innerPadding.calculateEndPadding(LocalLayoutDirection.current)
                        // Removed bottom padding to prevent white space
                    )
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                // Header Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Report,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Report a Problem",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Help us improve by reporting bugs and issues",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }

                // Problem Category Selection
                Text(
                    text = "What type of problem are you experiencing?",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                problemCategories.forEach { category ->
                    ProblemCategoryCard(
                        category = category,
                        isSelected = selectedCategory == category,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        selectedCategory = category
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Problem Description
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(
                            text = "Describe the problem",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        OutlinedTextField(
                            value = problemDescription,
                            onValueChange = { problemDescription = it },
                            placeholder = { Text("Please describe what happened, when it occurred, and any steps you took...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            maxLines = 5
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Contact email (optional)",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        OutlinedTextField(
                            value = contactEmail,
                            onValueChange = { contactEmail = it },
                            placeholder = { Text("your.email@example.com") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Urgent checkbox
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { isUrgent = !isUrgent }
                        ) {
                            Checkbox(
                                checked = isUrgent,
                                onCheckedChange = { isUrgent = it }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "This is an urgent issue",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Submit Button
                Button(
                    onClick = {
                        if (selectedCategory != null && problemDescription.isNotBlank()) {
                            showSuccessDialog = true
                        }
                    },
                    enabled = selectedCategory != null && problemDescription.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Submit Report",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun ProblemCategoryCard(
    category: ProblemCategory,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected)
            CardDefaults.outlinedCardBorder()
        else
            null,
        modifier = modifier
            .fillMaxWidth()
            .selectable(
                selected = isSelected,
                onClick = onClick
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = category.icon,
                contentDescription = null,
                tint = if (isSelected)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isSelected)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = category.description,
                    fontSize = 14.sp,
                    color = if (isSelected)
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
