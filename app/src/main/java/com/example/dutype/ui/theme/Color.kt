package com.example.dutype.ui.theme

import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// Global system bar colors - PERMANENT for whole app
val AppStatusBarBlue = Color(0xFF0066FF) // Light blue status bar
val AppNavigationBarBlack = Color(0xFF000000) // Black navigation bar

// Premium Colors
val PrimaryBlue = Color(0xFF2563EB)
val SecondaryTeal = Color(0xFF0D9488)
val TertiaryGold = Color(0xFFD97706)

val DarkBackground = Color(0xFF0F172A)
val LightBackground = Color(0xFFFFFFFF)  // Pure white background
val SurfaceDark = Color(0xFF1E293B)
val SurfaceLight = Color(0xFFFFFFFF)

// ============================================
// MEESHO-STYLE DESIGN SYSTEM COLORS
// ============================================

/**
 * Worker Screen Colors - Gray Gradient theme
 * Background: Professional gray gradient for home screen only
 * Cards: Pure white (#FFFFFF) with subtle elevation
 */
object WorkerColors {
    // Home Screen Gradient Colors - Black-based gradient (professional dark theme)
    val HomeGradientStart = Color(0xFF000000)      // Pure black
    val HomeGradientMiddle = Color(0xFF1A1A1A)     // Very dark gray
    val HomeGradientEnd = Color(0xFF2D2D2D)        // Dark gray
    
    // Status Bar Color - pure white for worker home
    val StatusBarColor = Color(0xFFFFFFFF)
    
    // Screen Backgrounds - White for consistent worker screens
    val ScreenBackground = Color(0xFFFFFFFF)       // White for worker home, my jobs, saved, applied, history
    val CardBackground = Color(0xFFFFFFFF)         // Pure white cards
    
    // Text Colors
    val TextPrimary = Color(0xFF1F2937)            // Dark gray for primary text
    val TextSecondary = Color(0xFF6B7280)          // Medium gray for secondary text
    val TextTertiary = Color(0xFF9CA3AF)           // Light gray for hints/captions
    val TextDisabled = Color(0xFFD1D5DB)           // Disabled text
    
    // Brand Colors
    val Primary = Color(0xFF570DF8)                // Purple (Meesho-like)
    val PrimaryLight = Color(0xFFE8DEFF)           // Light purple background
    val Secondary = Color(0xFFFF6B6B)              // Coral/Pink accent
    val Accent = Color(0xFF10B981)                 // Green for success
    
    // Status Colors
    val Success = Color(0xFF10B981)                // Green
    val SuccessLight = Color(0xFFD1FAE5)           // Light green background
    val Warning = Color(0xFFF59E0B)                // Amber
    val WarningLight = Color(0xFFFEF3C7)           // Light amber background
    val Error = Color(0xFFEF4444)                  // Red
    val ErrorLight = Color(0xFFFEE2E2)             // Light red background
    val Info = Color(0xFF3B82F6)                   // Blue
    val InfoLight = Color(0xFFDBEAFE)              // Light blue background
    
    // Dividers & Borders
    val Divider = Color(0xFFF3F4F6)                // Very light gray divider
    val Border = Color(0xFFE5E7EB)                 // Light gray border
    val BorderFocused = Color(0xFF570DF8)          // Purple when focused
    
    // Icon Colors
    val IconPrimary = Color(0xFF374151)            // Dark gray icons
    val IconSecondary = Color(0xFF9CA3AF)          // Light gray icons
    val IconAccent = Color(0xFF570DF8)             // Purple accent icons
    
    // Badge Colors
    val BadgeNew = Color(0xFF570DF8)               // Purple "New" badge
    val BadgeHot = Color(0xFFEF4444)               // Red "Hot" badge
    val BadgeVerified = Color(0xFF10B981)          // Green verified badge
    
    // Chip Colors
    val ChipBackground = Color(0xFFF3F4F6)         // Light gray chip background
    val ChipSelectedBackground = Color(0xFFE8DEFF) // Light purple selected
    val ChipText = Color(0xFF374151)               // Dark gray chip text
    val ChipSelectedText = Color(0xFF570DF8)       // Purple selected text
    
    // Shimmer Colors
    val ShimmerBase = Color(0xFFE5E7EB)            // Base shimmer color
    val ShimmerHighlight = Color(0xFFF9FAFB)       // Shimmer highlight
    
    // Bottom Navigation - Meesho style
    val BottomNavBackground = Color(0xFFFFFFFF)    // White bottom nav
    val BottomNavSelected = Color(0xFF1F2937)      // Dark gray/black for selected (Meesho style)
    val BottomNavUnselected = Color(0xFF9CA3AF)    // Gray unselected
    
    // Card Elevation Shadow
    val CardShadow = Color(0x0D000000)             // Very subtle shadow (5% black)
}

/**
 * Employer Screen Colors - Light Blue Gradient theme
 * Used for employer-side screens with a professional light blue gradient
 */
object EmployerColors {
    // Home Screen Gradient Colors - reduced blue intensity
    val HomeGradientStart = Color(0xFFF2F8FD)
    val HomeGradientMiddle = Color(0xFFEDF5FB)
    val HomeGradientEnd = Color(0xFFE9F2FA)
    
    // Status Bar Color - lighter blue tint to match reduced home background
    val StatusBarColor = Color(0xFFF2F8FD)
    
    // Screen Backgrounds - White for all other screens
    val ScreenBackground = Color(0xFFFFFFFF)       // Pure white for all screens except home
    val CardBackground = Color(0xFFFFFFFF)         // Pure white cards
    
    // Text Colors
    val TextPrimary = Color(0xFF1E293B)            // Dark slate for primary text
    val TextSecondary = Color(0xFF64748B)          // Slate gray for secondary text
    val TextTertiary = Color(0xFF94A3B8)           // Light slate for hints/captions
    val TextDisabled = Color(0xFFCBD5E1)           // Disabled text
    
    // Brand Colors - Blue theme
    val Primary = Color(0xFF2563EB)                // Professional blue
    val PrimaryLight = Color(0xFFDBEAFE)           // Light blue background
    val Secondary = Color(0xFF0EA5E9)              // Sky blue accent
    val SecondaryLight = Color(0xFFE0F2FE)         // Light sky blue
    val Accent = Color(0xFF0284C7)                 // Darker sky blue
    
    // Status Colors
    val Success = Color(0xFF10B981)                // Green
    val SuccessLight = Color(0xFFD1FAE5)           // Light green background
    val Warning = Color(0xFFF59E0B)                // Amber
    val WarningLight = Color(0xFFFEF3C7)           // Light amber background
    val Error = Color(0xFFEF4444)                  // Red
    val ErrorLight = Color(0xFFFEE2E2)             // Light red background
    val Info = Color(0xFF0EA5E9)                   // Sky blue
    val InfoLight = Color(0xFFE0F2FE)              // Light sky blue background
    
    // Dividers & Borders
    val Divider = Color(0xFFF1F5F9)                // Very light slate divider
    val Border = Color(0xFFE2E8F0)                 // Light slate border
    val BorderFocused = Color(0xFF2563EB)          // Blue when focused
    
    // Icon Colors
    val IconPrimary = Color(0xFF334155)            // Dark slate icons
    val IconSecondary = Color(0xFF94A3B8)          // Light slate icons
    val IconAccent = Color(0xFF2563EB)             // Blue accent icons
    
    // Badge Colors
    val BadgeNew = Color(0xFF2563EB)               // Blue "New" badge
    val BadgeHot = Color(0xFFEF4444)               // Red "Hot" badge
    val BadgeVerified = Color(0xFF10B981)          // Green verified badge
    
    // Chip Colors
    val ChipBackground = Color(0xFFF1F5F9)         // Light slate chip background
    val ChipSelectedBackground = Color(0xFFDBEAFE) // Light blue selected
    val ChipText = Color(0xFF334155)               // Dark slate chip text
    val ChipSelectedText = Color(0xFF2563EB)       // Blue selected text
    
    // Bottom Navigation - Professional blue style
    val BottomNavBackground = Color(0xFFFFFFFF)    // White bottom nav
    val BottomNavSelected = Color(0xFF2563EB)      // Blue for selected
    val BottomNavUnselected = Color(0xFF94A3B8)    // Slate gray unselected
    
    // Card Elevation Shadow
    val CardShadow = Color(0x0D000000)             // Very subtle shadow (5% black)
}

