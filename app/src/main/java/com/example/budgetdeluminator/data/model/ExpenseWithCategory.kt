package com.example.budgetdeluminator.data.model

import androidx.room.Embedded
import androidx.room.Relation
import com.example.budgetdeluminator.data.entity.BudgetCategory
import com.example.budgetdeluminator.data.entity.Expense

data class ExpenseWithCategory(
        @Embedded val expense: Expense,
        @Relation(parentColumn = "categoryId", entityColumn = "id") val category: BudgetCategory?
)
