package com.example.dutype.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ElectricalServices
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Plumbing
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.dutype.employer.models.JobCategory

object CategoryIcon {
    fun forJobCategory(category: JobCategory): ImageVector = when (category) {
        JobCategory.COOK, JobCategory.WAITER -> Icons.Default.Restaurant
        JobCategory.MAID -> Icons.Default.CleaningServices
        JobCategory.DRIVER -> Icons.Default.DirectionsCar
        JobCategory.HELPER, JobCategory.CARETAKER -> Icons.Default.Groups
        JobCategory.SECURITY -> Icons.Default.Security
        JobCategory.GARDENER -> Icons.Default.Category
        JobCategory.DELIVERY, JobCategory.PACKER -> Icons.Default.LocalShipping
        JobCategory.ELECTRICIAN -> Icons.Default.ElectricalServices
        JobCategory.PLUMBER -> Icons.Default.Plumbing
        JobCategory.PAINTER -> Icons.Default.Brush
        JobCategory.CARPENTER, JobCategory.MECHANIC -> Icons.Default.Handyman
        JobCategory.RECEPTIONIST, JobCategory.OFFICE_STAFF -> Icons.Default.BusinessCenter
        JobCategory.CASHIER, JobCategory.FINANCE -> Icons.Default.AccountBalance
        JobCategory.SALES -> Icons.Default.Storefront
        JobCategory.TELECALLER -> Icons.Default.Phone
        JobCategory.TEACHER -> Icons.Default.School
        JobCategory.CUSTOMER_SUPPORT -> Icons.Default.SupportAgent
        JobCategory.FIELD_EXECUTIVE -> Icons.Default.Work
        JobCategory.MARKETING -> Icons.Default.Campaign
        JobCategory.HEALTHCARE -> Icons.Default.LocalHospital
        JobCategory.BEAUTICIAN, JobCategory.TAILOR -> Icons.Default.ContentCut
        JobCategory.DATA_ENTRY -> Icons.Default.Keyboard
        JobCategory.LEGAL -> Icons.Default.Gavel
        JobCategory.OTHER -> Icons.Default.Category
    }

    fun forDisplayName(name: String): ImageVector = when (name.trim().lowercase()) {
        "all", "all jobs" -> Icons.Default.Work
        "delivery" -> Icons.Default.LocalShipping
        "shop helper" -> Icons.Default.Storefront
        "housekeeping" -> Icons.Default.CleaningServices
        "construction" -> Icons.Default.Construction
        "events" -> Icons.Default.Event
        "kitchen" -> Icons.Default.Restaurant
        "driver" -> Icons.Default.DirectionsCar
        "security" -> Icons.Default.Security
        "electrician" -> Icons.Default.ElectricalServices
        "plumber" -> Icons.Default.Plumbing
        else -> JobCategory.entries
            .firstOrNull { it.displayName.equals(name, ignoreCase = true) }
            ?.let(::forJobCategory)
            ?: Icons.Default.Category
    }
}