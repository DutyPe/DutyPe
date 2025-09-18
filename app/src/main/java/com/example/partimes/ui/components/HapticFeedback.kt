package com.example.partimes.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.material3.ripple // Add this import

/**
 * Professional haptic feedback system
 * Provides tactile feedback for better user experience
 */

/**
 * Haptic feedback types
 */
enum class HapticType {
    LIGHT_IMPACT,
    MEDIUM_IMPACT,
    HEAVY_IMPACT,
    SELECTION_CHANGE,
    LONG_PRESS,
    SUCCESS,
    ERROR,
    WARNING,
    CUSTOM
}

/**
 * Haptic feedback service
 */
@Composable
fun rememberHapticFeedback(): HapticFeedback {
    return LocalHapticFeedback.current
}

/**
 * Haptic feedback with custom vibration
 */
@Composable
fun rememberCustomHapticFeedback(): CustomHapticFeedback {
    val context = LocalContext.current
    return remember { CustomHapticFeedback(context) }
}

/**
 * Custom haptic feedback implementation
 */
class CustomHapticFeedback(private val context: Context) {
    private val vibrator: Vibrator by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }
    
    fun lightImpact() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(50)
        }
    }
    
    fun mediumImpact() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(100)
        }
    }
    
    fun heavyImpact() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(200)
        }
    }
    
    fun selectionChange() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(30)
        }
    }
    
    fun longPress() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(150)
        }
    }
    
    fun success() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val pattern = longArrayOf(0, 100, 50, 100)
            val amplitudes = intArrayOf(0, 100, 0, 100)
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, amplitudes, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(longArrayOf(0, 100, 50, 100), -1)
        }
    }
    
    fun error() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val pattern = longArrayOf(0, 200, 100, 200)
            val amplitudes = intArrayOf(0, 150, 0, 150)
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, amplitudes, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(longArrayOf(0, 200, 100, 200), -1)
        }
    }
    
    fun warning() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val pattern = longArrayOf(0, 150, 50, 150)
            val amplitudes = intArrayOf(0, 120, 0, 120)
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, amplitudes, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(longArrayOf(0, 150, 50, 150), -1)
        }
    }
    
    fun custom(duration: Long, amplitude: Int = VibrationEffect.DEFAULT_AMPLITUDE) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(duration, amplitude))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(duration)
        }
    }
    
    fun customPattern(pattern: LongArray, amplitudes: IntArray? = null) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (amplitudes != null) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, amplitudes, -1))
            } else {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
            }
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, -1)
        }
    }
}

/**
 * Haptic feedback modifier
 */
@Composable
fun Modifier.hapticFeedback(
    hapticType: HapticType = HapticType.LIGHT_IMPACT,
    onInteraction: (HapticType) -> Unit = {}
): Modifier {
    val hapticFeedback = rememberHapticFeedback()
    val customHaptic = rememberCustomHapticFeedback()
    
    return this.clickable(
        interactionSource = remember { MutableInteractionSource() },
        onClick = {
            when (hapticType) {
                HapticType.LIGHT_IMPACT -> {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    customHaptic.lightImpact()
                }
                HapticType.MEDIUM_IMPACT -> {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    customHaptic.mediumImpact()
                }
                HapticType.HEAVY_IMPACT -> {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    customHaptic.heavyImpact()
                }
                HapticType.SELECTION_CHANGE -> {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    customHaptic.selectionChange()
                }
                HapticType.LONG_PRESS -> {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    customHaptic.longPress()
                }
                HapticType.SUCCESS -> {
                    customHaptic.success()
                }
                HapticType.ERROR -> {
                    customHaptic.error()
                }
                HapticType.WARNING -> {
                    customHaptic.warning()
                }
                HapticType.CUSTOM -> {
                    // Custom implementation
                }
            }
            onInteraction(hapticType)
        }
    )
}

/**
 * Haptic feedback on press
 */
@Composable
fun Modifier.hapticOnPress(
    hapticType: HapticType = HapticType.LIGHT_IMPACT
): Modifier {
    val hapticFeedback = rememberHapticFeedback()
    val customHaptic = rememberCustomHapticFeedback()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    LaunchedEffect(isPressed) {
        if (isPressed) {
            when (hapticType) {
                HapticType.LIGHT_IMPACT -> {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    customHaptic.lightImpact()
                }
                HapticType.MEDIUM_IMPACT -> {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    customHaptic.mediumImpact()
                }
                HapticType.HEAVY_IMPACT -> {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    customHaptic.heavyImpact()
                }
                HapticType.SELECTION_CHANGE -> {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    customHaptic.selectionChange()
                }
                HapticType.LONG_PRESS -> {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    customHaptic.longPress()
                }
                HapticType.SUCCESS -> {
                    customHaptic.success()
                }
                HapticType.ERROR -> {
                    customHaptic.error()
                }
                HapticType.WARNING -> {
                    customHaptic.warning()
                }
                HapticType.CUSTOM -> {
                    // Custom implementation
                }
            }
        }
    }
    
    return this
}

/**
 * Haptic feedback on focus
 */
@Composable
fun Modifier.hapticOnFocus(
    hapticType: HapticType = HapticType.SELECTION_CHANGE
): Modifier {
    val hapticFeedback = rememberHapticFeedback()
    val customHaptic = rememberCustomHapticFeedback()
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    
    LaunchedEffect(isFocused) {
        if (isFocused) {
            when (hapticType) {
                HapticType.LIGHT_IMPACT -> {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    customHaptic.lightImpact()
                }
                HapticType.MEDIUM_IMPACT -> {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    customHaptic.mediumImpact()
                }
                HapticType.HEAVY_IMPACT -> {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    customHaptic.heavyImpact()
                }
                HapticType.SELECTION_CHANGE -> {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    customHaptic.selectionChange()
                }
                HapticType.LONG_PRESS -> {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    customHaptic.longPress()
                }
                HapticType.SUCCESS -> {
                    customHaptic.success()
                }
                HapticType.ERROR -> {
                    customHaptic.error()
                }
                HapticType.WARNING -> {
                    customHaptic.warning()
                }
                HapticType.CUSTOM -> {
                    // Custom implementation
                }
            }
        }
    }
    
    return this
}

/**
 * Haptic feedback on hover (for desktop)
 */
@Composable
fun Modifier.hapticOnHover(
    hapticType: HapticType = HapticType.SELECTION_CHANGE
): Modifier {
    val hapticFeedback = rememberHapticFeedback()
    val customHaptic = rememberCustomHapticFeedback()
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    
    LaunchedEffect(isHovered) {
        if (isHovered) {
            when (hapticType) {
                HapticType.LIGHT_IMPACT -> {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    customHaptic.lightImpact()
                }
                HapticType.MEDIUM_IMPACT -> {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    customHaptic.mediumImpact()
                }
                HapticType.HEAVY_IMPACT -> {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    customHaptic.heavyImpact()
                }
                HapticType.SELECTION_CHANGE -> {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    customHaptic.selectionChange()
                }
                HapticType.LONG_PRESS -> {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    customHaptic.longPress()
                }
                HapticType.SUCCESS -> {
                    customHaptic.success()
                }
                HapticType.ERROR -> {
                    customHaptic.error()
                }
                HapticType.WARNING -> {
                    customHaptic.warning()
                }
                HapticType.CUSTOM -> {
                    // Custom implementation
                }
            }
        }
    }
    
    return this
}

/**
 * Haptic feedback composable
 */
@Composable
fun HapticFeedbackTrigger(
    trigger: Boolean,
    hapticType: HapticType = HapticType.LIGHT_IMPACT
) {
    val hapticFeedback = rememberHapticFeedback()
    val customHaptic = rememberCustomHapticFeedback()
    
    LaunchedEffect(trigger) {
        if (trigger) {
            when (hapticType) {
                HapticType.LIGHT_IMPACT -> {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    customHaptic.lightImpact()
                }
                HapticType.MEDIUM_IMPACT -> {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    customHaptic.mediumImpact()
                }
                HapticType.HEAVY_IMPACT -> {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    customHaptic.heavyImpact()
                }
                HapticType.SELECTION_CHANGE -> {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    customHaptic.selectionChange()
                }
                HapticType.LONG_PRESS -> {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    customHaptic.longPress()
                }
                HapticType.SUCCESS -> {
                    customHaptic.success()
                }
                HapticType.ERROR -> {
                    customHaptic.error()
                }
                HapticType.WARNING -> {
                    customHaptic.warning()
                }
                HapticType.CUSTOM -> {
                    // Custom implementation
                }
            }
        }
    }
}

/**
 * Haptic feedback for button clicks
 */
@Composable
fun Modifier.hapticButtonClick(
    hapticType: HapticType = HapticType.MEDIUM_IMPACT
): Modifier {
    return this.hapticOnPress(hapticType)
}

/**
 * Haptic feedback for card taps
 */
@Composable
fun Modifier.hapticCardTap(
    hapticType: HapticType = HapticType.LIGHT_IMPACT
): Modifier {
    return this.hapticOnPress(hapticType)
}

/**
 * Haptic feedback for list item selection
 */
@Composable
fun Modifier.hapticListItemSelection(
    hapticType: HapticType = HapticType.SELECTION_CHANGE
): Modifier {
    return this.hapticOnPress(hapticType)
}

/**
 * Haptic feedback for toggle switches
 */
@Composable
fun Modifier.hapticToggle(
    hapticType: HapticType = HapticType.SELECTION_CHANGE
): Modifier {
    return this.hapticOnPress(hapticType)
}

/**
 * Haptic feedback for slider changes
 */
@Composable
fun Modifier.hapticSlider(
    hapticType: HapticType = HapticType.SELECTION_CHANGE
): Modifier {
    return this.hapticOnPress(hapticType)
}

/**
 * Haptic feedback for success actions
 */
@Composable
fun Modifier.hapticSuccess(
    hapticType: HapticType = HapticType.SUCCESS
): Modifier {
    return this.hapticOnPress(hapticType)
}

/**
 * Haptic feedback for error actions
 */
@Composable
fun Modifier.hapticError(
    hapticType: HapticType = HapticType.ERROR
): Modifier {
    return this.hapticOnPress(hapticType)
}

/**
 * Haptic feedback for warning actions
 */
@Composable
fun Modifier.hapticWarning(
    hapticType: HapticType = HapticType.WARNING
): Modifier {
    return this.hapticOnPress(hapticType)
}
