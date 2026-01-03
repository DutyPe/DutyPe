package com.example.dutype.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * LanguageOptionCard - Clean and modern language selection card
 * 
 * Design:
 * - White background for both selected and unselected states
 * - Clean border highlight for selected state
 * - Subtle shadow for depth
 * - Checkmark indicator for selection
 * 
 * Used by:
 * - OnboardingScreen (FirstTimeLanguageSelection)
 * - LanguageSelectionScreen (Settings)
 * 
 * @param emoji Flag emoji for the language
 * @param name English name of the language
 * @param nativeName Native name of the language
 * @param isSelected Whether this language is currently selected
 * @param accentColor The accent color for selection highlight
 * @param onClick Callback when card is clicked
 * 
 * @author DutyPe Engineering Team
 * @since 2.2.0
 */
@Composable
fun LanguageOptionCard(
    emoji: String,
    name: String,
    nativeName: String,
    isSelected: Boolean,
    accentColor: Color = Color(0xFFFF8C32), // Default orange
    onClick: () -> Unit
) {
    // Animation states
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.01f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "card_scale"
    )
    
    val borderWidth by animateDpAsState(
        targetValue = if (isSelected) 2.dp else 1.dp,
        animationSpec = tween(200),
        label = "border_width"
    )
    
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) accentColor else Color(0xFFE5E7EB),
        animationSpec = tween(200),
        label = "border_color"
    )
    
    val elevation by animateDpAsState(
        targetValue = if (isSelected) 4.dp else 1.dp,
        animationSpec = tween(200),
        label = "elevation"
    )
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .shadow(
                elevation = elevation,
                shape = RoundedCornerShape(16.dp),
                spotColor = if (isSelected) accentColor.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.1f)
            )
            .border(
                width = borderWidth,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Flag emoji with subtle background
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF8F9FA)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = emoji,
                    fontSize = 26.sp
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Language names
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = nativeName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1F2937)
                )
                if (nativeName != name) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF6B7280)
                    )
                }
            }
            
            // Selection indicator - clean checkmark
            AnimatedVisibility(
                visible = isSelected,
                enter = scaleIn(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ) + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(accentColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * Data class for language option
 */
data class LanguageOption(
    val code: String,
    val name: String,
    val nativeName: String,
    val emoji: String
)
