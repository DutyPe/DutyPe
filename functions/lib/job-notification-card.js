"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.buildJobNotificationCard = void 0;
function cleanText(value, maxLength = 160) {
    return String(value !== null && value !== void 0 ? value : "")
        .replace(/\s+/g, " ")
        .trim()
        .slice(0, maxLength);
}
function formatSalaryType(value) {
    const normalized = cleanText(value, 40).toUpperCase();
    switch (normalized) {
        case "HOURLY": return "hourly";
        case "DAILY": return "daily";
        case "WEEKLY": return "weekly";
        case "MONTHLY": return "monthly";
        case "TASK": return "per task";
        default: return "";
    }
}
function formatSalaryText(salary, salaryType) {
    const rawSalary = cleanText(salary, 80);
    if (!rawSalary)
        return "";
    const amount = /^(rs\.?|inr|₹)/i.test(rawSalary) ? rawSalary : `Rs. ${rawSalary}`;
    const period = formatSalaryType(salaryType);
    return period ? `${amount} ${period}` : amount;
}
function splitLocation(addressText, cityText) {
    const address = cleanText(addressText, 180);
    const explicitCity = cleanText(cityText, 80);
    const parts = address.split(",").map((part) => cleanText(part, 80)).filter(Boolean);
    if (explicitCity) {
        const locationParts = parts.length > 1 && parts[parts.length - 1].toLowerCase() === explicitCity.toLowerCase()
            ? parts.slice(0, -1)
            : parts;
        return {
            locationText: locationParts.join(", ") || address,
            cityText: explicitCity,
        };
    }
    if (parts.length > 1) {
        return {
            locationText: parts.slice(0, -1).join(", "),
            cityText: parts[parts.length - 1],
        };
    }
    return { locationText: address, cityText: "" };
}
function formatDistanceText(value) {
    const distance = Number(value);
    if (!Number.isFinite(distance) || distance <= 0)
        return "";
    return `${distance.toFixed(1)} km away`;
}
function buildJobNotificationCard(input) {
    const jobTitle = cleanText(input.title, 90) || "Job";
    const salaryText = formatSalaryText(input.salary, input.salaryType);
    const location = splitLocation(input.addressText, input.cityText);
    const distanceText = formatDistanceText(input.distanceKm);
    const messageLines = [
        jobTitle,
        salaryText ? `Salary : ${salaryText}` : "",
        location.locationText ? `Location : ${location.locationText}` : "",
        location.cityText ? `City : ${location.cityText}` : "",
        distanceText ? `Distance : ${distanceText}` : "",
    ].filter(Boolean);
    const title = `Job title : ${jobTitle}`;
    const message = messageLines.join("\n");
    const data = {
        notificationStyle: "JOB_CARD",
        notificationSource: input.source,
        jobCardTitle: title,
        jobTitle,
        deepLink: input.deepLink,
    };
    if (salaryText)
        data.salaryText = salaryText;
    if (location.locationText)
        data.locationText = location.locationText;
    if (location.cityText)
        data.cityText = location.cityText;
    if (distanceText)
        data.distanceText = distanceText;
    if (input.jobId)
        data.jobId = input.jobId;
    if (input.requestId)
        data.requestId = input.requestId;
    return { title, message, data };
}
exports.buildJobNotificationCard = buildJobNotificationCard;
//# sourceMappingURL=job-notification-card.js.map