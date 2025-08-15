package com.example.budgetdeluminator.utils

import java.util.*

/** Utility class for handling date calculations, especially for monthly budget periods */
object DateUtils {

    /** Gets the start of the current month (00:00:00 on the 1st day) */
    fun getCurrentMonthStart(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    /** Gets the end of the current month (23:59:59 on the last day) */
    fun getCurrentMonthEnd(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return calendar.timeInMillis
    }

    /** Gets the start of a specific month */
    fun getMonthStart(year: Int, month: Int): Long {
        val calendar = Calendar.getInstance()
        calendar.set(year, month, 1, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    /** Gets the end of a specific month */
    fun getMonthEnd(year: Int, month: Int): Long {
        val calendar = Calendar.getInstance()
        calendar.set(year, month, 1)
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return calendar.timeInMillis
    }

    /** Gets the month start and end for a given timestamp */
    fun getMonthBounds(timestamp: Long): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        return Pair(getMonthStart(year, month), getMonthEnd(year, month))
    }

    /** Gets the number of days in the current month */
    fun getCurrentMonthDays(): Int {
        val calendar = Calendar.getInstance()
        return calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    /** Gets the number of days elapsed in the current month (including today) */
    fun getCurrentMonthDaysElapsed(): Int {
        val calendar = Calendar.getInstance()
        return calendar.get(Calendar.DAY_OF_MONTH)
    }

    /**
     * Calculates the daily budget for the current month
     * @param monthlyBudget The total monthly budget
     * @return Daily budget amount
     */
    fun getDailyBudget(monthlyBudget: Double): Double {
        val daysInMonth = getCurrentMonthDays()
        return if (daysInMonth > 0) monthlyBudget / daysInMonth else 0.0
    }

    /**
     * Calculates expected spending up to the current day of the month
     * @param monthlyBudget The total monthly budget
     * @return Expected spending amount for days elapsed
     */
    fun getExpectedSpendingToDate(monthlyBudget: Double): Double {
        val daysElapsed = getCurrentMonthDaysElapsed()
        val dailyBudget = getDailyBudget(monthlyBudget)
        return dailyBudget * daysElapsed
    }

    /** Checks if a timestamp is in the current month */
    fun isInCurrentMonth(timestamp: Long): Boolean {
        val currentMonthStart = getCurrentMonthStart()
        val currentMonthEnd = getCurrentMonthEnd()
        return timestamp >= currentMonthStart && timestamp <= currentMonthEnd
    }

    /** Gets a human-readable month name for the current month */
    fun getCurrentMonthName(): String {
        val calendar = Calendar.getInstance()
        val monthNames =
                arrayOf(
                        "January",
                        "February",
                        "March",
                        "April",
                        "May",
                        "June",
                        "July",
                        "August",
                        "September",
                        "October",
                        "November",
                        "December"
                )
        return monthNames[calendar.get(Calendar.MONTH)]
    }

    /** Gets a human-readable month name for a specific month */
    fun getMonthName(month: Int): String {
        val monthNames =
                arrayOf(
                        "January",
                        "February",
                        "March",
                        "April",
                        "May",
                        "June",
                        "July",
                        "August",
                        "September",
                        "October",
                        "November",
                        "December"
                )
        return if (month in 0..11) monthNames[month] else "Unknown"
    }

    /** Gets month and year string for a given timestamp */
    fun getMonthYearString(timestamp: Long): String {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        val month = calendar.get(Calendar.MONTH)
        val year = calendar.get(Calendar.YEAR)
        return "${getMonthName(month)} $year"
    }

    /** Gets the current month and year as a pair (month, year) */
    fun getCurrentMonthYear(): Pair<Int, Int> {
        val calendar = Calendar.getInstance()
        return Pair(calendar.get(Calendar.MONTH), calendar.get(Calendar.YEAR))
    }

    /** Checks if two timestamps are in the same month */
    fun isSameMonth(timestamp1: Long, timestamp2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = timestamp1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = timestamp2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH)
    }
}
