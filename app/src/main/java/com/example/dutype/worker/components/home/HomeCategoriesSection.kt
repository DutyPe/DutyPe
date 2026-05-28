package com.example.dutype.worker.components.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
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
import com.dutype.app.R
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
    val icon: ImageVector
)

@Composable
fun HomeCategoriesSection(
    onCategoryClick: (String) -> Unit,
    onViewAllClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val categories = listOf(
        CategoryItem("Delivery", CategoryIcon.forDisplayName("Delivery")),
        CategoryItem("Shop Helper", CategoryIcon.forDisplayName("Shop Helper")),
        CategoryItem("Housekeeping", CategoryIcon.forDisplayName("Housekeeping")),
        CategoryItem("Construction", CategoryIcon.forDisplayName("Construction")),
        CategoryItem("Events", CategoryIcon.forDisplayName("Events")),
        CategoryItem("Kitchen", CategoryIcon.forDisplayName("Kitchen")),
        CategoryItem("Driver", CategoryIcon.forDisplayName("Driver")),
        CategoryItem("Security", CategoryIcon.forDisplayName("Security")),
        CategoryItem("Electrician", CategoryIcon.forDisplayName("Electrician")),
        CategoryItem("Plumber", CategoryIcon.forDisplayName("Plumber"))
    )
    
    Column(modifier = modifier.fillMaxWidth()) {
        // View all button only (no title)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier
                    .clickable { onViewAllClick() }
                    .padding(vertical = 4.dp, horizontal = 4.dp)
            ) {
                Text(
                    text = stringResource(R.string.view_all),
                    style = AppTypography.buttonSmall.copy(
                        color = WorkerColors.TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                )
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = WorkerColors.IconSecondary,
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
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .width(68.dp)
    ) {
        // Icon container - bigger size with bordered style
        Card(
            modifier = Modifier.size(60.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.Transparent
            ),
            shape = RoundedCornerShape(14.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            border = BorderStroke(1.dp, WorkerColors.Border)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = category.icon,
                    contentDescription = category.name,
                    tint = WorkerColors.IconAccent,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(6.dp))
        
        // Category name
        Text(
            text = category.name,
            style = AppTypography.labelSmall.copy(
                color = WorkerColors.TextPrimary,
                fontWeight = FontWeight.Medium,
                fontSize = 10.sp
            ),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
