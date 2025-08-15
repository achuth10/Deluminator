package com.example.budgetdeluminator.ui.recurring

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.example.budgetdeluminator.data.database.BudgetDatabase
import com.example.budgetdeluminator.data.entity.RecurringExpense
import com.example.budgetdeluminator.data.repository.BudgetRepository
import kotlinx.coroutines.launch

class RecurringExpensesViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: BudgetRepository

    init {
        val database = BudgetDatabase.getDatabase(application)
        repository = BudgetRepository(
            database.budgetCategoryDao(),
            database.expenseDao(),
            database.recurringExpenseDao()
        )
    }

    // LiveData for UI
    val allRecurringExpenses: LiveData<List<RecurringExpense>> = repository.getAllRecurringExpenses()
    val activeRecurringExpenses: LiveData<List<RecurringExpense>> = repository.getActiveRecurringExpenses()

    fun getRecurringExpensesByCategory(categoryId: Long): LiveData<List<RecurringExpense>> {
        return repository.getRecurringExpensesByCategory(categoryId)
    }

    fun insertRecurringExpense(recurringExpense: RecurringExpense) {
        viewModelScope.launch {
            repository.insertRecurringExpense(recurringExpense)
        }
    }

    fun updateRecurringExpense(recurringExpense: RecurringExpense) {
        viewModelScope.launch {
            repository.updateRecurringExpense(recurringExpense)
        }
    }

    fun deleteRecurringExpense(recurringExpense: RecurringExpense) {
        viewModelScope.launch {
            repository.deleteRecurringExpense(recurringExpense)
        }
    }

    fun toggleActiveStatus(recurringExpense: RecurringExpense, isActive: Boolean) {
        viewModelScope.launch {
            repository.updateRecurringExpenseActiveStatus(recurringExpense.id, isActive)
        }
    }

    fun updateLastGeneratedAt(id: Long, timestamp: Long) {
        viewModelScope.launch {
            repository.updateRecurringExpenseLastGeneratedAt(id, timestamp)
        }
    }
}
