package com.example.dutype.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dutype.worker.screens.EarningsPeriod
import com.example.dutype.worker.screens.EarningsTransaction
import com.example.dutype.worker.screens.PaymentStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.util.*
import javax.inject.Inject

/**
 * Earnings Dashboard ViewModel
 * 
 * Manages worker earnings data, transactions, and statistics
 */

data class EarningsUiState(
    val isLoading: Boolean = true,
    val totalEarnings: Double = 0.0,
    val pendingAmount: Double = 0.0,
    val completedJobs: Int = 0,
    val periodCompletedJobs: Int = 0,
    val avgEarningPerJob: Double = 0.0,
    val onTimePaymentPercentage: Int = 0,
    val totalHoursWorked: Int = 0,
    val weeklyEarnings: List<Double> = listOf(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0),
    val transactions: List<EarningsTransaction> = emptyList(),
    val selectedPeriod: EarningsPeriod = EarningsPeriod.THIS_MONTH,
    val error: String? = null
)

@HiltViewModel
class EarningsViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(EarningsUiState())
    val uiState: StateFlow<EarningsUiState> = _uiState.asStateFlow()
    
    fun loadEarnings() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                val userId = auth.currentUser?.uid ?: return@launch
                
                // Strict schema: applications contain only relationship/status fields.
                // Earnings fields are derived from jobs collection using jobId.
                val applications = firestore.collection(com.example.dutype.firestore.FirestoreCollections.APPLICATIONS)
                    .whereEqualTo("workerId", userId)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .limit(100) // P0 FIX: Cap at 100 for performance at scale
                    .get()
                    .await()

                // Bug #15 fix: earnings unlock when an application is HIRED
                // (in-progress) or COMPLETED (employer marked done = paid).
                // Legacy strings kept for backward compatibility with any
                // historical docs.
                val earningStatuses = setOf("hired", "completed", "accepted", "in_progress")
                val earningApplications = applications.documents.filter { doc ->
                    val status = doc.getString("status")?.lowercase()
                    status in earningStatuses
                }

                val jobIds = earningApplications.mapNotNull { it.getString("jobId") }.distinct()
                val jobInfoById = fetchJobEarningInfo(jobIds)
                
                val transactions = mutableListOf<EarningsTransaction>()
                var totalEarnings = 0.0
                var pendingAmount = 0.0
                var onTimePayments = 0
                var totalHours = 0
                
                for (doc in earningApplications) {
                    val jobId = doc.getString("jobId") ?: continue
                    val statusValue = doc.getString("status")?.lowercase().orEmpty()
                    val amount = jobInfoById[jobId]?.salary ?: 0.0
                    val isPaid = statusValue == "completed"
                    val completedAt = doc.getTimestamp("createdAt")?.toDate()?.time ?: System.currentTimeMillis()
                    val hoursWorked = 8
                    
                    val jobTitle = jobInfoById[jobId]?.title ?: "Job"
                    val companyName = ""
                    
                    val status = when {
                        isPaid -> PaymentStatus.PAID
                        else -> PaymentStatus.PENDING
                    }
                    
                    if (isPaid) {
                        totalEarnings += amount
                        onTimePayments++
                    } else {
                        pendingAmount += amount
                    }
                    
                    totalHours += hoursWorked
                    
                    transactions.add(
                        EarningsTransaction(
                            id = doc.id,
                            jobId = jobId,
                            jobTitle = jobTitle,
                            companyName = companyName,
                            amount = amount,
                            status = status,
                            date = completedAt
                        )
                    )
                }
                
                // Calculate weekly earnings
                val weeklyEarnings = calculateWeeklyEarnings(transactions)
                
                // Calculate stats
                val completedJobs = transactions.size
                val avgEarning = if (completedJobs > 0) totalEarnings / completedJobs else 0.0
                val onTimePercentage = if (completedJobs > 0) (onTimePayments * 100) / completedJobs else 0
                
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        totalEarnings = totalEarnings,
                        pendingAmount = pendingAmount,
                        completedJobs = completedJobs,
                        periodCompletedJobs = completedJobs,
                        avgEarningPerJob = avgEarning,
                        onTimePaymentPercentage = onTimePercentage,
                        totalHoursWorked = totalHours,
                        weeklyEarnings = weeklyEarnings,
                        transactions = transactions
                    )
                }
                
            } catch (e: Exception) {
                Timber.e(e, "Failed to load earnings")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
            }
        }
    }

    private data class JobEarningInfo(
        val title: String,
        val salary: Double
    )

    private suspend fun fetchJobEarningInfo(jobIds: List<String>): Map<String, JobEarningInfo> {
        if (jobIds.isEmpty()) return emptyMap()

        val result = mutableMapOf<String, JobEarningInfo>()
        jobIds.chunked(10).forEach { chunk ->
            val snapshot = firestore.collection(com.example.dutype.firestore.FirestoreCollections.JOBS)
                .whereIn(FieldPath.documentId(), chunk)
                .get()
                .await()

            snapshot.documents.forEach { doc ->
                val salary = (doc.get("salary") as? Number)?.toDouble() ?: 0.0
                result[doc.id] = JobEarningInfo(
                    title = doc.getString("title") ?: "Job",
                    salary = salary
                )
            }
        }
        return result
    }
    
    fun filterByPeriod(period: EarningsPeriod) {
        viewModelScope.launch {
            _uiState.update { it.copy(selectedPeriod = period) }
            
            val allTransactions = _uiState.value.transactions
            val now = System.currentTimeMillis()
            val calendar = Calendar.getInstance()
            
            val startTime = when (period) {
                EarningsPeriod.THIS_WEEK -> {
                    calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                    calendar.set(Calendar.HOUR_OF_DAY, 0)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.timeInMillis
                }
                EarningsPeriod.THIS_MONTH -> {
                    calendar.set(Calendar.DAY_OF_MONTH, 1)
                    calendar.set(Calendar.HOUR_OF_DAY, 0)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.timeInMillis
                }
                EarningsPeriod.LAST_MONTH -> {
                    calendar.add(Calendar.MONTH, -1)
                    calendar.set(Calendar.DAY_OF_MONTH, 1)
                    calendar.timeInMillis
                }
                EarningsPeriod.LAST_3_MONTHS -> {
                    calendar.add(Calendar.MONTH, -3)
                    calendar.timeInMillis
                }
                EarningsPeriod.ALL_TIME -> 0L
            }
            
            val filteredTransactions = allTransactions.filter { it.date >= startTime }
            val periodEarnings = filteredTransactions
                .filter { it.status == PaymentStatus.PAID }
                .sumOf { it.amount }
            val periodJobs = filteredTransactions.size
            val avgEarning = if (periodJobs > 0) periodEarnings / periodJobs else 0.0
            
            _uiState.update {
                it.copy(
                    periodCompletedJobs = periodJobs,
                    avgEarningPerJob = avgEarning,
                    weeklyEarnings = calculateWeeklyEarnings(filteredTransactions)
                )
            }
        }
    }
    
    private fun calculateWeeklyEarnings(transactions: List<EarningsTransaction>): List<Double> {
        val calendar = Calendar.getInstance()
        val weekStart = calendar.apply {
            set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }.timeInMillis
        
        val dailyEarnings = MutableList(7) { 0.0 }
        
        transactions
            .filter { it.date >= weekStart && it.status == PaymentStatus.PAID }
            .forEach { transaction ->
                val transactionCal = Calendar.getInstance().apply { timeInMillis = transaction.date }
                val dayOfWeek = (transactionCal.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7
                if (dayOfWeek in 0..6) {
                    dailyEarnings[dayOfWeek] += transaction.amount
                }
            }
        
        return dailyEarnings
    }
}
