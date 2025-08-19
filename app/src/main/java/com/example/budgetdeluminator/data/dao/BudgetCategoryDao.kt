package com.example.budgetdeluminator.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.budgetdeluminator.data.entity.BudgetCategory

@Dao
interface BudgetCategoryDao {

    @Query("SELECT * FROM budget_categories ORDER BY displayOrder ASC, name ASC")
    fun getAllCategories(): LiveData<List<BudgetCategory>>

    @Query("SELECT * FROM budget_categories ORDER BY displayOrder ASC, name ASC")
    suspend fun getAllCategoriesSync(): List<BudgetCategory>

    @Query("SELECT * FROM budget_categories WHERE id = :id")
    suspend fun getCategoryById(id: Long): BudgetCategory?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategory(category: BudgetCategory): Long

    @Update suspend fun updateCategory(category: BudgetCategory)

    @Delete suspend fun deleteCategory(category: BudgetCategory)

    @Query("DELETE FROM budget_categories WHERE id = :id") suspend fun deleteCategoryById(id: Long)

    @Query("UPDATE budget_categories SET displayOrder = :order WHERE id = :id")
    suspend fun updateCategoryOrder(id: Long, order: Int)

    @Query("SELECT MAX(displayOrder) FROM budget_categories") suspend fun getMaxDisplayOrder(): Int?

    @Query("SELECT * FROM budget_categories WHERE name = :name LIMIT 1")
    suspend fun getCategoryByName(name: String): BudgetCategory?
}
