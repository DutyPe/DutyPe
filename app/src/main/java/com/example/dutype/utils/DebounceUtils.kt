package com.example.dutype.utils

import android.os.SystemClock
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.semantics.Role

/**
 * Extension modifier that prevents double-taps and rapid accidental clicks
 * by throttling invocations within a sliding debounce window (default 800ms).
 */
fun Modifier.debouncedClickable(
    debounceMs: Long = 800L,
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    onClick: () -> Unit
): Modifier = composed {
    val multipleEventsCutter = remember { MultipleEventsCutter.get(debounceMs) }
    this.clickable(
        enabled = enabled,
        onClickLabel = onClickLabel,
        role = role
    ) {
        multipleEventsCutter.processEvent { onClick() }
    }
}

/**
 * Interface that guards against rapid repeated event execution.
 */
interface MultipleEventsCutter {
    fun processEvent(event: () -> Unit)

    companion object {
        fun get(debounceMs: Long = 800L): MultipleEventsCutter =
            MultipleEventsCutterImpl(debounceMs)
    }
}

private class MultipleEventsCutterImpl(private val debounceMs: Long) : MultipleEventsCutter {
    private val now: Long
        get() = SystemClock.uptimeMillis()

    private var lastEventTimeMs: Long = 0

    override fun processEvent(event: () -> Unit) {
        if (now - lastEventTimeMs >= debounceMs) {
            lastEventTimeMs = now
            event()
        }
    }
}

/**
 * Helper function to create a debounced callback lambda.
 */
@Composable
fun rememberDebouncedCallback(
    debounceMs: Long = 800L,
    action: () -> Unit
): () -> Unit {
    val multipleEventsCutter = remember { MultipleEventsCutter.get(debounceMs) }
    return remember(action, debounceMs) {
        {
            multipleEventsCutter.processEvent(action)
        }
    }
}
