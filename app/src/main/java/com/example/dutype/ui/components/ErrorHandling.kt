package com.example.dutype.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.pow

/**
 * Professional error handling components
 * Provides user-friendly error states with retry mechanisms
 */

/**
 * Error types for different scenarios
 */
enum class ErrorType {
    NETWORK_ERROR,
    SERVER_ERROR,
    VALIDATION_ERROR,
    AUTHENTICATION_ERROR,
    PERMISSION_ERROR,
    NOT_FOUND_ERROR,
    TIMEOUT_ERROR,
    UNKNOWN_ERROR
}

/**
 * Error state data class
 */
data class ErrorState(
    val type: ErrorType,
    val title: String,
    val message: String,
    val icon: ImageVector,
    val canRetry: Boolean = true,
    val retryAction: (() -> Unit)? = null,
    val alternativeAction: (() -> Unit)? = null,
    val alternativeActionText: String? = null
)

/**
 * Error screen component
 */
@Composable
fun ErrorScreen(
    errorState: ErrorState,
    modifier: Modifier = Modifier,
    onRetry: () -> Unit = {},
    onAlternativeAction: (() -> Unit)? = null
) {
    var isRetrying by remember { mutableStateOf(false) }
    
    // Reset retrying state after delay
    LaunchedEffect(isRetrying) {
        if (isRetrying) {
            delay(2000)
            isRetrying = false
        }
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Error icon with animation
        AnimatedVisibility(
            visible = !isRetrying,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut()
        ) {
            Icon(
                imageVector = errorState.icon,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.error
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Error title
        Text(
            text = errorState.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Error message
        Text(
            text = errorState.message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = MaterialTheme.typography.bodyLarge.lineHeight
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Action buttons
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Retry button
            if (errorState.canRetry) {
                Button(
                    onClick = {
                        isRetrying = true
                        onRetry()
                    },
                    enabled = !isRetrying,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isRetrying) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Retrying...")
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Try Again")
                    }
                }
            }
            
            // Alternative action button
            if (onAlternativeAction != null && errorState.alternativeActionText != null) {
                OutlinedButton(
                    onClick = onAlternativeAction,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(errorState.alternativeActionText)
                }
            }
        }
    }
}

/**
 * Error card component
 */
@Composable
fun ErrorCard(
    errorState: ErrorState,
    modifier: Modifier = Modifier,
    onRetry: () -> Unit = {},
    onDismiss: (() -> Unit)? = null
) {
    var isRetrying by remember { mutableStateOf(false) }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Error icon
            Icon(
                imageVector = errorState.icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onErrorContainer
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Error content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = errorState.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                
                Text(
                    text = errorState.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            
            // Action buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (errorState.canRetry) {
                    IconButton(
                        onClick = {
                            isRetrying = true
                            onRetry()
                        },
                        enabled = !isRetrying
                    ) {
                        if (isRetrying) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Retry",
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
                
                if (onDismiss != null) {
                    IconButton(
                        onClick = onDismiss
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
    }
}

/**
 * Error snackbar component
 */
@Composable
fun ErrorSnackbar(
    errorState: ErrorState,
    modifier: Modifier = Modifier,
    onRetry: () -> Unit = {},
    onDismiss: () -> Unit = {}
) {
    var isRetrying by remember { mutableStateOf(false) }
    
    // Reset retrying state after delay
    LaunchedEffect(isRetrying) {
        if (isRetrying) {
            delay(2000)
            isRetrying = false
        }
    }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.error
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = errorState.icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onError
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Text(
                text = errorState.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onError,
                modifier = Modifier.weight(1f)
            )
            
            if (errorState.canRetry) {
                TextButton(
                    onClick = {
                        isRetrying = true
                        onRetry()
                    },
                    enabled = !isRetrying
                ) {
                    if (isRetrying) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onError
                        )
                    } else {
                        Text(
                            text = "Retry",
                            color = MaterialTheme.colorScheme.onError
                        )
                    }
                }
            }
            
            IconButton(
                onClick = onDismiss
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = MaterialTheme.colorScheme.onError
                )
            }
        }
    }
}

/**
 * Empty state component
 */
@Composable
fun EmptyState(
    title: String,
    message: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        if (actionText != null && onAction != null) {
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = onAction,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(actionText)
            }
        }
    }
}

/**
 * Loading error state
 */
@Composable
fun LoadingErrorState(
    error: Throwable,
    modifier: Modifier = Modifier,
    onRetry: () -> Unit = {}
) {
    val errorState = when {
        error.message?.contains("network", ignoreCase = true) == true -> 
            ErrorState(
                type = ErrorType.NETWORK_ERROR,
                title = "Connection Error",
                message = "Please check your internet connection and try again.",
                icon = Icons.Default.WifiOff
            )
        error.message?.contains("timeout", ignoreCase = true) == true ->
            ErrorState(
                type = ErrorType.TIMEOUT_ERROR,
                title = "Request Timeout",
                message = "The request took too long to complete. Please try again.",
                icon = Icons.Default.Schedule
            )
        error.message?.contains("404", ignoreCase = true) == true ->
            ErrorState(
                type = ErrorType.NOT_FOUND_ERROR,
                title = "Not Found",
                message = "The requested resource was not found.",
                icon = Icons.Default.SearchOff
            )
        error.message?.contains("401", ignoreCase = true) == true ->
            ErrorState(
                type = ErrorType.AUTHENTICATION_ERROR,
                title = "Authentication Required",
                message = "Please log in again to continue.",
                icon = Icons.Default.Lock
            )
        error.message?.contains("403", ignoreCase = true) == true ->
            ErrorState(
                type = ErrorType.PERMISSION_ERROR,
                title = "Access Denied",
                message = "You don't have permission to access this resource.",
                icon = Icons.Default.Block
            )
        error.message?.contains("500", ignoreCase = true) == true ->
            ErrorState(
                type = ErrorType.SERVER_ERROR,
                title = "Server Error",
                message = "Something went wrong on our end. Please try again later.",
                icon = Icons.Default.Error
            )
        else ->
            ErrorState(
                type = ErrorType.UNKNOWN_ERROR,
                title = "Something Went Wrong",
                message = "An unexpected error occurred. Please try again.",
                icon = Icons.Default.Error
            )
    }
    
    ErrorScreen(
        errorState = errorState,
        modifier = modifier,
        onRetry = onRetry
    )
}

/**
 * Retry mechanism with exponential backoff
 */
@Composable
fun RetryWithBackoff(
    retryCount: Int,
    maxRetries: Int = 3,
    onRetry: () -> Unit
) {
    val delay = remember(retryCount) {
        (1000 * 2.0.pow(retryCount.toDouble())).toLong()
    }
    
    LaunchedEffect(retryCount) {
        if (retryCount < maxRetries) {
            delay(delay)
            onRetry()
        }
    }
}

/**
 * Error boundary component
 */
@Composable
fun ErrorBoundary(
    error: Throwable?,
    modifier: Modifier = Modifier,
    onRetry: () -> Unit = {},
    content: @Composable () -> Unit
) {
    if (error != null) {
        LoadingErrorState(
            error = error,
            modifier = modifier,
            onRetry = onRetry
        )
    } else {
        content()
    }
}

/**
 * Network status indicator
 */
@Composable
fun NetworkStatusIndicator(
    isOnline: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = !isOnline,
        enter = slideInVertically() + fadeIn(),
        exit = slideOutVertically() + fadeOut()
    ) {
        Card(
            modifier = modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.WifiOff,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = "You're offline. Some features may not be available.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}
