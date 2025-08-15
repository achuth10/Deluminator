package com.example.budgetdeluminator.data.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.map
import com.example.budgetdeluminator.data.dao.BudgetCategoryDao
import com.example.budgetdeluminator.data.dao.ExpenseDao
import com.example.budgetdeluminator.data.entity.BudgetCategory
import com.example.budgetdeluminator.data.entity.Expense
import com.example.budgetdeluminator.data.model.CategoryWithExpenses
import com.example.budgetdeluminator.data.model.ExpenseWithCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BudgetRepository(
    private val budgetCategoryDao: BudgetCategoryDao,
    private val expenseDao: ExpenseDao
) {
    
    // Category operations
    fun getAllCategories(): LiveData<List<BudgetCategory>> = budgetCategoryDao.getAllCategories()
    
    suspend fun getCategoryById(id: Long): BudgetCategory? = withContext(Dispatchers.IO) {
        budgetCategoryDao.getCategoryById(id)
    }
    
    suspend fun insertCategory(category: BudgetCategory): Long = withContext(Dispatchers.IO) {
        budgetCategoryDao.insertCategory(category)
    }
    
    suspend fun updateCategory(category: BudgetCategory) = withContext(Dispatchers.IO) {
        budgetCategoryDao.updateCategory(category)
    }
    
    suspend fun deleteCategory(category: BudgetCategory) = withContext(Dispatchers.IO) {
        budgetCategoryDao.deleteCategory(category)
    }
    
    suspend fun deleteCategoryById(id: Long) = withContext(Dispatchers.IO) {
        budgetCategoryDao.deleteCategoryById(id)
    }
    
    // Expense operations
    fun getAllExpenses(): LiveData<List<Expense>> = expenseDao.getAllExpenses()
    
    fun getAllExpensesWithCategory(): LiveData<List<ExpenseWithCategory>> = expenseDao.getAllExpensesWithCategory()
    
    fun getExpensesByCategory(categoryId: Long): LiveData<List<Expense>> = 
        expenseDao.getExpensesByCategory(categoryId)
    
    suspend fun getTotalSpentByCategory(categoryId: Long): Double = withContext(Dispatchers.IO) {
        expenseDao.getTotalSpentByCategory(categoryId) ?: 0.0
    }
    
    suspend fun insertExpense(expense: Expense): Long = withContext(Dispatchers.IO) {
        expenseDao.insertExpense(expense)
    }
    
    suspend fun updateExpense(expense: Expense) = withContext(Dispatchers.IO) {
        expenseDao.updateExpense(expense)
    }
    
    suspend fun deleteExpense(expense: Expense) = withContext(Dispatchers.IO) {
        expenseDao.deleteExpense(expense)
    }
    
    suspend fun deleteExpenseById(id: Long) = withContext(Dispatchers.IO) {
        expenseDao.deleteExpenseById(id)
    }
    
    // Combined operations for home screen
    fun getCategoriesWithExpenses(): LiveData<List<CategoryWithExpenses>> {
        val categoriesLiveData = budgetCategoryDao.getAllCategories()
        
        return categoriesLiveData.map { categories ->
            categories.map { category ->
                // This will be calculated in the ViewModel for better performance
                CategoryWithExpenses(category, 0.0)
            }
        }
    }
    
    // Get total budget and total spent for overview
    suspend fun getTotalBudget(): Double = withContext(Dispatchers.IO) {
        val categories = budgetCategoryDao.getAllCategories().value ?: emptyList()
        categories.sumOf { it.budgetLimit }
    }
    
    suspend fun getTotalSpent(): Double = withContext(Dispatchers.IO) {
        val categories = budgetCategoryDao.getAllCategories().value ?: emptyList()
        categories.sumOf { category ->
            expenseDao.getTotalSpentByCategory(category.id) ?: 0.0
        }
    }
}
