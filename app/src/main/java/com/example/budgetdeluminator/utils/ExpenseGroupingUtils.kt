package com.example.budgetdeluminator.utils

import com.example.budgetdeluminator.data.model.ExpenseListItem
import com.example.budgetdeluminator.data.model.ExpenseWithCategory
import java.text.SimpleDateFormat
import java.util.*

object ExpenseGroupingUtils {

    fun groupExpensesByDate(expenses: List<ExpenseWithCategory>): List<ExpenseListItem> {
        val result = mutableListOf<ExpenseListItem>()
        
        // Group expenses by date (ignoring time)
        val groupedExpenses = expenses
            .sortedByDescending { it.expense.createdAt }
            .groupBy { expense ->
                val calendar = Calendar.getInstance().apply {
                    timeInMillis = expense.expense.createdAt
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                calendar.timeInMillis
            }

        // Convert grouped expenses to list items
        for ((dateInMillis, expensesForDate) in groupedExpenses) {
            val date = Date(dateInMillis)
            val totalAmount = expensesForDate.sumOf { it.expense.amount }
            val formattedDate = formatDateHeader(date)
            
            // Add date header
            result.add(
                ExpenseListItem.DateHeader(
                    date = date,
                    totalAmount = totalAmount,
                    formattedDate = formattedDate
                )
            )
            
            // Add expenses for this date
            expensesForDate.forEach { expenseWithCategory ->
                result.add(
                    ExpenseListItem.ExpenseItem(expenseWithCategory)
                )
            }
        }
        
        return result
    }

    private fun formatDateHeader(date: Date): String {
        val calendar = Calendar.getInstance().apply { time = date }
        val today = Calendar.getInstance()
        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, -1) }

        return when {
            isSameDay(calendar, today) -> {
                val dayFormat = SimpleDateFormat("dd", Locale.US)
                "${today.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.US)} ${dayFormat.format(date)}, Today"
            }
            isSameDay(calendar, yesterday) -> {
                val dayFormat = SimpleDateFormat("dd", Locale.US)
                "${yesterday.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.US)} ${dayFormat.format(date)}, Yesterday"
            }
            else -> {
                val dayFormat = SimpleDateFormat("dd", Locale.US)
                val dayOfWeek = calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.US)
                val month = calendar.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.US)
                "$month ${dayFormat.format(date)}, $dayOfWeek"
            }
        }
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
}
