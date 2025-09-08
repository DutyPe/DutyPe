package com.example.partimes.utils

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.with
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun EnhancedAnimatedSearchBar(
    modifier: Modifier = Modifier,
    onSearchQueryChange: (String) -> Unit = {},
    placeholderTexts: List<String> = listOf(
        "Search jobs, companies...",
        "Find your next opportunity...",
        "Discover amazing careers..."
    ),
    searchType: SearchType = SearchType.GENERAL,
    onFilterClick: (() -> Unit)? = null,
    showVoiceSearch: Boolean = true,
    backgroundColor: Color = Color.White,
    focusedBorderColor: Color = Color(0xFF6366F1),
    unfocusedBorderColor: Color = Color(0xFFE5E7EB)
) {
    var searchText by remember { mutableStateOf("") }
    var isVoiceSearchActive by remember { mutableStateOf(false) }
    var currentPlaceholderIndex by remember { mutableStateOf(0) }
    val context = LocalContext.current

    // Animated placeholder text rotation
    LaunchedEffect(Unit) {
        while (true) {
            delay(3000) // Change placeholder every 3 seconds
            currentPlaceholderIndex = (currentPlaceholderIndex + 1) % placeholderTexts.size
        }
    }

    LaunchedEffect(searchText) {
        onSearchQueryChange(searchText)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        OutlinedTextField(
            value = searchText,
            onValueChange = { searchText = it },
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundColor),
            placeholder = {
                AnimatedContent(
                    targetState = currentPlaceholderIndex,
                    transitionSpec = {
                        slideInVertically(
                            animationSpec = tween(500),
                            initialOffsetY = { it }
                        ) + fadeIn(animationSpec = tween(500)) with
                        slideOutVertically(
                            animationSpec = tween(500),
                            targetOffsetY = { -it }
                        ) + fadeOut(animationSpec = tween(500))
                    },
                    label = "placeholder_animation"
                ) { index ->
                    Text(
                        text = placeholderTexts[index],
                        color = Color(0xFF6B7280)
                    )
                }
            },
            leadingIcon = {
                Icon(
                    imageVector = when (searchType) {
                        SearchType.JOBS -> Icons.Default.Work
                        SearchType.COMPANIES -> Icons.Default.Business
                        SearchType.CANDIDATES -> Icons.Default.Person
                        else -> Icons.Default.Search
                    },
                    contentDescription = "Search",
                    tint = if (searchText.isNotEmpty()) focusedBorderColor else Color(0xFF6B7280),
                    modifier = Modifier.size(20.dp)
                )
            },
            trailingIcon = {
                Row {
                    if (searchText.isNotEmpty()) {
                        IconButton(
                            onClick = { searchText = "" }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = Color(0xFF6B7280),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    onFilterClick?.let {
                        IconButton(onClick = it) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = "Filter",
                                tint = Color(0xFF6B7280),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    if (showVoiceSearch) {
                        AnimatedContent(
                            targetState = isVoiceSearchActive,
                            transitionSpec = {
                                scaleIn(animationSpec = tween(200)) + fadeIn() with
                                scaleOut(animationSpec = tween(200)) + fadeOut()
                            },
                            label = "voice_search_animation"
                        ) { isActive ->
                            IconButton(
                                onClick = {
                                    isVoiceSearchActive = !isVoiceSearchActive
                                    if (isVoiceSearchActive) {
                                        Toast.makeText(
                                            context,
                                            "Voice search activated",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Voice Search",
                                    tint = if (isActive) focusedBorderColor else Color(0xFF6B7280),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = focusedBorderColor,
                unfocusedBorderColor = unfocusedBorderColor,
                focusedTextColor = Color(0xFF111827),
                unfocusedTextColor = Color(0xFF111827),
                focusedContainerColor = backgroundColor,
                unfocusedContainerColor = backgroundColor
            ),
            shape = RoundedCornerShape(16.dp),
            singleLine = true
        )
    }
}

@Composable
fun QuickSearchFilters(
    selectedFilter: SearchFilter,
    onFilterSelected: (SearchFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(SearchFilter.values().toList()) { filter ->
            FilterChip(
                selected = selectedFilter == filter,
                onClick = { onFilterSelected(filter) },
                label = {
                    Text(
                        text = filter.displayName,
                        fontWeight = if (selectedFilter == filter) FontWeight.Bold else FontWeight.Normal
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = filter.icon,
                        contentDescription = filter.displayName,
                        modifier = Modifier.size(16.dp)
                    )
                }
            )
        }
    }
}

enum class SearchType {
    GENERAL,
    JOBS,
    COMPANIES,
    CANDIDATES
}

enum class SearchFilter(val displayName: String, val icon: ImageVector) {
    ALL("All", Icons.Default.Search),
    RECENT("Recent", Icons.Default.AccessTime),
    ACTIVE("Active", Icons.Default.CheckCircle),
    POPULAR("Popular", Icons.Default.TrendingUp),
    NEARBY("Nearby", Icons.Default.LocationOn)
}

// Legacy component for backward compatibility
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AnimatedSearchBar(
    modifier: Modifier = Modifier,
    onSearchQueryChange: (String) -> Unit = {},
    placeholderText: String = "Search jobs, companies..."
) {
    EnhancedAnimatedSearchBar(
        modifier = modifier,
        onSearchQueryChange = onSearchQueryChange,
        placeholderTexts = listOf(placeholderText),
        searchType = SearchType.GENERAL,
        showVoiceSearch = true
    )
}
