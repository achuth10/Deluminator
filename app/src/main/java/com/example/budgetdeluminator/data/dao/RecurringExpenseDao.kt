package com.example.budgetdeluminator.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.budgetdeluminator.data.entity.RecurringExpense

@Dao
interface RecurringExpenseDao {

    @Query("SELECT * FROM recurring_expenses ORDER BY createdAt DESC")
    fun getAllRecurringExpenses(): LiveData<List<RecurringExpense>>

    @Query("SELECT * FROM recurring_expenses WHERE isActive = 1 ORDER BY createdAt DESC")
    fun getActiveRecurringExpenses(): LiveData<List<RecurringExpense>>

    @Query("SELECT * FROM recurring_expenses WHERE categoryId = :categoryId ORDER BY createdAt DESC")
    fun getRecurringExpensesByCategory(categoryId: Long): LiveData<List<RecurringExpense>>

    @Query("SELECT * FROM recurring_expenses WHERE id = :id")
    suspend fun getRecurringExpenseById(id: Long): RecurringExpense?

    @Query("SELECT * FROM recurring_expenses WHERE isActive = 1")
    suspend fun getActiveRecurringExpensesSync(): List<RecurringExpense>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurringExpense(recurringExpense: RecurringExpense): Long

    @Update
    suspend fun updateRecurringExpense(recurringExpense: RecurringExpense)

    @Delete
    suspend fun deleteRecurringExpense(recurringExpense: RecurringExpense)

    @Query("UPDATE recurring_expenses SET isActive = :isActive WHERE id = :id")
    suspend fun updateActiveStatus(id: Long, isActive: Boolean)

    @Query("UPDATE recurring_expenses SET lastGeneratedAt = :timestamp WHERE id = :id")
    suspend fun updateLastGeneratedAt(id: Long, timestamp: Long)
}
