package com.example.dutype.components

import android.app.Activity
import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dutype.MainActivity
import com.example.dutype.utils.LocaleHelper

private val BrandBluePrimary = Color(0xFF2563EB)
private val SelectedBg = Color(0xFFEFF6FF)
private val SoftBlueBg = Color(0xFFDBEAFE)
private val Ink900 = Color(0xFF0F172A)
private val Ink600 = Color(0xFF475569)
private val BorderColor = Color(0xFFE2E8F0)

data class LanguageItem(
    val code: String,
    val name: String,
    val nativeName: String
)

private val languages = listOf(
    LanguageItem(
        code = LocaleHelper.LANGUAGE_ENGLISH,
        name = "English",
        nativeName = "English"
    ),
    LanguageItem(
        code = "hi",
        name = "Hindi",
        nativeName = "हिन्दी"
    ),
    LanguageItem(
        code = LocaleHelper.LANGUAGE_TELUGU,
        name = "Telugu",
        nativeName = "తెలుగు"
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSelectionBottomSheet(
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    val context = LocalContext.current
    val currentLanguage = remember { LocaleHelper.getLanguage(context) }
    var selectedLanguage by remember { mutableStateOf(currentLanguage) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 6.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFFCBD5E1))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header Bar: "Select Language" (No back arrow)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Select Language",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Ink900
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Language Options List matching Image 2
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                languages.forEach { item ->
                    val isSelected = selectedLanguage == item.code
                    LanguageCardItem(
                        item = item,
                        isSelected = isSelected,
                        onClick = {
                            if (selectedLanguage != item.code) {
                                selectedLanguage = item.code
                                LocaleHelper.saveLanguage(context, item.code)
                                LocaleHelper.setLocale(context.applicationContext, item.code)
                                LocaleHelper.setLocale(context, item.code)

                                onDismiss()

                                val activity = context as? Activity
                                if (activity != null) {
                                    val intent = Intent(activity, MainActivity::class.java).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                    }
                                    activity.startActivity(intent)
                                    activity.finish()
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun LanguageCardItem(
    item: LanguageItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) SelectedBg else Color.White
        ),
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) BrandBluePrimary else BorderColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Ink900
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.nativeName,
                    style = MaterialTheme.typography.bodySmall.copy(color = Ink600)
                )
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = BrandBluePrimary,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Ink600,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
