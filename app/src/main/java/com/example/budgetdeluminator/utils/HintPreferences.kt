package com.example.budgetdeluminator.utils

import android.content.Context
import android.content.SharedPreferences

/** Utility class for managing hint preferences Tracks which hints have been shown to the user */
class HintPreferences(context: Context) {

    companion object {
        private const val PREFS_NAME = "hint_preferences"
        private const val KEY_SETTINGS_HINT_SHOWN = "settings_hint_shown"
    }

    private val preferences: SharedPreferences =
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Check if the settings long-press hint has been shown
     * @return true if hint has been shown, false otherwise
     */
    fun isSettingsHintShown(): Boolean {
        return preferences.getBoolean(KEY_SETTINGS_HINT_SHOWN, false)
    }

    /** Mark the settings long-press hint as shown */
    fun markSettingsHintShown() {
        preferences.edit().putBoolean(KEY_SETTINGS_HINT_SHOWN, true).apply()
    }

    /** Reset all hints (useful for testing or user preference) */
    fun resetAllHints() {
        preferences.edit().clear().apply()
    }
}
