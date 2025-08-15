package com.example.budgetdeluminator.utils

import android.content.Context
import com.google.android.material.textfield.TextInputLayout
import java.text.NumberFormat
import java.util.*

/** Comprehensive validation utilities for production-ready input validation */
object ValidationUtils {

    /** Validates expense description */
    fun validateExpenseDescription(description: String): ValidationResult {
        return when {
            description.length > 200 ->
                    ValidationResult.Error("Description must be less than 200 characters")
            else -> ValidationResult.Success
        }
    }

    /** Validates expense amount */
    fun validateExpenseAmount(amount: Double): ValidationResult {
        return when {
            amount <= 0 -> ValidationResult.Error("Amount must be greater than zero")
            amount > 999999.99 -> ValidationResult.Error("Amount cannot exceed $999,999.99")
            amount.toString().split(".").getOrNull(1)?.length ?: 0 > 2 ->
                    ValidationResult.Error("Amount cannot have more than 2 decimal places")
            else -> ValidationResult.Success
        }
    }

    /** Validates category name */
    fun validateCategoryName(name: String): ValidationResult {
        return when {
            name.isBlank() -> ValidationResult.Error("Category name cannot be empty")
            name.length > 50 ->
                    ValidationResult.Error("Category name must be less than 50 characters")
            name.trim().length < 2 ->
                    ValidationResult.Error("Category name must be at least 2 characters")
            !name.matches(Regex("^[a-zA-Z0-9\\s&-]+$")) ->
                    ValidationResult.Error("Category name contains invalid characters")
            else -> ValidationResult.Success
        }
    }

    /** Validates budget limit */
    fun validateBudgetLimit(limit: Double): ValidationResult {
        return when {
            limit < 0 -> ValidationResult.Error("Budget limit cannot be negative")
            limit > 999999.99 -> ValidationResult.Error("Budget limit cannot exceed $999,999.99")
            limit.toString().split(".").getOrNull(1)?.length ?: 0 > 2 ->
                    ValidationResult.Error("Budget limit cannot have more than 2 decimal places")
            else -> ValidationResult.Success
        }
    }

    /** Validates expense date (allows future dates) */
    fun validateExpenseDate(timestamp: Long): ValidationResult {
        return when {
            timestamp < 0 -> ValidationResult.Error("Invalid date")
            else -> ValidationResult.Success
        }
    }

    /** Validates and formats currency amount */
    fun validateAndFormatCurrency(
            input: String,
            locale: Locale = Locale.getDefault()
    ): ValidationResult {
        return try {
            val cleanInput = input.replace(Regex("[^0-9.]"), "")
            val amount = cleanInput.toDoubleOrNull()

            when {
                amount == null -> ValidationResult.Error("Invalid amount format")
                amount < 0 -> ValidationResult.Error("Amount cannot be negative")
                amount > 999999.99 -> ValidationResult.Error("Amount too large")
                else -> {
                    val formatter = NumberFormat.getCurrencyInstance(locale)
                    ValidationResult.SuccessWithData(formatter.format(amount))
                }
            }
        } catch (e: Exception) {
            ValidationResult.Error("Invalid amount format")
        }
    }

    /** Shows validation error on TextInputLayout */
    fun showValidationError(textInputLayout: TextInputLayout, result: ValidationResult) {
        when (result) {
            is ValidationResult.Error -> {
                textInputLayout.error = result.message
                textInputLayout.isErrorEnabled = true
            }
            is ValidationResult.Success, is ValidationResult.SuccessWithData -> {
                textInputLayout.error = null
                textInputLayout.isErrorEnabled = false
            }
        }
    }

    /** Validates all required fields are filled */
    fun validateRequiredFields(vararg fields: Pair<String, String>): ValidationResult {
        fields.forEach { (fieldName, value) ->
            if (value.isBlank()) {
                return ValidationResult.Error("$fieldName is required")
            }
        }
        return ValidationResult.Success
    }

    /** Sanitizes input string for database storage */
    fun sanitizeInput(input: String): String {
        return input.trim()
                .replace(Regex("\\s+"), " ") // Replace multiple spaces with single space
                .take(200) // Limit length
    }

    /** Validates network connectivity requirement */
    fun validateNetworkConnectivity(context: Context): ValidationResult {
        // This is a placeholder - in a real app you'd check actual network connectivity
        // For this budget app, we don't need network connectivity as it's offline-first
        return ValidationResult.Success
    }
}

/** Sealed class representing validation results */
sealed class ValidationResult {
    object Success : ValidationResult()
    data class Error(val message: String) : ValidationResult()
    data class SuccessWithData(val data: String) : ValidationResult()

    fun isSuccess(): Boolean = this is Success || this is SuccessWithData
    fun isError(): Boolean = this is Error

    fun getErrorMessage(): String? = if (this is Error) message else null
    fun getSuccessData(): String? = if (this is SuccessWithData) data else null
}
