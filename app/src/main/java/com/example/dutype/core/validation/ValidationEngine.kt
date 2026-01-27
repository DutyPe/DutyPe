package com.example.dutype.core.validation

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Enterprise Validation Engine
 * 
 * Centralized validation system with declarative rules.
 * Supports client-side and server-side validation.
 * 
 * Features:
 * - Declarative validation rules
 * - Field-level validation
 * - Cross-field validation
 * - Async validation (server-side)
 * - Custom validators
 * - Error aggregation
 * 
 * @author DutyPe Engineering Team
 * @since 3.0.0
 */
@Singleton
class ValidationEngine @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    
    /**
     * Validate data against rules
     */
    fun validate(data: Map<String, Any?>, rules: ValidationRules): ValidationResult {
        val errors = mutableMapOf<String, List<String>>()
        
        rules.fieldRules.forEach { (field, fieldRules) ->
            val value = data[field]
            val fieldErrors = mutableListOf<String>()
            
            fieldRules.forEach { rule ->
                val result = rule.validate(value)
                if (!result.isValid) {
                    fieldErrors.add(result.errorMessage ?: "Validation failed")
                }
            }
            
            if (fieldErrors.isNotEmpty()) {
                errors[field] = fieldErrors
            }
        }
        
        // Cross-field validation
        rules.crossFieldRules.forEach { rule ->
            val result = rule.validate(data)
            if (!result.isValid) {
                errors["_cross_field"] = (errors["_cross_field"] ?: emptyList()) + 
                    (result.errorMessage ?: "Cross-field validation failed")
            }
        }
        
        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors
        )
    }
    
    /**
     * Validate with async rules (server-side validation)
     */
    suspend fun validateAsync(
        data: Map<String, Any?>,
        rules: ValidationRules
    ): ValidationResult {
        // First run sync validation
        val syncResult = validate(data, rules)
        if (!syncResult.isValid) {
            return syncResult
        }
        
        // Then run async validation
        val errors = mutableMapOf<String, List<String>>()
        
        rules.asyncRules.forEach { (field, asyncRule) ->
            val value = data[field]
            val result = asyncRule.validate(value)
            if (!result.isValid) {
                errors[field] = listOf(result.errorMessage ?: "Async validation failed")
            }
        }
        
        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors
        )
    }
    
    /**
     * Server-side validation (Firestore rules check)
     */
    suspend fun validateOnServer(
        collection: String,
        data: Map<String, Any?>
    ): ServerValidationResult {
        return try {
            // Attempt to write to Firestore (dry run)
            // This will trigger Firestore security rules
            val docRef = firestore.collection(collection).document("_validation_test")
            docRef.set(data).await()
            docRef.delete().await()
            
            ServerValidationResult(
                isValid = true,
                errors = emptyList()
            )
        } catch (e: Exception) {
            Timber.w("Server validation failed: ${e.message}")
            ServerValidationResult(
                isValid = false,
                errors = listOf(e.message ?: "Server validation failed")
            )
        }
    }
}

/**
 * Validation rules container
 */
data class ValidationRules(
    val fieldRules: Map<String, List<ValidationRule>> = emptyMap(),
    val crossFieldRules: List<CrossFieldRule> = emptyList(),
    val asyncRules: Map<String, AsyncValidationRule> = emptyMap()
)

/**
 * Validation rule interface
 */
interface ValidationRule {
    fun validate(value: Any?): RuleResult
}

/**
 * Cross-field validation rule
 */
interface CrossFieldRule {
    fun validate(data: Map<String, Any?>): RuleResult
}

/**
 * Async validation rule
 */
interface AsyncValidationRule {
    suspend fun validate(value: Any?): RuleResult
}

/**
 * Rule result
 */
data class RuleResult(
    val isValid: Boolean,
    val errorMessage: String? = null
)

/**
 * Validation result
 */
data class ValidationResult(
    val isValid: Boolean,
    val errors: Map<String, List<String>> = emptyMap()
) {
    fun getErrorsForField(field: String): List<String> = errors[field] ?: emptyList()
    fun getAllErrors(): List<String> = errors.values.flatten()
}

/**
 * Server validation result
 */
data class ServerValidationResult(
    val isValid: Boolean,
    val errors: List<String> = emptyList()
)

// ==================== BUILT-IN VALIDATORS ====================

/**
 * Required field validator
 */
class RequiredRule(private val message: String = "This field is required") : ValidationRule {
    override fun validate(value: Any?): RuleResult {
        val isValid = when (value) {
            null -> false
            is String -> value.isNotBlank()
            is Collection<*> -> value.isNotEmpty()
            else -> true
        }
        return RuleResult(isValid, if (isValid) null else message)
    }
}

/**
 * String length validator
 */
class LengthRule(
    private val min: Int? = null,
    private val max: Int? = null,
    private val message: String? = null
) : ValidationRule {
    override fun validate(value: Any?): RuleResult {
        if (value !is String) {
            return RuleResult(true)
        }
        
        val length = value.length
        val isValid = (min == null || length >= min) && (max == null || length <= max)
        
        val errorMsg = message ?: when {
            min != null && max != null -> "Length must be between $min and $max characters"
            min != null -> "Length must be at least $min characters"
            max != null -> "Length must be at most $max characters"
            else -> "Invalid length"
        }
        
        return RuleResult(isValid, if (isValid) null else errorMsg)
    }
}

/**
 * Email validator
 */
class EmailRule(private val message: String = "Invalid email address") : ValidationRule {
    private val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
    
    override fun validate(value: Any?): RuleResult {
        if (value !is String) {
            return RuleResult(false, message)
        }
        
        val isValid = emailRegex.matches(value)
        return RuleResult(isValid, if (isValid) null else message)
    }
}

/**
 * Phone number validator
 */
class PhoneRule(private val message: String = "Invalid phone number") : ValidationRule {
    private val phoneRegex = "^[6-9]\\d{9}$".toRegex() // Indian phone number
    
    override fun validate(value: Any?): RuleResult {
        if (value !is String) {
            return RuleResult(false, message)
        }
        
        val isValid = phoneRegex.matches(value)
        return RuleResult(isValid, if (isValid) null else message)
    }
}

/**
 * Numeric range validator
 */
class RangeRule(
    private val min: Number? = null,
    private val max: Number? = null,
    private val message: String? = null
) : ValidationRule {
    override fun validate(value: Any?): RuleResult {
        val number = when (value) {
            is Number -> value.toDouble()
            is String -> value.toDoubleOrNull()
            else -> null
        }
        
        if (number == null) {
            return RuleResult(false, "Invalid number")
        }
        
        val minVal = min?.toDouble()
        val maxVal = max?.toDouble()
        val isValid = (minVal == null || number >= minVal) && (maxVal == null || number <= maxVal)
        
        val errorMsg = message ?: when {
            minVal != null && maxVal != null -> "Value must be between $minVal and $maxVal"
            minVal != null -> "Value must be at least $minVal"
            maxVal != null -> "Value must be at most $maxVal"
            else -> "Invalid range"
        }
        
        return RuleResult(isValid, if (isValid) null else errorMsg)
    }
}

/**
 * Pattern validator (regex)
 */
class PatternRule(
    private val pattern: String,
    private val message: String = "Invalid format"
) : ValidationRule {
    private val regex = pattern.toRegex()
    
    override fun validate(value: Any?): RuleResult {
        if (value !is String) {
            return RuleResult(false, message)
        }
        
        val isValid = regex.matches(value)
        return RuleResult(isValid, if (isValid) null else message)
    }
}

/**
 * Custom validator
 */
class CustomRule(
    private val validator: (Any?) -> Boolean,
    private val message: String = "Validation failed"
) : ValidationRule {
    override fun validate(value: Any?): RuleResult {
        val isValid = validator(value)
        return RuleResult(isValid, if (isValid) null else message)
    }
}

/**
 * Match field validator (for password confirmation)
 */
class MatchFieldRule(
    private val otherField: String,
    private val message: String = "Fields do not match"
) : CrossFieldRule {
    override fun validate(data: Map<String, Any?>): RuleResult {
        val value1 = data[otherField]
        val value2 = data.values.firstOrNull()
        val isValid = value1 == value2
        return RuleResult(isValid, if (isValid) null else message)
    }
}

// ==================== VALIDATION RULE BUILDERS ====================

/**
 * Builder for validation rules
 */
class ValidationRulesBuilder {
    private val fieldRules = mutableMapOf<String, MutableList<ValidationRule>>()
    private val crossFieldRules = mutableListOf<CrossFieldRule>()
    private val asyncRules = mutableMapOf<String, AsyncValidationRule>()
    
    fun field(name: String, block: FieldRulesBuilder.() -> Unit) {
        val builder = FieldRulesBuilder()
        builder.block()
        fieldRules[name] = builder.rules
    }
    
    fun crossField(rule: CrossFieldRule) {
        crossFieldRules.add(rule)
    }
    
    fun asyncField(name: String, rule: AsyncValidationRule) {
        asyncRules[name] = rule
    }
    
    fun build(): ValidationRules {
        return ValidationRules(
            fieldRules = fieldRules,
            crossFieldRules = crossFieldRules,
            asyncRules = asyncRules
        )
    }
}

/**
 * Builder for field rules
 */
class FieldRulesBuilder {
    val rules = mutableListOf<ValidationRule>()
    
    fun required(message: String = "This field is required") {
        rules.add(RequiredRule(message))
    }
    
    fun length(min: Int? = null, max: Int? = null, message: String? = null) {
        rules.add(LengthRule(min, max, message))
    }
    
    fun email(message: String = "Invalid email address") {
        rules.add(EmailRule(message))
    }
    
    fun phone(message: String = "Invalid phone number") {
        rules.add(PhoneRule(message))
    }
    
    fun range(min: Number? = null, max: Number? = null, message: String? = null) {
        rules.add(RangeRule(min, max, message))
    }
    
    fun pattern(pattern: String, message: String = "Invalid format") {
        rules.add(PatternRule(pattern, message))
    }
    
    fun custom(message: String = "Validation failed", validator: (Any?) -> Boolean) {
        rules.add(CustomRule(validator, message))
    }
}

/**
 * DSL for building validation rules
 */
fun validationRules(block: ValidationRulesBuilder.() -> Unit): ValidationRules {
    val builder = ValidationRulesBuilder()
    builder.block()
    return builder.build()
}
