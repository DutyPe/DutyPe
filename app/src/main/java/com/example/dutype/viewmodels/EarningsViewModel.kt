package com.example.dutype.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dutype.worker.screens.EarningsPeriod
import com.example.dutype.worker.screens.EarningsTransaction
import com.example.dutype.worker.screens.PaymentStatus
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
    private val applicationsStore: com.example.dutype.applications.WorkerApplicationsStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(EarningsUiState())
    val uiState: StateFlow<EarningsUiState> = _uiState.asStateFlow()
    private var collecting = false

    /**
     * Earnings from the worker's applications already kept live by [WorkerApplicationsStore]
     * (each with its job card), so the home screen costs no extra reads: hired = pending pay,
     * completed = paid, at the job's pay amount.
     */
    fun loadEarnings() {
        applicationsStore.start()
        if (collecting) return
        collecting = true
        viewModelScope.launch {
            applicationsStore.applications.collect { list -> publish(list) }
        }
    }

    private fun publish(list: List<com.example.dutype.models.JobApplication>) {
        val earning = list.filter {
            it.status == com.example.dutype.models.ApplicationStatus.HIRED ||
                it.status == com.example.dutype.models.ApplicationStatus.COMPLETED
        }
        var totalEarnings = 0.0
        var pendingAmount = 0.0
        var paid = 0
        val transactions = earning.map { app ->
            val amount = (app.job?.payAmount ?: 0L).toDouble()
            val isPaid = app.status == com.example.dutype.models.ApplicationStatus.COMPLETED
            if (isPaid) { totalEarnings += amount; paid++ } else pendingAmount += amount
            EarningsTransaction(
                id = app.id,
                jobId = app.jobId,
                jobTitle = app.jobTitle.ifBlank { "Job" },
                companyName = app.companyName,
                amount = amount,
                status = if (isPaid) PaymentStatus.PAID else PaymentStatus.PENDING,
                date = app.completedAt.takeIf { it > 0 } ?: app.hiredAt.takeIf { it > 0 } ?: app.createdAt
            )
        }
        val count = transactions.size
        _uiState.update {
            it.copy(
                isLoading = false,
                totalEarnings = totalEarnings,
                pendingAmount = pendingAmount,
                completedJobs = count,
                periodCompletedJobs = count,
                avgEarningPerJob = if (paid > 0) totalEarnings / paid else 0.0,
                onTimePaymentPercentage = if (count > 0) (paid * 100) / count else 0,
                totalHoursWorked = count * 8,
                weeklyEarnings = calculateWeeklyEarnings(transactions),
                transactions = transactions,
                error = null
            )
        }
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
