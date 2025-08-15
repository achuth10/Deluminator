package com.example.budgetdeluminator.utils

/**
 * Application-wide constants for the Budget Deluminator app.
 *
 * This file contains all the constant values used throughout the application to ensure consistency
 * and maintainability.
 */
object Constants {

    // Validation Constants
    const val MAX_EXPENSE_AMOUNT = 999999.99
    const val MIN_EXPENSE_AMOUNT = 0.01
    const val MAX_BUDGET_LIMIT = 999999.99
    const val MAX_DESCRIPTION_LENGTH = 200
    const val MIN_DESCRIPTION_LENGTH = 2
    const val MAX_CATEGORY_NAME_LENGTH = 50
    const val MIN_CATEGORY_NAME_LENGTH = 2

    // Calculator Constants
    const val CALCULATOR_MAX_DIGITS = 15
    const val CALCULATOR_DECIMAL_PLACES = 10
    const val CALCULATOR_MAX_VALUE = "999999999999999"
    const val CALCULATOR_MIN_VALUE = "-999999999999999"

    // Date Constants
    const val ONE_DAY_IN_MILLIS = 24 * 60 * 60 * 1000L
    const val DATE_FORMAT_DISPLAY = "MMM dd, yyyy"

    // UI Constants
    const val ANIMATION_DURATION_SHORT = 200L
    const val ANIMATION_DURATION_MEDIUM = 300L
    const val ANIMATION_DURATION_LONG = 500L

    // Database Constants
    const val DATABASE_NAME = "budget_database"
    const val DATABASE_VERSION = 1

    // Preferences Constants
    const val PREFS_NAME = "budget_deluminator_prefs"
    const val PREF_CURRENCY_CODE = "currency_code"
    const val PREF_FIRST_LAUNCH = "first_launch"
    const val PREF_THEME_MODE = "theme_mode"

    // Default Values
    const val DEFAULT_CURRENCY_CODE = "USD"
    const val DEFAULT_BUDGET_LIMIT = 500.0

    // Error Messages
    const val ERROR_INVALID_AMOUNT = "Please enter a valid amount"
    const val ERROR_AMOUNT_TOO_LARGE = "Amount cannot exceed $%.2f"
    const val ERROR_AMOUNT_TOO_SMALL = "Amount must be at least $%.2f"
    const val ERROR_DESCRIPTION_TOO_LONG = "Description must be less than %d characters"
    const val ERROR_DESCRIPTION_TOO_SHORT = "Description must be at least %d characters"
    const val ERROR_CATEGORY_REQUIRED = "Please select a category"
    const val ERROR_FUTURE_DATE = "Expense date cannot be more than 1 day in the future"
    const val ERROR_DATABASE_OPERATION = "Database operation failed. Please try again."
    const val ERROR_CALCULATOR_OVERFLOW = "Number too large"
    const val ERROR_CALCULATOR_DIVISION_BY_ZERO = "Cannot divide by zero"

    // Success Messages
    const val SUCCESS_EXPENSE_SAVED = "Expense saved successfully"
    const val SUCCESS_EXPENSE_UPDATED = "Expense updated successfully"
    const val SUCCESS_EXPENSE_DELETED = "Expense deleted successfully"
    const val SUCCESS_CATEGORY_SAVED = "Category saved successfully"
    const val SUCCESS_CATEGORY_UPDATED = "Category updated successfully"
    const val SUCCESS_CATEGORY_DELETED = "Category deleted successfully"

    // Default Categories
    val DEFAULT_CATEGORIES =
            listOf(
                    CategoryDefault("Food & Dining", 500.0, "#4CAF50"),
                    CategoryDefault("Transportation", 300.0, "#2196F3"),
                    CategoryDefault("Shopping", 400.0, "#FF9800"),
                    CategoryDefault("Entertainment", 200.0, "#E91E63"),
                    CategoryDefault("Bills & Utilities", 800.0, "#9C27B0"),
                    CategoryDefault("Healthcare", 300.0, "#FF5722"),
                    CategoryDefault("Personal Care", 150.0, "#00BCD4"),
                    CategoryDefault("Education", 250.0, "#3F51B5")
            )

    // Regular Expressions
    const val REGEX_CATEGORY_NAME = "^[a-zA-Z0-9\\s&-]+$"
    const val REGEX_AMOUNT = "^\\d+(\\.\\d{1,2})?$"

    // Intent Extras
    const val EXTRA_CATEGORY_ID = "category_id"
    const val EXTRA_EXPENSE_ID = "expense_id"
    const val EXTRA_AMOUNT = "amount"
    const val EXTRA_PRESELECTED_CATEGORY = "preselected_category"

    // Bundle Keys
    const val BUNDLE_SELECTED_DATE = "selected_date"
    const val BUNDLE_CALCULATOR_STATE = "calculator_state"
    const val BUNDLE_FORM_DATA = "form_data"
}

/** Data class for default category configuration */
data class CategoryDefault(val name: String, val budgetLimit: Double, val color: String)
