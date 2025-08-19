package com.example.budgetdeluminator.data.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import com.example.budgetdeluminator.data.dao.BudgetCategoryDao
import com.example.budgetdeluminator.data.dao.ExpenseDao
import com.example.budgetdeluminator.data.dao.RecurringExpenseDao
import com.example.budgetdeluminator.data.entity.BudgetCategory
import com.example.budgetdeluminator.data.entity.Expense
import com.example.budgetdeluminator.data.entity.RecurringExpense
import com.example.budgetdeluminator.data.model.CategoryWithExpenses
import com.example.budgetdeluminator.data.model.ExpenseWithCategory
import java.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BudgetRepository(
        private val budgetCategoryDao: BudgetCategoryDao,
        private val expenseDao: ExpenseDao,
        private val recurringExpenseDao: RecurringExpenseDao
) {

        // Category operations
        fun getAllCategories(): LiveData<List<BudgetCategory>> =
                budgetCategoryDao.getAllCategories()

        suspend fun getCategoryById(id: Long): BudgetCategory? =
                withContext(Dispatchers.IO) { budgetCategoryDao.getCategoryById(id) }

        suspend fun getAllCategoriesSync(): List<BudgetCategory> =
                withContext(Dispatchers.IO) { budgetCategoryDao.getAllCategoriesSync() }

        suspend fun insertCategory(category: BudgetCategory): Long =
                withContext(Dispatchers.IO) {
                        // If no display order is set, assign the next available order
                        val categoryWithOrder =
                                if (category.displayOrder == 0) {
                                        val maxOrder = budgetCategoryDao.getMaxDisplayOrder() ?: 0
                                        category.copy(displayOrder = maxOrder + 1)
                                } else {
                                        category
                                }
                        val insertedId = budgetCategoryDao.insertCategory(categoryWithOrder)
                        android.util.Log.d(
                                "BudgetRepository",
                                "Inserted category: ${categoryWithOrder.name} with ID: $insertedId"
                        )
                        insertedId
                }

        suspend fun updateCategory(category: BudgetCategory) =
                withContext(Dispatchers.IO) { budgetCategoryDao.updateCategory(category) }

        suspend fun deleteCategory(category: BudgetCategory) =
                withContext(Dispatchers.IO) {
                        // The foreign key cascade will automatically delete related expenses
                        budgetCategoryDao.deleteCategory(category)
                }

        suspend fun deleteCategoryById(id: Long) =
                withContext(Dispatchers.IO) {
                        // The foreign key cascade will automatically delete related expenses
                        budgetCategoryDao.deleteCategoryById(id)
                }

        suspend fun getCategoryExpenseCount(categoryId: Long): Int =
                withContext(Dispatchers.IO) { expenseDao.getExpenseCountByCategory(categoryId) }

        suspend fun updateCategoryOrder(id: Long, order: Int) =
                withContext(Dispatchers.IO) { budgetCategoryDao.updateCategoryOrder(id, order) }

        suspend fun getMaxDisplayOrder(): Int =
                withContext(Dispatchers.IO) { budgetCategoryDao.getMaxDisplayOrder() ?: 0 }

        suspend fun reorderCategories(categories: List<BudgetCategory>) =
                withContext(Dispatchers.IO) {
                        categories.forEachIndexed { index, category ->
                                budgetCategoryDao.updateCategoryOrder(category.id, index)
                        }
                }

        // Expense operations
        fun getAllExpenses(): LiveData<List<Expense>> = expenseDao.getAllExpenses()

        fun getAllExpensesWithCategory(): LiveData<List<ExpenseWithCategory>> =
                expenseDao.getAllExpensesWithCategory()

        fun getExpensesByCategory(categoryId: Long): LiveData<List<Expense>> =
                expenseDao.getExpensesByCategory(categoryId)

        fun getExpensesByCategoryInDateRange(
                categoryId: Long,
                startDate: Long,
                endDate: Long
        ): LiveData<List<Expense>> =
                expenseDao.getExpensesByCategoryInDateRange(categoryId, startDate, endDate)

        suspend fun getTotalSpentByCategory(categoryId: Long): Double =
                withContext(Dispatchers.IO) {
                        expenseDao.getTotalSpentByCategory(categoryId) ?: 0.0
                }

        suspend fun getTotalSpentByCategoryThisMonth(categoryId: Long): Double =
                withContext(Dispatchers.IO) {
                        val monthStart =
                                com.example.budgetdeluminator.utils.DateUtils.getCurrentMonthStart()
                        val monthEnd =
                                com.example.budgetdeluminator.utils.DateUtils.getCurrentMonthEnd()
                        expenseDao.getTotalSpentByCategoryInDateRange(
                                categoryId,
                                monthStart,
                                monthEnd
                        )
                                ?: 0.0
                }

        suspend fun insertExpense(expense: Expense): Long =
                withContext(Dispatchers.IO) {
                        // Validate that the category exists before inserting expense
                        val category = budgetCategoryDao.getCategoryById(expense.categoryId)
                        if (category == null) {
                                throw IllegalArgumentException(
                                        "Category with ID ${expense.categoryId} does not exist"
                                )
                        }
                        expenseDao.insertExpense(expense)
                }

        suspend fun updateExpense(expense: Expense) =
                withContext(Dispatchers.IO) { expenseDao.updateExpense(expense) }

        suspend fun deleteExpense(expense: Expense) =
                withContext(Dispatchers.IO) { expenseDao.deleteExpense(expense) }

        suspend fun deleteExpenseById(id: Long) =
                withContext(Dispatchers.IO) { expenseDao.deleteExpenseById(id) }

        suspend fun cleanupOrphanedExpenses(): Int =
                withContext(Dispatchers.IO) { expenseDao.deleteOrphanedExpenses() }

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
        suspend fun getTotalBudget(): Double =
                withContext(Dispatchers.IO) {
                        val categories = budgetCategoryDao.getAllCategories().value ?: emptyList()
                        categories.sumOf { it.budgetLimit }
                }

        suspend fun getTotalSpent(): Double =
                withContext(Dispatchers.IO) {
                        val categories = budgetCategoryDao.getAllCategories().value ?: emptyList()
                        categories.sumOf { category ->
                                expenseDao.getTotalSpentByCategory(category.id) ?: 0.0
                        }
                }

        suspend fun getTotalSpentThisMonth(): Double =
                withContext(Dispatchers.IO) {
                        val categories = budgetCategoryDao.getAllCategories().value ?: emptyList()
                        val monthStart =
                                com.example.budgetdeluminator.utils.DateUtils.getCurrentMonthStart()
                        val monthEnd =
                                com.example.budgetdeluminator.utils.DateUtils.getCurrentMonthEnd()
                        categories.sumOf { category ->
                                expenseDao.getTotalSpentByCategoryInDateRange(
                                        category.id,
                                        monthStart,
                                        monthEnd
                                )
                                        ?: 0.0
                        }
                }

        suspend fun getTotalSpentByCategoryForMonth(
                categoryId: Long,
                year: Int,
                month: Int
        ): Double =
                withContext(Dispatchers.IO) {
                        val monthStart =
                                com.example.budgetdeluminator.utils.DateUtils.getMonthStart(
                                        year,
                                        month
                                )
                        val monthEnd =
                                com.example.budgetdeluminator.utils.DateUtils.getMonthEnd(
                                        year,
                                        month
                                )
                        expenseDao.getTotalSpentByCategoryInDateRange(
                                categoryId,
                                monthStart,
                                monthEnd
                        )
                                ?: 0.0
                }

        suspend fun getAvailableMonths(): List<Pair<Int, Int>> =
                withContext(Dispatchers.IO) {
                        val expenseDates = expenseDao.getAllExpenseDates()
                        val months = mutableSetOf<Pair<Int, Int>>()

                        expenseDates.forEach { timestamp ->
                                val calendar = Calendar.getInstance()
                                calendar.timeInMillis = timestamp
                                val month = calendar.get(Calendar.MONTH)
                                val year = calendar.get(Calendar.YEAR)
                                months.add(Pair(month, year))
                        }

                        // Sort by year and month (most recent first)
                        months.sortedWith(
                                compareByDescending<Pair<Int, Int>> { it.second }.thenByDescending {
                                        it.first
                                }
                        )
                }

        // Recurring expense operations
        fun getAllRecurringExpenses(): LiveData<List<RecurringExpense>> =
                recurringExpenseDao.getAllRecurringExpenses()

        fun getActiveRecurringExpenses(): LiveData<List<RecurringExpense>> =
                recurringExpenseDao.getActiveRecurringExpenses()

        fun getRecurringExpensesByCategory(categoryId: Long): LiveData<List<RecurringExpense>> =
                recurringExpenseDao.getRecurringExpensesByCategory(categoryId)

        suspend fun getRecurringExpenseById(id: Long): RecurringExpense? =
                withContext(Dispatchers.IO) { recurringExpenseDao.getRecurringExpenseById(id) }

        suspend fun getActiveRecurringExpensesSync(): List<RecurringExpense> =
                withContext(Dispatchers.IO) { recurringExpenseDao.getActiveRecurringExpensesSync() }

        suspend fun insertRecurringExpense(recurringExpense: RecurringExpense): Long =
                withContext(Dispatchers.IO) {
                        recurringExpenseDao.insertRecurringExpense(recurringExpense)
                }

        suspend fun updateRecurringExpense(recurringExpense: RecurringExpense) =
                withContext(Dispatchers.IO) {
                        recurringExpenseDao.updateRecurringExpense(recurringExpense)
                }

        suspend fun deleteRecurringExpense(recurringExpense: RecurringExpense) =
                withContext(Dispatchers.IO) {
                        recurringExpenseDao.deleteRecurringExpense(recurringExpense)
                }

        suspend fun updateRecurringExpenseActiveStatus(id: Long, isActive: Boolean) =
                withContext(Dispatchers.IO) { recurringExpenseDao.updateActiveStatus(id, isActive) }

        suspend fun updateRecurringExpenseLastGeneratedAt(id: Long, timestamp: Long) =
                withContext(Dispatchers.IO) {
                        recurringExpenseDao.updateLastGeneratedAt(id, timestamp)
                }
}
