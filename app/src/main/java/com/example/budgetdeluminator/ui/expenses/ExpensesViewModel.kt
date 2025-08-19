package com.example.budgetdeluminator.ui.expenses

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.budgetdeluminator.data.database.BudgetDatabase
import com.example.budgetdeluminator.data.entity.BudgetCategory
import com.example.budgetdeluminator.data.entity.Expense
import com.example.budgetdeluminator.data.model.ExpenseWithCategory
import com.example.budgetdeluminator.data.repository.BudgetRepository
import kotlinx.coroutines.launch

class ExpensesViewModel(application: Application) : AndroidViewModel(application) {

        private val repository: BudgetRepository

        val allCategories: LiveData<List<BudgetCategory>>
        val allExpenses: LiveData<List<Expense>>
        val allExpensesWithCategory: LiveData<List<ExpenseWithCategory>>

        private val _selectedCategoryId = MutableLiveData<Long?>()
        val selectedCategoryId: LiveData<Long?> = _selectedCategoryId

        init {
                val database = BudgetDatabase.getDatabase(application)
                repository =
                        BudgetRepository(
                                database.budgetCategoryDao(),
                                database.expenseDao(),
                                database.recurringExpenseDao()
                        )
                allCategories = repository.getAllCategories()
                allExpenses = repository.getAllExpenses()
                allExpensesWithCategory = repository.getAllExpensesWithCategory()
        }

        fun getExpensesByCategory(categoryId: Long): LiveData<List<Expense>> {
                return repository.getExpensesByCategory(categoryId)
        }

        fun getExpensesByCategoryInDateRange(
                categoryId: Long,
                startDate: Long,
                endDate: Long
        ): LiveData<List<Expense>> {
                return repository.getExpensesByCategoryInDateRange(categoryId, startDate, endDate)
        }

        fun insertExpense(expense: Expense) =
                viewModelScope.launch { repository.insertExpense(expense) }

        fun updateExpense(expense: Expense) =
                viewModelScope.launch { repository.updateExpense(expense) }

        fun deleteExpense(expense: Expense) =
                viewModelScope.launch { repository.deleteExpense(expense) }

        fun deleteExpenseById(id: Long) = viewModelScope.launch { repository.deleteExpenseById(id) }

        fun setSelectedCategory(categoryId: Long?) {
                _selectedCategoryId.value = categoryId
        }
}
