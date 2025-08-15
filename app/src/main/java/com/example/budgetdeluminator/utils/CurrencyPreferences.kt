package com.example.budgetdeluminator.utils

import android.content.Context
import android.content.SharedPreferences
import com.example.budgetdeluminator.data.model.Currency
import com.example.budgetdeluminator.data.model.CurrencyManager
import java.util.*

class CurrencyPreferences(context: Context) {
    private val preferences: SharedPreferences =
            context.getSharedPreferences("currency_preferences", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_CURRENCY_CODE = "selected_currency_code"
        private const val DEFAULT_CURRENCY_CODE = "INR"
    }

    fun setSelectedCurrency(currency: Currency) {
        preferences.edit().putString(KEY_CURRENCY_CODE, currency.code).apply()
    }

    fun getSelectedCurrency(): Currency {
        val code = preferences.getString(KEY_CURRENCY_CODE, DEFAULT_CURRENCY_CODE)
        return CurrencyManager.getCurrencyByCode(code ?: DEFAULT_CURRENCY_CODE)
                ?: CurrencyManager.getDefaultCurrency()
    }

    fun formatAmount(amount: Double): String {
        val currency = getSelectedCurrency()
        return "${currency.symbol} ${String.format("%.2f", amount)}"
    }

    fun formatAmountWithoutDecimals(amount: Double): String {
        val currency = getSelectedCurrency()
        return if (amount == amount.toLong().toDouble()) {
            "${currency.symbol} ${amount.toLong()}"
        } else {
            "${currency.symbol} ${String.format("%.2f", amount)}"
        }
    }

    fun getCurrencySymbol(): String {
        return getSelectedCurrency().symbol
    }

    fun parseAmount(formattedAmount: String): Double {
        val currency = getSelectedCurrency()
        // Remove currency symbol and spaces, then parse
        val cleanAmount = formattedAmount.replace(currency.symbol, "").trim()
        return cleanAmount.toDoubleOrNull() ?: 0.0
    }
}
