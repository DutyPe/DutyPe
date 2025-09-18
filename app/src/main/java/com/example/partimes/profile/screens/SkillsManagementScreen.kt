package com.example.partimes.profile.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.partimes.profile.models.Skill
import com.example.partimes.profile.viewmodels.SkillsManagementViewModel
import com.example.partimes.ui.components.*

/**
 * Skill Item Composable
 */
@Composable
fun SkillItem(
    skill: Skill,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = skill.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${skill.level.name} • ${skill.yearsOfExperience} years",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Outlined.Edit, contentDescription = "Edit Skill")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Outlined.Delete, contentDescription = "Delete Skill")
                }
            }
        }
    }
}

/**
 * Skills Management Screen
 * Add, edit, and manage user skills with proficiency levels
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkillsManagementScreen(
    navController: NavController,
    onStatusBarColorChange: (Color) -> Unit = {},
    viewModel: SkillsManagementViewModel = hiltViewModel()
) {
    val skills by viewModel.skills.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    var showAddSkillDialog by remember { mutableStateOf(false) }
    
    // Set status bar color
    LaunchedEffect(Unit) {
        onStatusBarColorChange(Color.White)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top App Bar
            TopAppBar(
                title = {
                    Text(
                        text = "Skills Management",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showAddSkillDialog = true }) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Add Skill",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF90D5FF)
                )
            )

            // Content
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFF90D5FF)
                    )
                }
            } else if (error != null) {
                ErrorScreen(
                    errorState = ErrorState(
                        type = ErrorType.UNKNOWN_ERROR,
                        title = "Error",
                        message = error!!,
                        icon = Icons.Default.Error,
                        canRetry = true,
                        retryAction = { viewModel.loadSkills() }
                    ),
                    onRetry = { viewModel.loadSkills() }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Skills Summary Card
                    item {
                        SkillsSummaryCard(
                            totalSkills = skills.size,
                            categories = skills.groupBy { it.category }.size
                        )
                    }

                    // Skills by Category
                    val skillsByCategory = skills.groupBy { it.category }
                    skillsByCategory.forEach { (category, categorySkills) ->
                        // Category Header
                        item {
                            Text(
                                text = category.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                        
                        // Skills in this category
                        items(categorySkills) { skill ->
                            SkillItem(
                                skill = skill,
                                onEdit = { viewModel.editSkill(skill) },
                                onDelete = { viewModel.deleteSkill(skill.id) }
                            )
                        }
                    }

                    // Add Skill Button
                    item {
                        AddSkillButton(
                            onClick = { showAddSkillDialog = true }
                        )
                    }

                    // Spacer for bottom padding
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }

        // Add Skill Dialog
        if (showAddSkillDialog) {
            AddSkillDialog(
                onDismiss = { showAddSkillDialog = false },
                onAddSkill = { skill ->
                    viewModel.addSkill(skill)
                    showAddSkillDialog = false
                }
            )
        }
    }
}

@Composable
fun SkillsSummaryCard(
    totalSkills: Int,
    categories: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            SkillStatItem(
                icon = Icons.Outlined.Psychology,
                label = "Total Skills",
                value = totalSkills.toString()
            )
            
            SkillStatItem(
                icon = Icons.Outlined.Category,
                label = "Categories",
                value = categories.toString()
            )
        }
    }
}

@Composable
fun SkillStatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFE8F4FD)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = Color(0xFF90D5FF),
                modifier = Modifier.size(24.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2C3E50)
        )
        
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color(0xFF7F8C8D),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun SkillsCategorySection(
    category: String,
    skills: List<com.example.partimes.profile.models.Skill>,
    onEditSkill: (com.example.partimes.profile.models.Skill) -> Unit,
    onDeleteSkill: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = category,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2C3E50)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(skills) { skill ->
                    SkillChip(
                        skill = skill,
                        onEdit = { onEditSkill(skill) },
                        onDelete = { onDeleteSkill(skill.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun SkillChip(
    skill: com.example.partimes.profile.models.Skill,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    
    Box {
        Card(
            modifier = Modifier.clickable { showMenu = true },
            colors = CardDefaults.cardColors(
                containerColor = when (skill.level) {
                    com.example.partimes.profile.models.SkillLevel.BEGINNER -> Color(0xFFE8F5E8)
                    com.example.partimes.profile.models.SkillLevel.INTERMEDIATE -> Color(0xFFFFF3E0)
                    com.example.partimes.profile.models.SkillLevel.ADVANCED -> Color(0xFFE3F2FD)
                    com.example.partimes.profile.models.SkillLevel.EXPERT -> Color(0xFFF3E5F5)
                    else -> Color(0xFFE8F4FD)
                }
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = skill.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF2C3E50)
                )
                
                Spacer(modifier = Modifier.width(4.dp))
                
                Text(
                    text = skill.level.name,
                    fontSize = 10.sp,
                    color = Color(0xFF7F8C8D)
                )
            }
        }
        
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("Edit") },
                onClick = {
                    onEdit()
                    showMenu = false
                },
                leadingIcon = {
                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                }
            )
            DropdownMenuItem(
                text = { Text("Delete") },
                onClick = {
                    onDelete()
                    showMenu = false
                },
                leadingIcon = {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                }
            )
        }
    }
}

@Composable
fun AddSkillButton(
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F4FD)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = "Add Skill",
                tint = Color(0xFF90D5FF)
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Text(
                text = "Add New Skill",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF90D5FF)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSkillDialog(
    onDismiss: () -> Unit,
    onAddSkill: (com.example.partimes.profile.models.Skill) -> Unit
) {
    var skillName by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(com.example.partimes.profile.models.SkillCategory.TECHNICAL) }
    var selectedProficiency by remember { mutableStateOf(com.example.partimes.profile.models.SkillLevel.INTERMEDIATE) }
    
    val categories = com.example.partimes.profile.models.SkillCategory.values().toList()
    val proficiencyLevels = com.example.partimes.profile.models.SkillLevel.values().toList()
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add New Skill",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = skillName,
                    onValueChange = { skillName = it },
                    label = { Text("Skill Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                var expandedCategory by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expandedCategory,
                    onExpandedChange = { expandedCategory = !expandedCategory }
                ) {
                    OutlinedTextField(
                        value = selectedCategory.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCategory) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedCategory,
                        onDismissRequest = { expandedCategory = false }
                    ) {
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.name) },
                                onClick = {
                                    selectedCategory = category
                                    expandedCategory = false
                                }
                            )
                        }
                    }
                }
                
                var expandedProficiency by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expandedProficiency,
                    onExpandedChange = { expandedProficiency = !expandedProficiency }
                ) {
                    OutlinedTextField(
                        value = selectedProficiency.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Proficiency Level") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedProficiency) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedProficiency,
                        onDismissRequest = { expandedProficiency = false }
                    ) {
                        proficiencyLevels.forEach { proficiency ->
                            DropdownMenuItem(
                                text = { Text(proficiency.name) },
                                onClick = {
                                    selectedProficiency = proficiency
                                    expandedProficiency = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (skillName.isNotBlank()) {
                        val newSkill = com.example.partimes.profile.models.Skill(
                            id = java.util.UUID.randomUUID().toString(),
                            name = skillName,
                            category = selectedCategory,
                            level = selectedProficiency,
                            yearsOfExperience = 0,
                            isVerified = false
                        )
                        onAddSkill(newSkill)
                    }
                },
                enabled = skillName.isNotBlank()
            ) {
                Text("Add Skill")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
