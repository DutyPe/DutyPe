package com.example.dutype.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Material Design 3 Icon Sizes
 * 
 * Standardized icon sizes following Google's Material Design guidelines:
 * - Small: 20dp (inline with text, compact UI elements)
 * - Standard: 24dp (navigation, actions, most common use)
 * - Large: 36dp (app bar, headers, prominent actions)
 * - ExtraLarge: 48dp (featured content, hero elements)
 * 
 * @see <a href="https://m3.material.io/">Material Design 3</a>
 */
object IconSizes {
    /**
     * Small icons - 20dp
     * Use for: Inline with text, compact UI elements, secondary actions
     */
    val Small: Dp = 20.dp
    
    /**
     * Standard icons - 24dp (MOST COMMON)
     * Use for: Navigation bars, action buttons, toolbars, most UI icons
     */
    val Standard: Dp = 24.dp
    
    /**
     * Large icons - 36dp
     * Use for: App bar icons, headers, prominent actions
     */
    val Large: Dp = 36.dp
    
    /**
     * Extra large icons - 48dp
     * Use for: Featured content, hero elements, empty states
     */
    val ExtraLarge: Dp = 48.dp
}

/**
 * Material Design 3 Component Heights
 */
object ComponentHeights {
    /**
     * Bottom navigation bar height - 80dp (Material Design 3)
     * Previous: 56dp (Material Design 2)
     */
    val BottomNavigationBar: Dp = 80.dp
    
    /**
     * Top app bar height - 64dp (Material Design 3)
     */
    val TopAppBar: Dp = 64.dp
    
    /**
     * Minimum touch target size - 48dp (Accessibility requirement)
     */
    val MinimumTouchTarget: Dp = 48.dp
}
