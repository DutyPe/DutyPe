package com.example.dutype.utils

import com.dutype.app.R
import com.example.dutype.employer.models.EmploymentType
import com.example.dutype.employer.models.PayType

/**
 * What DutyPe is for, in pay terms (same numbers as functions/src/lib/pay-rules.ts):
 *
 *  - Regular vacancies are steady jobs paid WEEKLY or MONTHLY (shops, malls, homes, offices in a
 *    tier-2 city, not IT salaries): ₹3,000–₹40,000 a month, ₹1,000–₹10,000 a week; a full-time
 *    job at least ₹8,000 a month / ₹2,000 a week.
 *  - Urgent posts are short work (hours to a couple of days) paid the same day: ₹200–₹2,000 per person.
 *  Below about ₹12,750 a month for full-time work the employer sees the Telangana minimum-wage hint.
 */
object PayRules {
    const val MONTHLY_MIN = 3_000L
    const val MONTHLY_MIN_FULL_TIME = 8_000L
    const val MONTHLY_MAX = 40_000L
    const val WEEKLY_MIN = 1_000L
    const val WEEKLY_MIN_FULL_TIME = 2_000L
    const val WEEKLY_MAX = 10_000L
    const val URGENT_MIN = 200L
    const val URGENT_MAX = 2_000L
    /** Telangana Zone I minimum wage for unskilled full-time work (approx., Apr 2025). */
    const val MIN_WAGE_MONTHLY_HINT = 12_750L

    val VACANCY_PAY_TYPES = listOf(PayType.MONTHLY, PayType.WEEKLY)
    val VACANCY_EMPLOYMENT_TYPES = listOf(EmploymentType.FULL_TIME, EmploymentType.PART_TIME)

    /** Old daily / hourly / negotiable values become monthly for vacancies. */
    fun vacancyPayType(type: PayType): PayType = if (type in VACANCY_PAY_TYPES) type else PayType.MONTHLY
    fun vacancyEmploymentType(type: EmploymentType): EmploymentType = if (type in VACANCY_EMPLOYMENT_TYPES) type else EmploymentType.FULL_TIME

    fun range(type: PayType, employment: EmploymentType): LongRange {
        val full = employment == EmploymentType.FULL_TIME
        return when (type) {
            PayType.WEEKLY -> (if (full) WEEKLY_MIN_FULL_TIME else WEEKLY_MIN)..WEEKLY_MAX
            else -> (if (full) MONTHLY_MIN_FULL_TIME else MONTHLY_MIN)..MONTHLY_MAX
        }
    }

    /** Error message for a vacancy's pay, or null when it is fine. */
    fun vacancyError(context: android.content.Context, type: PayType, employment: EmploymentType, amount: Long): String? {
        val r = range(type, employment)
        val unit = context.getString(if (type == PayType.WEEKLY) R.string.per_week else R.string.per_month)
        return when {
            amount <= 0L -> null
            amount < r.first -> context.getString(R.string.pay_rule_too_low, r.first.toInt(), unit)
            amount > r.last -> context.getString(R.string.pay_rule_too_high, r.last.toInt(), unit)
            else -> null
        }
    }

    /** Soft hint when full-time monthly pay is below the state minimum wage. */
    fun minimumWageHint(type: PayType, employment: EmploymentType, amount: Long): Boolean =
        employment == EmploymentType.FULL_TIME && type == PayType.MONTHLY && amount in 1 until MIN_WAGE_MONTHLY_HINT
}
