package com.example.budgetdeluminator.utils

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat

/**
 * Professional calculator implementation with proper edge case handling Handles division by zero,
 * overflow, underflow, and precision issues
 */
class Calculator {

    companion object {
        private const val MAX_DIGITS = 15
        private const val DECIMAL_PLACES = 10
        private val ZERO = BigDecimal.ZERO
        private val MAX_VALUE = BigDecimal("999999999999999")
        private val MIN_VALUE = BigDecimal("-999999999999999")
    }

    enum class Operation {
        ADD,
        SUBTRACT,
        MULTIPLY,
        DIVIDE,
        NONE
    }

    data class CalculatorState(
            val display: String = "0",
            val expression: String = "",
            val storedValue: BigDecimal = ZERO,
            val currentOperation: Operation = Operation.NONE,
            val waitingForOperand: Boolean = true,
            val hasError: Boolean = false,
            val errorMessage: String = "",
            val justCalculated: Boolean = false
    )

    private var state = CalculatorState()

    /** Get current calculator state */
    fun getState(): CalculatorState = state.copy()

    /** Reset calculator to initial state */
    fun clear() {
        state = CalculatorState()
    }

    /** Add a digit to the current number */
    fun addDigit(digit: String): CalculatorState {
        if (state.hasError) {
            clear()
        }

        // Handle special case for "00"
        if (digit == "00" &&
                        (state.display == "0" || state.waitingForOperand || state.justCalculated)
        ) {
            return addDigit("0")
        }

        val newDisplay =
                when {
                    state.waitingForOperand || state.display == "0" || state.justCalculated -> {
                        digit
                    }
                    state.display.replace(".", "").replace("-", "").length >= MAX_DIGITS -> {
                        state.display // Don't add more digits if we're at the limit
                    }
                    else -> {
                        state.display + digit
                    }
                }

        val newExpression = if (state.justCalculated) "" else state.expression

        state =
                state.copy(
                        display = newDisplay,
                        expression = newExpression,
                        waitingForOperand = false,
                        justCalculated = false
                )

        return state
    }

    /** Add decimal point */
    fun addDecimal(): CalculatorState {
        if (state.hasError) {
            clear()
        }

        val newDisplay =
                when {
                    state.waitingForOperand || state.justCalculated -> "0."
                    state.display.contains(".") -> state.display // Already has decimal
                    else -> state.display + "."
                }

        val newExpression = if (state.justCalculated) "" else state.expression

        state =
                state.copy(
                        display = newDisplay,
                        expression = newExpression,
                        waitingForOperand = false,
                        justCalculated = false
                )

        return state
    }

    /** Set operation (+, -, ×, ÷) */
    fun setOperation(operation: Operation): CalculatorState {
        if (state.hasError) return state

        val currentValue = parseDisplay()
        if (currentValue == null) {
            return setError("Invalid number")
        }

        // If we have a pending operation and aren't waiting for operand, calculate first
        if (state.currentOperation != Operation.NONE && !state.waitingForOperand) {
            val result = performCalculation()
            if (result.hasError) return result
            // After calculation, the storedValue is already set to the result
            // Don't overwrite it with currentValue
        }

        val operatorSymbol =
                when (operation) {
                    Operation.ADD -> " + "
                    Operation.SUBTRACT -> " - "
                    Operation.MULTIPLY -> " × "
                    Operation.DIVIDE -> " ÷ "
                    Operation.NONE -> ""
                }

        val newExpression =
                if (state.expression.isEmpty()) {
                    state.display + operatorSymbol
                } else {
                    state.expression + operatorSymbol
                }

        // Use the stored value from calculation if we just performed one,
        // otherwise use the current value
        val valueToStore = if (state.justCalculated) state.storedValue else currentValue

        state =
                state.copy(
                        storedValue = valueToStore,
                        currentOperation = operation,
                        expression = newExpression,
                        waitingForOperand = true,
                        justCalculated = false
                )

        return state
    }

    /** Perform calculation */
    fun calculate(): CalculatorState {
        if (state.hasError) return state

        if (state.currentOperation == Operation.NONE || state.waitingForOperand) {
            // No operation to perform or missing operand
            state = state.copy(justCalculated = true)
            return state
        }

        return performCalculation()
    }

    /** Backspace - remove last character */
    fun backspace(): CalculatorState {
        if (state.hasError) {
            clear()
            return state
        }

        if (state.justCalculated || state.waitingForOperand) {
            return state // Don't allow backspace after calculation or while waiting for operand
        }

        val newDisplay =
                when {
                    state.display.length <= 1 || state.display == "-0" -> "0"
                    state.display.endsWith(".") -> state.display.dropLast(1)
                    else -> state.display.dropLast(1)
                }

        state =
                state.copy(
                        display =
                                if (newDisplay.isEmpty() || newDisplay == "-") "0" else newDisplay,
                        waitingForOperand = newDisplay == "0"
                )

        return state
    }

    /** Toggle sign of current number */
    fun toggleSign(): CalculatorState {
        if (state.hasError) return state

        if (state.display == "0") return state

        val newDisplay =
                if (state.display.startsWith("-")) {
                    state.display.substring(1)
                } else {
                    "-${state.display}"
                }

        state = state.copy(display = newDisplay)
        return state
    }

    /** Get current value as Double for external use */
    fun getCurrentValue(): Double {
        // If there's a pending operation and we're not waiting for operand,
        // we need to perform the calculation first to get the final result
        if (state.currentOperation != Operation.NONE && !state.waitingForOperand) {
            val currentValue = parseDisplay()
            if (currentValue != null) {
                try {
                    val result =
                            when (state.currentOperation) {
                                Operation.ADD -> state.storedValue.add(currentValue)
                                Operation.SUBTRACT -> state.storedValue.subtract(currentValue)
                                Operation.MULTIPLY -> state.storedValue.multiply(currentValue)
                                Operation.DIVIDE -> {
                                    if (currentValue == ZERO) {
                                        return 0.0 // Return 0 for division by zero in
                                        // getCurrentValue
                                    }
                                    state.storedValue.divide(
                                            currentValue,
                                            DECIMAL_PLACES,
                                            RoundingMode.HALF_UP
                                    )
                                }
                                Operation.NONE -> currentValue
                            }
                    return result.toDouble()
                } catch (e: Exception) {
                    return 0.0
                }
            }
        }

        // If just calculated, return the stored value (which is the result)
        if (state.justCalculated) {
            return state.storedValue.toDouble()
        }

        // Otherwise return the current display value
        return parseDisplay()?.toDouble() ?: 0.0
    }

    private fun performCalculation(): CalculatorState {
        val currentValue = parseDisplay()
        if (currentValue == null) {
            return setError("Invalid number")
        }

        val result =
                try {
                    when (state.currentOperation) {
                        Operation.ADD -> state.storedValue.add(currentValue)
                        Operation.SUBTRACT -> state.storedValue.subtract(currentValue)
                        Operation.MULTIPLY -> state.storedValue.multiply(currentValue)
                        Operation.DIVIDE -> {
                            if (currentValue == ZERO) {
                                return setError("Cannot divide by zero")
                            }
                            state.storedValue.divide(
                                    currentValue,
                                    DECIMAL_PLACES,
                                    RoundingMode.HALF_UP
                            )
                        }
                        Operation.NONE -> currentValue
                    }
                } catch (e: ArithmeticException) {
                    return setError("Math error")
                } catch (e: Exception) {
                    return setError("Calculation error")
                }

        // Check for overflow/underflow
        if (result > MAX_VALUE) {
            return setError("Number too large")
        }
        if (result < MIN_VALUE) {
            return setError("Number too small")
        }

        val formattedResult = formatResult(result)
        val newExpression = state.expression + state.display

        state =
                state.copy(
                        display = formattedResult,
                        expression = newExpression,
                        storedValue = result,
                        currentOperation = Operation.NONE,
                        waitingForOperand = true,
                        justCalculated = true
                )

        return state
    }

    private fun parseDisplay(): BigDecimal? {
        return try {
            BigDecimal(state.display)
        } catch (e: NumberFormatException) {
            null
        }
    }

    private fun formatResult(result: BigDecimal): String {
        // Remove trailing zeros and unnecessary decimal point
        val stripped = result.stripTrailingZeros()

        // If the result is a whole number, display as integer
        return if (stripped.scale() <= 0) {
            stripped.toLong().toString()
        } else {
            // Format with appropriate decimal places
            val formatter =
                    DecimalFormat().apply {
                        maximumFractionDigits = DECIMAL_PLACES
                        isGroupingUsed = false
                    }
            formatter.format(stripped)
        }
    }

    private fun setError(message: String): CalculatorState {
        state =
                state.copy(
                        hasError = true,
                        errorMessage = message,
                        display = "Error",
                        expression = message
                )
        return state
    }
}
