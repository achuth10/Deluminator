package com.example.budgetdeluminator.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.budgetdeluminator.data.dao.BudgetCategoryDao
import com.example.budgetdeluminator.data.dao.ExpenseDao
import com.example.budgetdeluminator.data.dao.RecurringExpenseDao
import com.example.budgetdeluminator.data.entity.BudgetCategory
import com.example.budgetdeluminator.data.entity.Expense
import com.example.budgetdeluminator.data.entity.RecurringExpense

@Database(
        entities = [BudgetCategory::class, Expense::class, RecurringExpense::class],
        version = 5,
        exportSchema = false
)
abstract class BudgetDatabase : RoomDatabase() {

    abstract fun budgetCategoryDao(): BudgetCategoryDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun recurringExpenseDao(): RecurringExpenseDao

    companion object {
        @Volatile private var INSTANCE: BudgetDatabase? = null

        fun getDatabase(context: Context): BudgetDatabase {
            return INSTANCE
                    ?: synchronized(this) {
                        val instance =
                                Room.databaseBuilder(
                                                context.applicationContext,
                                                BudgetDatabase::class.java,
                                                "budget_database"
                                        )
                                        .fallbackToDestructiveMigration()
                                        .addCallback(
                                                object : RoomDatabase.Callback() {
                                                    override fun onOpen(db: SupportSQLiteDatabase) {
                                                        super.onOpen(db)
                                                        // Enable foreign key constraints with
                                                        // proper validation
                                                        db.execSQL("PRAGMA foreign_keys=ON")
                                                        // Clean up any orphaned expenses from
                                                        // schema changes
                                                        db.execSQL(
                                                                "DELETE FROM expenses WHERE categoryId NOT IN (SELECT id FROM budget_categories)"
                                                        )
                                                    }
                                                }
                                        )
                                        .build()
                        INSTANCE = instance
                        instance
                    }
        }
    }
}
