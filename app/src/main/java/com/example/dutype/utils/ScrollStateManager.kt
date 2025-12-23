package com.example.dutype.utils

import androidx.compose.runtime.*

@Stable
class ScrollStateManager {
    // Bottom bar visibility - hides on scroll down, shows on scroll up
    private val _isBottomBarVisible = mutableStateOf(true)
    val isBottomBarVisible: State<Boolean> = derivedStateOf { _isBottomBarVisible.value }

    private val _isScrollingUp = mutableStateOf(false)
    val isScrollingUp: State<Boolean> = derivedStateOf { _isScrollingUp.value }

    private var lastScrollOffset = 0f
    private var accumulatedDelta = 0f
    private val scrollThreshold = 50f // Minimum scroll distance to trigger hide/show

    fun onScroll(offset: Float) {
        val delta = offset - lastScrollOffset
        lastScrollOffset = offset
        
        // Accumulate scroll delta
        accumulatedDelta += delta
        
        // Only change visibility after scrolling past threshold
        if (accumulatedDelta > scrollThreshold) {
            // Scrolling down - hide bottom bar
            _isBottomBarVisible.value = false
            _isScrollingUp.value = false
            accumulatedDelta = 0f
        } else if (accumulatedDelta < -scrollThreshold) {
            // Scrolling up - show bottom bar
            _isBottomBarVisible.value = true
            _isScrollingUp.value = true
            accumulatedDelta = 0f
        }
    }
    
    fun onScrollDelta(delta: Float) {
        accumulatedDelta += delta
        
        if (accumulatedDelta > scrollThreshold) {
            _isBottomBarVisible.value = false
            _isScrollingUp.value = false
            accumulatedDelta = 0f
        } else if (accumulatedDelta < -scrollThreshold) {
            _isBottomBarVisible.value = true
            _isScrollingUp.value = true
            accumulatedDelta = 0f
        }
    }

    fun showBottomBar() {
        _isBottomBarVisible.value = true
        accumulatedDelta = 0f
    }

    fun hideBottomBar() {
        _isBottomBarVisible.value = false
        accumulatedDelta = 0f
    }

    fun reset() {
        _isBottomBarVisible.value = true
        _isScrollingUp.value = false
        lastScrollOffset = 0f
        accumulatedDelta = 0f
    }
}

@Composable
fun rememberScrollStateManager(): ScrollStateManager {
    return remember { ScrollStateManager() }
}
