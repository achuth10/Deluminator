package com.example.budgetdeluminator.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budget_categories")
data class BudgetCategory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val budgetLimit: Double,
    val color: String = "#4CAF50", // Default green color
    val createdAt: Long = System.currentTimeMillis()
)
