package com.example.dutype.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dutype.utils.NetworkMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Connectivity ViewModel - Provides network status to UI
 * 
 * This ViewModel is shared across all screens via Hilt.
 * It monitors network connectivity and exposes it as a StateFlow.
 */
@HiltViewModel
class ConnectivityViewModel @Inject constructor(
    networkMonitor: NetworkMonitor
) : ViewModel() {
    
    /**
     * StateFlow that emits true when online, false when offline
     * 
     * Usage in Composable:
     * ```
     * val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
     * ```
     */
    val isOnline: StateFlow<Boolean> = networkMonitor.isOnline
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = networkMonitor.isCurrentlyOnline()
        )
}
