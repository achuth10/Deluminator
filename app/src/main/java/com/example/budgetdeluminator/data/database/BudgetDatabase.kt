package com.example.budgetdeluminator.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.budgetdeluminator.data.dao.BudgetCategoryDao
import com.example.budgetdeluminator.data.dao.ExpenseDao
import com.example.budgetdeluminator.data.entity.BudgetCategory
import com.example.budgetdeluminator.data.entity.Expense

@Database(
    entities = [BudgetCategory::class, Expense::class],
    version = 2,
    exportSchema = false
)
abstract class BudgetDatabase : RoomDatabase() {
    
    abstract fun budgetCategoryDao(): BudgetCategoryDao
    abstract fun expenseDao(): ExpenseDao
    
    companion object {
        @Volatile
        private var INSTANCE: BudgetDatabase? = null
        
        fun getDatabase(context: Context): BudgetDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BudgetDatabase::class.java,
                    "budget_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
