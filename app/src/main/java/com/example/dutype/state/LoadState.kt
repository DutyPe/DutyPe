package com.example.dutype.state

/**
 * Sealed class representing the loading state of data operations.
 * Use this instead of multiple boolean flags (isLoading, hasError, etc.)
 * 
 * Benefits:
 * - Type-safe state handling
 * - Impossible invalid states (can't be loading AND error at same time)
 * - Exhaustive when expressions
 * - Better testability
 * 
 * Usage:
 * ```kotlin
 * sealed class JobListState {
 *     object Idle : JobListState()
 *     object Loading : JobListState()
 *     data class Success(val jobs: List<Job>) : JobListState()
 *     data class Error(val message: String) : JobListState()
 * }
 * ```
 */
sealed class LoadState<out T> {
    /** Initial state before any loading has started */
    object Idle : LoadState<Nothing>()
    
    /** Data is currently being loaded */
    object Loading : LoadState<Nothing>()
    
    /** Data is being refreshed (pull-to-refresh) while showing existing data */
    data class Refreshing<T>(val data: T) : LoadState<T>()
    
    /** Data loaded successfully */
    data class Success<T>(val data: T) : LoadState<T>()
    
    /** Loading failed with an error */
    data class Error(val message: String, val throwable: Throwable? = null) : LoadState<Nothing>()
    
    /** No data available (empty state) */
    object Empty : LoadState<Nothing>()
    
    // Helper properties
    val isLoading: Boolean get() = this is Loading
    val isRefreshing: Boolean get() = this is Refreshing<*>
    val isSuccess: Boolean get() = this is Success<*>
    val isError: Boolean get() = this is Error
    val isEmpty: Boolean get() = this is Empty
    
    /** Get data if available, null otherwise */
    fun getDataOrNull(): T? = when (this) {
        is Success -> data
        is Refreshing -> data
        else -> null
    }
    
    /** Get error message if in error state, null otherwise */
    fun getErrorOrNull(): String? = when (this) {
        is Error -> message
        else -> null
    }
}

/**
 * Extension function to map LoadState data
 */
inline fun <T, R> LoadState<T>.map(transform: (T) -> R): LoadState<R> = when (this) {
    is LoadState.Idle -> LoadState.Idle
    is LoadState.Loading -> LoadState.Loading
    is LoadState.Refreshing -> LoadState.Refreshing(transform(data))
    is LoadState.Success -> LoadState.Success(transform(data))
    is LoadState.Error -> LoadState.Error(message, throwable)
    is LoadState.Empty -> LoadState.Empty
}

/**
 * Extension function to handle LoadState in Composables
 */
inline fun <T> LoadState<T>.fold(
    onIdle: () -> Unit = {},
    onLoading: () -> Unit = {},
    onRefreshing: (T) -> Unit = {},
    onSuccess: (T) -> Unit = {},
    onError: (String) -> Unit = {},
    onEmpty: () -> Unit = {}
) {
    when (this) {
        is LoadState.Idle -> onIdle()
        is LoadState.Loading -> onLoading()
        is LoadState.Refreshing -> onRefreshing(data)
        is LoadState.Success -> onSuccess(data)
        is LoadState.Error -> onError(message)
        is LoadState.Empty -> onEmpty()
    }
}

/**
 * Simple LoadState without data (for operations like submit, delete)
 */
sealed class OperationState {
    object Idle : OperationState()
    object InProgress : OperationState()
    object Success : OperationState()
    data class Error(val message: String) : OperationState()
    
    val isInProgress: Boolean get() = this is InProgress
    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error
}
