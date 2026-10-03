"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.PAY_RULES = void 0;
exports.vacancyPayProblem = vacancyPayProblem;
exports.urgentPayProblem = urgentPayProblem;
/**
 * Pay rules for a hyper-local job app (same numbers as the app's PayRules.kt):
 *
 *  - Regular vacancies are steady jobs paid WEEKLY or MONTHLY at shops, malls, homes and offices
 *    in a tier-2 city: ₹3,000–₹40,000 a month or ₹1,000–₹10,000 a week; full-time work at least
 *    ₹8,000 a month / ₹2,000 a week. (Telangana Zone I minimum wage for unskilled shop work is
 *    about ₹12,750 a month; part-time house help in Khammam is ₹3,000–₹5,000.)
 *  - Urgent posts are short work, hours to a couple of days, paid the same day: ₹200–₹2,000 per person.
 */
exports.PAY_RULES = {
    vacancyPayTypes: ["MONTHLY", "WEEKLY"],
    vacancyEmploymentTypes: ["FULL_TIME", "PART_TIME"],
    monthly: { min: 3000, minFullTime: 8000, max: 40000 },
    weekly: { min: 1000, minFullTime: 2000, max: 10000 },
    urgent: { min: 200, max: 2000 },
};
/** Why this vacancy pay is not allowed, or null. */
function vacancyPayProblem(payType, employmentType, amount) {
    if (!exports.PAY_RULES.vacancyPayTypes.includes(payType)) {
        return "Regular jobs are paid weekly or monthly. For daily or hourly work, post an urgent need.";
    }
    if (!exports.PAY_RULES.vacancyEmploymentTypes.includes(employmentType)) {
        return "Choose full-time or part-time. For daily work, post an urgent need.";
    }
    const r = payType === "WEEKLY" ? exports.PAY_RULES.weekly : exports.PAY_RULES.monthly;
    const unit = payType === "WEEKLY" ? "a week" : "a month";
    const min = employmentType === "FULL_TIME" ? r.minFullTime : r.min;
    if (amount < min)
        return `Pay should be at least ₹${min.toLocaleString("en-IN")} ${unit} for this job`;
    if (amount > r.max)
        return `DutyPe jobs are local jobs: pay can be at most ₹${r.max.toLocaleString("en-IN")} ${unit}`;
    return null;
}
/** Why this urgent pay per person is not allowed, or null. */
function urgentPayProblem(amount) {
    const r = exports.PAY_RULES.urgent;
    if (amount < r.min || amount > r.max) {
        return `Urgent work is short work paid the same day: ₹${r.min} to ₹${r.max.toLocaleString("en-IN")} per person. For a regular salary, post a job.`;
    }
    return null;
}
//# sourceMappingURL=pay-rules.js.map