package com.example.budgetdeluminator.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.budgetdeluminator.data.entity.BudgetCategory

@Dao
interface BudgetCategoryDao {

    @Query("SELECT * FROM budget_categories ORDER BY name ASC")
    fun getAllCategories(): LiveData<List<BudgetCategory>>

    @Query("SELECT * FROM budget_categories WHERE id = :id")
    suspend fun getCategoryById(id: Long): BudgetCategory?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: BudgetCategory): Long

    @Update suspend fun updateCategory(category: BudgetCategory)

    @Delete suspend fun deleteCategory(category: BudgetCategory)

    @Query("DELETE FROM budget_categories WHERE id = :id") suspend fun deleteCategoryById(id: Long)

    @Query("UPDATE budget_categories SET color = '#4CAF50' WHERE color IS NULL OR color = ''")
    suspend fun fixMissingColors()
}
