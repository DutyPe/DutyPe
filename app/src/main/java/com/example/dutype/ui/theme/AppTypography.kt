package com.example.dutype.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * DutyPe App Typography System
 * Standardized text styles for consistent UI across the entire app
 * 
 * Usage: Import AppTypography and use like AppTypography.screenTitle
 */
object AppTypography {
    
    // ============================================
    // SCREEN TITLES (Headers, Navigation)
    // ============================================
    
    /**
     * Main screen title - Used in CommonHeader and top-level screens
     * Example: "Notifications", "My Jobs", "Profile"
     */
    val screenTitle = TextStyle(
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.sp,
        lineHeight = 28.sp
    )
    
    /**
     * Large display title - Used for hero sections, onboarding
     * Example: "How can we help you today?"
     */
    val displayTitle = TextStyle(
        fontSize = 28.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = (-0.5).sp,
        lineHeight = 36.sp
    )
    
    /**
     * Page title - Used for main content headers
     * Example: "Profile", "Dashboard"
     */
    val pageTitle = TextStyle(
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.sp,
        lineHeight = 32.sp
    )
    
    // ============================================
    // SECTION HEADERS
    // ============================================
    
    /**
     * Section header - Used for grouping content
     * Example: "App Settings", "Recent Jobs", "Your Dashboard"
     */
    val sectionHeader = TextStyle(
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.sp,
        lineHeight = 24.sp
    )
    
    /**
     * Subsection header - Used for smaller groupings
     * Example: "Contact Information", "Job Details"
     */
    val subsectionHeader = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.sp,
        lineHeight = 22.sp
    )
    
    // ============================================
    // CARD & LIST ITEM TITLES
    // ============================================
    
    /**
     * Card title - Primary text in cards
     * Example: Job title, Company name
     */
    val cardTitle = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.sp,
        lineHeight = 22.sp
    )
    
    /**
     * List item title - Primary text in list items
     * Example: Setting name, Menu item
     */
    val listItemTitle = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.sp,
        lineHeight = 22.sp
    )
    
    // ============================================
    // BODY TEXT
    // ============================================
    
    /**
     * Body large - Primary body text
     * Example: Descriptions, paragraphs
     */
    val bodyLarge = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.sp,
        lineHeight = 24.sp
    )
    
    /**
     * Body medium - Secondary body text
     * Example: Subtitles, secondary info
     */
    val bodyMedium = TextStyle(
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
        fontSize = 12.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.sp,
        lineHeight = 16.sp
    )
    
    // ============================================
    // LABELS & CAPTIONS
    // ============================================
    
    /**
     * Label large - Form labels, chip text
     * Example: Input labels, filter chips
     */
    val labelLarge = TextStyle(
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
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.sp,
        lineHeight = 16.sp
    )
    
    /**
     * Label small - Smallest labels
     * Example: Unread count, tiny badges
     */
    val labelSmall = TextStyle(
        fontSize = 10.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.sp,
        lineHeight = 14.sp
    )
    
    /**
     * Caption - Helper text, hints
     * Example: Form hints, timestamps
     */
    val caption = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.sp,
        lineHeight = 16.sp
    )
    
    // ============================================
    // BUTTONS & INTERACTIVE
    // ============================================
    
    /**
     * Button large - Primary buttons
     * Example: "Continue", "Apply Now"
     */
    val buttonLarge = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.sp,
        lineHeight = 22.sp
    )
    
    /**
     * Button medium - Secondary buttons
     * Example: "View Details", "Cancel"
     */
    val buttonMedium = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.sp,
        lineHeight = 20.sp
    )
    
    /**
     * Button small - Tertiary buttons, text buttons
     * Example: "See all", "Clear"
     */
    val buttonSmall = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.sp,
        lineHeight = 16.sp
    )
    
    // ============================================
    // SPECIAL STYLES
    // ============================================
    
    /**
     * Stat number - Large numbers in stats
     * Example: "24", "156"
     */
    val statNumber = TextStyle(
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.sp,
        lineHeight = 32.sp
    )
    
    /**
     * Price/Amount - Money values
     * Example: "₹500/day", "₹15,000"
     */
    val price = TextStyle(
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.sp,
        lineHeight = 24.sp
    )
    
    /**
     * Status text - Status badges
     * Example: "Active", "Pending", "Expired"
     */
    val status = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.sp,
        lineHeight = 16.sp
    )
    
    /**
     * Tab text - Tab bar labels
     * Example: "All Jobs", "Applied", "Saved"
     */
    val tab = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.sp,
        lineHeight = 20.sp
    )
    
    /**
     * Empty state title - Empty state headers
     * Example: "No jobs found", "No notifications"
     */
    val emptyStateTitle = TextStyle(
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.sp,
        lineHeight = 24.sp
    )
    
    /**
     * Empty state subtitle - Empty state descriptions
     * Example: "Your saved jobs will appear here"
     */
    val emptyStateSubtitle = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.sp,
        lineHeight = 20.sp
    )
}
