package com.example.dutype.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dutype.app.R
import com.example.dutype.models.UserRole
import com.example.dutype.models.isValidNormalizedReferralCode
import com.example.dutype.models.normalizeReferralCode
import com.example.dutype.ui.theme.WorkerColors
import com.example.dutype.ui.theme.bd
import com.example.dutype.ui.theme.bg
import com.example.dutype.ui.theme.fg
import com.example.dutype.viewmodels.ProfileCompletionViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

/**
 * Referral validation result
 */
data class ReferralValidationResult(
    val isValid: Boolean,
    val message: String,
    val referrerName: String? = null
)

/**
 * Validate referral code format.
 * Accepts canonical uppercase codes and legacy alphanumeric codes.
 */
fun isValidReferralCode(code: String): Boolean {
    return code.isNotBlank() && isValidNormalizedReferralCode(code)
}

/**
 * Primary Placement: Optional expandable referral code card for Profile Setup screens.
 * Sits directly below the Full Name field in Worker and Employer setup wizards.
 * Pre-fills automatically if opened via deep link.
 */
@Composable
fun ProfileSetupReferralCard(
    role: UserRole,
    profileCompletionViewModel: ProfileCompletionViewModel,
    modifier: Modifier = Modifier,
    onCodeApplied: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isExpanded by remember { mutableStateOf(false) }
    var codeText by remember { mutableStateOf("") }
    var isValidating by remember { mutableStateOf(false) }
    var isApplied by remember { mutableStateOf(false) }
    var appliedCode by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Pre-fill automatically if deep-linked or saved in onboarding state
    LaunchedEffect(Unit) {
        val saved = profileCompletionViewModel.getReferralCode()
        if (!saved.isNullOrBlank()) {
            codeText = saved
            isExpanded = true
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFFAFAFA).bg(),
        border = BorderStroke(1.dp, if (isApplied) Color(0xFF10B981).bd() else Color(0xFFE2E8F0).bd())
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Row: Expandable / Collapse Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { if (!isApplied) isExpanded = !isExpanded }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CardGiftcard,
                        contentDescription = null,
                        tint = if (isApplied) Color(0xFF10B981).fg() else Color(0xFF64748B).fg(),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.referral_have_code_optional),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.5.sp,
                            color = Color(0xFF0F172A).fg()
                        )
                    )
                }

                if (isApplied) {
                    Surface(
                        color = Color(0xFFDCFCE7).bg(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.referral_applied),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFF15803D).fg(),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                } else {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = Color(0xFF64748B).fg(),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                        .padding(bottom = 14.dp)
                ) {
                    if (!isApplied) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = codeText,
                                onValueChange = { input ->
                                    codeText = normalizeReferralCode(input).take(12)
                                    errorMessage = null
                                },
                                placeholder = {
                                    Text(
                                        text = stringResource(R.string.referral_enter_code_placeholder),
                                        fontSize = 13.sp,
                                        color = Color(0xFF94A3B8).fg()
                                    )
                                },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp),
                                shape = RoundedCornerShape(10.dp),
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    letterSpacing = 1.sp,
                                    color = Color(0xFF0F172A).fg()
                                )
                            )

                            Button(
                                onClick = {
                                    val code = normalizeReferralCode(codeText)
                                    if (code.length < 6) {
                                        errorMessage = context.getString(R.string.referral_code_not_found)
                                        return@Button
                                    }
                                    scope.launch {
                                        isValidating = true
                                        errorMessage = null
                                        try {
                                            val info = profileCompletionViewModel.validateReferralCode(code)
                                            if (info.isValid) {
                                                profileCompletionViewModel.saveReferralCode(code)
                                                runCatching { profileCompletionViewModel.applyReferralCode(code) }
                                                isApplied = true
                                                appliedCode = code
                                                onCodeApplied?.invoke(code)
                                            } else {
                                                errorMessage = info.errorMessage ?: context.getString(R.string.referral_code_not_found)
                                            }
                                        } catch (e: Exception) {
                                            errorMessage = context.getString(R.string.referral_code_not_found)
                                        } finally {
                                            isValidating = false
                                        }
                                    }
                                },
                                enabled = codeText.length >= 6 && !isValidating,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF0F172A).bg(),
                                    contentColor = Color.White
                                ),
                                modifier = Modifier.height(52.dp)
                            ) {
                                if (isValidating) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(18.dp)
                                    )
                                } else {
                                    Text(
                                        text = stringResource(R.string.referral_apply),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }

                        if (errorMessage != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = errorMessage.orEmpty(),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFFDC2626).fg(),
                                    fontSize = 12.sp
                                ),
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    } else {
                        // Applied Confirmation State
                        Surface(
                            color = Color(0xFFF8FAFC).bg(),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0).bd()),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981).fg(),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "$appliedCode applied! Bonus will be unlocked upon first job.",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color(0xFF334155).fg(),
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 12.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Secondary / Fallback Placement: Referral redemption card for Refer & Earn screens.
 * Sits below the Share card in WorkerReferEarnScreen and EmployerReferEarnScreen.
 * Shown only if the user hasn't already joined via a referral code and joined within the last 7 days.
 */
@Composable
fun ReferAndEarnRedemptionCard(
    hasExistingReferrer: Boolean,
    onSuccess: () -> Unit,
    modifier: Modifier = Modifier,
    profileCompletionViewModel: ProfileCompletionViewModel = hiltViewModel()
) {
    if (hasExistingReferrer) return

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val auth = FirebaseAuth.getInstance()
    val creationTime = auth.currentUser?.metadata?.creationTimestamp ?: 0L
    val accountAgeMillis = if (creationTime > 0L) System.currentTimeMillis() - creationTime else 0L
    val isWithin7Days = creationTime == 0L || accountAgeMillis <= (7L * 24 * 60 * 60 * 1000)

    if (!isWithin7Days) return

    var codeText by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isApplied by remember { mutableStateOf(false) }

    if (isApplied) return

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFE2E8F0).bd(), RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.bg()),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.CardGiftcard,
                    contentDescription = null,
                    tint = Color(0xFF10B981).fg(),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.referral_referred_by_friend),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color(0xFF0F172A).fg()
                    )
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.referral_redeem_within_7_days),
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFF64748B).fg(),
                    fontSize = 12.sp
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = codeText,
                    onValueChange = { input ->
                        codeText = normalizeReferralCode(input).take(12)
                        errorMessage = null
                    },
                    placeholder = {
                        Text(
                            text = stringResource(R.string.referral_enter_code_placeholder),
                            fontSize = 13.sp,
                            color = Color(0xFF94A3B8).fg()
                        )
                    },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(10.dp),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        letterSpacing = 1.sp,
                        color = Color(0xFF0F172A).fg()
                    )
                )

                Button(
                    onClick = {
                        val code = normalizeReferralCode(codeText)
                        if (code.length < 6) {
                            errorMessage = context.getString(R.string.referral_code_not_found)
                            return@Button
                        }
                        scope.launch {
                            isSubmitting = true
                            errorMessage = null
                            try {
                                val result = profileCompletionViewModel.applyReferralCode(code)
                                result.fold(
                                    onSuccess = {
                                        isApplied = true
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.referral_code_applied_success),
                                            Toast.LENGTH_LONG
                                        ).show()
                                        onSuccess()
                                    },
                                    onFailure = { err ->
                                        errorMessage = err.message ?: context.getString(R.string.referral_code_not_found)
                                    }
                                )
                            } catch (e: Exception) {
                                errorMessage = e.message ?: context.getString(R.string.referral_code_not_found)
                            } finally {
                                isSubmitting = false
                            }
                        }
                    },
                    enabled = codeText.length >= 6 && !isSubmitting,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0F172A).bg(),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.height(52.dp)
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.referral_apply),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = errorMessage.orEmpty(),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFFDC2626).fg(),
                        fontSize = 12.sp
                    ),
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
    }
}

/**
 * Referral Code Input Component (Legacy support)
 */
@Composable
fun ReferralCodeInput(
    value: String,
    onValueChange: (String) -> Unit,
    validationResult: ReferralValidationResult?,
    isValidating: Boolean = false,
    modifier: Modifier = Modifier,
    label: String = "Referral Code (Optional)",
    placeholder: String = "Enter code"
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = { newValue ->
                onValueChange(normalizeReferralCode(newValue))
            },
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            singleLine = true,
            trailingIcon = {
                when {
                    isValidating -> {
                        CircularProgressIndicator(
                            modifier = Modifier.height(20.dp),
                            strokeWidth = 2.dp
                        )
                    }

                    validationResult != null -> {
                        Icon(
                            imageVector = if (validationResult.isValid) {
                                Icons.Default.CheckCircle
                            } else {
                                Icons.Default.Error
                            },
                            contentDescription = null,
                            tint = if (validationResult.isValid) {
                                WorkerColors.Success
                            } else {
                                WorkerColors.Error
                            }
                        )
                    }
                }
            },
            isError = validationResult != null && !validationResult.isValid,
            modifier = Modifier.fillMaxWidth()
        )

        if (validationResult != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = validationResult.message,
                style = MaterialTheme.typography.bodySmall,
                color = if (validationResult.isValid) {
                    WorkerColors.Success
                } else {
                    WorkerColors.Error
                },
                modifier = Modifier.padding(start = 16.dp)
            )
        }

        if (value.isEmpty() && validationResult == null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.auto_use_a_referral_code_to_unlock_your_rs_20_s),
                style = MaterialTheme.typography.bodySmall,
                color = WorkerColors.TextSecondary,
                modifier = Modifier.padding(start = 16.dp)
            )
        }
    }
}
