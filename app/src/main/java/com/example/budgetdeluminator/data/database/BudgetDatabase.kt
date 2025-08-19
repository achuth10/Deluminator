package com.example.budgetdeluminator.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.budgetdeluminator.data.dao.BudgetCategoryDao
import com.example.budgetdeluminator.data.dao.ExpenseDao
import com.example.budgetdeluminator.data.dao.RecurringExpenseDao
import com.example.budgetdeluminator.data.entity.BudgetCategory
import com.example.budgetdeluminator.data.entity.Expense
import com.example.budgetdeluminator.data.entity.RecurringExpense

@Database(
        entities = [BudgetCategory::class, Expense::class, RecurringExpense::class],
        version = 6,
        exportSchema = true
)
abstract class BudgetDatabase : RoomDatabase() {

    abstract fun budgetCategoryDao(): BudgetCategoryDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun recurringExpenseDao(): RecurringExpenseDao

    companion object {
        @Volatile private var INSTANCE: BudgetDatabase? = null

        /** Migration from version 5 to 6 Make budgetLimit nullable for tracking-only categories */
        private val MIGRATION_5_6 =
                object : Migration(5, 6) {
                    override fun migrate(database: SupportSQLiteDatabase) {
                        // Create a new table with the updated schema
                        database.execSQL(
                                """
                                CREATE TABLE budget_categories_new (
                                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                    name TEXT NOT NULL,
                                    budgetLimit REAL,
                                    color TEXT NOT NULL DEFAULT '#4CAF50',
                                    createdAt INTEGER NOT NULL,
                                    displayOrder INTEGER NOT NULL DEFAULT 0
                                )
                                """.trimIndent()
                        )

                        // Copy data from old table to new table
                        database.execSQL(
                                """
                                INSERT INTO budget_categories_new (id, name, budgetLimit, color, createdAt, displayOrder)
                                SELECT id, name, budgetLimit, color, createdAt, displayOrder
                                FROM budget_categories
                                """.trimIndent()
                        )

                        // Drop the old table
                        database.execSQL("DROP TABLE budget_categories")

                        // Rename the new table to the original name
                        database.execSQL(
                                "ALTER TABLE budget_categories_new RENAME TO budget_categories"
                        )
                    }
                }

        /**
         * Migration from version 4 to 5 Added source and recurringExpenseId columns to expenses
         * table
         */
        private val MIGRATION_4_5 =
                object : Migration(4, 5) {
                    override fun migrate(database: SupportSQLiteDatabase) {
                        // Add new columns to expenses table with default values
                        database.execSQL(
                                "ALTER TABLE expenses ADD COLUMN source TEXT NOT NULL DEFAULT 'MANUAL'"
                        )
                        database.execSQL(
                                "ALTER TABLE expenses ADD COLUMN recurringExpenseId INTEGER DEFAULT NULL"
                        )

                        // Create index for better performance on recurring expense lookups
                        database.execSQL(
                                "CREATE INDEX IF NOT EXISTS index_expenses_recurringExpenseId ON expenses (recurringExpenseId)"
                        )
                    }
                }

        /** Migration from version 3 to 4 Example migration for future reference */
        private val MIGRATION_3_4 =
                object : Migration(3, 4) {
                    override fun migrate(database: SupportSQLiteDatabase) {
                        // This would contain the actual migration logic for 3->4
                        // Keeping as example for future migrations
                    }
                }

        /** Migration from version 2 to 3 Example migration for future reference */
        private val MIGRATION_2_3 =
                object : Migration(2, 3) {
                    override fun migrate(database: SupportSQLiteDatabase) {
                        // This would contain the actual migration logic for 2->3
                        // Keeping as example for future migrations
                    }
                }

        /** Migration from version 1 to 2 Example migration for future reference */
        private val MIGRATION_1_2 =
                object : Migration(1, 2) {
                    override fun migrate(database: SupportSQLiteDatabase) {
                        // This would contain the actual migration logic for 1->2
                        // Keeping as example for future migrations
                    }
                }

        fun getDatabase(context: Context): BudgetDatabase {
            return INSTANCE
                    ?: synchronized(this) {
                        val instance =
                                Room.databaseBuilder(
                                                context.applicationContext,
                                                BudgetDatabase::class.java,
                                                "budget_database"
                                        )
                                        // Add all migrations instead of destructive migration
                                        .addMigrations(
                                                MIGRATION_1_2,
                                                MIGRATION_2_3,
                                                MIGRATION_3_4,
                                                MIGRATION_4_5,
                                                MIGRATION_5_6
                                        )
                                        // Only fallback to destructive migration in debug builds
                                        .apply {
                                            if (android.os.Build.VERSION.SDK_INT <
                                                            android.os.Build.VERSION_CODES.O
                                            ) {
                                                // For older devices, allow destructive migration as
                                                // last resort
                                                fallbackToDestructiveMigration()
                                            }
                                        }
                                        .addCallback(
                                                object : RoomDatabase.Callback() {
                                                    override fun onOpen(db: SupportSQLiteDatabase) {
                                                        super.onOpen(db)
                                                        // Enable foreign key constraints
                                                        db.execSQL("PRAGMA foreign_keys=ON")
                                                        // Clean up any orphaned data
                                                        db.execSQL(
                                                                "DELETE FROM expenses WHERE categoryId NOT IN (SELECT id FROM budget_categories)"
                                                        )
                                                    }

                                                    override fun onCreate(
                                                            db: SupportSQLiteDatabase
                                                    ) {
                                                        super.onCreate(db)
                                                        // Enable foreign keys on database creation
                                                        db.execSQL("PRAGMA foreign_keys=ON")
                                                    }
                                                }
                                        )
                                        .build()
                        INSTANCE = instance
                        instance
                    }
        }

        /** For testing purposes - allows creating an in-memory database */
        fun getTestDatabase(context: Context): BudgetDatabase {
            return Room.inMemoryDatabaseBuilder(
                            context.applicationContext,
                            BudgetDatabase::class.java
                    )
                    .allowMainThreadQueries()
                    .addCallback(
                            object : RoomDatabase.Callback() {
                                override fun onCreate(db: SupportSQLiteDatabase) {
                                    super.onCreate(db)
                                    db.execSQL("PRAGMA foreign_keys=ON")
                                }
                            }
                    )
                    .build()
        }
    }
}
