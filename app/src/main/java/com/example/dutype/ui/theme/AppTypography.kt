package com.example.dutype.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.dutype.app.R

// ============================================
// IBM PLEX SANS - Body / UI text (matches web app)
// ============================================

val IBMPlexSansFamily = FontFamily(
    Font(R.font.ibm_plex_sans_regular, FontWeight.Normal),
    Font(R.font.ibm_plex_sans_medium, FontWeight.Medium),
    Font(R.font.ibm_plex_sans_semibold, FontWeight.SemiBold),
    Font(R.font.ibm_plex_sans_bold, FontWeight.Bold)
)

// ============================================
// SORA - Display / headings (matches web app)
// ============================================

val SoraFamily = FontFamily(
    Font(R.font.sora_regular, FontWeight.Normal),
    Font(R.font.sora_medium, FontWeight.Medium),
    Font(R.font.sora_semibold, FontWeight.SemiBold),
    Font(R.font.sora_bold, FontWeight.Bold)
)

// Keep legacy alias for any remaining references
val MeeshoFontFamily = IBMPlexSansFamily

// ============================================
// MATERIAL 3 TYPOGRAPHY (for Theme.kt)
// ============================================

/**
 * Material 3 Typography configuration
 * Used by MaterialTheme in Theme.kt
 * All styles use MeeshoFontFamily for consistency
 */
val Typography = Typography(
    // Display styles - Sora (display font, matches web)
    displayLarge = TextStyle(
        fontFamily = SoraFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.25).sp
    ),
    displayMedium = TextStyle(
        fontFamily = SoraFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),
    displaySmall = TextStyle(
        fontFamily = SoraFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),
    
    // Headline styles - Sora (display font)
    headlineLarge = TextStyle(
        fontFamily = SoraFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = SoraFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = SoraFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    ),
    
    // Title styles
    titleLarge = TextStyle(
        fontFamily = MeeshoFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = MeeshoFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp
    ),
    titleSmall = TextStyle(
        fontFamily = MeeshoFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    ),
    
    // Body styles
    bodyLarge = TextStyle(
        fontFamily = MeeshoFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = MeeshoFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    ),
    bodySmall = TextStyle(
        fontFamily = MeeshoFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp
    ),
    
    // Label styles
    labelLarge = TextStyle(
        fontFamily = MeeshoFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    ),
    labelMedium = TextStyle(
        fontFamily = MeeshoFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontFamily = MeeshoFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.sp
    )
)

// ============================================
// DUTYPE APP TYPOGRAPHY SYSTEM - MEESHO STYLE
// ============================================

/**
 * DutyPe App Typography System - Meesho Inspired
 * Single font family (SansSerif/Roboto) for consistency
 * 
 * Based on Meesho app design:
 * - Clean, readable fonts
 * - Consistent weight hierarchy
 * - Proper line heights for readability
 * 
 * Usage: Import AppTypography and use like AppTypography.screenTitle
 */
object AppTypography {
    
    // ============================================
    // SCREEN TITLES (Headers, Navigation)
    // ============================================
    
    /**
     * Main screen title - Used in CommonHeader and top-level screens
     * Example: "ACCOUNT", "My Jobs", "Profile"
     * Meesho style: Bold, 16-18sp, uppercase for main headers
     */
    val screenTitle = TextStyle(
        fontFamily = SoraFamily,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.5.sp,
        lineHeight = 22.sp
    )
    
    /**
     * Large display title - Used for hero sections, onboarding
     * Example: "How can we help you today?"
     */
    val displayTitle = TextStyle(
        fontFamily = SoraFamily,
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.sp,
        lineHeight = 32.sp
    )
    
    /**
     * Page title - Used for main content headers
     * Example: "Profile", "Dashboard"
     */
    val pageTitle = TextStyle(
        fontFamily = SoraFamily,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.sp,
        lineHeight = 28.sp
    )
    
    // ============================================
    // SECTION HEADERS (Meesho style)
    // ============================================
    
    /**
     * Section header - Used for grouping content
     * Example: "My Payments", "My Activity", "Others"
     * Meesho style: SemiBold, 14sp, dark gray
     */
    val sectionHeader = TextStyle(
        fontFamily = MeeshoFontFamily,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.sp,
        lineHeight = 20.sp
    )
    
    /**
     * Subsection header - Used for smaller groupings
     * Example: "Contact Information", "Job Details"
     */
    val subsectionHeader = TextStyle(
        fontFamily = MeeshoFontFamily,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.sp,
        lineHeight = 20.sp
    )
    
    // ============================================
    // CARD & LIST ITEM TITLES (Meesho style)
    // ============================================
    
    /**
     * Card title - Primary text in cards
     * Example: Job title, Company name
     * Meesho style: Medium, 15sp
     */
    val cardTitle = TextStyle(
        fontFamily = MeeshoFontFamily,
        fontSize = 15.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.sp,
        lineHeight = 21.sp
    )
    
    /**
     * List item title - Primary text in list items
     * Example: "Bank & UPI Details", "Payment & Refund"
     * Meesho style: Normal, 15sp, dark text
     */
    val listItemTitle = TextStyle(
        fontFamily = MeeshoFontFamily,
        fontSize = 15.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.sp,
        lineHeight = 21.sp
    )
    
    /**
     * Menu item title - Profile menu items, settings items
     * Example: "Ratings & Reviews", "Help & Support"
     * Meesho style: Normal, 15sp
     */
    val menuItemTitle = TextStyle(
        fontFamily = MeeshoFontFamily,
        fontSize = 15.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.sp,
        lineHeight = 21.sp
    )
    
    /**
     * Menu item subtitle - Secondary text in menu items
     * Example: "View and update your profile details"
     * Meesho style: Normal, 13sp, gray
     */
    val menuItemSubtitle = TextStyle(
        fontFamily = MeeshoFontFamily,
        fontSize = 13.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.sp,
        lineHeight = 18.sp
    )
    
    // ============================================
    // BODY TEXT (Meesho style)
    // ============================================
    
    /**
     * Body large - Primary body text
     * Example: Descriptions, paragraphs
     */
    val bodyLarge = TextStyle(
        fontFamily = MeeshoFontFamily,
        fontSize = 15.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.sp,
        lineHeight = 22.sp
    )
    
    /**
     * Body medium - Secondary body text
     * Example: Subtitles, secondary info
     */
    val bodyMedium = TextStyle(
        fontFamily = MeeshoFontFamily,
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.sp,
        lineHeight = 20.sp
    )
    
    /**
     * Body small - Tertiary body text
     * Example: Timestamps, metadata
     */
    val bodySmall = TextStyle(
        fontFamily = MeeshoFontFamily,
        fontSize = 12.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.sp,
        lineHeight = 16.sp
    )
    
    // ============================================
    // LABELS & CAPTIONS (Meesho style)
    // ============================================
    
    /**
     * Label large - Form labels, chip text
     * Example: Input labels, filter chips
     */
    val labelLarge = TextStyle(
        fontFamily = MeeshoFontFamily,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.sp,
        lineHeight = 20.sp
    )
    
    /**
     * Label medium - Secondary labels
     * Example: Badge text, small chips
     */
    val labelMedium = TextStyle(
        fontFamily = MeeshoFontFamily,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.sp,
        lineHeight = 16.sp
    )
    
    /**
     * Label small - Smallest labels
     * Example: Unread count, tiny badges, "New" tags
     * Meesho style: Medium, 11sp
     */
    val labelSmall = TextStyle(
        fontFamily = MeeshoFontFamily,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.sp,
        lineHeight = 14.sp
    )
    
    /**
     * Caption - Helper text, hints
     * Example: Form hints, timestamps
     */
    val caption = TextStyle(
        fontFamily = MeeshoFontFamily,
        fontSize = 12.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.sp,
        lineHeight = 16.sp
    )
    
    // ============================================
    // BUTTONS & INTERACTIVE (Meesho style)
    // ============================================
    
    /**
     * Button large - Primary buttons
     * Example: "Sign up", "Apply Now"
     * Meesho style: SemiBold, 14sp
     */
    val buttonLarge = TextStyle(
        fontFamily = MeeshoFontFamily,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.sp,
        lineHeight = 20.sp
    )
    
    /**
     * Button medium - Secondary buttons
     * Example: "View Details", "Cancel"
     */
    val buttonMedium = TextStyle(
        fontFamily = MeeshoFontFamily,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.sp,
        lineHeight = 18.sp
    )
    
    /**
     * Button small - Tertiary buttons, text buttons
     * Example: "See all", "Clear"
     */
    val buttonSmall = TextStyle(
        fontFamily = MeeshoFontFamily,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.sp,
        lineHeight = 16.sp
    )
    
    // ============================================
    // QUICK ACTION BUTTONS (Meesho style)
    // ============================================
    
    /**
     * Quick action label - Text under quick action icons
     * Example: "Help Centre", "Change Language"
     * Meesho style: Normal, 12sp
     */
    val quickActionLabel = TextStyle(
        fontFamily = MeeshoFontFamily,
        fontSize = 12.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.sp,
        lineHeight = 16.sp
    )
    
    // ============================================
    // SPECIAL STYLES (Meesho style)
    // ============================================
    
    /**
     * Stat number - Large numbers in stats
     * Example: "24", "156"
     */
    val statNumber = TextStyle(
        fontFamily = SoraFamily,
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.sp,
        lineHeight = 28.sp
    )
    
    /**
     * Price/Amount - Money values
     * Example: "₹500/day", "₹15,000"
     */
    val price = TextStyle(
        fontFamily = MeeshoFontFamily,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.sp,
        lineHeight = 22.sp
    )
    
    /**
     * Status text - Status badges
     * Example: "Active", "Pending", "Expired"
     */
    val status = TextStyle(
        fontFamily = MeeshoFontFamily,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.sp,
        lineHeight = 14.sp
    )
    
    /**
     * Tab text - Tab bar labels
     * Example: "All Jobs", "Applied", "Saved"
     */
    val tab = TextStyle(
        fontFamily = MeeshoFontFamily,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.sp,
        lineHeight = 18.sp
    )
    
    /**
     * Bottom nav label - Bottom navigation labels
     * Example: "Home", "Categories", "My Orders"
     * Meesho style: Normal, 11sp
     */
    val bottomNavLabel = TextStyle(
        fontFamily = MeeshoFontFamily,
        fontSize = 11.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.sp,
        lineHeight = 14.sp
    )
    
    /**
     * Bottom nav label selected - Selected bottom nav
     * Meesho style: Medium, 11sp
     */
    val bottomNavLabelSelected = TextStyle(
        fontFamily = MeeshoFontFamily,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.sp,
        lineHeight = 14.sp
    )
    
    /**
     * Empty state title - Empty state headers
     * Example: "No jobs found", "No notifications"
     */
    val emptyStateTitle = TextStyle(
        fontFamily = SoraFamily,
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.sp,
        lineHeight = 22.sp
    )
    
    /**
     * Empty state subtitle - Empty state descriptions
     * Example: "Your saved jobs will appear here"
     */
    val emptyStateSubtitle = TextStyle(
        fontFamily = MeeshoFontFamily,
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.sp,
        lineHeight = 20.sp
    )
    
    /**
     * Footer text - Footer branding text
     * Example: "Made With Love in Bharat 💙"
     */
    val footerText = TextStyle(
        fontFamily = MeeshoFontFamily,
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.sp,
        lineHeight = 20.sp
    )
    
    /**
     * New badge text - "New" tag text
     * Meesho style: Medium, 11sp, purple color
     */
    val newBadge = TextStyle(
        fontFamily = MeeshoFontFamily,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.sp,
        lineHeight = 14.sp
    )
}
