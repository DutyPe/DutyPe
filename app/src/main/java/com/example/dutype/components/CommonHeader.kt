package com.example.dutype.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.dutype.ui.theme.AppTypography
import com.example.dutype.ui.theme.WorkerColors

/**
 * Common header component used across all info and settings screens
 * Provides consistent styling with optional back button and title
 * 
 * @param title The header title text
 * @param navController Navigation controller for back navigation (optional)
 * @param onBackClick Custom back click handler (optional, uses navController.popBackStack() if not provided)
 * @param showBackButton Whether to show the back button (default: true)
 * @param backgroundColor Background color of the header (default: White)
 * @param titleColor Color of the title text (default: Black)
 * @param subtitle Optional subtitle text (e.g., "3 unread")
 * @param subtitleColor Color of the subtitle text (default: Gray)
 * @param actions Optional composable for action buttons on the right side
 * @param includeStatusBarPadding Whether to include status bar padding (default: true)
 */
@Composable
fun CommonHeader(
    title: String,
    navController: NavController? = null,
    onBackClick: (() -> Unit)? = null,
    showBackButton: Boolean = true,
    backgroundColor: Color = Color.White,
    titleColor: Color = Color(0xFF0F172A),
    subtitle: String? = null,
    subtitleColor: Color = Color(0xFF6B7280),
    actions: @Composable (() -> Unit)? = null,
    includeStatusBarPadding: Boolean = true
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
    ) {
        // Status bar spacer - only if includeStatusBarPadding is true
        if (includeStatusBarPadding) {
            Spacer(modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars))
        }
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back button - only show if showBackButton is true
            if (showBackButton) {
                IconButton(
                    onClick = { 
                        onBackClick?.invoke() ?: navController?.popBackStack()
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = titleColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
            } else {
                Spacer(modifier = Modifier.width(16.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = AppTypography.screenTitle.copy(
                        color = titleColor,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = AppTypography.labelSmall.copy(color = subtitleColor)
                    )
                }
            }
            
            // Action buttons on the right
            if (actions != null) {
                actions()
            }
        }
        HorizontalDivider(color = WorkerColors.Divider, thickness = 1.dp)
    }
}
