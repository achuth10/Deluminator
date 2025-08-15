package com.example.budgetdeluminator.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.budgetdeluminator.data.database.BudgetDatabase
import com.example.budgetdeluminator.data.entity.BudgetCategory
import com.example.budgetdeluminator.data.model.CategoryWithExpenses
import com.example.budgetdeluminator.data.repository.BudgetRepository
import com.example.budgetdeluminator.utils.DateUtils
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: BudgetRepository
    private val _categoriesWithExpenses = MediatorLiveData<List<CategoryWithExpenses>>()
    private val _availableMonths = MutableLiveData<List<Pair<Int, Int>>>()
    private val _selectedMonth = MutableLiveData<Pair<Int, Int>>()

    val categoriesWithExpenses: LiveData<List<CategoryWithExpenses>> = _categoriesWithExpenses
    val availableMonths: LiveData<List<Pair<Int, Int>>> = _availableMonths
    val selectedMonth: LiveData<Pair<Int, Int>> = _selectedMonth

    init {
        val database = BudgetDatabase.getDatabase(application)
        repository = BudgetRepository(database.budgetCategoryDao(), database.expenseDao())

        // Set current month as default selection
        _selectedMonth.value = DateUtils.getCurrentMonthYear()

        // Load available months
        loadAvailableMonths()

        // Observe both categories and expenses for real-time updates
        val categoriesLiveData = repository.getAllCategories()
        val expensesLiveData = repository.getAllExpenses()

        // Update whenever categories change
        _categoriesWithExpenses.addSource(categoriesLiveData) { categories ->
            updateCategoriesWithExpenses(categories)
        }

        // Update whenever expenses change
        _categoriesWithExpenses.addSource(expensesLiveData) { _ ->
            // Re-fetch categories to trigger update
            categoriesLiveData.value?.let { categories -> updateCategoriesWithExpenses(categories) }
            // Also reload available months when expenses change
            loadAvailableMonths()
        }

        // Update when selected month changes
        _categoriesWithExpenses.addSource(_selectedMonth) { _ ->
            categoriesLiveData.value?.let { categories -> updateCategoriesWithExpenses(categories) }
        }
    }

    private fun updateCategoriesWithExpenses(categories: List<BudgetCategory>) {
        viewModelScope.launch {
            val selectedMonthYear = _selectedMonth.value ?: DateUtils.getCurrentMonthYear()
            val categoriesWithExpenses =
                    categories.map { category ->
                        // Use selected month calculation for budget tracking
                        val totalSpent =
                                repository.getTotalSpentByCategoryForMonth(
                                        category.id,
                                        selectedMonthYear.second,
                                        selectedMonthYear.first
                                )
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

    fun selectMonth(month: Int, year: Int) {
        _selectedMonth.value = Pair(month, year)
    }

    fun getCurrentSelectedMonthName(): String {
        val selectedMonthYear = _selectedMonth.value ?: DateUtils.getCurrentMonthYear()
        return "${DateUtils.getMonthName(selectedMonthYear.first)} ${selectedMonthYear.second}"
    }

    private fun loadAvailableMonths() {
        viewModelScope.launch {
            val months = repository.getAvailableMonths()
            _availableMonths.value = months
        }
    }
}
