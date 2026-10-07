package com.example.dutype.worker.components

import com.example.dutype.ui.theme.fg
import com.example.dutype.ui.theme.bg
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dutype.employer.models.JobCategory
import com.example.dutype.utils.JobCategoryResolver

/**
 * Category look shared by the Find Jobs category rail and every job card tile, so a
 * Driver job shows the same green truck in the rail and on its card.
 */
private val JobCategoryPalette = listOf(
    Color(0xFFF5F3FF) to Color(0xFF7C3AED),
    Color(0xFFECFEFF) to Color(0xFF0891B2),
    Color(0xFFFFF7ED) to Color(0xFFEA580C),
    Color(0xFFFDF2F8) to Color(0xFFDB2777),
    Color(0xFFF1F5F9) to Color(0xFF0F172A)
)

/** (tile background, icon tint) for a category display name ("Driver", "All Jobs", ...). */
fun jobCategoryTint(label: String): Pair<Color, Color> = when (label) {
    "All Jobs" -> Color(0xFFF1F5F9) to Color(0xFF0F172A)
    "Electrician" -> Color(0xFFFEF3C7) to Color(0xFFD97706)
    "Plumber" -> Color(0xFFEFF6FF) to Color(0xFF2563EB)
    "Driver" -> Color(0xFFF0FDF4) to Color(0xFF16A34A)
    "Cook" -> Color(0xFFFEF2F2) to Color(0xFFDC2626)
    else -> JobCategoryPalette[(label.hashCode() and 0x7fffffff) % JobCategoryPalette.size]
}

/** Material icon for the categories. Uses company/business icon for other/unmapped. */
fun jobCategoryIcon(label: String): ImageVector = when (label) {
    "All Jobs" -> Icons.Default.Apps
    "Electrician" -> Icons.Default.Bolt
    "Plumber" -> Icons.Default.Build
    "Driver" -> Icons.Default.LocalShipping
    "Cook" -> Icons.Default.Restaurant
    "Other", "OTHER" -> Icons.Default.Business
    else -> Icons.Default.Business
}

/** Best category for a job: its stored type, else inferred from title/description. */
fun resolveJobCategory(category: String, title: String, description: String = ""): JobCategory {
    val explicit = JobCategory.entries.firstOrNull { it.name == category }
        ?: JobCategoryResolver.enumNameForDisplay(category)
            ?.let { name -> runCatching { JobCategory.valueOf(name) }.getOrNull() }
    return explicit?.takeIf { it != JobCategory.OTHER }
        ?: JobCategoryResolver.inferCategory(title, description)
        ?: JobCategory.OTHER
}

/** Rounded square with the category icon on its tint, as used in the rail and card tiles. */
@Composable
fun JobCategoryIconTile(
    category: String,
    title: String,
    description: String = "",
    size: Dp = 48.dp,
    cornerRadius: Dp = 14.dp,
    modifier: Modifier = Modifier
) {
    val resolved = remember(category, title, description) { resolveJobCategory(category, title, description) }
    val (lightContainer, lightTint) = jobCategoryTint(resolved.displayName)
    val container = lightContainer.bg()
    val tint = lightTint.fg()
    val icon = jobCategoryIcon(resolved.displayName)
    Box(
        modifier = modifier
            .size(size)
            .background(container, RoundedCornerShape(cornerRadius)),
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icon, contentDescription = resolved.displayName, tint = tint, modifier = Modifier.size(size * 0.5f))
    }
}
