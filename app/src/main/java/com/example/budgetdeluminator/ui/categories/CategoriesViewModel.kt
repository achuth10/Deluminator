package com.example.budgetdeluminator.ui.categories

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.example.budgetdeluminator.data.database.BudgetDatabase
import com.example.budgetdeluminator.data.entity.BudgetCategory
import com.example.budgetdeluminator.data.repository.BudgetRepository
import kotlinx.coroutines.launch

class CategoriesViewModel(application: Application) : AndroidViewModel(application) {

        private val repository: BudgetRepository

        val allCategories: LiveData<List<BudgetCategory>>

        init {
                val database = BudgetDatabase.getDatabase(application)
                repository = BudgetRepository(database.budgetCategoryDao(), database.expenseDao())
                allCategories = repository.getAllCategories()
        }

        fun insertCategory(category: BudgetCategory) =
                viewModelScope.launch { repository.insertCategory(category) }

        fun updateCategory(category: BudgetCategory) =
                viewModelScope.launch { repository.updateCategory(category) }

        fun deleteCategory(category: BudgetCategory) =
                viewModelScope.launch { repository.deleteCategory(category) }

        fun deleteCategoryById(id: Long) =
                viewModelScope.launch { repository.deleteCategoryById(id) }

        fun fixMissingColors() = viewModelScope.launch { repository.fixMissingColors() }
}
