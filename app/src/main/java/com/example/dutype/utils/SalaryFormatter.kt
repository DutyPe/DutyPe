package com.example.dutype.utils

import com.example.dutype.employer.models.PayType
import java.text.NumberFormat
import java.util.Locale

/**
 * Display text for a job's pay (`payAmount` whole rupees + `payType`).
 * `payAmount` 0 or payType NEGOTIABLE means the employer did not fix a wage.
 */
object SalaryFormatter {

    /** Highest pay a job may offer, whatever the pay type (the server enforces the same). */
    const val MAX_PAY_RUPEES = 50_000L

    private val indianGrouping: NumberFormat = NumberFormat.getIntegerInstance(Locale("en", "IN"))

    /** "15,000" — no currency symbol. */
    fun amount(payAmount: Long): String = indianGrouping.format(payAmount)

    /** "₹15,000/month" or "Negotiable". */
    fun display(payAmount: Long, payType: String): String {
        val type = PayType.fromKey(payType)
        if (payAmount <= 0L || type == PayType.NEGOTIABLE) return "Negotiable"
        return "₹${amount(payAmount)}/${type.perUnit}"
    }

    /** Approximate monthly rupees, used to compare jobs with different pay types. */
    fun monthlyEquivalent(payAmount: Long, payType: String): Long = when (PayType.fromKey(payType)) {
        PayType.HOURLY -> payAmount * 8 * 26
        PayType.DAILY -> payAmount * 26
        PayType.WEEKLY -> payAmount * 13 / 3
        PayType.MONTHLY -> payAmount
        PayType.NEGOTIABLE -> 0L
    }
}
