package com.example.dutype.common

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.dutype.MainActivity
import com.example.dutype.utils.LocaleHelper

/**
 * Enhanced Language Selection Screen
 * 
 * Features:
 * - Beautiful modern UI with animations
 * - Shows all text in the SELECTED language (not mixed)
 * - Smooth transitions and visual feedback
 * - Works for both settings and first-time setup
 * 
 * @author DutyPe Engineering Team
 * @since 2.2.0
 */

// Color palette
private val AccentBlue = Color(0xFF3B82F6)
private val AccentPurple = Color(0xFF8B5CF6)
private val TextDark = Color(0xFF1F2937)
private val TextGray = Color(0xFF6B7280)
private val BackgroundLight = Color(0xFFF9FAFB)
private val SuccessGreen = Color(0xFF10B981)

// Language data with translations
data class LanguageData(
    val code: String,
    val emoji: String,
    val nameInEnglish: String,
    val nameInNative: String,
    // Translations for UI elements
    val chooseLanguageTitle: String,
    val chooseLanguageSubtitle: String,
    val continueButton: String,
    val confirmTitle: String,
    val confirmMessage: String,
    val yesButton: String,
    val noButton: String,
    val infoText: String,
    val changeAnytime: String
)

private val englishData = LanguageData(
    code = LocaleHelper.LANGUAGE_ENGLISH,
    emoji = "🇬🇧",
    nameInEnglish = "English",
    nameInNative = "English",
    chooseLanguageTitle = "Choose Your Language",
    chooseLanguageSubtitle = "Select your preferred language",
    continueButton = "Continue",
    confirmTitle = "Change Language?",
    confirmMessage = "App will restart to apply the new language",
    yesButton = "Yes, Change",
    noButton = "Cancel",
    infoText = "App will restart to apply changes",
    changeAnytime = "You can change this anytime in Settings"
)

private val teluguData = LanguageData(
    code = LocaleHelper.LANGUAGE_TELUGU,
    emoji = "🇮🇳",
    nameInEnglish = "Telugu",
    nameInNative = "తెలుగు",
    chooseLanguageTitle = "మీ భాషను ఎంచుకోండి",
    chooseLanguageSubtitle = "మీకు ఇష్టమైన భాషను ఎంచుకోండి",
    continueButton = "కొనసాగించు",
    confirmTitle = "భాష మార్చాలా?",
    confirmMessage = "కొత్త భాషను వర్తింపజేయడానికి యాప్ రీస్టార్ట్ అవుతుంది",
    yesButton = "అవును, మార్చు",
    noButton = "రద్దు చేయి",
    infoText = "మార్పులను వర్తింపజేయడానికి యాప్ రీస్టార్ట్ అవుతుంది",
    changeAnytime = "మీరు దీన్ని ఎప్పుడైనా సెట్టింగ్స్‌లో మార్చవచ్చు"
)

private fun getLanguageData(code: String): LanguageData {
    return when (code) {
        LocaleHelper.LANGUAGE_TELUGU -> teluguData
        else -> englishData
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSelectionScreen(
    navController: NavController,
    onLanguageChanged: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val currentLanguage = remember { LocaleHelper.getLanguage(context) }
    var selectedLanguage by remember { mutableStateOf(currentLanguage) }
    var showRestartDialog by remember { mutableStateOf(false) }
    
    // Get translations based on selected language
    val translations = getLanguageData(selectedLanguage)
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Translate,
                            contentDescription = null,
                            tint = AccentBlue,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = translations.chooseLanguageTitle,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack, 
                            contentDescription = "Back",
                            tint = TextDark
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(BackgroundLight)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            // Animated header illustration
            AnimatedLanguageIcon(selectedLanguage = selectedLanguage)
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Title - shows in selected language
            Text(
                text = translations.chooseLanguageTitle,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = TextDark,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = translations.chooseLanguageSubtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = TextGray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
            
            Spacer(modifier = Modifier.height(40.dp))
            
            // Language options
            EnhancedLanguageCard(
                languageData = englishData,
                isSelected = selectedLanguage == LocaleHelper.LANGUAGE_ENGLISH,
                onClick = {
                    if (selectedLanguage != LocaleHelper.LANGUAGE_ENGLISH) {
                        selectedLanguage = LocaleHelper.LANGUAGE_ENGLISH
                        if (currentLanguage != LocaleHelper.LANGUAGE_ENGLISH) {
                            showRestartDialog = true
                        }
                    }
                }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            EnhancedLanguageCard(
                languageData = teluguData,
                isSelected = selectedLanguage == LocaleHelper.LANGUAGE_TELUGU,
                onClick = {
                    if (selectedLanguage != LocaleHelper.LANGUAGE_TELUGU) {
                        selectedLanguage = LocaleHelper.LANGUAGE_TELUGU
                        if (currentLanguage != LocaleHelper.LANGUAGE_TELUGU) {
                            showRestartDialog = true
                        }
                    }
                }
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Info card - shows in selected language
            InfoCard(translations = translations)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Change anytime text
            Text(
                text = translations.changeAnytime,
                style = MaterialTheme.typography.bodySmall,
                color = TextGray,
                textAlign = TextAlign.Center
            )
        }
    }
    
    // Restart confirmation dialog - shows in selected language
    if (showRestartDialog) {
        RestartConfirmationDialog(
            translations = translations,
            onConfirm = {
                LocaleHelper.saveLanguage(context, selectedLanguage)
                
                // Restart the app
                val intent = Intent(context, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                context.startActivity(intent)
                
                (context as? Activity)?.finish()
            },
            onDismiss = {
                showRestartDialog = false
                selectedLanguage = currentLanguage
            }
        )
    }
}

@Composable
private fun AnimatedLanguageIcon(selectedLanguage: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "icon_animation")
    
    val rotation by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rotation"
    )
    
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )
    
    Box(
        modifier = Modifier
            .size(120.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    colors = listOf<Color>(
                        AccentBlue.copy(alpha = 0.15f),
                        AccentPurple.copy(alpha = 0.1f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Background globe
        Icon(
            imageVector = Icons.Default.Language,
            contentDescription = null,
            modifier = Modifier
                .size(80.dp)
                .graphicsLayer { rotationZ = rotation },
            tint = AccentBlue.copy(alpha = 0.3f)
        )
        
        // Foreground emoji
        Text(
            text = if (selectedLanguage == LocaleHelper.LANGUAGE_TELUGU) "🇮🇳" else "🇬🇧",
            fontSize = 48.sp,
            modifier = Modifier.graphicsLayer { rotationZ = -rotation }
        )
    }
}

@Composable
private fun EnhancedLanguageCard(
    languageData: LanguageData,
    isSelected: Boolean,
    onClick: () -> Unit
) {
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
        targetValue = if (isSelected) AccentBlue else Color(0xFFE5E7EB),
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
            .border(
                width = borderWidth,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
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
                    text = languageData.emoji,
                    fontSize = 26.sp
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Language names
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = languageData.nameInNative,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextDark
                )
                if (languageData.nameInNative != languageData.nameInEnglish) {
                    Text(
                        text = languageData.nameInEnglish,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextGray
                    )
                }
            }
            
            // Selection indicator - clean checkmark
            AnimatedVisibility(
                visible = isSelected,
                enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(AccentBlue),
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

@Composable
private fun InfoCard(translations: LanguageData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFEF3C7)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFCD34D).copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Text("💡", fontSize = 20.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = translations.infoText,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF92400E),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun RestartConfirmationDialog(
    translations: LanguageData,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = Color.White,
        icon = {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(AccentBlue.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Translate,
                    contentDescription = null,
                    tint = AccentBlue,
                    modifier = Modifier.size(32.dp)
                )
            }
        },
        title = {
            Text(
                text = translations.confirmTitle,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Text(
                text = translations.confirmMessage,
                textAlign = TextAlign.Center,
                color = TextGray,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentBlue
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = translations.yesButton,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = TextGray
                )
            ) {
                Text(
                    text = translations.noButton,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    )
}
