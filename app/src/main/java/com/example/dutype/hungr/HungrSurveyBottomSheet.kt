package com.example.dutype.hungr

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dutype.announcements.AnnouncementManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.launch
import timber.log.Timber

private const val PREFS_HUNGR = "dutype_hungr_survey_prefs"
private const val KEY_HUNGR_SUBMITTED = "hungr_survey_submitted"

object HungrSurveyTracker {
    var hasDismissedThisSession: Boolean = false

    fun hasSubmitted(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_HUNGR, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_HUNGR_SUBMITTED, false)) return true

        // Also check if logged-in user or their phone has submitted record in local cache
        val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        val uid = user?.uid
        if (!uid.isNullOrBlank() && prefs.getBoolean("hungr_sub_$uid", false)) return true
        val phone = user?.phoneNumber?.removePrefix("+91")?.trim()
        if (!phone.isNullOrBlank() && prefs.getBoolean("hungr_sub_$phone", false)) return true

        return false
    }

    fun markSubmitted(context: Context, phone: String? = null) {
        hasDismissedThisSession = true
        val prefs = context.getSharedPreferences(PREFS_HUNGR, Context.MODE_PRIVATE)
        val editor = prefs.edit().putBoolean(KEY_HUNGR_SUBMITTED, true)
        if (!phone.isNullOrBlank()) {
            val clean = phone.removePrefix("+91").trim()
            editor.putString("hungr_phone", clean)
            editor.putBoolean("hungr_sub_$clean", true)
        }
        val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        val uid = user?.uid
        if (!uid.isNullOrBlank()) {
            editor.putBoolean("hungr_sub_$uid", true)
        }
        val userPhone = user?.phoneNumber?.removePrefix("+91")?.trim()
        if (!userPhone.isNullOrBlank()) {
            editor.putBoolean("hungr_sub_$userPhone", true)
        }
        editor.apply()
    }

    fun checkAndSyncUser(context: Context) {
        val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser ?: return
        val uid = user.uid
        val phone = user.phoneNumber?.removePrefix("+91")?.trim() ?: ""

        val prefs = context.getSharedPreferences(PREFS_HUNGR, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_HUNGR_SUBMITTED, false)) {
            // Ensure remote firestore user record reflects completion
            FirebaseFirestore.getInstance().collection("users").document(uid)
                .update("hungrSurveySubmitted", true)
            return
        }

        FirebaseFirestore.getInstance().collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                if (doc != null && doc.getBoolean("hungrSurveySubmitted") == true) {
                    markSubmitted(context, phone)
                } else if (phone.isNotBlank()) {
                    FirebaseFirestore.getInstance().collection("hungr_surveys")
                        .document("hungr_$phone")
                        .get()
                        .addOnSuccessListener { surveyDoc ->
                            if (surveyDoc != null && surveyDoc.exists()) {
                                markSubmitted(context, phone)
                                FirebaseFirestore.getInstance().collection("users").document(uid)
                                    .update("hungrSurveySubmitted", true)
                            }
                        }
                }
            }
    }
}

private val KHAMMAM_LOCALITIES = listOf(
    "Mamillagudem",
    "Wyra Road",
    "Moti Nagar",
    "Balaji Nagar",
    "NSP Colony",
    "Rotary Nagar",
    "Trunk Road",
    "Gandhi Chowk",
    "Prakash Nagar",
    "Other Khammam Area"
)

private data class FoodOption(val id: String, val label: String, val icon: String, val sub: String)

private val FOOD_OPTIONS = listOf(
    FoodOption("TIFFIN_IDLI_DOSA", "Idli, Dosa, Vada & Poori", "🥞", "Fresh hot morning tiffins"),
    FoodOption("FRESH_JUICE", "Fresh Juices & Shakes", "🥤", "Pure fruit & milkshakes"),
    FoodOption("FILTER_COFFEE", "Filter Coffee & Chai", "☕", "South Indian filter brew"),
    FoodOption("HEALTHY_MEALS", "Healthy Sprouts & Millet Bowls", "🥗", "Nutrient fitness breakfast"),
    FoodOption("EVENING_SNACKS", "Evening Bajji & Samosa", "🥟", "Hot teatime bites")
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HungrSurveyBottomSheet(
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var currentStep by remember { mutableIntStateOf(1) }
    var isSuccess by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Form values
    var eatingFrequency by remember { mutableStateOf("DAILY") }
    var willingToPreOrder by remember { mutableStateOf("YES_DEFINITELY") }
    val selectedFoods = remember { mutableStateOf(setOf("TIFFIN_IDLI_DOSA", "FRESH_JUICE")) }

    var isKhammam by remember { mutableStateOf(true) }
    var selectedLocality by remember { mutableStateOf("Mamillagudem") }
    var customArea by remember { mutableStateOf("") }
    var otherCity by remember { mutableStateOf("") }
    var otherArea by remember { mutableStateOf("") }
    var apartmentName by remember { mutableStateOf("") }

    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    // Auto-fill from current Firebase Auth user if available
    LaunchedEffect(Unit) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            val userPhone = user.phoneNumber?.replace("+91", "") ?: ""
            if (userPhone.length == 10) phone = userPhone
            user.displayName?.takeIf { it.isNotBlank() }?.let { name = it }
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            HungrSurveyTracker.hasDismissedThisSession = true
            onDismissRequest()
            AnnouncementManager.closeHungrSurvey()
        },
        sheetState = sheetState,
        containerColor = Color.White,
        tonalElevation = 8.dp,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFF6B00)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🥞", fontSize = 20.sp)
                    }
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "hungr",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFEA580C)
                            )
                            Text(
                                text = "by DutyPe",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF9A3412)
                            )
                        }
                        SuperCharacterWritingSubtitle(
                            text = "Morning Tiffins at Shop Prices",
                            color = Color(0xFF64748B),
                            fontSize = 11.sp
                        )
                    }
                }

                IconButton(
                    onClick = {
                        HungrSurveyTracker.hasDismissedThisSession = true
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            onDismissRequest()
                            AnnouncementManager.closeHungrSurvey()
                        }
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close",
                        tint = Color(0xFF64748B)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (!isSuccess) {
                // Stepper progress indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    (1..4).forEach { i ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(if (currentStep >= i) Color(0xFFEA580C) else Color(0xFFE2E8F0))
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                errorMessage?.let { msg ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFFEF2F2))
                            .border(1.dp, Color(0xFFFECACA), RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        Text(text = msg, fontSize = 12.sp, color = Color(0xFF991B1B))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // STEP 1: Frequency & Willingness
                if (currentStep == 1) {
                    Text(
                        text = "STEP 1 OF 4 · YOUR MORNING ROUTINE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFEA580C),
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "How often do you eat breakfast outside?",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    listOf(
                        "DAILY" to "🔥 Almost Every Morning (Daily Power Eater)",
                        "3_TO_4_DAYS" to "⚡ 3–4 Days a week",
                        "WEEKENDS" to "✨ Weekends only",
                        "RARELY" to "🌱 Rarely / Occasional cravings"
                    ).forEach { (id, label) ->
                        val selected = eatingFrequency == id
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selected) Color(0xFFFFF7ED) else Color(0xFFF8FAFC))
                                .border(
                                    width = if (selected) 2.dp else 1.dp,
                                    color = if (selected) Color(0xFFEA580C) else Color(0xFFE2E8F0),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { eatingFrequency = id }
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 13.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selected) Color(0xFFC2410C) else Color(0xFF1E293B)
                                )
                                if (selected) {
                                    Icon(
                                        imageVector = Icons.Rounded.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFFEA580C),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "If fresh tiffins are delivered to your gate at exact shop price (₹35), will you pre-order?",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    listOf(
                        "YES_DEFINITELY" to "✅ Yes, definitely! Saves time & ₹60+ daily.",
                        "SOMETIMES" to "🤔 2–3 times a week when busy.",
                        "MAYBE" to "👀 Maybe, if taste matches my favorite center."
                    ).forEach { (id, label) ->
                        val selected = willingToPreOrder == id
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selected) Color(0xFFFFF7ED) else Color(0xFFF8FAFC))
                                .border(
                                    width = if (selected) 2.dp else 1.dp,
                                    color = if (selected) Color(0xFFEA580C) else Color(0xFFE2E8F0),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { willingToPreOrder = id }
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = label,
                                fontSize = 13.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = if (selected) Color(0xFFC2410C) else Color(0xFF1E293B)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = { currentStep = 2 },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C))
                    ) {
                        Text(text = "Continue to Food Choices", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(imageVector = Icons.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }

                // STEP 2: Food Choices
                if (currentStep == 2) {
                    Text(
                        text = "STEP 2 OF 4 · WHAT YOU CRAVE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFEA580C),
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "What items would you pre-order most?",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                    Text(
                        text = "Select all that apply. We onboard local partners based on this.",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    FOOD_OPTIONS.forEach { opt ->
                        val selected = selectedFoods.value.contains(opt.id)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selected) Color(0xFFFFF7ED) else Color(0xFFF8FAFC))
                                .border(
                                    width = if (selected) 2.dp else 1.dp,
                                    color = if (selected) Color(0xFFEA580C) else Color(0xFFE2E8F0),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    val current = selectedFoods.value.toMutableSet()
                                    if (selected) current.remove(opt.id) else current.add(opt.id)
                                    selectedFoods.value = current
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(text = opt.icon, fontSize = 22.sp)
                                    Column {
                                        Text(
                                            text = opt.label,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (selected) Color(0xFFC2410C) else Color(0xFF1E293B)
                                        )
                                        Text(text = opt.sub, fontSize = 11.sp, color = Color(0xFF64748B))
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(if (selected) Color(0xFFEA580C) else Color(0xFFE2E8F0)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (selected) {
                                        Icon(
                                            imageVector = Icons.Rounded.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = { currentStep = 1 },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F5F9), contentColor = Color(0xFF475569))
                        ) {
                            Text("Back", fontWeight = FontWeight.SemiBold)
                        }
                        Button(
                            onClick = { currentStep = 3 },
                            modifier = Modifier
                                .weight(2f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C))
                        ) {
                            Text("Set Location", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(imageVector = Icons.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                // STEP 3: Location (Khammam & Outside Khammam)
                if (currentStep == 3) {
                    Text(
                        text = "STEP 3 OF 4 · YOUR LOCATION",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFEA580C),
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Where should we batch deliver?",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Khammam vs Other City Switch
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFF1F5F9))
                            .padding(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isKhammam) Color.White else Color.Transparent)
                                .clickable { isKhammam = true }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "📍 Khammam City",
                                fontSize = 12.sp,
                                fontWeight = if (isKhammam) FontWeight.Bold else FontWeight.Medium,
                                color = if (isKhammam) Color(0xFFEA580C) else Color(0xFF64748B)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (!isKhammam) Color.White else Color.Transparent)
                                .clickable { isKhammam = false }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "🚀 Other City (Outside)",
                                fontSize = 12.sp,
                                fontWeight = if (!isKhammam) FontWeight.Bold else FontWeight.Medium,
                                color = if (!isKhammam) Color(0xFF9333EA) else Color(0xFF64748B)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (isKhammam) {
                        Text(
                            text = "Choose your locality in Khammam:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF334155)
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            KHAMMAM_LOCALITIES.forEach { loc ->
                                val selected = selectedLocality == loc
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (selected) Color(0xFFFFF7ED) else Color(0xFFF1F5F9))
                                        .border(
                                            1.dp,
                                            if (selected) Color(0xFFEA580C) else Color(0xFFE2E8F0),
                                            RoundedCornerShape(16.dp)
                                        )
                                        .clickable { selectedLocality = loc }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = loc,
                                        fontSize = 11.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selected) Color(0xFFC2410C) else Color(0xFF475569)
                                    )
                                }
                            }
                        }

                        if (selectedLocality == "Other Khammam Area") {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = customArea,
                                onValueChange = { customArea = it },
                                placeholder = { Text("Enter colony or landmark name", fontSize = 13.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFFEA580C),
                                    unfocusedBorderColor = Color(0xFFCBD5E1)
                                )
                            )
                        }
                    } else {
                        // Outside Khammam Inputs
                        OutlinedTextField(
                            value = otherCity,
                            onValueChange = { otherCity = it },
                            label = { Text("Your City (e.g. Hyderabad, Warangal)", fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF9333EA),
                                unfocusedBorderColor = Color(0xFFCBD5E1)
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = otherArea,
                            onValueChange = { otherArea = it },
                            label = { Text("Your Area / Colony", fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF9333EA),
                                unfocusedBorderColor = Color(0xFFCBD5E1)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = apartmentName,
                        onValueChange = { apartmentName = it },
                        label = { Text("Apartment / Colony / Flat (Optional)", fontSize = 12.sp) },
                        placeholder = { Text("e.g. Sri Ram Towers, Flat 204", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFEA580C),
                            unfocusedBorderColor = Color(0xFFCBD5E1)
                        )
                    )
                    Text(
                        text = "💡 Apartment gates with 8+ waitlist members get launched on Day 1.",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B),
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(20.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = { currentStep = 2 },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F5F9), contentColor = Color(0xFF475569))
                        ) {
                            Text("Back", fontWeight = FontWeight.SemiBold)
                        }
                        Button(
                            onClick = { currentStep = 4 },
                            modifier = Modifier
                                .weight(2f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C))
                        ) {
                            Text("Claim VIP Pass", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(imageVector = Icons.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                // STEP 4: Contact & VIP Submission
                if (currentStep == 4) {
                    Text(
                        text = "STEP 4 OF 4 · VIP LAUNCH ACCESS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFEA580C),
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Claim ₹50 Launch Credit",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                    Text(
                        text = "We will send your invite & voucher code on WhatsApp when live.",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Your Full Name", fontSize = 12.sp) },
                        placeholder = { Text("e.g. Kiran Kumar", fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFEA580C),
                            unfocusedBorderColor = Color(0xFFCBD5E1)
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it.filter { char -> char.isDigit() }.take(10) },
                        label = { Text("WhatsApp Phone Number", fontSize = 12.sp) },
                        placeholder = { Text("10-digit mobile number", fontSize = 13.sp) },
                        prefix = { Text("+91 ", fontWeight = FontWeight.Bold, color = Color(0xFF475569)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFEA580C),
                            unfocusedBorderColor = Color(0xFFCBD5E1)
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Voucher Callout
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFFFF7ED))
                            .border(1.dp, Color(0xFFFDBA74), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(text = "🎁", fontSize = 20.sp)
                            Column {
                                Text(
                                    text = "Voucher HUNGR50 Reserved For You",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFC2410C)
                                )
                                Text(
                                    text = "₹50 Free Credit + Zero Gate Delivery Charges on launch.",
                                    fontSize = 11.sp,
                                    color = Color(0xFF9A3412)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = { currentStep = 3 },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F5F9), contentColor = Color(0xFF475569))
                        ) {
                            Text("Back", fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = {
                                val cleanPhone = phone.trim()
                                if (cleanPhone.length < 10) {
                                    errorMessage = "Please enter a valid 10-digit WhatsApp number."
                                    return@Button
                                }
                                if (name.trim().isBlank()) {
                                    errorMessage = "Please enter your name."
                                    return@Button
                                }

                                isSubmitting = true
                                errorMessage = null

                                val finalCity = if (isKhammam) "Khammam" else otherCity.trim().ifBlank { "Outside Khammam" }
                                val finalArea = if (isKhammam) {
                                    if (selectedLocality == "Other Khammam Area") customArea.trim().ifBlank { "Khammam Town" } else selectedLocality
                                } else {
                                    otherArea.trim().ifBlank { "Town" }
                                }

                                val docId = "hungr_${cleanPhone}"
                                val data = hashMapOf<String, Any>(
                                    "surveyId" to docId,
                                    "name" to name.trim(),
                                    "phone" to "+91$cleanPhone",
                                    "city" to finalCity,
                                    "area" to finalArea,
                                    "apartmentName" to apartmentName.trim(),
                                    "eatingFrequency" to eatingFrequency,
                                    "willingToPreOrder" to (willingToPreOrder != "NO"),
                                    "preferredCategories" to selectedFoods.value.toList(),
                                    "source" to "ANDROID_APP_MODAL",
                                    "status" to "VIP_WAITLIST",
                                    "voucherCode" to "HUNGR50",
                                    "createdAt" to FieldValue.serverTimestamp(),
                                    "updatedAt" to FieldValue.serverTimestamp()
                                )

                                FirebaseFirestore.getInstance()
                                    .collection("hungr_surveys")
                                    .document(docId)
                                    .set(data, SetOptions.merge())
                                    .addOnSuccessListener {
                                        HungrSurveyTracker.markSubmitted(context, cleanPhone)
                                        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                                        if (!uid.isNullOrBlank()) {
                                            FirebaseFirestore.getInstance().collection("users").document(uid)
                                                .update("hungrSurveySubmitted", true)
                                        }
                                        isSubmitting = false
                                        isSuccess = true
                                    }
                                    .addOnFailureListener { err ->
                                        isSubmitting = false
                                        errorMessage = err.localizedMessage ?: "Failed to save response. Please try again."
                                    }
                            },
                            enabled = !isSubmitting,
                            modifier = Modifier
                                .weight(2f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C))
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                Text("Join VIP Waitlist 🎉", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                }
            } else {
                // SUCCESS VIEW
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFDCFCE7)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF16A34A),
                            modifier = Modifier.size(38.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "VIP ACCESS CONFIRMED!",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF16A34A),
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "You're on the Hungr Priority List",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF1E293B)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Thank you, $name! When batch morning delivery starts in your area, you'll receive the launch link directly on WhatsApp.",
                        fontSize = 13.sp,
                        color = Color(0xFF64748B),
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Voucher Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFFFFF7ED))
                            .border(2.dp, Color(0xFFEA580C), RoundedCornerShape(14.dp))
                            .padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "YOUR LAUNCH VOUCHER", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF9A3412))
                            Text(
                                text = "HUNGR50",
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFEA580C),
                                letterSpacing = 2.sp
                            )
                            Text(text = "₹50 Free Credit + Zero Gate Delivery", fontSize = 11.sp, color = Color(0xFFC2410C))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // WhatsApp Share Button
                    Button(
                        onClick = {
                            val text = "🥞 Hey! Check out Hungr — they are delivering morning tiffins (Idli/Dosa) and juices at exact offline shop prices (₹35) to apartment gates with ₹0 markup!\n\nJoin with me so our apartment gets batch delivery on Day 1: https://dutype.in/hungr"
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                data = Uri.parse("https://api.whatsapp.com/send?text=${Uri.encode(text)}")
                            }
                            context.startActivity(intent)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                    ) {
                        Icon(imageVector = Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Share on WhatsApp Group", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            scope.launch { sheetState.hide() }.invokeOnCompletion {
                                onDismissRequest()
                                AnnouncementManager.closeHungrSurvey()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F5F9), contentColor = Color(0xFF475569))
                    ) {
                        Text(text = "Done", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}


@Composable
fun SuperCharacterWritingSubtitle(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF64748B),
    fontSize: androidx.compose.ui.unit.TextUnit = 11.sp,
    charDelayMs: Long = 16L
) {
    var visibleCount by remember { mutableIntStateOf(0) }
    LaunchedEffect(text) {
        visibleCount = 0
        for (i in 1..text.length) {
            visibleCount = i
            kotlinx.coroutines.delay(charDelayMs)
        }
    }

    Text(
        text = text.take(visibleCount),
        fontSize = fontSize,
        color = color,
        fontWeight = FontWeight.Medium,
        modifier = modifier
    )
}
