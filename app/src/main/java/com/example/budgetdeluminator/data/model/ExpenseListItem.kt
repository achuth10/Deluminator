package com.example.budgetdeluminator.data.model

import java.util.*

sealed class ExpenseListItem {
    data class DateHeader(
        val date: Date,
        val totalAmount: Double,
        val formattedDate: String
    ) : ExpenseListItem()
    
    data class ExpenseItem(
        val expenseWithCategory: ExpenseWithCategory
    ) : ExpenseListItem()
}
