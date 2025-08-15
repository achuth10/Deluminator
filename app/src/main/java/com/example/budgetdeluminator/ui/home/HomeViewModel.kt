package com.example.budgetdeluminator.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.viewModelScope
import com.example.budgetdeluminator.data.database.BudgetDatabase
import com.example.budgetdeluminator.data.entity.BudgetCategory
import com.example.budgetdeluminator.data.model.CategoryWithExpenses
import com.example.budgetdeluminator.data.repository.BudgetRepository
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository: BudgetRepository
    private val _categoriesWithExpenses = MediatorLiveData<List<CategoryWithExpenses>>()
    
    val categoriesWithExpenses: LiveData<List<CategoryWithExpenses>> = _categoriesWithExpenses
    
    init {
        val database = BudgetDatabase.getDatabase(application)
        repository = BudgetRepository(database.budgetCategoryDao(), database.expenseDao())
        
        // Observe both categories and expenses for real-time updates
        val categoriesLiveData = repository.getAllCategories()
        val expensesLiveData = repository.getAllExpenses()
        
        // Update whenever categories change
        _categoriesWithExpenses.addSource(categoriesLiveData) { categories ->
            updateCategoriesWithExpenses(categories)
        }
        
        // Update whenever expenses change (this was missing!)
        _categoriesWithExpenses.addSource(expensesLiveData) { _ ->
            // Re-fetch categories to trigger update
            categoriesLiveData.value?.let { categories ->
                updateCategoriesWithExpenses(categories)
            }
        }
    }
    
    private fun updateCategoriesWithExpenses(categories: List<BudgetCategory>) {
        viewModelScope.launch {
            val categoriesWithExpenses = categories.map { category ->
                val totalSpent = repository.getTotalSpentByCategory(category.id)
                CategoryWithExpenses(category, totalSpent)
            }
            _categoriesWithExpenses.value = categoriesWithExpenses
        }
    }
    
    fun getTotalBudget(): Double {
        return categoriesWithExpenses.value?.sumOf { it.category.budgetLimit } ?: 0.0
    }
    
    fun getTotalSpent(): Double {
        return categoriesWithExpenses.value?.sumOf { it.totalSpent } ?: 0.0
    }
    
    fun getRemainingBudget(): Double {
        return getTotalBudget() - getTotalSpent()
    }
}
