package com.example.budgetdeluminator.utils

import android.content.Context

object SpendingRoastPreferences {

    private const val SPENDING_ROAST_PREFS = "spending_roast_prefs"
    private const val KEY_SPENDING_ROAST_ENABLED = "spending_roast_enabled"

    fun isSpendingRoastEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(SPENDING_ROAST_PREFS, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_SPENDING_ROAST_ENABLED, true)
    }

    fun setSpendingRoastEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(SPENDING_ROAST_PREFS, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_SPENDING_ROAST_ENABLED, enabled).apply()
    }
}
