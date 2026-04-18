package com.example.dutype.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.dutype.models.UserRole
import com.example.dutype.navigation.Routes
import com.example.dutype.ui.theme.AppTypography
import com.example.dutype.ui.theme.WorkerColors

/**
 * Shared auth-flow helpers — used by [EnhancedLoginScreen] and [RegisterScreen].
 *
 * Extracted in 2026-04 cleanup; previously each screen carried byte-identical
 * copies of [safeAuthBackNavigation], [navigateToHome], [navigateToProfileSetup]
 * and a fixed-size [AuthOtpBoxes].
 *
 * The login bottom-sheet uses a slightly different (weight-based) OTP box layout
 * and continues to host its own composable.
 */

internal fun safeAuthBackNavigation(navController: NavController) {
    val popped = navController.popBackStack()
    if (!popped) {
        navController.navigate(Routes.SELECT_ROLE) {
            popUpTo(navController.graph.startDestinationId) { inclusive = true }
            launchSingleTop = true
        }
    }
}

internal fun navigateToHome(role: UserRole, navController: NavController) {
    val target = when (role) {
        UserRole.EMPLOYER -> Routes.EMPLOYER_HOME
        else -> Routes.WORKER_HOME
    }
    navController.navigate(target) {
        popUpTo(navController.graph.startDestinationId) { inclusive = true }
        launchSingleTop = true
    }
}

internal fun navigateToProfileSetup(role: UserRole, navController: NavController) {
    val target = when (role) {
        UserRole.EMPLOYER -> Routes.EMPLOYER_PROFILE_SETUP
        else -> Routes.PROFILE_SETUP
    }
    navController.navigate(target) {
        popUpTo(navController.graph.startDestinationId) { inclusive = true }
        launchSingleTop = true
    }
}

/**
 * 6-digit OTP input row used by login and registration flows.
 *
 * Renders [digitCount] fixed-size cells (48.dp) with a hidden BasicTextField on
 * top to capture input — supports SMS auto-fill and IME paste.
 */
@Composable
internal fun AuthOtpBoxes(
    otpValue: String,
    onOtpChange: (String) -> Unit,
    digitCount: Int = 6
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    var isFocused by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isFocused = true
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                isFocused = true
                focusRequester.requestFocus()
                keyboardController?.show()
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center),
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
        ) {
            repeat(digitCount) { index ->
                val isFocusedIndex = index == otpValue.length && isFocused
                val isFilledIndex = index < otpValue.length
                val digit = otpValue.getOrNull(index)?.toString() ?: ""

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(WorkerColors.CardBackground, RoundedCornerShape(8.dp))
                        .border(
                            width = 2.dp,
                            color = when {
                                isFocusedIndex -> WorkerColors.TextPrimary
                                isFilledIndex -> WorkerColors.TextPrimary
                                else -> WorkerColors.Border
                            },
                            shape = RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.Text(
                        text = digit,
                        style = AppTypography.pageTitle.copy(fontWeight = FontWeight.Bold),
                        color = WorkerColors.TextPrimary
                    )
                }
            }
        }

        BasicTextField(
            value = otpValue,
            onValueChange = { newValue ->
                val normalized = newValue.filter { it.isDigit() }.take(digitCount)
                if (normalized != otpValue) onOtpChange(normalized)
                isFocused = true
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .focusRequester(focusRequester)
                .onFocusChanged { focusState ->
                    isFocused = focusState.isFocused || focusState.hasFocus
                }
                .alpha(0f),
            singleLine = true,
            textStyle = TextStyle(color = Color.Transparent),
            cursorBrush = SolidColor(Color.Transparent),
            decorationBox = { inner -> inner() }
        )
    }
}
