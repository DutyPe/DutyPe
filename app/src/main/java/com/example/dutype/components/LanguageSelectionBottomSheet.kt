package com.example.dutype.components

import android.app.Activity
import android.content.Intent
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dutype.app.R
import com.example.dutype.MainActivity
import com.example.dutype.ui.theme.AppTypography
import com.example.dutype.ui.theme.WorkerColors
import com.example.dutype.utils.LocaleHelper

/**
 * Language Selection Bottom Sheet - Meesho Style
 * 
 * A clean bottom sheet for language selection with:
 * - 2 languages (English and Telugu)
 * - Native script display with emoji
 * - Selection indicator with checkmark
 * - Consistent with app typography
 */

// Language item data
data class LanguageItem(
    val code: String,
    val nativeScript: String,  // Script character like "అ" or "A"
    val nativeName: String,    // Name in native language
    val scriptColor: Color     // Color for the script character
)

private val languages = listOf(
    LanguageItem(
        code = LocaleHelper.LANGUAGE_TELUGU,
        nativeScript = "అ",
        nativeName = "తెలుగు",
        scriptColor = Color(0xFF1F2937)  // Black/Dark gray
    ),
    LanguageItem(
        code = LocaleHelper.LANGUAGE_ENGLISH,
        nativeScript = "A",
        nativeName = "English",
        scriptColor = Color(0xFF1F2937)  // Black/Dark gray
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSelectionBottomSheet(
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState()
) {
    val context = LocalContext.current
    val currentLanguage = remember { LocaleHelper.getLanguage(context) }
    var selectedLanguage by remember { mutableStateOf(currentLanguage) }
    var showRestartDialog by remember { mutableStateOf(false) }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = WorkerColors.CardBackground,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            // Header with close button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.size(24.dp)) // Balance for close button
                
                Text(
                    text = stringResource(R.string.change_language_title),
                    style = AppTypography.screenTitle.copy(
                        color = WorkerColors.TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = WorkerColors.TextSecondary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Language options in 2-column grid
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                languages.forEach { language ->
                    LanguageOptionCard(
                        language = language,
                        isSelected = selectedLanguage == language.code,
                        onClick = {
                            if (selectedLanguage != language.code) {
                                selectedLanguage = language.code
                                if (currentLanguage != language.code) {
                                    showRestartDialog = true
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
    
    // Restart confirmation dialog
    if (showRestartDialog) {
        AlertDialog(
            onDismissRequest = {
                showRestartDialog = false
                selectedLanguage = currentLanguage
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = WorkerColors.CardBackground,
            title = {
                Text(
                    text = stringResource(R.string.change_language_title),
                    style = AppTypography.pageTitle.copy(
                        fontWeight = FontWeight.Bold,
                        color = WorkerColors.TextPrimary
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.change_language_message),
                    style = AppTypography.bodyMedium.copy(
                        color = WorkerColors.TextSecondary
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        LocaleHelper.saveLanguage(context, selectedLanguage)
                        
                        // Restart the app
                        val intent = Intent(context, MainActivity::class.java)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        context.startActivity(intent)
                        
                        (context as? Activity)?.finish()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WorkerColors.TextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.yes_change),
                        style = AppTypography.buttonLarge.copy(
                            color = Color.White
                        ),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showRestartDialog = false
                        selectedLanguage = currentLanguage
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = WorkerColors.TextSecondary
                    )
                ) {
                    Text(
                        text = stringResource(R.string.cancel),
                        style = AppTypography.buttonLarge.copy(
                            color = WorkerColors.TextSecondary
                        ),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        )
    }
}

@Composable
private fun LanguageOptionCard(
    language: LanguageItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) Color(0xFF1F2937) else WorkerColors.Border,
        animationSpec = tween(200),
        label = "border_color"
    )
    
    val borderWidth by animateDpAsState(
        targetValue = if (isSelected) 2.dp else 1.dp,
        animationSpec = tween(200),
        label = "border_width"
    )
    
    Card(
        modifier = modifier
            .border(
                width = borderWidth,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = WorkerColors.CardBackground
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                // Script character box - Black background
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1F2937)), // Black background
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = language.nativeScript,
                        style = AppTypography.pageTitle.copy(
                            color = Color.White, // White text on black
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // Language name
                Text(
                    text = language.nativeName,
                    style = AppTypography.cardTitle.copy(
                        color = WorkerColors.TextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
            
            // Selection checkmark - top right corner (Black)
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 8.dp, y = (-8).dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1F2937)), // Black checkmark background
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
