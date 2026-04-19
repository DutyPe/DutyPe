"use strict";
/**
 * P0 SECURITY FIX: Input Validation for Cloud Functions
 *
 * Prevents injection attacks, XSS, and malformed data from reaching the database.
 * All Cloud Functions MUST validate inputs using these utilities.
 *
 * @author DutyPe Security Team
 * @since 2.4.1
 */
Object.defineProperty(exports, "__esModule", { value: true });
exports.assertAppCheck = exports.validateEnum = exports.validateArray = exports.validateMessage = exports.sanitizeText = exports.validateDocumentId = exports.validateUserId = exports.validatePhone = exports.validateEmail = exports.validateBoolean = exports.validateNumber = exports.validateString = void 0;
const functions = require("firebase-functions");
/**
 * Validate string input
 */
function validateString(value, fieldName, options = {}) {
    // Check if required
    if (options.required && (value === null || value === undefined || value === "")) {
        throw new functions.https.HttpsError("invalid-argument", `${fieldName} is required`);
    }
    // If not required and empty, return empty string
    if (!value) {
        return "";
    }
    // Check type
    if (typeof value !== "string") {
        throw new functions.https.HttpsError("invalid-argument", `${fieldName} must be a string`);
    }
    // Trim whitespace
    const trimmed = value.trim();
    // Check min length
    if (options.minLength && trimmed.length < options.minLength) {
        throw new functions.https.HttpsError("invalid-argument", `${fieldName} must be at least ${options.minLength} characters`);
    }
    // Check max length
    if (options.maxLength && trimmed.length > options.maxLength) {
        throw new functions.https.HttpsError("invalid-argument", `${fieldName} must be at most ${options.maxLength} characters`);
    }
    // Check pattern
    if (options.pattern && !options.pattern.test(trimmed)) {
        throw new functions.https.HttpsError("invalid-argument", `${fieldName} has invalid format`);
    }
    return trimmed;
}
exports.validateString = validateString;
/**
 * Validate number input
 */
function validateNumber(value, fieldName, options = {}) {
    // Check if required
    if (options.required && (value === null || value === undefined)) {
        throw new functions.https.HttpsError("invalid-argument", `${fieldName} is required`);
    }
    // If not required and empty, return 0
    if (value === null || value === undefined) {
        return 0;
    }
    // Convert to number
    const num = Number(value);
    // Check if valid number
    if (isNaN(num)) {
        throw new functions.https.HttpsError("invalid-argument", `${fieldName} must be a valid number`);
    }
    // Check if integer required
    if (options.integer && !Number.isInteger(num)) {
        throw new functions.https.HttpsError("invalid-argument", `${fieldName} must be an integer`);
    }
    // Check min
    if (options.min !== undefined && num < options.min) {
        throw new functions.https.HttpsError("invalid-argument", `${fieldName} must be at least ${options.min}`);
    }
    // Check max
    if (options.max !== undefined && num > options.max) {
        throw new functions.https.HttpsError("invalid-argument", `${fieldName} must be at most ${options.max}`);
    }
    return num;
}
exports.validateNumber = validateNumber;
/**
 * Validate boolean input
 */
function validateBoolean(value, fieldName, required = false) {
    if (required && (value === null || value === undefined)) {
        throw new functions.https.HttpsError("invalid-argument", `${fieldName} is required`);
    }
    if (value === null || value === undefined) {
        return false;
    }
    if (typeof value !== "boolean") {
        throw new functions.https.HttpsError("invalid-argument", `${fieldName} must be a boolean`);
    }
    return value;
}
exports.validateBoolean = validateBoolean;
/**
 * Validate email format
 */
function validateEmail(email, required = false) {
    const emailStr = validateString(email, "email", {
        required,
        maxLength: 255,
        pattern: /^[^\s@]+@[^\s@]+\.[^\s@]+$/,
    });
    return emailStr.toLowerCase();
}
exports.validateEmail = validateEmail;
/**
 * Validate phone number format (Indian format)
 */
function validatePhone(phone, required = false) {
    return validateString(phone, "phone", {
        required,
        minLength: 10,
        maxLength: 15,
        pattern: /^\+?[1-9]\d{9,14}$/,
    });
}
exports.validatePhone = validatePhone;
/**
 * Validate user ID format
 */
function validateUserId(userId, required = true) {
    return validateString(userId, "userId", {
        required,
        minLength: 10,
        maxLength: 128,
        pattern: /^[a-zA-Z0-9_-]+$/,
    });
}
exports.validateUserId = validateUserId;
/**
 * Validate document ID format
 */
function validateDocumentId(docId, fieldName = "documentId", required = true) {
    return validateString(docId, fieldName, {
        required,
        minLength: 10,
        maxLength: 128,
        pattern: /^[a-zA-Z0-9_-]+$/,
    });
}
exports.validateDocumentId = validateDocumentId;
/**
 * Sanitize text to prevent XSS
 * Removes HTML tags and dangerous characters
 */
function sanitizeText(text) {
    if (!text)
        return "";
    return text
        .replace(/<[^>]*>/g, "") // Remove HTML tags
        .replace(/[<>'"]/g, "") // Remove dangerous characters
        .trim();
}
exports.sanitizeText = sanitizeText;
/**
 * Validate and sanitize message text
 */
function validateMessage(message, maxLength = 1000) {
    const messageStr = validateString(message, "message", {
        required: true,
        minLength: 1,
        maxLength,
    });
    return sanitizeText(messageStr);
}
exports.validateMessage = validateMessage;
/**
 * Validate array input
 */
function validateArray(value, fieldName, options = {}) {
    if (options.required && (!value || !Array.isArray(value))) {
        throw new functions.https.HttpsError("invalid-argument", `${fieldName} is required and must be an array`);
    }
    if (!value) {
        return [];
    }
    if (!Array.isArray(value)) {
        throw new functions.https.HttpsError("invalid-argument", `${fieldName} must be an array`);
    }
    if (options.minLength && value.length < options.minLength) {
        throw new functions.https.HttpsError("invalid-argument", `${fieldName} must have at least ${options.minLength} items`);
    }
    if (options.maxLength && value.length > options.maxLength) {
        throw new functions.https.HttpsError("invalid-argument", `${fieldName} must have at most ${options.maxLength} items`);
    }
    return value;
}
exports.validateArray = validateArray;
/**
 * Validate enum value
 */
function validateEnum(value, fieldName, allowedValues, required = true) {
    if (required && (value === null || value === undefined)) {
        throw new functions.https.HttpsError("invalid-argument", `${fieldName} is required`);
    }
    if (!allowedValues.includes(value)) {
        throw new functions.https.HttpsError("invalid-argument", `${fieldName} must be one of: ${allowedValues.join(", ")}`);
    }
    return value;
}
exports.validateEnum = validateEnum;
/**
 * Require a valid App Check token on a callable request. Rejects the call
 * with failed-precondition if absent. Use on sensitive paths (money,
 * identity lookups, signed URL issuers).
 */
function assertAppCheck(context) {
    if (!context.app) {
        throw new functions.https.HttpsError("failed-precondition", "app check required");
    }
}
exports.assertAppCheck = assertAppCheck;
//# sourceMappingURL=validation.js.map