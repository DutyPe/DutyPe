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
import android.content.Intent
import android.widget.Toast
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import com.example.dutype.location.TopCityChips
import com.example.dutype.services.LocationDemandService
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer

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
 * Deduplicates tokens in location name strings (e.g. "Nizamabad, Nizamabad, Telangana" -> "Nizamabad, Telangana" or "Nizamabad, Nizamabad" -> "Nizamabad").
 */
fun formatCleanLocationName(rawLocation: String?): String? {
    if (rawLocation.isNullOrBlank()) return null
    val parts = rawLocation.split(",")
        .map { it.trim() }
        .filter { it.isNotBlank() }
    
    val uniqueParts = mutableListOf<String>()
    for (part in parts) {
        if (!uniqueParts.any { it.equals(part, ignoreCase = true) }) {
            uniqueParts.add(part)
        }
    }
    return when {
        uniqueParts.isEmpty() -> null
        uniqueParts.size == 1 -> uniqueParts[0]
        else -> uniqueParts.take(2).joinToString(", ")
    }
}

/**
 * PROMISSORY EXPANSION EMPTY STATE
 *
 * Rendered when no jobs exist in the worker's current area, city, or state.
 * Instead of a dead-end message, this promises upcoming expansion, registers
 * their location alert interest in Firestore, allows 1-tap WhatsApp sharing to local employers,
 * lets users explore active nearby hubs, and provides a direct support action.
 */
@Composable
fun DutyPeExpandingLocationState(
    modifier: Modifier = Modifier,
    locationName: String? = null,
    categoryFilter: String? = null,
    suggestedCities: List<TopCityChips.CityLocationChip> = emptyList(),
    onCitySelected: ((TopCityChips.CityLocationChip) -> Unit)? = null,
    onHelpDesk: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val cleanLocation = remember(locationName) {
        formatCleanLocationName(locationName)
    }
    
    var isNotified by remember(cleanLocation) {
        mutableStateOf(LocationDemandService.isLocationNotified(context, cleanLocation))
    }

    // Micro-animation for rocket badge
    val infiniteTransition = rememberInfiniteTransition(label = "expansion_anim")
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float_offset"
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val displayTitle = if (!cleanLocation.isNullOrBlank()) {
        stringResource(R.string.dutype_expanding_title, cleanLocation)
    } else {
        stringResource(R.string.dutype_expanding_title_generic)
    }

    // Strict priority hub order: Hyderabad -> Nizamabad -> Andhra Pradesh -> Karnataka -> West Bengal -> Maharashtra
    val defaultHubs = remember(cleanLocation) {
        TopCityChips.defaultCities.filterNot { chip ->
            !cleanLocation.isNullOrBlank() && (
                chip.city.equals(cleanLocation, ignoreCase = true) ||
                cleanLocation.contains(chip.city, ignoreCase = true)
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = WorkerColors.CardBackground,
            border = BorderStroke(1.dp, WorkerColors.Border.copy(alpha = 0.5f)),
            shadowElevation = 0.dp
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 24.dp)
            ) {
                // Animated Expansion Graphic
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(80.dp)
                ) {
                    // Soft pulsing breathing halo
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .graphicsLayer {
                                scaleX = pulseScale
                                scaleY = pulseScale
                            }
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        WorkerColors.Primary.copy(alpha = 0.25f),
                                        Color(0xFF6366F1).copy(alpha = 0.10f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    // Floating Rocket Circle
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .graphicsLayer {
                                translationY = floatOffset
                            }
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        WorkerColors.Primary.copy(alpha = 0.18f),
                                        Color(0xFF6366F1).copy(alpha = 0.18f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🚀",
                            style = MaterialTheme.typography.headlineMedium
                        )
                    }
                }

                // Title (Clean, deduplicated location name)
                Text(
                    text = displayTitle,
                    style = AppTypography.emptyStateTitle.copy(
                        fontWeight = FontWeight.Bold,
                        color = WorkerColors.TextPrimary,
                        fontSize = 19.sp
                    ),
                    textAlign = TextAlign.Center
                )

                // Description / Promise
                Text(
                    text = stringResource(R.string.dutype_expanding_desc),
                    style = AppTypography.emptyStateSubtitle.copy(
                        color = WorkerColors.TextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier.padding(horizontal = 6.dp)
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Action 1: Notify Me / Priority Alert (Solid, filled, user-friendly button)
                if (!isNotified) {
                    Button(
                        onClick = {
                            LocationDemandService.recordLocationDemand(
                                context = context,
                                locationName = cleanLocation ?: "Current Area",
                                category = categoryFilter
                            ) {
                                isNotified = true
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.dutype_notified_success),
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = WorkerColors.Primary,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 2.dp,
                            pressedElevation = 4.dp
                        ),
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .height(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.dutype_notify_me_btn),
                            style = AppTypography.buttonMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFE8F5E9))
                            .border(
                                width = 1.dp,
                                color = Color(0xFF81C784),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.dutype_notified_success),
                            style = AppTypography.bodyMedium.copy(
                                color = Color(0xFF1B5E20),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            ),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Action 2: Tell Local Employers / WhatsApp Share (Clean filled button)
                Button(
                    onClick = {
                        val shareLocation = cleanLocation ?: "your city"
                        val shareText = context.getString(R.string.dutype_share_employer_msg, shareLocation)
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, shareText)
                            type = "text/plain"
                        }
                        val shareIntent = Intent.createChooser(sendIntent, context.getString(R.string.dutype_share_employer_btn))
                        context.startActivity(shareIntent)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (com.example.dutype.ui.theme.isAppInDarkTheme()) Color(0xFF334155) else Color(0xFFF1F5F9),
                        contentColor = WorkerColors.TextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .height(46.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        tint = Color(0xFF25D366),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.dutype_share_employer_btn),
                        style = AppTypography.bodySmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = WorkerColors.TextPrimary
                        ),
                        textAlign = TextAlign.Center
                    )
                }

                // Action 3: Explore active hubs in India (Ordered strictly: Telangana -> AP -> Karnataka -> WB -> Maharashtra)
                if (onCitySelected != null && defaultHubs.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = stringResource(R.string.dutype_explore_active_hubs),
                        style = AppTypography.bodySmall.copy(
                            color = WorkerColors.TextTertiary,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp
                        ),
                        textAlign = TextAlign.Center
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(defaultHubs) { chip ->
                            FilterChip(
                                selected = false,
                                onClick = { onCitySelected(chip) },
                                label = {
                                    Text(
                                        text = "${chip.city} (${chip.state})",
                                        style = AppTypography.bodySmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        modifier = Modifier.size(13.dp),
                                        tint = WorkerColors.Primary
                                    )
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = WorkerColors.ChipBackground,
                                    labelColor = WorkerColors.TextPrimary
                                )
                            )
                        }
                    }
                }

                // Action 4: Help Desk / Support
                if (onHelpDesk != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    HorizontalDivider(
                        modifier = Modifier.fillMaxWidth(0.9f),
                        color = WorkerColors.Border.copy(alpha = 0.5f),
                        thickness = 0.8.dp
                    )
                    Spacer(modifier = Modifier.height(2.dp))

                    OutlinedButton(
                        onClick = onHelpDesk,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(0.92f),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF6366F1)
                        ),
                        border = BorderStroke(1.2.dp, Color(0xFF6366F1).copy(alpha = 0.35f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.HelpOutline,
                            contentDescription = null,
                            tint = Color(0xFF6366F1),
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Need help? Contact DutyPe Support",
                            style = AppTypography.bodySmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF6366F1),
                                fontSize = 12.5.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

/**
 * Empty state when no jobs are found near current location.
 * Shows promising expansion card with notifications and suggested active hubs.
 */
@Composable
fun EmptyLocationState(
    categoryFilter: String = "All Jobs",
    locationName: String? = null,
    suggestedCities: List<TopCityChips.CityLocationChip> = emptyList(),
    modifier: Modifier = Modifier,
    onChangeLocation: () -> Unit = {},
    onCitySuggestionClick: (TopCityChips.CityLocationChip) -> Unit = {},
    onHelpDesk: (() -> Unit)? = null
) {
    DutyPeExpandingLocationState(
        modifier = modifier,
        locationName = locationName,
        categoryFilter = categoryFilter,
        suggestedCities = suggestedCities,
        onCitySelected = onCitySuggestionClick,
        onHelpDesk = onHelpDesk
    )
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

// ================= 8. LOCATION PERMISSION EMPTY STATE & BOTTOM SHEET =================

/**
 * Modern, friendly empty state shown when location permission has not been granted.
 * Encourages the user to enable location to discover hyper-local jobs around them.
 */
@Composable
fun LocationPermissionRequiredState(
    modifier: Modifier = Modifier,
    onRequestPermissionClick: () -> Unit
) {
    val context = LocalContext.current
    val isTelugu = com.example.dutype.utils.LocaleHelper.getLanguage(context) == com.example.dutype.utils.LocaleHelper.LANGUAGE_TELUGU

    val title = if (isTelugu) "మీ సమీపంలోని ఉద్యోగాలను చూడండి" else "Find Jobs Near You"
    val subtitle = if (isTelugu)
        "మీ పరిసరాల్లోని ఉద్యోగ ఖాళీలను చూడటానికి దయచేసి లొకేషన్ అనుమతిని ఆన్ చేయండి."
    else
        "Enable location to discover verified job vacancies and daily wage work in your area."
    val buttonText = if (isTelugu) "లొకేషన్ ఆన్ చేయండి" else "Enable Location"

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clickable { onRequestPermissionClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Icon Badge with pulse glow effect
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF2563EB).copy(alpha = 0.18f),
                                Color(0xFF2563EB).copy(alpha = 0.06f)
                            )
                        )
                    )
                    .border(2.dp, Color(0xFF2563EB).copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = Color(0xFF2563EB),
                    modifier = Modifier.size(38.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF0F172A)
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color(0xFF64748B),
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onRequestPermissionClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2563EB),
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = buttonText,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                )
            }
        }
    }
}

/**
 * Smooth Modal Bottom Sheet that explains why DutyPe needs location access,
 * providing users with full transparency before triggering the system Android dialog.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationPermissionBottomSheet(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onAllowLocation: () -> Unit
) {
    if (!isVisible) return

    val context = LocalContext.current
    val isTelugu = com.example.dutype.utils.LocaleHelper.getLanguage(context) == com.example.dutype.utils.LocaleHelper.LANGUAGE_TELUGU

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 6.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFFCBD5E1))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Compact Header: Icon + Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEFF6FF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Color(0xFF2563EB),
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = if (isTelugu) "లొకేషన్ అనుమతి ఎందుకు అవసరం?" else "Why We Need Location",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = Color(0xFF0F172A)
                        )
                    )
                    Text(
                        text = if (isTelugu) "మీ ఏరియా ఉద్యోగాలను వేగంగా చూడటానికి" else "To show nearby jobs in your area",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF64748B),
                            fontSize = 12.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Compact Benefits Card (All 3 in 1 lightweight container)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF8FAFC))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CompactBenefitRow(
                    emoji = "📍",
                    text = if (isTelugu) "మీకు 5–25 కి.మీ సమీపంలోని ఉద్యోగాలు" else "Jobs within 5–25 km of your location"
                )
                CompactBenefitRow(
                    emoji = "⏱️",
                    text = if (isTelugu) "ఖచ్చితమైన ప్రయాణ దూరం మరియు సమయం" else "Accurate travel distance & commute time"
                )
                CompactBenefitRow(
                    emoji = "🔒",
                    text = if (isTelugu) "100% సురక్షితం · అనుమతి లేకుండా షేర్ చేయబడదు" else "100% Safe · Never shared without consent"
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Primary CTA: Allow Location Access (Always 100% visible)
            Button(
                onClick = onAllowLocation,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2563EB),
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isTelugu) "లొకేషన్ అనుమతించండి" else "Allow Location Access",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Secondary CTA: Not Now
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFF475569)
                )
            ) {
                Text(
                    text = if (isTelugu) "ఇప్పుడు వద్దు" else "Not Now",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun CompactBenefitRow(
    emoji: String,
    text: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = emoji, fontSize = 15.sp)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                color = Color(0xFF334155)
            )
        )
    }
}
