package com.example.dutype.worker.components.home

import com.dutype.app.R
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dutype.components.CategoryIcon
import com.example.dutype.ui.theme.AppTypography
import com.example.dutype.ui.theme.WorkerColors

/**
 * P2 PERFORMANCE FIX: Extracted HomeCategoriesSection composable
 * 
 * Reduces recomposition scope - only this component recomposes when
 * category data changes.
 * 
 * @author DutyPe Engineering Team
 * @since 2.4.0
 */

data class CategoryItem(
    val name: String,
    val emoji: String,
    val nameRes: Int
)

@Composable
fun HomeCategoriesSection(
    onCategoryClick: (String) -> Unit,
    onViewAllClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val categories = listOf(
        CategoryItem("Delivery", "🚴", R.string.category_delivery),
        CategoryItem("Shop Helper", "🏪", R.string.category_shop_helper),
        CategoryItem("Housekeeping", "🧹", R.string.category_housekeeping),
        CategoryItem("Construction", "👷", R.string.category_construction),
        CategoryItem("Events", "🎪", R.string.category_events),
        CategoryItem("Kitchen", "🍳", R.string.category_kitchen),
        CategoryItem("Driver", "🚗", R.string.category_driver),
        CategoryItem("Security", "💂", R.string.category_security),
        CategoryItem("Electrician", "💡", R.string.category_electrician),
        CategoryItem("Plumber", "🔧", R.string.category_plumber)
    )
    
    Column(modifier = modifier.fillMaxWidth()) {
        // Header matching Image 2: "Popular Categories" on left, "See all >" on right
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Popular Categories",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier
                    .clickable { onViewAllClick() }
                    .padding(vertical = 4.dp, horizontal = 4.dp)
            ) {
                Text(
                    text = "See all",
                    style = AppTypography.buttonSmall.copy(
                        color = Color(0xFF64748B),
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp
                    )
                )
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Categories Grid - 5 per row
        val chunkedCategories = categories.chunked(5)
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            chunkedCategories.forEach { rowCategories ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    rowCategories.forEach { category ->
                        CategoryChip(
                            category = category,
                            onClick = { onCategoryClick(category.name) }
                        )
                    }
                    // Fill empty spaces if row has less than 5 items
                    repeat(5 - rowCategories.size) {
                        Spacer(modifier = Modifier.width(68.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryChip(
    category: CategoryItem,
    onClick: () -> Unit
) {
    val categoryName = stringResource(category.nameRes)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current

    // Zomato-style micro-spring bounce: soft scale down on press, springs up on release
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "cat_spring"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(68.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClick()
                }
            )
    ) {
        // Icon container - card with emoji matching Image 2
        Card(
            modifier = Modifier.size(60.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
            border = BorderStroke(0.8.dp, Color(0xFFE2E8F0))
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = category.emoji,
                    fontSize = 25.sp
                )
            }
        }
        
        Spacer(modifier = Modifier.height(6.dp))
        
        // Category name
        Text(
            text = categoryName,
            style = AppTypography.labelSmall.copy(
                color = WorkerColors.TextPrimary,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp
            ),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
