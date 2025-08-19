package com.example.budgetdeluminator.ui.adapter

import android.content.Context
import android.widget.ArrayAdapter

class DateRangeDropdownAdapter(
        context: Context,
        private val rangeNames: List<String>,
        private val rangeValues: List<String>
) : ArrayAdapter<String>(context, android.R.layout.simple_dropdown_item_1line, rangeNames) {

    fun getRangeValueAt(position: Int): String? {
        return if (position >= 0 && position < rangeValues.size) {
            rangeValues[position]
        } else null
    }

    fun getPositionOfRangeValue(rangeValue: String): Int {
        return rangeValues.indexOf(rangeValue)
    }

    fun getRangeNameAt(position: Int): String? {
        return if (position >= 0 && position < rangeNames.size) {
            rangeNames[position]
        } else null
    }
}
