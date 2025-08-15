package com.example.budgetdeluminator.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate

/**
 * Utility class for managing theme preferences Handles storage and retrieval of user's theme
 * selection
 */
class ThemePreferences(context: Context) {

    companion object {
        private const val PREFS_NAME = "theme_preferences"
        private const val KEY_THEME_MODE = "theme_mode"

        // Theme mode constants
        const val THEME_LIGHT = "light"
        const val THEME_DARK = "dark"
        const val THEME_SYSTEM = "system"
    }

    private val preferences: SharedPreferences =
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Get the currently selected theme mode
     * @return Theme mode string (light, dark, or system)
     */
    fun getThemeMode(): String {
        return preferences.getString(KEY_THEME_MODE, THEME_SYSTEM) ?: THEME_SYSTEM
    }

    /**
     * Set the theme mode preference
     * @param themeMode The theme mode to set (light, dark, or system)
     */
    fun setThemeMode(themeMode: String) {
        preferences.edit().putString(KEY_THEME_MODE, themeMode).apply()
    }

    /**
     * Apply the selected theme to the app This should be called in Application.onCreate() or
     * MainActivity.onCreate()
     */
    fun applyTheme() {
        val mode =
                when (getThemeMode()) {
                    THEME_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
                    THEME_DARK -> AppCompatDelegate.MODE_NIGHT_YES
                    THEME_SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                    else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                }
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    /**
     * Get human-readable theme name for display
     * @param themeMode The theme mode string
     * @return Human-readable theme name
     */
    fun getThemeDisplayName(themeMode: String): String {
        return when (themeMode) {
            THEME_LIGHT -> "Light"
            THEME_DARK -> "Dark"
            THEME_SYSTEM -> "System Default"
            else -> "System Default"
        }
    }

    /**
     * Get all available theme options
     * @return List of theme mode strings
     */
    fun getAvailableThemes(): List<String> {
        return listOf(THEME_LIGHT, THEME_DARK, THEME_SYSTEM)
    }
}
