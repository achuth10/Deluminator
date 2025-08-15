package com.example.budgetdeluminator.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.work.*
import com.example.budgetdeluminator.data.database.BudgetDatabase
import com.example.budgetdeluminator.data.entity.Expense
import com.example.budgetdeluminator.data.entity.ExpenseSource
import com.example.budgetdeluminator.data.entity.RecurrenceType
import com.example.budgetdeluminator.data.entity.RecurringExpense
import com.example.budgetdeluminator.data.repository.BudgetRepository
import java.util.*
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Handles scheduling and generation of recurring expenses */
class RecurrenceScheduler(private val context: Context) {

    companion object {
        const val RECURRING_EXPENSE_WORK_NAME = "RecurringExpenseWork"
        const val WORK_TAG = "recurring_expenses"
    }

    /** Schedule periodic work to check and generate recurring expenses */
    fun scheduleRecurringExpenseWork() {
        val constraints =
                Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                        .setRequiresBatteryNotLow(true) // Only run when battery is not low
                        .setRequiresDeviceIdle(false) // Can run when device is in use
                        .setRequiresCharging(false) // Don't require charging
                        .build()

        val recurringExpenseWork =
                PeriodicWorkRequestBuilder<RecurringExpenseWorker>(
                                1,
                                TimeUnit.HOURS // Check every hour
                        )
                        .setConstraints(constraints)
                        .addTag(WORK_TAG)
                        .build()

        WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                        RECURRING_EXPENSE_WORK_NAME,
                        ExistingPeriodicWorkPolicy.KEEP,
                        recurringExpenseWork
                )
    }

    /** Cancel recurring expense work */
    fun cancelRecurringExpenseWork() {
        WorkManager.getInstance(context).cancelUniqueWork(RECURRING_EXPENSE_WORK_NAME)
    }

    /** Check if recurring expense work is scheduled */
    fun isRecurringExpenseWorkScheduled(): Boolean {
        val workInfos =
                WorkManager.getInstance(context)
                        .getWorkInfosForUniqueWork(RECURRING_EXPENSE_WORK_NAME)
                        .get()
        return workInfos.any { !it.state.isFinished }
    }

    /** Check if app can run in background (battery optimization disabled) */
    fun isBackgroundProcessingAllowed(): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            powerManager.isIgnoringBatteryOptimizations(context.packageName)
        } else {
            true // Pre-Marshmallow doesn't have battery optimization
        }
    }

    /** Get intent to request battery optimization exemption */
    fun getBatteryOptimizationIntent(): Intent? {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        } else {
            null
        }
    }

    /** Process recurring expenses manually (fallback when background processing fails) */
    suspend fun processRecurringExpensesManually(): Int {
        val result = processRecurringExpenses()
        return result.getOrNull() ?: 0
    }

    /** Process all active recurring expenses and generate expenses if needed */
    suspend fun processRecurringExpenses(): Result<Int> =
            withContext(Dispatchers.IO) {
                return@withContext try {
                    val database = BudgetDatabase.getDatabase(context)
                    val repository =
                            BudgetRepository(
                                    database.budgetCategoryDao(),
                                    database.expenseDao(),
                                    database.recurringExpenseDao()
                            )

                    val activeRecurringExpenses = repository.getActiveRecurringExpensesSync()

                    // Early exit if no active recurring expenses - saves battery
                    if (activeRecurringExpenses.isEmpty()) {
                        return@withContext Result.success(0)
                    }

                    var expensesGenerated = 0

                    for (recurringExpense in activeRecurringExpenses) {
                        if (shouldGenerateExpense(recurringExpense)) {
                            val expense = createExpenseFromRecurring(recurringExpense)
                            repository.insertExpense(expense)
                            repository.updateRecurringExpenseLastGeneratedAt(
                                    recurringExpense.id,
                                    System.currentTimeMillis()
                            )
                            expensesGenerated++
                        }
                    }

                    Result.success(expensesGenerated)
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }

    /** Determine if a recurring expense should generate a new expense now */
    private fun shouldGenerateExpense(recurringExpense: RecurringExpense): Boolean {
        val now = Calendar.getInstance()
        val lastGenerated =
                if (recurringExpense.lastGeneratedAt > 0) {
                    Calendar.getInstance().apply { timeInMillis = recurringExpense.lastGeneratedAt }
                } else {
                    null
                }

        return when (recurringExpense.recurrenceType) {
            RecurrenceType.DAILY -> {
                val targetHour = recurringExpense.recurrenceValue
                val currentHour = now.get(Calendar.HOUR_OF_DAY)

                // Generate if we've passed the target hour and haven't generated today
                currentHour >= targetHour &&
                        (lastGenerated == null || !isSameDay(now, lastGenerated))
            }
            RecurrenceType.WEEKLY -> {
                val targetDayOfWeek = recurringExpense.recurrenceValue
                val currentDayOfWeek = now.get(Calendar.DAY_OF_WEEK)
                val currentHour = now.get(Calendar.HOUR_OF_DAY)

                // Generate if it's the right day, past 5 AM, and haven't generated this week
                currentDayOfWeek == targetDayOfWeek &&
                        currentHour >= 5 &&
                        (lastGenerated == null || !isSameWeek(now, lastGenerated))
            }
            RecurrenceType.MONTHLY -> {
                val targetDayOfMonth = recurringExpense.recurrenceValue
                val currentDayOfMonth = now.get(Calendar.DAY_OF_MONTH)
                val currentHour = now.get(Calendar.HOUR_OF_DAY)

                // Generate if it's the right day (or closest valid day), past 5 AM, and haven't
                // generated this month
                (currentDayOfMonth == targetDayOfMonth ||
                        (targetDayOfMonth > now.getActualMaximum(Calendar.DAY_OF_MONTH) &&
                                currentDayOfMonth ==
                                        now.getActualMaximum(Calendar.DAY_OF_MONTH))) &&
                        currentHour >= 5 &&
                        (lastGenerated == null || !isSameMonth(now, lastGenerated))
            }
        }
    }

    /** Create an expense from a recurring expense template */
    private fun createExpenseFromRecurring(recurringExpense: RecurringExpense): Expense {
        val now = Calendar.getInstance()

        // Set time based on recurrence type
        val timestamp =
                when (recurringExpense.recurrenceType) {
                    RecurrenceType.DAILY -> {
                        now
                                .apply {
                                    set(Calendar.HOUR_OF_DAY, recurringExpense.recurrenceValue)
                                    set(Calendar.MINUTE, 0)
                                    set(Calendar.SECOND, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }
                                .timeInMillis
                    }
                    RecurrenceType.WEEKLY, RecurrenceType.MONTHLY -> {
                        now
                                .apply {
                                    set(Calendar.HOUR_OF_DAY, 5) // 5:00 AM
                                    set(Calendar.MINUTE, 0)
                                    set(Calendar.SECOND, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }
                                .timeInMillis
                    }
                }

        return Expense(
                categoryId = recurringExpense.categoryId,
                amount = recurringExpense.amount,
                description = recurringExpense.description, // Clean description without suffix
                createdAt = timestamp,
                source = ExpenseSource.RECURRING,
                recurringExpenseId = recurringExpense.id
        )
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun isSameWeek(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.WEEK_OF_YEAR) == cal2.get(Calendar.WEEK_OF_YEAR)
    }

    private fun isSameMonth(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH)
    }
}

/** WorkManager worker to process recurring expenses */
class RecurringExpenseWorker(context: Context, workerParams: WorkerParameters) :
        CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val scheduler = RecurrenceScheduler(applicationContext)
        return try {
            val result = scheduler.processRecurringExpenses()
            if (result.isSuccess) {
                val count = result.getOrNull() ?: 0
                if (count > 0) {
                    // Could show notification here if desired
                }
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            Result.failure()
        }
    }
}
