package com.example.budgetdeluminator.utils

import android.content.Context
import android.util.Log
import android.view.View
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar
import java.io.IOException
import java.sql.SQLException

/** Centralized error handling for production-ready error management */
object ErrorHandler {

    private const val TAG = "BudgetApp_ErrorHandler"

    /** Handle and display errors appropriately */
    fun handleError(
            context: Context,
            error: Throwable,
            userMessage: String? = null,
            view: View? = null,
            showSnackbar: Boolean = false
    ) {
        // Log the error for debugging
        logError(error)

        // Determine appropriate user message
        val message = userMessage ?: getUserFriendlyMessage(error)

        // Display error to user
        if (showSnackbar && view != null) {
            showErrorSnackbar(view, message)
        } else {
            showErrorToast(context, message)
        }
    }

    /** Handle database errors specifically */
    fun handleDatabaseError(
            context: Context,
            error: Throwable,
            operation: String,
            view: View? = null
    ) {
        logError(error, "Database operation failed: $operation")

        val message =
                when (error) {
                    is SQLException -> "Database error occurred. Please try again."
                    is IOException -> "Storage error. Please check available space and try again."
                    else -> "An error occurred while $operation. Please try again."
                }

        if (view != null) {
            showErrorSnackbar(view, message, "Retry") {
                // Retry callback - implement retry logic in calling code
            }
        } else {
            showErrorToast(context, message)
        }
    }

    /** Handle network errors (for future use if needed) */
    fun handleNetworkError(context: Context, error: Throwable, view: View? = null) {
        logError(error, "Network operation failed")

        val message = "Network error. Please check your connection and try again."

        if (view != null) {
            showErrorSnackbar(view, message, "Retry") {
                // Retry callback
            }
        } else {
            showErrorToast(context, message)
        }
    }

    /** Handle validation errors */
    fun handleValidationError(
            context: Context,
            validationResult: ValidationResult,
            view: View? = null
    ) {
        if (validationResult is ValidationResult.Error) {
            if (view != null) {
                showErrorSnackbar(view, validationResult.message)
            } else {
                showErrorToast(context, validationResult.message)
            }
        }
    }

    /** Show error toast */
    private fun showErrorToast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    /** Show error snackbar with optional action */
    private fun showErrorSnackbar(
            view: View,
            message: String,
            actionText: String? = null,
            action: (() -> Unit)? = null
    ) {
        val snackbar = Snackbar.make(view, message, Snackbar.LENGTH_LONG)

        if (actionText != null && action != null) {
            snackbar.setAction(actionText) { action() }
        }

        snackbar.show()
    }

    /** Log error for debugging */
    private fun logError(error: Throwable, context: String = "") {
        val logMessage = if (context.isNotEmpty()) "$context: ${error.message}" else error.message
        Log.e(TAG, logMessage, error)
    }

    /** Convert technical errors to user-friendly messages */
    private fun getUserFriendlyMessage(error: Throwable): String {
        return when (error) {
            is OutOfMemoryError ->
                    "The app is running low on memory. Please close other apps and try again."
            is SecurityException -> "Permission denied. Please check app permissions."
            is IllegalArgumentException ->
                    "Invalid input provided. Please check your data and try again."
            is IllegalStateException ->
                    "The app is in an unexpected state. Please restart and try again."
            is SQLException -> "Database error occurred. Please try again."
            is IOException -> "File operation failed. Please check storage space and try again."
            is NumberFormatException -> "Invalid number format. Please enter a valid number."
            is NullPointerException -> "An unexpected error occurred. Please try again."
            else -> "An unexpected error occurred. Please try again."
        }
    }

    /** Safe execution wrapper that handles errors automatically */
    inline fun <T> safeExecute(
            context: Context,
            view: View? = null,
            errorMessage: String? = null,
            action: () -> T
    ): T? {
        return try {
            action()
        } catch (e: Exception) {
            handleError(context, e, errorMessage, view)
            null
        }
    }

    /** Safe execution for database operations */
    inline fun <T> safeDatabaseExecute(
            context: Context,
            operation: String,
            view: View? = null,
            action: () -> T
    ): T? {
        return try {
            action()
        } catch (e: Exception) {
            handleDatabaseError(context, e, operation, view)
            null
        }
    }

    /** Check if error is recoverable */
    fun isRecoverableError(error: Throwable): Boolean {
        return when (error) {
            is IOException -> true
            is SQLException -> true
            is IllegalArgumentException -> true
            is NumberFormatException -> true
            is OutOfMemoryError -> false
            is SecurityException -> false
            else -> true
        }
    }
}

/** Extension function for easier error handling */
fun <T> T?.orHandleError(
        context: Context,
        error: Throwable,
        view: View? = null,
        defaultValue: T
): T {
    return if (this != null) {
        this
    } else {
        ErrorHandler.handleError(context, error, view = view)
        defaultValue
    }
}
