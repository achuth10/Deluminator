package com.example.budgetdeluminator.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents a recurring expense template that generates actual expenses based on schedule
 */
@Entity(
    tableName = "recurring_expenses",
    foreignKeys = [
        ForeignKey(
            entity = BudgetCategory::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["categoryId"])
    ]
)
data class RecurringExpense(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val categoryId: Long,
    val amount: Double,
    val description: String,
    val recurrenceType: RecurrenceType,
    val recurrenceValue: Int, // Day of month (1-31), day of week (1-7), or hour for daily (0-23)
    val isActive: Boolean = true,
    val lastGeneratedAt: Long = 0, // Timestamp of last generated expense
    val createdAt: Long = System.currentTimeMillis()
)

enum class RecurrenceType {
    DAILY,    // recurrenceValue = hour (0-23)
    WEEKLY,   // recurrenceValue = day of week (1=Sunday, 7=Saturday)
    MONTHLY   // recurrenceValue = day of month (1-31)
}
