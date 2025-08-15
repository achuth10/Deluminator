package com.example.budgetdeluminator.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
        tableName = "expenses",
        foreignKeys =
                [
                        ForeignKey(
                                entity = BudgetCategory::class,
                                parentColumns = ["id"],
                                childColumns = ["categoryId"],
                                onDelete = ForeignKey.CASCADE
                        )],
        indices = [Index(value = ["categoryId"])]
)
data class Expense(
        @PrimaryKey(autoGenerate = true) val id: Long = 0,
        val categoryId: Long,
        val amount: Double,
        val description: String,
        val createdAt: Long = System.currentTimeMillis(),
        val source: ExpenseSource = ExpenseSource.MANUAL,
        val recurringExpenseId: Long? = null // Link back to the recurring expense that created this
)

enum class ExpenseSource {
    MANUAL, // User manually added
    RECURRING, // Auto-generated from recurring expense
    IMPORT // Future: imported from bank/CSV
}
