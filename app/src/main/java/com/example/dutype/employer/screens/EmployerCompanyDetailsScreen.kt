package com.example.dutype.employer.screens

import com.example.dutype.ui.theme.bd
import com.example.dutype.ui.theme.bg
import com.example.dutype.ui.theme.fg
import com.example.dutype.firestore.FirestoreSchema.Values
import com.example.dutype.firestore.FirestoreSchema.EmployerProfiles
import com.dutype.app.R
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.dutype.viewmodels.ProfileCompletionViewModel
import androidx.compose.ui.res.stringResource
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import timber.log.Timber

// ---------------------------------------------------------------------------
// Screen 38 design tokens (Employer "Company Details")
// ---------------------------------------------------------------------------
private val CdNavy = Color(0xFF0F172A)
private val CdCobalt = Color(0xFF2563EB)
private val CdBorder = Color(0xFFE2E8F0)
private val CdMuted = Color(0xFF64748B)
private val CdHint = Color(0xFF94A3B8)
private val CdChipText = Color(0xFF475569)
private val CdGreen = Color(0xFF16A34A)
private val CdGreenBg = Color(0xFFF0FDF4)
private val CdGstGreen = Color(0xFF10B981)

private data class CdCategoryItem(val key: String, val labelRes: Int)

private val CdCategories = listOf(
    CdCategoryItem("Retail Shop", R.string.company_category_retail),
    CdCategoryItem("Construction", R.string.company_category_construction),
    CdCategoryItem("Logistics", R.string.company_category_logistics),
    CdCategoryItem("Hotel/Restaurant", R.string.company_category_hotel),
    CdCategoryItem("Manufacturing", R.string.company_category_manufacturing),
    CdCategoryItem("Individual", R.string.company_category_individual)
)

// Local format check only. Server-side GST verification is NOT implemented.
private val CdGstinRegex = Regex("^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][1-9A-Z]Z[0-9A-Z]$")

/**
 * Employer Company Details screen (business profile editor).
 * Layout follows Screen38-CompanyDetails mockup.
 */
@Composable
fun EmployerCompanyDetailsScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val profileCompletionViewModel: ProfileCompletionViewModel = hiltViewModel()

    // Form state
    var employerType by remember { mutableStateOf("COMPANY") }
    var companyName by remember { mutableStateOf("") }
    var contactPerson by remember { mutableStateOf("") }
    var contactPhone by remember { mutableStateOf("") }
    var businessAddress by remember { mutableStateOf("") }
    var industry by remember { mutableStateOf("") }
    var gstin by remember { mutableStateOf("") }
    var gstVerified by remember { mutableStateOf(false) }

    // Profile image
    var profileImageUrl by remember { mutableStateOf<String?>(null) }
    var profileImageUri by remember { mutableStateOf<Uri?>(null) }
    var isUploadingImage by remember { mutableStateOf(false) }

    var isSaving by remember { mutableStateOf(false) }

    // Image picker + upload (unchanged behavior)
    val imagePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            profileImageUri = selectedUri
            isUploadingImage = true
            scope.launch {
                try {
                    val currentUser = FirebaseAuth.getInstance().currentUser
                    if (currentUser != null) {
                        val uploadResult = profileCompletionViewModel.uploadProfileImage(
                            selectedUri, currentUser.uid, "employer"
                        )
                        uploadResult.fold(
                            onSuccess = { imageUrl ->
                                profileImageUrl = imageUrl
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.company_logo_updated),
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            onFailure = { exception ->
                                Timber.e(exception, "Failed to upload company logo")
                                profileImageUri = null
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.failed_upload_logo),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                    } else {
                        profileImageUri = null
                        Toast.makeText(context, context.getString(R.string.please_login_upload_logo), Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Error uploading company logo")
                    profileImageUri = null
                    Toast.makeText(context, context.getString(R.string.error_uploading_logo), Toast.LENGTH_SHORT).show()
                } finally {
                    isUploadingImage = false
                }
            }
        }
    }

    // Load profile
    LaunchedEffect(Unit) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            try {
                profileCompletionViewModel.getEmployer(currentUser.uid)
                    .onSuccess { saved ->
                        if (saved != null) {
                            employerType = saved.employerType
                            companyName = if (saved.isCompany) saved.businessName else saved.ownerName
                            contactPerson = saved.ownerName
                            contactPhone = saved.phone
                            businessAddress = saved.address
                            industry = saved.businessType
                            gstin = saved.gstin
                            gstVerified = CdGstinRegex.matches(gstin)
                            profileImageUrl = saved.photoUrl.ifBlank { null }
                        }
                    }
                    .onFailure { Timber.e(it, "Error loading employer profile data") }
            } catch (e: Exception) {
                Timber.e(e, "Error loading employer profile data")
            }
        }
    }

    val doSave: () -> Unit = {
        if (!isSaving) {
            if (gstin.isNotBlank() && !CdGstinRegex.matches(gstin)) {
                Toast.makeText(context, context.getString(R.string.valid_gstin_required), Toast.LENGTH_SHORT).show()
            } else {
                isSaving = true
                scope.launch {
                    try {
                        val currentUser = FirebaseAuth.getInstance().currentUser
                        if (currentUser != null) {
                            val isCompany = employerType == Values.EmployerType.COMPANY
                            profileCompletionViewModel.saveEmployer(
                                mapOf(
                                    EmployerProfiles.EMPLOYER_TYPE to employerType,
                                    EmployerProfiles.OWNER_NAME to (if (isCompany) contactPerson.ifBlank { companyName } else companyName).trim(),
                                    EmployerProfiles.BUSINESS_NAME to if (isCompany) companyName.trim() else "",
                                    EmployerProfiles.BUSINESS_TYPE to industry.trim(),
                                    EmployerProfiles.GSTIN to gstin,
                                    EmployerProfiles.ADDRESS to businessAddress.trim()
                                )
                            ).getOrThrow()
                            Toast.makeText(
                                context,
                                context.getString(R.string.profile_saved_success),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Error saving company details")
                        Toast.makeText(
                            context,
                            context.getString(R.string.profile_saved_failed),
                            Toast.LENGTH_SHORT
                        ).show()
                    } finally {
                        isSaving = false
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White.bg())
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            CdTopBar(
                isSaving = isSaving,
                onBack = { navController.popBackStack() },
                onSave = doSave
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CdPhotoCard(
                    profileImageUrl = profileImageUrl,
                    profileImageUri = profileImageUri,
                    isUploading = isUploadingImage,
                    onChoose = { imagePickerLauncher.launch("image/*") }
                )
                CdBasicsCard(
                    companyName = companyName,
                    onCompanyName = { companyName = it },
                    contactPerson = contactPerson,
                    onContactPerson = { contactPerson = it },
                    contactPhone = contactPhone
                )
                CdCategoryTaxCard(
                    industry = industry,
                    onIndustry = { industry = it },
                    gstin = gstin,
                    gstVerified = gstVerified,
                    onGstin = { input ->
                        gstin = input.uppercase().filter { it.isLetterOrDigit() }.take(15)
                        gstVerified = false
                    },
                    onVerify = {
                        // Format validation only; server verification not implemented.
                        if (CdGstinRegex.matches(gstin)) {
                            gstVerified = true
                        } else {
                            gstVerified = false
                            Toast.makeText(
                                context,
                                context.getString(R.string.valid_gstin_required),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                )
            }
        }

        CdBottomBar(
            isSaving = isSaving,
            onSave = doSave,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

// ============================================
// SECTIONS
// ============================================

@Composable
private fun CdTopBar(
    isSaving: Boolean,
    onBack: () -> Unit,
    onSave: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.back),
                tint = CdNavy.fg(),
                modifier = Modifier
                    .size(24.dp)
                    .clickable(onClick = onBack)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.company_details_section),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = CdNavy.fg()
            )
        }
        Text(
            text = stringResource(R.string.save),
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = CdCobalt.fg(),
            modifier = Modifier.clickable(enabled = !isSaving, onClick = onSave)
        )
    }
}

@Composable
private fun CdPhotoCard(
    profileImageUrl: String?,
    profileImageUri: Uri?,
    isUploading: Boolean,
    onChoose: () -> Unit
) {
    val dashColor = CdHint.fg()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                val sw = 1.5.dp.toPx()
                drawRoundRect(
                    color = dashColor,
                    topLeft = Offset(sw / 2f, sw / 2f),
                    size = Size(size.width - sw, size.height - sw),
                    cornerRadius = CornerRadius(16.dp.toPx()),
                    style = Stroke(
                        width = sw,
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(6.dp.toPx(), 4.dp.toPx()), 0f
                        )
                    )
                )
            }
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val hasImage = profileImageUri != null || !profileImageUrl.isNullOrBlank()
        when {
            isUploading -> {
                Box(
                    modifier = Modifier.size(72.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = CdCobalt.fg(),
                        strokeWidth = 3.dp
                    )
                }
            }
            hasImage -> {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    com.example.dutype.components.OptimizedProfileImage(
                        imageUrl = profileImageUri?.toString() ?: profileImageUrl.orEmpty(),
                        contentDescription = stringResource(R.string.company_photo),
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            else -> {
                Icon(
                    imageVector = Icons.Outlined.CameraAlt,
                    contentDescription = null,
                    tint = CdMuted.fg(),
                    modifier = Modifier.size(36.dp)
                )
            }
        }
        Text(
            text = stringResource(R.string.upload_workplace_photo_title),
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = CdNavy.fg(),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            text = stringResource(R.string.upload_workplace_photo_desc),
            fontSize = 12.sp,
            lineHeight = 17.sp,
            color = CdMuted.fg(),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp)
        )
        Box(
            modifier = Modifier
                .padding(top = 12.dp)
                .height(36.dp)
                .clip(RoundedCornerShape(18.dp))
                .border(BorderStroke(1.dp, CdNavy.bd()), RoundedCornerShape(18.dp))
                .clickable(enabled = !isUploading, onClick = onChoose)
                .padding(horizontal = 18.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.choose_photo),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = CdNavy.fg()
            )
        }
    }
}

@Composable
private fun CdCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(BorderStroke(1.dp, CdBorder.bd()), RoundedCornerShape(16.dp))
            .padding(16.dp),
        content = content
    )
}

@Composable
private fun CdBasicsCard(
    companyName: String,
    onCompanyName: (String) -> Unit,
    contactPerson: String,
    onContactPerson: (String) -> Unit,
    contactPhone: String
) {
    CdCard {
        Text(
            text = stringResource(R.string.business_basics),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = CdHint.fg(),
            letterSpacing = 0.6.sp
        )
        Spacer(modifier = Modifier.height(10.dp))
        CdField(
            label = stringResource(R.string.business_name),
            value = companyName,
            onValueChange = onCompanyName
        )
        Spacer(modifier = Modifier.height(10.dp))
        CdField(
            label = stringResource(R.string.owner_contact_name),
            value = contactPerson,
            onValueChange = onContactPerson
        )
        Spacer(modifier = Modifier.height(10.dp))
        CdField(
            label = stringResource(R.string.contact_phone_number_label),
            value = contactPhone,
            onValueChange = {},
            enabled = false,
            keyboardType = KeyboardType.Phone,
            trailing = if (contactPhone.isNotBlank()) {
                { CdVerifiedBadge() }
            } else null
        )
    }
}

@Composable
private fun CdCategoryTaxCard(
    industry: String,
    onIndustry: (String) -> Unit,
    gstin: String,
    gstVerified: Boolean,
    onGstin: (String) -> Unit,
    onVerify: () -> Unit
) {
    CdCard {
        Text(
            text = stringResource(R.string.business_category),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = CdNavy.fg()
        )
        Row(
            modifier = Modifier
                .padding(top = 10.dp)
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CdCategories.forEach { category ->
                CdChip(
                    text = stringResource(category.labelRes),
                    selected = industry == category.key,
                    onClick = { onIndustry(category.key) }
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        CdField(
            label = stringResource(R.string.gstin_optional),
            value = gstin,
            onValueChange = onGstin,
            capitalization = KeyboardCapitalization.Characters,
            trailing = {
                Box(
                    modifier = Modifier
                        .height(36.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(CdCobalt.bg())
                        .clickable(onClick = onVerify)
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.verify),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
        )
        if (gstVerified && gstin.isNotBlank()) {
            Text(
                text = stringResource(R.string.gst_active, gstin),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = CdGstGreen.fg(),
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun CdBottomBar(
    isSaving: Boolean,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White.bg())
            .navigationBarsPadding()
    ) {
        HorizontalDivider(thickness = 1.dp, color = CdBorder.bd())
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(79.dp)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(CdNavy.bg())
                    .clickable(enabled = !isSaving, onClick = onSave),
                contentAlignment = Alignment.Center
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Text(
                        text = stringResource(R.string.save_company_details_action),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

// ============================================
// SMALL BUILDING BLOCKS
// ============================================

@Composable
private fun CdChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(17.dp)
    Box(
        modifier = Modifier
            .height(34.dp)
            .clip(shape)
            .background(if (selected) CdNavy.bg() else Color.White.bg())
            .border(BorderStroke(1.dp, if (selected) CdNavy.bd() else CdBorder.bd()), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (selected) Color.White else CdChipText.fg(),
            maxLines = 1
        )
    }
}

@Composable
private fun CdVerifiedBadge() {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(CdGreenBg.bg())
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = null,
            tint = CdGreen.fg(),
            modifier = Modifier.size(12.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = stringResource(R.string.verified_label),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = CdGreen.fg(),
            maxLines = 1
        )
    }
}

/**
 * 56dp outlined field with a stacked label (11sp) above the value (14sp).
 */
@Composable
private fun CdField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    enabled: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .border(BorderStroke(1.dp, CdBorder.bd()), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                color = CdMuted.fg(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = enabled,
                singleLine = true,
                textStyle = TextStyle(
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    color = CdNavy.fg()
                ),
                cursorBrush = SolidColor(CdCobalt.fg()),
                keyboardOptions = KeyboardOptions(
                    keyboardType = keyboardType,
                    capitalization = capitalization
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                decorationBox = { innerTextField ->
                    Box {
                        if (value.isEmpty() && placeholder.isNotEmpty()) {
                            Text(
                                text = placeholder,
                                fontSize = 14.sp,
                                lineHeight = 18.sp,
                                color = CdHint.fg(),
                                maxLines = 1
                            )
                        }
                        innerTextField()
                    }
                }
            )
        }
        if (trailing != null) {
            Spacer(modifier = Modifier.width(8.dp))
            trailing()
        }
    }
}
