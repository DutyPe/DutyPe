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
        val raw = text.text.filter { it.isDigit() }.take(12)
        val out = StringBuilder()

        for (i in raw.indices) {
            out.append(raw[i])
            if ((i == 3 || i == 7) && i != raw.lastIndex) {
                out.append(" ")
            }
        }

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 0) return 0
                val clamped = offset.coerceAtMost(raw.length)
                return when {
                    clamped <= 4 -> clamped
                    clamped <= 8 -> (clamped + 1).coerceAtMost(out.length)
                    else -> (clamped + 2).coerceAtMost(out.length)
                }
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 0) return 0
                val clamped = offset.coerceAtMost(out.length)
                return when {
                    clamped <= 4 -> clamped
                    clamped <= 9 -> (clamped - 1).coerceAtLeast(0).coerceAtMost(raw.length)
                    else -> (clamped - 2).coerceAtLeast(0).coerceAtMost(raw.length)
                }
            }
        }

        return TransformedText(AnnotatedString(out.toString()), offsetMapping)
    }
}
