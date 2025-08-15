package com.example.budgetdeluminator.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.budgetdeluminator.data.entity.Expense
import com.example.budgetdeluminator.data.model.ExpenseWithCategory

@Dao
interface ExpenseDao {
    
    @Query("SELECT * FROM expenses ORDER BY createdAt DESC")
    fun getAllExpenses(): LiveData<List<Expense>>
    
    @Transaction
    @Query("SELECT * FROM expenses ORDER BY createdAt DESC")
    fun getAllExpensesWithCategory(): LiveData<List<ExpenseWithCategory>>
    
    @Query("SELECT * FROM expenses WHERE categoryId = :categoryId ORDER BY createdAt DESC")
    fun getExpensesByCategory(categoryId: Long): LiveData<List<Expense>>
    
    @Query("SELECT SUM(amount) FROM expenses WHERE categoryId = :categoryId")
    suspend fun getTotalSpentByCategory(categoryId: Long): Double?
    
    @Query("SELECT SUM(amount) FROM expenses WHERE categoryId = :categoryId")
    fun getTotalSpentByCategoryLive(categoryId: Long): LiveData<Double?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense): Long
    
    @Update
    suspend fun updateExpense(expense: Expense)
    
    @Delete
    suspend fun deleteExpense(expense: Expense)
    
    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteExpenseById(id: Long)
}
