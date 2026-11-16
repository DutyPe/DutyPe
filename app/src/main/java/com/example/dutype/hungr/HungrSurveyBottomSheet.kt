package com.example.dutype.hungr

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
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
private const val KEY_HUNGR_LANG = "hungr_lang"

object HungrSurveyTracker {
    var hasDismissedThisSession: Boolean = false

    fun hasSubmitted(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_HUNGR, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_HUNGR_SUBMITTED, false)) return true

        val user = FirebaseAuth.getInstance().currentUser
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
        val user = FirebaseAuth.getInstance().currentUser
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
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val uid = user.uid
        val phone = user.phoneNumber?.removePrefix("+91")?.trim() ?: ""

        val prefs = context.getSharedPreferences(PREFS_HUNGR, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_HUNGR_SUBMITTED, false)) {
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

    /**
     * Determines whether user is in Telangana (Khammam, Hyderabad, Secunderabad, Warangal, etc.)
     * Used to make the Hungr survey strictly mandatory for Telangana residents while remaining optional outside.
     */
    fun isTelanganaUser(context: Context): Boolean {
        // 1. Check LocationPreferences
        try {
            val loc = com.example.dutype.location.LocationPreferences(context).getSavedLocation()
            if (loc != null) {
                val fullText = "${loc.state.orEmpty()} ${loc.city.orEmpty()} ${loc.area.orEmpty()} ${loc.address.orEmpty()}".lowercase()
                val isTgName = fullText.contains("telangana") ||
                        fullText.contains("khammam") ||
                        fullText.contains("hyderabad") ||
                        fullText.contains("secunderabad") ||
                        fullText.contains("warangal") ||
                        fullText.contains("nizamabad") ||
                        fullText.contains("karimnagar") ||
                        fullText.contains("kothagudem") ||
                        fullText.contains("suryapet") ||
                        fullText.contains("nalgonda") ||
                        fullText.contains("siddipet") ||
                        fullText.contains("mancherial") ||
                        fullText.contains("mahabubnagar") ||
                        fullText.contains("adilabad") ||
                        fullText.contains("bhupalpally") ||
                        fullText.contains("wanaparthy")
                if (isTgName) return true

                // GPS coordinates check: Telangana roughly 15.8°N - 19.9°N, 77.2°E - 81.9°E
                if (loc.latitude in 15.8..19.9 && loc.longitude in 77.2..81.9) {
                    return true
                }

                // If coordinates or state are explicitly non-zero and clearly outside Telangana (e.g. Bangalore, Delhi, Mumbai)
                val isOutsideState = fullText.contains("karnataka") ||
                        fullText.contains("bengaluru") ||
                        fullText.contains("bangalore") ||
                        fullText.contains("delhi") ||
                        fullText.contains("maharashtra") ||
                        fullText.contains("mumbai") ||
                        fullText.contains("tamil nadu") ||
                        fullText.contains("chennai") ||
                        fullText.contains("bihar") ||
                        fullText.contains("uttar pradesh") ||
                        fullText.contains("haryana") ||
                        fullText.contains("gurugram")
                if (isOutsideState || (loc.latitude != 0.0 && loc.longitude != 0.0)) {
                    return false
                }
            }
        } catch (_: Exception) {}

        // 2. Check SharedPreferences cached user address / profile
        try {
            val userPrefs = context.getSharedPreferences("dutype_saved_addresses", Context.MODE_PRIVATE)
            val allPrefs = userPrefs.all.values.joinToString(" ").lowercase()
            if (allPrefs.contains("telangana") || allPrefs.contains("khammam") || allPrefs.contains("hyderabad")) {
                return true
            }
        } catch (_: Exception) {}

        // 3. Check SharedPreferences cached location
        try {
            val locPrefs = context.getSharedPreferences("location_preferences", Context.MODE_PRIVATE)
            val addr = (locPrefs.getString("address", "") + " " + locPrefs.getString("city", "") + " " + locPrefs.getString("state", "")).lowercase()
            if (addr.contains("telangana") || addr.contains("khammam") || addr.contains("hyderabad")) {
                return true
            }
        } catch (_: Exception) {}

        // 4. Default: DutyPe's primary active regional hub is Telangana
        return true
    }
}

private data class FoodOption(
    val id: String,
    val labelEn: String,
    val labelTe: String,
    val icon: String,
    val subEn: String,
    val subTe: String
)

private val FOOD_OPTIONS = listOf(
    FoodOption(
        id = "TIFFIN_IDLI_DOSA",
        labelEn = "Idli, Dosa, Vada & Poori",
        labelTe = "ఇడ్లీ, దోశ, వడ & పూరి",
        icon = "🥞",
        subEn = "Fresh hot morning tiffins at shop price",
        subTe = "హోటల్ ధరలకే ఉదయం వేడి వేడి టిఫిన్లు"
    ),
    FoodOption(
        id = "FRESH_JUICE",
        labelEn = "Fresh Juices & Milkshakes",
        labelTe = "ఫ్రెష్ జ్యూస్‌లు & మిల్క్‌షేక్స్",
        icon = "🥤",
        subEn = "Pure fresh fruit juices & thick shakes",
        subTe = "స్వచ్ఛమైన పండ్ల రసాలు & మిల్క్‌షేక్స్"
    ),
    FoodOption(
        id = "FILTER_COFFEE",
        labelEn = "Filter Coffee & Hot Chai",
        labelTe = "ఫిల్టర్ కాఫీ & వేడి టీ",
        icon = "☕",
        subEn = "South Indian aromatic brew & masala tea",
        subTe = "ఘుమఘుమలాడే దక్షిణ భారత కాఫీ & టీ"
    ),
    FoodOption(
        id = "HEALTHY_MEALS",
        labelEn = "Healthy Sprouts & Millet Bowls",
        labelTe = "మొలకలు & మిల్లెట్ బౌల్స్",
        icon = "🥗",
        subEn = "Nutritious fitness & diet breakfast",
        subTe = "ఆరోగ్యకరమైన ఫిట్‌నెస్ & డైట్ బ్రేక్‌ఫాస్ట్"
    ),
    FoodOption(
        id = "EVENING_SNACKS",
        labelEn = "Evening Bajji, Mysore Bonda & Samosa",
        labelTe = "సాయంత్రం బజ్జీ, మైసూర్ బోండా & సమోసా",
        icon = "🥟",
        subEn = "Hot teatime snacks straight from tiffin centers",
        subTe = "సాయంత్రం వేడి వేడి టీటైమ్ స్నాక్స్"
    )
)

private val KHAMMAM_LOCALITIES_DATA = listOf(
    "Mamillagudem" to "మామిళ్ళగూడెం",
    "Wyra Road" to "వైరా రోడ్",
    "Moti Nagar" to "మోతీ నగర్",
    "Balaji Nagar" to "బాలాజీ నగర్",
    "NSP Colony" to "ఎన్‌ఎస్‌పి కాలనీ",
    "Rotary Nagar" to "రోటరీ నగర్",
    "Trunk Road" to "ట్రంక్ రోడ్",
    "Gandhi Chowk" to "గాంధీ చౌక్",
    "Prakash Nagar" to "ప్రకాష్ నగర్",
    "Other Khammam Area" to "ఇతర ఖమ్మం ఏరియా"
)

private val HYDERABAD_LOCALITIES_DATA = listOf(
    "Madhapur" to "మాదాపూర్",
    "Gachibowli" to "గచ్చిబౌలి",
    "Kukatpally" to "కూకట్‌పల్లి",
    "Kondapur" to "కొండాపూర్",
    "Ameerpet" to "అమీర్‌పేట్",
    "Dilsukhnagar" to "దిల్‌సుఖ్‌నగర్",
    "Miyapur" to "మియాపూర్",
    "Uppal" to "ఉప్పల్",
    "Secunderabad" to "సికింద్రాబాద్",
    "Other Hyderabad Area" to "ఇతర హైదరాబాద్ ప్రాంతం"
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HungrSurveyBottomSheet(
    isMandatory: Boolean = HungrSurveyTracker.isTelanganaUser(LocalContext.current),
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Language state: defaults to Telugu for Telangana users or if device is set to Telugu
    val prefs = remember { context.getSharedPreferences(PREFS_HUNGR, Context.MODE_PRIVATE) }
    val defaultLangIsTelugu = remember {
        val savedLang = prefs.getString(KEY_HUNGR_LANG, null)
        if (savedLang != null) {
            savedLang == "te"
        } else {
            isMandatory || java.util.Locale.getDefault().language == "te"
        }
    }
    var isTelugu by remember { mutableStateOf(defaultLangIsTelugu) }

    fun toggleLanguage(telugu: Boolean) {
        isTelugu = telugu
        prefs.edit().putString(KEY_HUNGR_LANG, if (telugu) "te" else "en").apply()
    }

    var currentStep by remember { mutableIntStateOf(1) }
    var isSuccess by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // FORM VALUES: NO PRE-SELECTED DEFAULTS! Users must select themselves.
    var eatingFrequency by remember { mutableStateOf("") }
    var willingToPreOrder by remember { mutableStateOf("") }
    val selectedFoods = remember { mutableStateOf(emptySet<String>()) }
    var customFoodItem by remember { mutableStateOf("") }

    // City & Locality Tabs: 0 = Khammam, 1 = Hyderabad, 2 = Other Telangana City, 3 = Outside TG
    var selectedCityTab by remember { mutableIntStateOf(0) }
    var selectedLocality by remember { mutableStateOf("") }
    var customArea by remember { mutableStateOf("") }
    var otherCity by remember { mutableStateOf("") }
    var otherArea by remember { mutableStateOf("") }
    var apartmentName by remember { mutableStateOf("") }

    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    // Auto-fill phone/name from current Firebase Auth user if available
    LaunchedEffect(Unit) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            val userPhone = user.phoneNumber?.replace("+91", "") ?: ""
            if (userPhone.length == 10) phone = userPhone
            user.displayName?.takeIf { it.isNotBlank() }?.let { name = it }
        }
    }

    // Intercept back navigation: if mandatory, go to previous step or prevent leaving without completing
    BackHandler(enabled = isMandatory && !isSuccess) {
        if (currentStep > 1) {
            errorMessage = null
            currentStep -= 1
        } else {
            Toast.makeText(
                context,
                if (isTelugu) "దయచేసి ఈ చిన్న హంగర్ సర్వేను పూర్తి చేయండి (కేవలం 30 సెకన్లు మాత్రమే)!"
                else "Please complete this quick 30-sec Hungr survey to continue!",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            if (!isMandatory) {
                HungrSurveyTracker.hasDismissedThisSession = true
                onDismissRequest()
                AnnouncementManager.closeHungrSurvey()
            } else {
                Toast.makeText(
                    context,
                    if (isTelugu) "దయచేసి హంగర్ సర్వే పూర్తి చేయండి — కేవలం 30 సెకన్లు మాత్రమే!"
                    else "Please complete this quick 30-sec survey to continue!",
                    Toast.LENGTH_SHORT
                ).show()
            }
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
                // Left: Logo & Subtitle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
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
                            text = if (isTelugu) "షాప్ ధరలకే ఉదయం వేడి వేడి టిఫిన్లు" else "Morning Tiffins at Shop Prices",
                            color = Color(0xFF64748B),
                            fontSize = 11.sp
                        )
                    }
                }

                // Right: Language Selector Chip + Optional Close Button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Telugu / English Language Chip (Requirement 1.2)
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFFFFF7ED))
                            .border(1.dp, Color(0xFFFDBA74), RoundedCornerShape(20.dp))
                            .padding(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (!isTelugu) Color(0xFFEA580C) else Color.Transparent)
                                .clickable { toggleLanguage(false) }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "English",
                                fontSize = 11.sp,
                                fontWeight = if (!isTelugu) FontWeight.Bold else FontWeight.Medium,
                                color = if (!isTelugu) Color.White else Color(0xFF9A3412)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isTelugu) Color(0xFFEA580C) else Color.Transparent)
                                .clickable { toggleLanguage(true) }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "తెలుగు",
                                fontSize = 11.sp,
                                fontWeight = if (isTelugu) FontWeight.Bold else FontWeight.Medium,
                                color = if (isTelugu) Color.White else Color(0xFF9A3412)
                            )
                        }
                    }

                    // Close Button: ONLY available for outside non-mandatory users
                    if (!isMandatory && !isSuccess) {
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
                    } else if (isMandatory && !isSuccess) {
                        // Subtle Mandatory / Telangana Special Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFFEF3C7))
                                .border(1.dp, Color(0xFFFDE68A), RoundedCornerShape(12.dp))
                                .padding(horizontal = 7.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = if (isTelugu) "తెలంగాణ" else "TG Hub",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFB45309)
                            )
                        }
                    }
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

                // ==================== STEP 1: Frequency & Willingness ====================
                if (currentStep == 1) {
                    Text(
                        text = if (isTelugu) "స్టెప్ 1 / 4 · మీ ఉదయపు అలవాటు" else "STEP 1 OF 4 · YOUR MORNING ROUTINE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFEA580C),
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isTelugu) "మీరు ఉదయం టిఫిన్ బయట ఎంత తరచుగా తింటారు?" else "How often do you eat breakfast outside?",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val frequencyList = listOf(
                        "DAILY" to if (isTelugu) "🔥 దాదాపు ప్రతిరోజూ (డైలీ పవర్ ఈటర్)" else "🔥 Almost Every Morning (Daily Power Eater)",
                        "3_TO_4_DAYS" to if (isTelugu) "⚡ వారానికి 3–4 రోజులు" else "⚡ 3–4 Days a week",
                        "WEEKENDS" to if (isTelugu) "✨ వీకెండ్స్ / సెలవుల్లో మాత్రమే" else "✨ Weekends only",
                        "RARELY" to if (isTelugu) "🌱 చాలా అరుదుగా / ఎప్పుడో ఒకసారి" else "🌱 Rarely / Occasional cravings"
                    )

                    frequencyList.forEach { (id, label) ->
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
                                .clickable {
                                    eatingFrequency = id
                                    errorMessage = null
                                }
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
                        text = if (isTelugu)
                            "తాజా వేడి టిఫిన్లు మీ గేట్ వద్దకు హోటల్ అసలు రేటుకే (₹35) డెలివరీ చేస్తే, మీరు ప్రీ-ఆర్డర్ చేస్తారా?"
                        else
                            "If fresh tiffins are delivered to your gate at exact shop price (₹35), will you pre-order?",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val preOrderList = listOf(
                        "YES_DEFINITELY" to if (isTelugu) "✅ తప్పకుండా! సమయం ఆదా అవుతుంది & రోజూ ₹60+ మిగులుతాయి." else "✅ Yes, definitely! Saves time & ₹60+ daily.",
                        "SOMETIMES" to if (isTelugu) "🤔 బిజీగా ఉన్నప్పుడు వారానికి 2–3 సార్లు." else "🤔 2–3 times a week when busy.",
                        "MAYBE" to if (isTelugu) "👀 నాకిష్టమైన హోటల్ రుచి ఉంటే తప్పకుండా." else "👀 Maybe, if taste matches my favorite center."
                    )

                    preOrderList.forEach { (id, label) ->
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
                                .clickable {
                                    willingToPreOrder = id
                                    errorMessage = null
                                }
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
                        onClick = {
                            if (eatingFrequency.isBlank() || willingToPreOrder.isBlank()) {
                                errorMessage = if (isTelugu) {
                                    "దయచేసి మీ టిఫిన్ అలవాటు మరియు ప్రీ-ఆర్డర్ ఆప్షన్‌ను ఎంచుకోండి."
                                } else {
                                    "Please select how often you eat outside and if you would like to pre-order."
                                }
                                return@Button
                            }
                            errorMessage = null
                            currentStep = 2
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C))
                    ) {
                        Text(
                            text = if (isTelugu) "టిఫిన్ ఎంపికలకు వెళ్లండి" else "Continue to Food Choices",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(imageVector = Icons.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }

                // ==================== STEP 2: Food Choices & Custom Items ====================
                if (currentStep == 2) {
                    Text(
                        text = if (isTelugu) "స్టెప్ 2 / 4 · మీకు ఇష్టమైన టిఫిన్లు" else "STEP 2 OF 4 · WHAT YOU CRAVE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFEA580C),
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isTelugu) "మీరు ఎక్కువగా ఏ ఐటెమ్స్ ప్రీ-ఆర్డర్ చేయాలనుకుంటున్నారు?" else "What items would you pre-order most?",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                    Text(
                        text = if (isTelugu)
                            "మీకు నచ్చిన వాటిని ఎంచుకోండి లేదా మీ సొంత ఐటెమ్ రాయండి."
                        else
                            "Select all that apply or add your custom craving below.",
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
                                    errorMessage = null
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(text = opt.icon, fontSize = 22.sp)
                                    Column {
                                        Text(
                                            text = if (isTelugu) opt.labelTe else opt.labelEn,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (selected) Color(0xFFC2410C) else Color(0xFF1E293B)
                                        )
                                        Text(
                                            text = if (isTelugu) opt.subTe else opt.subEn,
                                            fontSize = 11.sp,
                                            color = Color(0xFF64748B)
                                        )
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

                    Spacer(modifier = Modifier.height(10.dp))

                    // Custom Tiffin or Item Input (Requirement 1.3)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFFFF7ED))
                            .border(1.dp, Color(0xFFFDBA74), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(text = "✍️", fontSize = 16.sp)
                                Text(
                                    text = if (isTelugu) "మీకు నచ్చిన ఇతర టిఫిన్ లేదా ఐటెమ్ రాయండి:"
                                    else "Any other custom tiffin or item you crave:",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFC2410C)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = customFoodItem,
                                onValueChange = {
                                    customFoodItem = it
                                    errorMessage = null
                                },
                                placeholder = {
                                    Text(
                                        text = if (isTelugu) "ఉదా: పెసరట్టు, ఉప్మా, మైసూర్ బజ్జి, పొంగల్, రాగి జావ, పునుగులు..."
                                        else "e.g. Upma, Pesarattu, Mysore Bajji, Pongal, Ragi Java, Punugulu...",
                                        fontSize = 12.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFFEA580C),
                                    unfocusedBorderColor = Color(0xFFCBD5E1),
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White
                                ),
                                singleLine = true
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = {
                                errorMessage = null
                                currentStep = 1
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F5F9), contentColor = Color(0xFF475569))
                        ) {
                            Text(if (isTelugu) "వెనుకకు" else "Back", fontWeight = FontWeight.SemiBold)
                        }
                        Button(
                            onClick = {
                                if (selectedFoods.value.isEmpty() && customFoodItem.trim().isEmpty()) {
                                    errorMessage = if (isTelugu) {
                                        "దయచేసి కనీసం ఒక టిఫిన్ ఎంచుకోండి లేదా మీ సొంత ఐటెమ్ రాయండి."
                                    } else {
                                        "Please select at least one food option or write your custom craving."
                                    }
                                    return@Button
                                }
                                errorMessage = null
                                currentStep = 3
                            },
                            modifier = Modifier
                                .weight(2f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C))
                        ) {
                            Text(if (isTelugu) "లొకేషన్ ఎంచుకోండి" else "Set Location", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(imageVector = Icons.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                // ==================== STEP 3: Location (Khammam, Hyderabad & Other TG) ====================
                if (currentStep == 3) {
                    Text(
                        text = if (isTelugu) "స్టెప్ 3 / 4 · మీ లొకేషన్" else "STEP 3 OF 4 · YOUR LOCATION",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFEA580C),
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isTelugu) "మేము ఎక్కడ డెలివరీ చేయాలి?" else "Where should we batch deliver?",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Location Tabs: Khammam | Hyderabad | Other Telangana City
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
                                .background(if (selectedCityTab == 0) Color.White else Color.Transparent)
                                .clickable {
                                    selectedCityTab = 0
                                    selectedLocality = ""
                                    errorMessage = null
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isTelugu) "📍 ఖమ్మం" else "📍 Khammam",
                                fontSize = 12.sp,
                                fontWeight = if (selectedCityTab == 0) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedCityTab == 0) Color(0xFFEA580C) else Color(0xFF64748B)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selectedCityTab == 1) Color.White else Color.Transparent)
                                .clickable {
                                    selectedCityTab = 1
                                    selectedLocality = ""
                                    errorMessage = null
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isTelugu) "📍 హైదరాబాద్" else "📍 Hyderabad",
                                fontSize = 12.sp,
                                fontWeight = if (selectedCityTab == 1) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedCityTab == 1) Color(0xFFEA580C) else Color(0xFF64748B)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selectedCityTab == 2) Color.White else Color.Transparent)
                                .clickable {
                                    selectedCityTab = 2
                                    selectedLocality = ""
                                    errorMessage = null
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isTelugu) "🚀 ఇతర నగరాలు" else "🚀 Other Cities",
                                fontSize = 12.sp,
                                fontWeight = if (selectedCityTab == 2) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedCityTab == 2) Color(0xFF9333EA) else Color(0xFF64748B)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (selectedCityTab == 0) {
                        Text(
                            text = if (isTelugu) "ఖమ్మంలో మీ ప్రాంతాన్ని ఎంచుకోండి:" else "Choose your locality in Khammam:",
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
                            KHAMMAM_LOCALITIES_DATA.forEach { (en, te) ->
                                val selected = selectedLocality == en
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (selected) Color(0xFFFFF7ED) else Color(0xFFF1F5F9))
                                        .border(
                                            1.dp,
                                            if (selected) Color(0xFFEA580C) else Color(0xFFE2E8F0),
                                            RoundedCornerShape(16.dp)
                                        )
                                        .clickable {
                                            selectedLocality = en
                                            errorMessage = null
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = if (isTelugu) te else en,
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
                                onValueChange = {
                                    customArea = it
                                    errorMessage = null
                                },
                                placeholder = {
                                    Text(
                                        if (isTelugu) "కాలనీ లేదా ల్యాండ్‌మార్క్ పేరు నమోదు చేయండి" else "Enter colony or landmark name",
                                        fontSize = 13.sp
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFFEA580C),
                                    unfocusedBorderColor = Color(0xFFCBD5E1)
                                )
                            )
                        }
                    } else if (selectedCityTab == 1) {
                        Text(
                            text = if (isTelugu) "హైదరాబాద్‌లో మీ ప్రాంతాన్ని ఎంచుకోండి:" else "Choose your locality in Hyderabad:",
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
                            HYDERABAD_LOCALITIES_DATA.forEach { (en, te) ->
                                val selected = selectedLocality == en
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (selected) Color(0xFFFFF7ED) else Color(0xFFF1F5F9))
                                        .border(
                                            1.dp,
                                            if (selected) Color(0xFFEA580C) else Color(0xFFE2E8F0),
                                            RoundedCornerShape(16.dp)
                                        )
                                        .clickable {
                                            selectedLocality = en
                                            errorMessage = null
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = if (isTelugu) te else en,
                                        fontSize = 11.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selected) Color(0xFFC2410C) else Color(0xFF475569)
                                    )
                                }
                            }
                        }

                        if (selectedLocality == "Other Hyderabad Area") {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = customArea,
                                onValueChange = {
                                    customArea = it
                                    errorMessage = null
                                },
                                placeholder = {
                                    Text(
                                        if (isTelugu) "ఏరియా లేదా కాలనీ పేరు నమోదు చేయండి" else "Enter your area or colony name",
                                        fontSize = 13.sp
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFFEA580C),
                                    unfocusedBorderColor = Color(0xFFCBD5E1)
                                )
                            )
                        }
                    } else {
                        // Other Telangana or Outside City Inputs
                        OutlinedTextField(
                            value = otherCity,
                            onValueChange = {
                                otherCity = it
                                errorMessage = null
                            },
                            label = {
                                Text(
                                    if (isTelugu) "మీ నగరం / పట్టణం (ఉదా: వరంగల్, కరీంనగర్, నిజామాబాద్)" else "Your City (e.g. Warangal, Karimnagar, Nizamabad)",
                                    fontSize = 12.sp
                                )
                            },
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
                            onValueChange = {
                                otherArea = it
                                errorMessage = null
                            },
                            label = {
                                Text(
                                    if (isTelugu) "మీ ప్రాంతం / కాలనీ పేరు" else "Your Area / Colony",
                                    fontSize = 12.sp
                                )
                            },
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
                        label = {
                            Text(
                                if (isTelugu) "అపార్ట్‌మెంట్ / గేటెడ్ కాలనీ / ఫ్లాట్ (ఐచ్ఛికం)" else "Apartment / Gated Community / Flat (Optional)",
                                fontSize = 12.sp
                            )
                        },
                        placeholder = {
                            Text(
                                if (isTelugu) "ఉదా: శ్రీ రామ్ టవర్స్, ఫ్లాట్ 204" else "e.g. Sri Ram Towers, Flat 204",
                                fontSize = 12.sp
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFEA580C),
                            unfocusedBorderColor = Color(0xFFCBD5E1)
                        )
                    )
                    Text(
                        text = if (isTelugu)
                            "💡 మీ అపార్ట్‌మెంట్ గేట్ వద్ద 8+ ఆర్డర్లు ఉంటే మొదటి రోజే డెలివరీ ప్రారంభమవుతుంది."
                        else
                            "💡 Apartment gates with 8+ waitlist members get launched on Day 1.",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B),
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(20.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = {
                                errorMessage = null
                                currentStep = 2
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F5F9), contentColor = Color(0xFF475569))
                        ) {
                            Text(if (isTelugu) "వెనుకకు" else "Back", fontWeight = FontWeight.SemiBold)
                        }
                        Button(
                            onClick = {
                                val localityOk = when (selectedCityTab) {
                                    0 -> selectedLocality.isNotBlank() && (selectedLocality != "Other Khammam Area" || customArea.trim().isNotBlank())
                                    1 -> selectedLocality.isNotBlank() && (selectedLocality != "Other Hyderabad Area" || customArea.trim().isNotBlank())
                                    else -> otherCity.trim().isNotBlank() && otherArea.trim().isNotBlank()
                                }
                                if (!localityOk) {
                                    errorMessage = if (isTelugu) {
                                        "దయచేసి మీ ప్రాంతం లేదా కాలనీ పేరును ఎంచుకోండి/నమోదు చేయండి."
                                    } else {
                                        "Please select or enter your area/colony name."
                                    }
                                    return@Button
                                }
                                errorMessage = null
                                currentStep = 4
                            },
                            modifier = Modifier
                                .weight(2f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C))
                        ) {
                            Text(if (isTelugu) "విఐపి పాస్ పొందండి" else "Claim VIP Pass", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(imageVector = Icons.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                // ==================== STEP 4: Contact & VIP Submission ====================
                if (currentStep == 4) {
                    Text(
                        text = if (isTelugu) "స్టెప్ 4 / 4 · విఐపి లాంచ్ యాక్సెస్" else "STEP 4 OF 4 · VIP LAUNCH ACCESS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFEA580C),
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isTelugu) "₹50 లాంచ్ క్రెడిట్ క్లెయిమ్ చేయండి" else "Claim ₹50 Launch Credit",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                    Text(
                        text = if (isTelugu)
                            "మీ ఏరియాలో ప్రారంభం కాగానే వాట్సాప్‌లో ఆహ్వానం మరియు వోచర్ కోడ్ పంపుతాము."
                        else
                            "We will send your invite & voucher code on WhatsApp when live.",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            errorMessage = null
                        },
                        label = { Text(if (isTelugu) "మీ పూర్తి పేరు" else "Your Full Name", fontSize = 12.sp) },
                        placeholder = { Text(if (isTelugu) "ఉదా: కిరణ్ కుమార్" else "e.g. Kiran Kumar", fontSize = 13.sp) },
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
                        onValueChange = {
                            phone = it.filter { char -> char.isDigit() }.take(10)
                            errorMessage = null
                        },
                        label = { Text(if (isTelugu) "వాట్సాప్ మొబైల్ నంబర్" else "WhatsApp Phone Number", fontSize = 12.sp) },
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
                                    text = if (isTelugu) "వోచర్ HUNGR50 మీ కోసం రిజర్వ్ చేయబడింది" else "Voucher HUNGR50 Reserved For You",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFC2410C)
                                )
                                Text(
                                    text = if (isTelugu)
                                        "ప్రారంభ ఆఫర్‌గా ₹50 ఉచిత క్రెడిట్ + ₹0 డెలివరీ ఛార్జీలు."
                                    else
                                        "₹50 Free Credit + Zero Gate Delivery Charges on launch.",
                                    fontSize = 11.sp,
                                    color = Color(0xFF9A3412)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = {
                                errorMessage = null
                                currentStep = 3
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F5F9), contentColor = Color(0xFF475569))
                        ) {
                            Text(if (isTelugu) "వెనుకకు" else "Back", fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = {
                                val cleanPhone = phone.trim()
                                if (name.trim().isBlank()) {
                                    errorMessage = if (isTelugu) "దయచేసి మీ పేరు నమోదు చేయండి." else "Please enter your name."
                                    return@Button
                                }
                                if (cleanPhone.length < 10) {
                                    errorMessage = if (isTelugu) "దయచేసి సరైన 10 అంకెల వాట్సాప్ నంబర్ ఇవ్వండి." else "Please enter a valid 10-digit WhatsApp number."
                                    return@Button
                                }

                                isSubmitting = true
                                errorMessage = null

                                val finalCity = when (selectedCityTab) {
                                    0 -> "Khammam"
                                    1 -> "Hyderabad"
                                    else -> otherCity.trim().ifBlank { "Telangana" }
                                }
                                val finalArea = when (selectedCityTab) {
                                    0 -> if (selectedLocality == "Other Khammam Area") customArea.trim().ifBlank { "Khammam Town" } else selectedLocality
                                    1 -> if (selectedLocality == "Other Hyderabad Area") customArea.trim().ifBlank { "Hyderabad City" } else selectedLocality
                                    else -> otherArea.trim().ifBlank { "Town" }
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
                                    "customFoodItem" to customFoodItem.trim(),
                                    "language" to if (isTelugu) "te" else "en",
                                    "isTelangana" to isMandatory,
                                    "isMandatory" to isMandatory,
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
                                        val uid = FirebaseAuth.getInstance().currentUser?.uid
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
                                Text(
                                    if (isTelugu) "విఐపి వెయిట్‌లిస్ట్‌లో చేరండి 🎉" else "Join VIP Waitlist 🎉",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            } else {
                // ==================== SUCCESS VIEW ====================
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
                        text = if (isTelugu) "విఐపి యాక్సెస్ కన్ఫర్మ్ అయింది!" else "VIP ACCESS CONFIRMED!",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF16A34A),
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isTelugu) "మీరు హంగర్ ప్రయారిటీ లిస్ట్‌లో ఉన్నారు" else "You're on the Hungr Priority List",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF1E293B),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isTelugu)
                            "ధన్యవాదాలు, $name గారూ! మీ ప్రాంతంలో బ్యాచ్ మార్నింగ్ డెలివరీ ప్రారంభమైన వెంటనే నేరుగా వాట్సాప్‌కు లాంచ్ లింక్ మరియు మీ వోచర్ అందుతాయి."
                        else
                            "Thank you, $name! When batch morning delivery starts in your area, you'll receive the launch link directly on WhatsApp.",
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
                            Text(
                                text = if (isTelugu) "మీ లాంచ్ వోచర్" else "YOUR LAUNCH VOUCHER",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF9A3412)
                            )
                            Text(
                                text = "HUNGR50",
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFEA580C),
                                letterSpacing = 2.sp
                            )
                            Text(
                                text = if (isTelugu) "₹50 ఉచిత క్రెడిట్ + జీరో డెలివరీ ఛార్జీలు" else "₹50 Free Credit + Zero Gate Delivery",
                                fontSize = 11.sp,
                                color = Color(0xFFC2410C)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // WhatsApp Share Button
                    Button(
                        onClick = {
                            val shareText = if (isTelugu) {
                                "🥞 నమస్తే! హంగర్ (Hungr by DutyPe) గురించి తెలుసా? ఉదయం వేడి వేడి టిఫిన్లు (ఇడ్లీ, దోశ, వడ) మరియు ఫ్రెష్ జ్యూస్‌లు బయట షాప్ ధరలకే (₹35) మన గేట్ వద్దకు ఫ్రీగా డెలివరీ చేస్తున్నారు!\n\nమన ఏరియాకు కూడా మొదటి రోజే డెలివరీ ప్రారంభం కావడానికి నాతో పాటు వెయిట్‌లిస్ట్‌లో చేరండి: https://dutype.in/hungr"
                            } else {
                                "🥞 Hey! Check out Hungr — they are delivering morning tiffins (Idli/Dosa) and juices at exact offline shop prices (₹35) to apartment gates with ₹0 markup!\n\nJoin with me so our apartment gets batch delivery on Day 1: https://dutype.in/hungr"
                            }
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                data = Uri.parse("https://api.whatsapp.com/send?text=${Uri.encode(shareText)}")
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
                        Text(
                            text = if (isTelugu) "వాట్సాప్ గ్రూప్‌లో షేర్ చేయండి" else "Share on WhatsApp Group",
                            fontWeight = FontWeight.Bold
                        )
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
                        Text(text = if (isTelugu) "పూర్తయింది" else "Done", fontWeight = FontWeight.SemiBold)
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
