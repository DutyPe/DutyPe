package com.example.dutype.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.dutype.ui.theme.AppTypography

/**
 * Common header component used across all info and settings screens
 * Provides consistent styling with back button and title
 * 
 * @param title The header title text
 * @param navController Navigation controller for back navigation (optional)
 * @param onBackClick Custom back click handler (optional, uses navController.popBackStack() if not provided)
 * @param backgroundColor Background color of the header (default: White)
 * @param titleColor Color of the title text (default: Black)
 * @param subtitle Optional subtitle text (e.g., "3 unread")
 * @param subtitleColor Color of the subtitle text (default: Gray)
 * @param actions Optional composable for action buttons on the right side
 */
@Composable
fun CommonHeader(
    title: String,
    navController: NavController? = null,
    onBackClick: (() -> Unit)? = null,
    backgroundColor: Color = Color.White,
    titleColor: Color = Color.Black,
    subtitle: String? = null,
    subtitleColor: Color = Color(0xFF6B7280),
    actions: @Composable (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back button
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
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = AppTypography.screenTitle.copy(color = titleColor)
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
        Divider(color = Color(0xFFE5E7EB), thickness = 1.dp)
    }
}
