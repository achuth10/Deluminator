package com.example.budgetdeluminator.ui.adapter

import android.content.Context
import android.widget.ArrayAdapter

class ThemeDropdownAdapter(
        context: Context,
        private val themeNames: List<String>,
        private val themeValues: List<String>
) : ArrayAdapter<String>(context, android.R.layout.simple_dropdown_item_1line, themeNames) {

    fun getThemeValueAt(position: Int): String? {
        return if (position >= 0 && position < themeValues.size) {
            themeValues[position]
        } else null
    }

    fun getPositionOfThemeValue(themeValue: String): Int {
        return themeValues.indexOf(themeValue)
    }
}
