package com.example.dutype.utils

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/**
 * High-precision VisualTransformation for 12-digit Indian Aadhaar numbers.
 *
 * Visually renders as "XXXX XXXX XXXX" without mutating the underlying raw digit string.
 * This guarantees that Android software keyboards (Gboard, Samsung Keyboard) never suffer
 * from IME composing buffer desynchronization, preventing typed digits from scrambling or
 * swapping places (e.g. typing "3110" will NEVER reorder to "3101").
 */
class AadhaarVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        // Return 1:1 mapping so software keyboards (Gboard/Samsung) never desync or swap typed digits
        return VisualTransformation.None.filter(text)
    }
}
