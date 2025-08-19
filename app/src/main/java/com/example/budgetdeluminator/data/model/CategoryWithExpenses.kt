package com.example.budgetdeluminator.data.model

import com.example.budgetdeluminator.data.entity.BudgetCategory

data class CategoryWithExpenses(val category: BudgetCategory, val totalSpent: Double = 0.0) {
    val remainingBudget: Double?
        get() = category.budgetLimit?.let { it - totalSpent }

    val budgetPercentage: Float
        get() =
                category.budgetLimit?.let { limit ->
                    if (limit > 0) {
                        (totalSpent / limit * 100).toFloat()
                    } else 0f
                }
                        ?: 0f

    val isOverBudget: Boolean
        get() = category.budgetLimit?.let { totalSpent > it } ?: false

    val hasBudgetLimit: Boolean
        get() = category.budgetLimit != null

    val isTrackingOnly: Boolean
        get() = category.budgetLimit == null
}
