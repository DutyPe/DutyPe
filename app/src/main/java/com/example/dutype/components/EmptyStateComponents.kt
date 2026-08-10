package com.example.dutype.components

import com.dutype.app.R
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.dutype.ui.theme.AppTypography
import com.example.dutype.ui.theme.WorkerColors
import androidx.compose.ui.res.stringResource

/**
 * CENTRALIZED EMPTY STATE COMPONENTS
 * 
 * Provides reusable empty state UI patterns for consistent UX across the app.
 * Each composable is designed to be flexible via parameters while maintaining
 * a unified look and feel.
 * 
 * Usage patterns:
 * 1. List Empty State: EmptyListState (when no items)
 * 2. Search Empty State: EmptySearchState (when search returns no results)
 * 3. Location-Based Empty State: EmptyLocationState (when no items near location)
 * 4. Action-Based Empty State: EmptyActionState (when no items, with CTA)
 * 
 * BEST PRACTICE: Use callbacks rather than direct navigation to keep components decoupled.
 * 
 * @author DutyPe Engineering Team
 * @since 2.3.0
 */

// ================= 1. BASIC LIST EMPTY STATE =================

/**
 * Generic empty state when list has no items.
 * Use this as the baseline for most empty states.
 * 
 * @param icon Icon to display
 * @param title Main title text
 * @param subtitle Subtitle/description text
 * @param actionButton Optional action button (primary CTA)
 * @param secondaryAction Optional secondary action button
 * 
 * @sample
 * EmptyListState(
 *     icon = Icons.Default.Work,
 *     title = "No jobs applied yet",
 *     subtitle = "Start exploring and find your next opportunity",
 *     actionButton = EmptyStateAction(label = "Browse Jobs") { navigateToHome() }
 * )
 */
@Composable
fun EmptyListState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    containerColor: Color = WorkerColors.ScreenBackground,
    actionButton: EmptyStateAction? = null,
    secondaryAction: EmptyStateAction? = null
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(containerColor)
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Icon with subtle background
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(WorkerColors.ChipBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = WorkerColors.IconSecondary
                )
            }

            // Title and subtitle
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = title,
                    style = AppTypography.emptyStateTitle.copy(
                        color = WorkerColors.TextPrimary
                    ),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = subtitle,
                    style = AppTypography.emptyStateSubtitle.copy(
                        color = WorkerColors.TextSecondary
                    ),
                    textAlign = TextAlign.Center
                )
            }

            // Action buttons
            if (actionButton != null || secondaryAction != null) {
                Spacer(modifier = Modifier.height(8.dp))
                
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth(0.7f)
                ) {
                    actionButton?.let {
                        Button(
                            onClick = it.onClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = WorkerColors.Primary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = it.icon ?: Icons.Default.Search,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = it.label,
                                style = AppTypography.buttonMedium
                            )
                        }
                    }
                    
                    secondaryAction?.let {
                        TextButton(
                            onClick = it.onClick,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = it.label,
                                color = com.example.dutype.ui.theme.WorkerColors.TextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

// ================= 2. SEARCH EMPTY STATE =================

/**
 * Empty state when search returns no results.
 * Shows "no results found" with the search query.
 * 
 * @param searchQuery The query that returned no results
 * @param onClearSearch Callback when user wants to clear search
 * 
 * @sample
 * EmptySearchState(
 *     searchQuery = "xyz",
 *     onClearSearch = { clearSearchQuery() }
 * )
 */
@Composable
fun EmptySearchState(
    searchQuery: String,
    modifier: Modifier = Modifier,
    containerColor: Color = WorkerColors.ScreenBackground,
    onClearSearch: () -> Unit = {}
) {
    EmptyListState(
        modifier = modifier,
        containerColor = containerColor,
        icon = Icons.Default.SearchOff,
        title = stringResource(R.string.no_results_found),
        subtitle = stringResource(R.string.empty_search_subtitle, searchQuery),
        actionButton = EmptyStateAction(
            label = stringResource(R.string.clear_search),
            onClick = onClearSearch
        )
    )
}

// ================= 3. LOCATION-BASED EMPTY STATE =================

/**
 * Empty state when no jobs are found near current location.
 * Shows suggested cities and location change CTA.
 * 
 * @param categoryFilter Current category filter (e.g., "Plumbing")
 * @param suggestedCities List of nearby cities to suggest
 * @param onChangeLocation Callback to navigate to location selection
 * @param onCitySuggestionClick Callback when user clicks a suggested city
 * 
 * @sample
 * EmptyLocationState(
 *     categoryFilter = "Plumbing",
 *     suggestedCities = listOf("Delhi", "Bangalore", "Mumbai"),
 *     onChangeLocation = { openLocationPicker() },
 *     onCitySuggestionClick = { city -> searchInCity(city) }
 * )
 */
@Composable
fun EmptyLocationState(
    categoryFilter: String = "All Jobs",
    suggestedCities: List<String> = emptyList(),
    modifier: Modifier = Modifier,
    onChangeLocation: () -> Unit = {},
    onCitySuggestionClick: (String) -> Unit = {}
) {
    val humorMessages = listOf(
        "We checked nearby jobs, but nothing matches right now.",
        "No jobs available here at the moment.",
        "Nothing relevant has come in for this area yet.",
        "No nearby jobs are available right now."
    )
    val humorMessage = humorMessages.random()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(WorkerColors.ScreenBackground)
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            EmptyStateIcon(
                imageVector = Icons.Default.LocationOn,
                tint = WorkerColors.Primary,
                size = 72
            )

            // Title
            Text(
                text = "No $categoryFilter nearby",
                style = AppTypography.emptyStateTitle.copy(
                    color = WorkerColors.TextPrimary
                ),
                textAlign = TextAlign.Center
            )

            // Subtitle with humor
            Text(
                text = humorMessage,
                style = AppTypography.emptyStateSubtitle.copy(
                    color = WorkerColors.TextSecondary,
                    textAlign = TextAlign.Center
                )
            )
        }
    }
}

// ================= 4. ACTION-BASED EMPTY STATE =================

/**
 * Empty state with primary action button.
 * Use for cases where user needs to take an action to create content
 * (e.g., "Post your first job").
 * 
 * @param icon Icon to display
 * @param title Main title
 * @param subtitle Description
 * @param actionLabel Primary button label
 * @param onAction Callback when action button is clicked
 * 
 * @sample
 * EmptyActionState(
 *     icon = Icons.Default.Work,
 *     title = "No jobs posted yet",
 *     subtitle = "Start building your team",
 *     actionLabel = "Post Your First Job",
 *     onAction = { navigateToPostJob() }
 * )
 */
@Composable
fun EmptyActionState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    actionLabel: String,
    modifier: Modifier = Modifier,
    onAction: () -> Unit = {}
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(WorkerColors.ScreenBackground)
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Icon with circular background
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(WorkerColors.ChipBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = WorkerColors.IconSecondary
                )
            }

            // Text
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = title,
                    style = AppTypography.emptyStateTitle.copy(
                        color = WorkerColors.TextPrimary
                    ),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = subtitle,
                    style = AppTypography.emptyStateSubtitle.copy(
                        color = WorkerColors.TextSecondary
                    ),
                    textAlign = TextAlign.Center
                )
            }

            // Action button
            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(
                    containerColor = WorkerColors.Primary
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(0.7f)
            ) {
                Text(
                    text = actionLabel,
                    style = AppTypography.buttonMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ================= 5. SAVED ITEMS EMPTY STATE =================

/**
 * Empty state for saved/bookmarked items (jobs, profiles, etc).
 * 
 * @param itemType Type of item (e.g., "jobs")
 * @param onBrowse Callback to navigate to browse screen
 * 
 * @sample
 * EmptySavedItemsState(
 *     itemType = "jobs",
 *     onBrowse = { navigateToHome() }
 * )
 */
@Composable
fun EmptySavedItemsState(
    itemType: String = "jobs",
    modifier: Modifier = Modifier,
    containerColor: Color = WorkerColors.ScreenBackground,
    onBrowse: () -> Unit = {}
) {
    EmptyListState(
        modifier = modifier,
        containerColor = containerColor,
        icon = Icons.Default.Bookmark,
        title = stringResource(R.string.no_saved_item_type, itemType),
        subtitle = stringResource(R.string.save_item_type_later, itemType),
        actionButton = EmptyStateAction(
            label = stringResource(R.string.browse_item_type, itemType.replaceFirstChar { it.uppercase() }),
            onClick = onBrowse
        )
    )
}

// ================= DATA CLASSES =================

/**
 * Data class for empty state action buttons
 * 
 * @param label Button text
 * @param icon Optional icon (defaults to Search)
 * @param onClick Callback when button is clicked
 */
data class EmptyStateAction(
    val label: String,
    val icon: ImageVector? = null,
    val onClick: () -> Unit
)

// ================= MODULAR BUILDING BLOCKS =================

/**
 * Reusable empty state icon with circular background
 */
@Composable
fun EmptyStateIcon(
    imageVector: ImageVector,
    modifier: Modifier = Modifier,
    tint: Color = WorkerColors.IconSecondary,
    size: Int = 72
) {
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(WorkerColors.ChipBackground),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = null,
            modifier = Modifier.size((size / 2).dp),
            tint = tint
        )
    }
}

/**
 * Reusable empty state text section (title + subtitle)
 */
@Composable
fun EmptyStateText(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    titleColor: Color = WorkerColors.TextPrimary,
    subtitleColor: Color = WorkerColors.TextSecondary
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        Text(
            text = title,
            style = AppTypography.emptyStateTitle.copy(color = titleColor),
            textAlign = TextAlign.Center
        )
        Text(
            text = subtitle,
            style = AppTypography.emptyStateSubtitle.copy(color = subtitleColor),
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Reusable empty state button group
 */
@Composable
fun EmptyStateButtons(
    primaryAction: EmptyStateAction?,
    secondaryAction: EmptyStateAction? = null,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxWidth(0.7f)
    ) {
        primaryAction?.let {
            Button(
                onClick = it.onClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = WorkerColors.Primary
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = it.icon ?: Icons.Default.Search,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = it.label,
                    style = AppTypography.buttonMedium
                )
            }
        }
        
        secondaryAction?.let {
            TextButton(
                onClick = it.onClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = it.label,
                    color = com.example.dutype.ui.theme.WorkerColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
