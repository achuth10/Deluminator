package com.example.budgetdeluminator.data.model

import com.example.budgetdeluminator.data.entity.BudgetCategory

data class CategoryWithExpenses(
    val category: BudgetCategory,
    val totalSpent: Double = 0.0
) {
    val remainingBudget: Double
        get() = category.budgetLimit - totalSpent
    
    val budgetPercentage: Float
        get() = if (category.budgetLimit > 0) {
            (totalSpent / category.budgetLimit * 100).toFloat()
        } else 0f
    
    val isOverBudget: Boolean
        get() = totalSpent > category.budgetLimit
}
