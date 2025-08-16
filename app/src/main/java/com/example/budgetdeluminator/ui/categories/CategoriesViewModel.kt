package com.example.budgetdeluminator.ui.categories

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.budgetdeluminator.data.database.BudgetDatabase
import com.example.budgetdeluminator.data.entity.BudgetCategory
import com.example.budgetdeluminator.data.repository.BudgetRepository
import kotlinx.coroutines.launch

class CategoriesViewModel(application: Application) : AndroidViewModel(application) {

        private val repository: BudgetRepository

        val allCategories: LiveData<List<BudgetCategory>>

        private val _operationResult = MutableLiveData<OperationResult?>()
        val operationResult: LiveData<OperationResult?> = _operationResult

        init {
                val database = BudgetDatabase.getDatabase(application)
                repository =
                        BudgetRepository(
                                database.budgetCategoryDao(),
                                database.expenseDao(),
                                database.recurringExpenseDao()
                        )
                allCategories = repository.getAllCategories()
        }

        fun insertCategory(category: BudgetCategory) =
                viewModelScope.launch {
                        try {
                                repository.insertCategory(category)
                                _operationResult.value =
                                        OperationResult.Success("Category created successfully")
                        } catch (e: Exception) {
                                _operationResult.value =
                                        OperationResult.Error(
                                                "Failed to create category: ${e.message}"
                                        )
                        }
                }

        fun updateCategory(category: BudgetCategory) =
                viewModelScope.launch {
                        try {
                                repository.updateCategory(category)
                                _operationResult.value =
                                        OperationResult.Success("Category updated successfully")
                        } catch (e: Exception) {
                                _operationResult.value =
                                        OperationResult.Error(
                                                "Failed to update category: ${e.message}"
                                        )
                        }
                }

        fun deleteCategory(category: BudgetCategory) =
                viewModelScope.launch {
                        try {
                                val expenseCount = repository.getCategoryExpenseCount(category.id)
                                repository.deleteCategory(category)
                                val message =
                                        if (expenseCount > 0) {
                                                "Category and $expenseCount related expense(s) deleted successfully"
                                        } else {
                                                "Category deleted successfully"
                                        }
                                _operationResult.value = OperationResult.Success(message)
                        } catch (e: Exception) {
                                _operationResult.value =
                                        OperationResult.Error(
                                                "Failed to delete category: ${e.message}"
                                        )
                        }
                }

        suspend fun getCategoryExpenseCount(categoryId: Long): Int {
                return try {
                        repository.getCategoryExpenseCount(categoryId)
                } catch (e: Exception) {
                        0
                }
        }

        fun deleteCategoryById(id: Long) =
                viewModelScope.launch {
                        try {
                                repository.deleteCategoryById(id)
                                _operationResult.value =
                                        OperationResult.Success("Category deleted successfully")
                        } catch (e: Exception) {
                                _operationResult.value =
                                        OperationResult.Error(
                                                "Failed to delete category: ${e.message}"
                                        )
                        }
                }

        fun clearOperationResult() {
                _operationResult.value = null
        }

        sealed class OperationResult {
                data class Success(val message: String) : OperationResult()
                data class Error(val message: String) : OperationResult()
        }
}
