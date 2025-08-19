package com.example.budgetdeluminator

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.budgetdeluminator.data.entity.BudgetCategory
import com.example.budgetdeluminator.data.entity.Expense
import com.example.budgetdeluminator.data.entity.RecurrenceType
import com.example.budgetdeluminator.data.entity.RecurringExpense
import com.example.budgetdeluminator.data.model.CategoryWithExpenses
import com.example.budgetdeluminator.databinding.ActivityMainBinding
import com.example.budgetdeluminator.databinding.DialogAddCategoryBinding
import com.example.budgetdeluminator.databinding.DialogAddExpenseBinding
import com.example.budgetdeluminator.databinding.DialogAddRecurringExpenseBinding
import com.example.budgetdeluminator.databinding.DialogCalculatorBinding
import com.example.budgetdeluminator.databinding.DialogCategorySelectionBinding
import com.example.budgetdeluminator.databinding.DialogExpensesListBinding
import com.example.budgetdeluminator.ui.adapter.CategorySelectionAdapter
import com.example.budgetdeluminator.ui.adapter.ExpenseAdapter
import com.example.budgetdeluminator.ui.categories.CategoriesFragment
import com.example.budgetdeluminator.ui.categories.CategoriesViewModel
import com.example.budgetdeluminator.ui.expenses.AllExpensesFragment
import com.example.budgetdeluminator.ui.expenses.ExpensesViewModel
import com.example.budgetdeluminator.ui.home.HomeFragment
import com.example.budgetdeluminator.ui.home.HomeViewModel
import com.example.budgetdeluminator.ui.recurring.RecurringExpensesFragment
import com.example.budgetdeluminator.ui.recurring.RecurringExpensesViewModel
import com.example.budgetdeluminator.utils.BiometricAuthHelper
import com.example.budgetdeluminator.utils.Calculator
import com.example.budgetdeluminator.utils.Constants
import com.example.budgetdeluminator.utils.CurrencyPreferences
import com.example.budgetdeluminator.utils.ErrorHandler
import com.example.budgetdeluminator.utils.RecurrenceScheduler
import com.example.budgetdeluminator.utils.ThemePreferences
import com.example.budgetdeluminator.utils.ValidationResult
import com.example.budgetdeluminator.utils.ValidationUtils
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch

/**
 * Main activity of the Budget Deluminator app.
 *
 * This activity serves as the primary container for the app's navigation and core functionality. It
 * implements a bottom navigation pattern with three main sections:
 * - Home: Overview of budget categories and spending
 * - All Expenses: Comprehensive list of all expenses
 * - Categories: Management of budget categories
 *
 * Key features:
 * - Professional calculator with edge case handling
 * - Material Design 3 components with neumorphic design elements
 * - Comprehensive input validation and error handling
 * - Offline-first architecture with Room database
 *
 * @author Budget Deluminator Team
 * @version 1.0
 */
class MainActivity :
        AppCompatActivity(),
        HomeFragment.OnCategoryClickListener,
        AllExpensesFragment.OnExpenseClickListener,
        AllExpensesFragment.OnExpenseDeleteListener {

    private lateinit var binding: ActivityMainBinding
    private val homeViewModel: HomeViewModel by viewModels()
    private val categoriesViewModel: CategoriesViewModel by viewModels()
    private val expensesViewModel: ExpensesViewModel by viewModels()
    private val recurringExpensesViewModel: RecurringExpensesViewModel by viewModels()
    private lateinit var currencyPreferences: CurrencyPreferences
    private val dateFormat = SimpleDateFormat(Constants.DATE_FORMAT_DISPLAY, Locale.US)

    private lateinit var homeFragment: HomeFragment
    private lateinit var allExpensesFragment: AllExpensesFragment
    private lateinit var categoriesFragment: CategoriesFragment
    private lateinit var recurringExpensesFragment: RecurringExpensesFragment

    // Track current fragment for FAB behavior
    private var currentFragmentType: FragmentType = FragmentType.HOME

    enum class FragmentType {
        HOME,
        ALL_EXPENSES,
        CATEGORIES,
        RECURRING_EXPENSES
    }

    private var hasShownBackgroundPermissionDialog = false

    private val settingsLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    result.data?.let { data ->
                        if (data.getBooleanExtra(
                                        com.example.budgetdeluminator.ui.settings.SettingsActivity
                                                .RESULT_CURRENCY_CHANGED,
                                        false
                                )
                        ) {
                            // Currency changed, refresh all fragments
                            refreshAllFragments()
                        }
                    }
                }
            }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply theme before calling super.onCreate()
        val themePreferences = ThemePreferences(this)
        themePreferences.applyTheme()

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currencyPreferences = CurrencyPreferences(this)

        // Set secure flag if biometric authentication is enabled
        BiometricAuthHelper.updateSecureFlag(this)

        // Check biometric authentication if enabled
        checkBiometricAuthentication()

        setupFragments()
        setupBottomNavigation()
        setupClickListeners()

        // Initialize recurring expense scheduling
        val recurrenceScheduler = RecurrenceScheduler(this)
        recurrenceScheduler.scheduleRecurringExpenseWork()
    }

    private fun setupFragments() {
        homeFragment = HomeFragment()
        allExpensesFragment = AllExpensesFragment()
        categoriesFragment = CategoriesFragment()
        recurringExpensesFragment = RecurringExpensesFragment()

        // Show home fragment by default
        supportFragmentManager
                .beginTransaction()
                .replace(binding.fragmentContainer.id, homeFragment)
                .commit()
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    replaceFragment(homeFragment)
                    supportActionBar?.title = "Budget Overview"
                    currentFragmentType = FragmentType.HOME
                    binding.fabAddExpense.show()
                    true
                }
                R.id.nav_all_expenses -> {
                    replaceFragment(allExpensesFragment)
                    supportActionBar?.title = "All Expenses"
                    currentFragmentType = FragmentType.ALL_EXPENSES
                    binding.fabAddExpense.show()
                    true
                }
                R.id.nav_categories -> {
                    replaceFragment(categoriesFragment)
                    supportActionBar?.title = "Categories"
                    currentFragmentType = FragmentType.CATEGORIES
                    binding.fabAddExpense.show()
                    true
                }
                R.id.nav_recurring_expenses -> {
                    replaceFragment(recurringExpensesFragment)
                    supportActionBar?.title = "Recurring Expenses"
                    currentFragmentType = FragmentType.RECURRING_EXPENSES
                    binding.fabAddExpense.show()
                    true
                }
                else -> false
            }
        }

        // Set home as selected by default
        binding.bottomNavigation.selectedItemId = R.id.nav_home
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager
                .beginTransaction()
                .replace(binding.fragmentContainer.id, fragment)
                .commit()
    }

    private fun setupClickListeners() {
        binding.fabAddExpense.setOnClickListener {
            when (currentFragmentType) {
                FragmentType.HOME, FragmentType.ALL_EXPENSES -> startExpenseEntryFlow()
                FragmentType.CATEGORIES -> showAddCategoryDialog()
                FragmentType.RECURRING_EXPENSES -> showAddRecurringExpenseDialog()
            }
        }
        binding.fabAddExpense.setOnLongClickListener {
            settingsLauncher.launch(
                    Intent(
                            this,
                            com.example.budgetdeluminator.ui.settings.SettingsActivity::class.java
                    )
            )
            true
        }
    }

    private fun startExpenseEntryFlow() {
        startExpenseEntryFlow(null)
    }

    private fun startExpenseEntryFlow(preselectedCategory: BudgetCategory?) {
        showCalculatorDialog(preselectedCategory)
    }

    private fun showCalculatorDialog() {
        showCalculatorDialog(null)
    }

    /**
     * Shows the professional calculator dialog for expense amount entry.
     *
     * Features:
     * - Handles division by zero and other arithmetic edge cases
     * - Supports chained calculations
     * - Proper error handling and validation
     * - Material Design 3 styling
     *
     * @param preselectedCategory Optional category to pre-select after amount entry
     */
    private fun showCalculatorDialog(preselectedCategory: BudgetCategory?) {
        val dialogBinding = DialogCalculatorBinding.inflate(LayoutInflater.from(this))
        val calculator = Calculator()

        val dialog = AlertDialog.Builder(this).setView(dialogBinding.root).create()

        fun updateDisplay() {
            val state = calculator.getState()
            dialogBinding.tvCalculatorDisplay.text = state.display
            dialogBinding.tvCalculatorExpression.text = state.expression

            // Show error state with appropriate styling
            if (state.hasError) {
                dialogBinding.tvCalculatorDisplay.setTextColor(getColor(R.color.error_color))
                Toast.makeText(this@MainActivity, state.errorMessage, Toast.LENGTH_SHORT).show()
            } else {
                dialogBinding.tvCalculatorDisplay.setTextColor(
                        getColor(R.color.md_theme_light_onBackground)
                )
            }
        }

        fun addNumber(number: String) {
            calculator.addDigit(number)
            updateDisplay()
        }

        fun addDecimal() {
            calculator.addDecimal()
            updateDisplay()
        }

        fun addOperator(op: String) {
            val operation =
                    when (op) {
                        "+" -> Calculator.Operation.ADD
                        "-" -> Calculator.Operation.SUBTRACT
                        "×" -> Calculator.Operation.MULTIPLY
                        "÷" -> Calculator.Operation.DIVIDE
                        else -> Calculator.Operation.NONE
                    }
            calculator.setOperation(operation)
            updateDisplay()
        }

        fun performCalculation() {
            calculator.calculate()
            updateDisplay()
        }

        fun backspace() {
            calculator.backspace()
            updateDisplay()
        }

        fun clear() {
            calculator.clear()
            updateDisplay()
        }

        // Set up number buttons
        dialogBinding.btn0.setOnClickListener { addNumber("0") }
        dialogBinding.btn1.setOnClickListener { addNumber("1") }
        dialogBinding.btn2.setOnClickListener { addNumber("2") }
        dialogBinding.btn3.setOnClickListener { addNumber("3") }
        dialogBinding.btn4.setOnClickListener { addNumber("4") }
        dialogBinding.btn5.setOnClickListener { addNumber("5") }
        dialogBinding.btn6.setOnClickListener { addNumber("6") }
        dialogBinding.btn7.setOnClickListener { addNumber("7") }
        dialogBinding.btn8.setOnClickListener { addNumber("8") }
        dialogBinding.btn9.setOnClickListener { addNumber("9") }
        dialogBinding.btn00.setOnClickListener { addNumber("00") }
        dialogBinding.btnDot.setOnClickListener { addDecimal() }

        // Operator buttons
        dialogBinding.btnAdd.setOnClickListener { addOperator("+") }
        dialogBinding.btnSubtract.setOnClickListener { addOperator("-") }
        dialogBinding.btnMultiply.setOnClickListener { addOperator("×") }
        dialogBinding.btnDivide.setOnClickListener { addOperator("÷") }

        // Clear and backspace buttons
        dialogBinding.btnClearCalculator.setOnClickListener { backspace() }
        dialogBinding.btnClearAll.setOnClickListener { clear() }

        // Action buttons
        dialogBinding.btnCancelCalculator.setOnClickListener { dialog.dismiss() }

        dialogBinding.btnOkayCalculator.setOnClickListener {
            val state = calculator.getState()

            if (state.hasError) {
                Toast.makeText(this, "Please fix the error first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Get the final calculated value (handles pending calculations internally)
            val finalValue = calculator.getCurrentValue()

            if (finalValue > 0) {
                dialog.dismiss()
                showExpenseFormDialog(finalValue, preselectedCategory)
            } else {
                Toast.makeText(
                                this,
                                "Please enter a valid amount greater than 0",
                                Toast.LENGTH_SHORT
                        )
                        .show()
            }
        }

        // Initialize display
        updateDisplay()

        dialog.show()
    }

    private fun showCalculatorDialogForEdit(
            initialAmount: Double,
            onAmountSelected: (Double) -> Unit
    ) {
        val dialogBinding = DialogCalculatorBinding.inflate(LayoutInflater.from(this))
        val calculator = Calculator()

        // Set initial amount if greater than 0
        if (initialAmount > 0) {
            val initialStr =
                    if (initialAmount == initialAmount.toLong().toDouble()) {
                        initialAmount.toLong().toString()
                    } else {
                        initialAmount.toString()
                    }
            // Input the initial amount digit by digit
            initialStr.forEach { char ->
                when (char) {
                    '.' -> calculator.addDecimal()
                    in '0'..'9' -> calculator.addDigit(char.toString())
                }
            }
        }

        val dialog = AlertDialog.Builder(this).setView(dialogBinding.root).create()

        fun updateDisplay() {
            val state = calculator.getState()
            dialogBinding.tvCalculatorDisplay.text = state.display
            dialogBinding.tvCalculatorExpression.text = state.expression

            // Show error state with appropriate styling
            if (state.hasError) {
                dialogBinding.tvCalculatorDisplay.setTextColor(getColor(R.color.error_color))
                Toast.makeText(this@MainActivity, state.errorMessage, Toast.LENGTH_SHORT).show()
            } else {
                dialogBinding.tvCalculatorDisplay.setTextColor(
                        getColor(R.color.md_theme_light_onBackground)
                )
            }
        }

        fun addNumber(number: String) {
            calculator.addDigit(number)
            updateDisplay()
        }

        fun addDecimal() {
            calculator.addDecimal()
            updateDisplay()
        }

        fun addOperator(op: String) {
            val operation =
                    when (op) {
                        "+" -> Calculator.Operation.ADD
                        "-" -> Calculator.Operation.SUBTRACT
                        "×" -> Calculator.Operation.MULTIPLY
                        "÷" -> Calculator.Operation.DIVIDE
                        else -> Calculator.Operation.NONE
                    }
            calculator.setOperation(operation)
            updateDisplay()
        }

        fun performCalculation() {
            calculator.calculate()
            updateDisplay()
        }

        fun backspace() {
            calculator.backspace()
            updateDisplay()
        }

        fun clear() {
            calculator.clear()
            updateDisplay()
        }

        // Set up number buttons
        dialogBinding.btn0.setOnClickListener { addNumber("0") }
        dialogBinding.btn1.setOnClickListener { addNumber("1") }
        dialogBinding.btn2.setOnClickListener { addNumber("2") }
        dialogBinding.btn3.setOnClickListener { addNumber("3") }
        dialogBinding.btn4.setOnClickListener { addNumber("4") }
        dialogBinding.btn5.setOnClickListener { addNumber("5") }
        dialogBinding.btn6.setOnClickListener { addNumber("6") }
        dialogBinding.btn7.setOnClickListener { addNumber("7") }
        dialogBinding.btn8.setOnClickListener { addNumber("8") }
        dialogBinding.btn9.setOnClickListener { addNumber("9") }
        dialogBinding.btn00.setOnClickListener { addNumber("00") }
        dialogBinding.btnDot.setOnClickListener { addDecimal() }

        // Operator buttons
        dialogBinding.btnAdd.setOnClickListener { addOperator("+") }
        dialogBinding.btnSubtract.setOnClickListener { addOperator("-") }
        dialogBinding.btnMultiply.setOnClickListener { addOperator("×") }
        dialogBinding.btnDivide.setOnClickListener { addOperator("÷") }

        // Clear and backspace buttons
        dialogBinding.btnClearCalculator.setOnClickListener { backspace() }
        dialogBinding.btnClearAll.setOnClickListener { clear() }

        // Action buttons
        dialogBinding.btnCancelCalculator.setOnClickListener { dialog.dismiss() }

        dialogBinding.btnOkayCalculator.setOnClickListener {
            val state = calculator.getState()

            if (state.hasError) {
                Toast.makeText(this, "Please fix the error first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Get the final calculated value (handles pending calculations internally)
            val finalValue = calculator.getCurrentValue()
            if (finalValue > 0) {
                dialog.dismiss()
                onAmountSelected(finalValue)
            } else {
                Toast.makeText(
                                this,
                                "Please enter a valid amount greater than 0",
                                Toast.LENGTH_SHORT
                        )
                        .show()
            }
        }

        // Initialize display
        updateDisplay()

        dialog.show()
    }

    private fun showExpenseFormDialog(amount: Double) {
        showExpenseFormDialog(amount, null)
    }

    /**
     * Shows the expense form dialog for creating new expenses.
     *
     * Features:
     * - Comprehensive input validation
     * - Date picker with smart defaults (Today/Yesterday)
     * - Category selection with quick access
     * - Amount editing via calculator
     * - Proper error handling and user feedback
     *
     * @param amount The expense amount (validated)
     * @param preselectedCategory Optional pre-selected category
     */
    private fun showExpenseFormDialog(amount: Double, preselectedCategory: BudgetCategory?) {
        val dialogBinding = DialogAddExpenseBinding.inflate(LayoutInflater.from(this))
        var selectedCategory: BudgetCategory? = preselectedCategory
        var selectedDate = System.currentTimeMillis() // Default to current time

        dialogBinding.tvExpenseAmount.setText(currencyPreferences.formatAmount(amount))
        updateDateDisplay(dialogBinding, selectedDate)

        val dialog = AlertDialog.Builder(this).setView(dialogBinding.root).create()

        // Close button
        dialogBinding.btnCloseExpenseForm.setOnClickListener { dialog.dismiss() }

        // Amount field - open calculator when clicked
        dialogBinding.tvExpenseAmount.setOnClickListener {
            // Get current amount from the field
            val currentAmountText = dialogBinding.tvExpenseAmount.text.toString()
            val currentAmount =
                    try {
                        currencyPreferences.parseAmount(currentAmountText)
                    } catch (e: Exception) {
                        amount // fallback to original amount parameter if parsing fails
                    }

            showCalculatorDialogForEdit(currentAmount) { newAmount ->
                dialogBinding.tvExpenseAmount.setText(currencyPreferences.formatAmount(newAmount))
            }
        }

        // Date picker
        dialogBinding.tvExpenseDate.setOnClickListener {
            showDatePicker { newDate ->
                selectedDate = newDate
                updateDateDisplay(dialogBinding, selectedDate)
            }
        }

        // Category selector
        dialogBinding.tvSelectedCategory.setOnClickListener {
            showCategorySelectionDialog { category ->
                selectedCategory = category
                dialogBinding.tvSelectedCategory.setText(category.name)
            }
        }

        // Save button
        dialogBinding.btnSaveExpense.setOnClickListener {
            ErrorHandler.safeExecute(this, dialogBinding.root, "Failed to save expense") {
                selectedCategory?.let { category ->
                    val description =
                            ValidationUtils.sanitizeInput(
                                    dialogBinding.etExpenseNote.text.toString()
                            )

                    // Validate all inputs
                    val amountValidation = ValidationUtils.validateExpenseAmount(amount)
                    val descriptionValidation =
                            ValidationUtils.validateExpenseDescription(description)
                    val dateValidation = ValidationUtils.validateExpenseDate(selectedDate)

                    when {
                        !amountValidation.isSuccess() -> {
                            ErrorHandler.handleValidationError(
                                    this,
                                    amountValidation,
                                    dialogBinding.root
                            )
                        }
                        !descriptionValidation.isSuccess() -> {
                            ErrorHandler.handleValidationError(
                                    this,
                                    descriptionValidation,
                                    dialogBinding.root
                            )
                            dialogBinding.etExpenseNote.error =
                                    descriptionValidation.getErrorMessage()
                        }
                        !dateValidation.isSuccess() -> {
                            ErrorHandler.handleValidationError(
                                    this,
                                    dateValidation,
                                    dialogBinding.root
                            )
                        }
                        else -> {
                            val expense =
                                    Expense(
                                            categoryId = category.id,
                                            amount = amount,
                                            description = description,
                                            createdAt = selectedDate
                                    )

                            ErrorHandler.safeDatabaseExecute(
                                    this,
                                    "saving expense",
                                    dialogBinding.root
                            ) {
                                expensesViewModel.insertExpense(expense)
                                dialog.dismiss()
                            }
                        }
                    }
                }
                        ?: run {
                            ErrorHandler.handleValidationError(
                                    this,
                                    ValidationResult.Error("Please select a category"),
                                    dialogBinding.root
                            )
                        }
            }
        }

        // Set preselected category or auto-select first category if available
        if (preselectedCategory != null) {
            dialogBinding.tvSelectedCategory.setText(preselectedCategory.name)
        } else {
            categoriesViewModel.allCategories.observe(this) { categories ->
                if (categories.isNotEmpty() && selectedCategory == null) {
                    selectedCategory = categories[0]
                    dialogBinding.tvSelectedCategory.setText(categories[0].name)
                }
            }
        }

        dialog.show()
    }

    private fun showDatePicker(onDateSelected: (Long) -> Unit) {
        val calendar = java.util.Calendar.getInstance()
        val year = calendar.get(java.util.Calendar.YEAR)
        val month = calendar.get(java.util.Calendar.MONTH)
        val day = calendar.get(java.util.Calendar.DAY_OF_MONTH)

        val datePickerDialog =
                android.app.DatePickerDialog(
                        this,
                        { _, selectedYear, selectedMonth, selectedDay ->
                            val selectedCalendar = java.util.Calendar.getInstance()
                            selectedCalendar.set(selectedYear, selectedMonth, selectedDay)
                            onDateSelected(selectedCalendar.timeInMillis)
                        },
                        year,
                        month,
                        day
                )
        datePickerDialog.show()
    }

    private fun updateDateDisplay(dialogBinding: DialogAddExpenseBinding, timestamp: Long) {
        val date = java.util.Date(timestamp)
        val today = java.util.Date()
        val yesterday = java.util.Date(today.time - 24 * 60 * 60 * 1000)

        dialogBinding.tvExpenseDate.setText(
                when {
                    isSameDay(date, today) -> "Today"
                    isSameDay(date, yesterday) -> "Yesterday"
                    else -> dateFormat.format(date)
                }
        )
    }

    private fun isSameDay(date1: java.util.Date, date2: java.util.Date): Boolean {
        val cal1 = java.util.Calendar.getInstance().apply { time = date1 }
        val cal2 = java.util.Calendar.getInstance().apply { time = date2 }
        return cal1.get(java.util.Calendar.YEAR) == cal2.get(java.util.Calendar.YEAR) &&
                cal1.get(java.util.Calendar.DAY_OF_YEAR) == cal2.get(java.util.Calendar.DAY_OF_YEAR)
    }

    private fun showCategorySelectionDialog(onCategorySelected: (BudgetCategory) -> Unit) {
        val dialogBinding = DialogCategorySelectionBinding.inflate(LayoutInflater.from(this))

        val dialog = AlertDialog.Builder(this).setView(dialogBinding.root).create()

        val categorySelectionAdapter = CategorySelectionAdapter { category ->
            onCategorySelected(category)
            dialog.dismiss()
        }

        dialogBinding.recyclerViewCategorySelection.apply {
            adapter = categorySelectionAdapter
            layoutManager = LinearLayoutManager(this@MainActivity)
        }

        categoriesViewModel.allCategories.observe(this) { categories ->
            categorySelectionAdapter.submitList(categories)
        }

        dialogBinding.btnAddCategoryFromSelection.setOnClickListener {
            dialog.dismiss()
            // Navigate to categories fragment
            replaceFragment(categoriesFragment)
            supportActionBar?.title = "Categories"
            binding.bottomNavigation.selectedItemId = R.id.nav_categories
            binding.fabAddExpense.hide()
        }

        dialog.show()
    }

    private fun showEditExpenseDialog(expense: Expense) {
        val dialogBinding = DialogAddExpenseBinding.inflate(LayoutInflater.from(this))
        var selectedCategory: BudgetCategory? = null
        var selectedDate = expense.createdAt

        // Set initial values
        dialogBinding.tvExpenseAmount.setText(currencyPreferences.formatAmount(expense.amount))
        dialogBinding.etExpenseNote.setText(expense.description)
        updateDateDisplay(dialogBinding, selectedDate)

        val dialog = AlertDialog.Builder(this).setView(dialogBinding.root).create()

        // Update the layout's title instead of AlertDialog title
        dialogBinding.tvExpenseFormTitle.text = "Edit Expense"

        // Close button
        dialogBinding.btnCloseExpenseForm.setOnClickListener { dialog.dismiss() }

        // Date picker
        dialogBinding.tvExpenseDate.setOnClickListener {
            showDatePicker { newDate ->
                selectedDate = newDate
                updateDateDisplay(dialogBinding, selectedDate)
            }
        }

        // Find and set the current category
        categoriesViewModel.allCategories.observe(this) { categories ->
            val currentCategory = categories.find { it.id == expense.categoryId }
            currentCategory?.let { category ->
                selectedCategory = category
                dialogBinding.tvSelectedCategory.setText(category.name)
            }
        }

        // Category selector
        dialogBinding.tvSelectedCategory.setOnClickListener {
            showCategorySelectionDialog { category ->
                selectedCategory = category
                dialogBinding.tvSelectedCategory.setText(category.name)
            }
        }

        // Amount editing - make it clickable to open calculator
        dialogBinding.tvExpenseAmount.setOnClickListener {
            showCalculatorDialogForEdit(expense.amount) { newAmount: Double ->
                dialogBinding.tvExpenseAmount.setText(currencyPreferences.formatAmount(newAmount))
            }
        }

        // Save button
        dialogBinding.btnSaveExpense.setOnClickListener {
            ErrorHandler.safeExecute(this, dialogBinding.root, "Failed to update expense") {
                selectedCategory?.let { category ->
                    val description =
                            ValidationUtils.sanitizeInput(
                                    dialogBinding.etExpenseNote.text.toString()
                            )
                    val amountText = dialogBinding.tvExpenseAmount.text.toString()

                    // Extract and validate amount
                    val amount =
                            try {
                                currencyPreferences.parseAmount(amountText)
                            } catch (e: Exception) {
                                ErrorHandler.handleError(
                                        this,
                                        e,
                                        "Invalid amount format",
                                        dialogBinding.root
                                )
                                return@safeExecute
                            }

                    // Validate all inputs
                    val amountValidation = ValidationUtils.validateExpenseAmount(amount)
                    val descriptionValidation =
                            ValidationUtils.validateExpenseDescription(description)
                    val dateValidation = ValidationUtils.validateExpenseDate(selectedDate)

                    when {
                        !amountValidation.isSuccess() -> {
                            ErrorHandler.handleValidationError(
                                    this,
                                    amountValidation,
                                    dialogBinding.root
                            )
                        }
                        !descriptionValidation.isSuccess() -> {
                            ErrorHandler.handleValidationError(
                                    this,
                                    descriptionValidation,
                                    dialogBinding.root
                            )
                            dialogBinding.etExpenseNote.error =
                                    descriptionValidation.getErrorMessage()
                        }
                        !dateValidation.isSuccess() -> {
                            ErrorHandler.handleValidationError(
                                    this,
                                    dateValidation,
                                    dialogBinding.root
                            )
                        }
                        else -> {
                            val updatedExpense =
                                    expense.copy(
                                            categoryId = category.id,
                                            amount = amount,
                                            description = description,
                                            createdAt = selectedDate
                                    )

                            ErrorHandler.safeDatabaseExecute(
                                    this,
                                    "updating expense",
                                    dialogBinding.root
                            ) {
                                expensesViewModel.updateExpense(updatedExpense)
                                dialog.dismiss()
                            }
                        }
                    }
                }
                        ?: run {
                            ErrorHandler.handleValidationError(
                                    this,
                                    ValidationResult.Error("Please select a category"),
                                    dialogBinding.root
                            )
                        }
            }
        }

        dialog.show()
    }

    private fun showExpensesDialog(categoryWithExpenses: CategoryWithExpenses) {
        val dialogBinding = DialogExpensesListBinding.inflate(LayoutInflater.from(this))
        val category = categoryWithExpenses.category

        // Get the selected month from HomeViewModel to filter expenses
        val selectedMonth =
                homeViewModel.selectedMonth.value
                        ?: com.example.budgetdeluminator.utils.DateUtils.getCurrentMonthYear()
        val monthName =
                "${com.example.budgetdeluminator.utils.DateUtils.getMonthName(selectedMonth.first)} ${selectedMonth.second}"

        dialogBinding.tvExpensesTitle.text = "${category.name} Expenses - $monthName"

        val expenseAdapter =
                ExpenseAdapter(
                        this,
                        onExpenseClick = { expense -> showEditExpenseDialog(expense) },
                        onExpenseLongClick = { expense -> showDeleteExpenseDialog(expense) }
                )

        dialogBinding.recyclerViewExpenses.apply {
            adapter = expenseAdapter
            layoutManager = LinearLayoutManager(this@MainActivity)
        }

        val dialog = AlertDialog.Builder(this).setView(dialogBinding.root).create()

        // Calculate date range for the selected month
        val monthStart =
                com.example.budgetdeluminator.utils.DateUtils.getMonthStart(
                        selectedMonth.second,
                        selectedMonth.first
                )
        val monthEnd =
                com.example.budgetdeluminator.utils.DateUtils.getMonthEnd(
                        selectedMonth.second,
                        selectedMonth.first
                )

        // Observe expenses for this category within the selected month
        expensesViewModel.getExpensesByCategoryInDateRange(category.id, monthStart, monthEnd)
                .observe(this) { expenses ->
                    if (expenses.isEmpty()) {
                        dialogBinding.recyclerViewExpenses.visibility = android.view.View.GONE
                        dialogBinding.tvNoExpenses.visibility = android.view.View.VISIBLE
                    } else {
                        dialogBinding.recyclerViewExpenses.visibility = android.view.View.VISIBLE
                        dialogBinding.tvNoExpenses.visibility = android.view.View.GONE
                        expenseAdapter.submitList(expenses)
                    }
                }

        dialogBinding.fabAddExpenseFromList.setOnClickListener {
            dialog.dismiss()
            startExpenseEntryFlow(category)
        }

        dialogBinding.btnCloseExpenses.setOnClickListener { dialog.dismiss() }

        dialog.show()
    }

    private fun showDeleteExpenseDialog(expense: Expense) {
        AlertDialog.Builder(this)
                .setTitle("Delete Expense")
                .setMessage(
                        "Are you sure you want to delete this expense? This action cannot be undone."
                )
                .setPositiveButton("Delete") { _, _ ->
                    ErrorHandler.safeDatabaseExecute(this, "deleting expense") {
                        expensesViewModel.deleteExpense(expense)
                        Toast.makeText(this, Constants.SUCCESS_EXPENSE_DELETED, Toast.LENGTH_SHORT)
                                .show()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
    }

    // Fragment interface implementations
    override fun onCategoryClicked(categoryWithExpenses: CategoryWithExpenses) {
        showExpensesDialog(categoryWithExpenses)
    }

    override fun onExpenseClicked(expense: Expense) {
        showEditExpenseDialog(expense)
    }

    override fun onExpenseDeleteRequested(expense: Expense) {
        androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Delete Expense")
                .setMessage(
                        "Are you sure you want to delete this expense? This action cannot be undone."
                )
                .setPositiveButton("Delete") { _, _ ->
                    ErrorHandler.safeDatabaseExecute(this, "deleting expense") {
                        expensesViewModel.deleteExpense(expense)
                        Toast.makeText(this, Constants.SUCCESS_EXPENSE_DELETED, Toast.LENGTH_SHORT)
                                .show()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
    }

    // Category Dialog Methods
    private fun showAddCategoryDialog(categoryToEdit: BudgetCategory? = null) {
        val dialogBinding = DialogAddCategoryBinding.inflate(LayoutInflater.from(this))

        // Available color options
        val colorOptions =
                arrayOf(
                        "#4CAF50",
                        "#2196F3",
                        "#FF9800",
                        "#E91E63",
                        "#9C27B0",
                        "#FF5722",
                        "#00BCD4",
                        "#3F51B5",
                        "#FFC107",
                        "#795548"
                )
        var selectedColor = categoryToEdit?.color?.takeIf { it.isNotEmpty() } ?: "#4CAF50"

        // Pre-fill if editing
        categoryToEdit?.let { category ->
            dialogBinding.etCategoryName.setText(category.name)
            dialogBinding.etBudgetLimit.setText(category.budgetLimit.toString())
            selectedColor = category.color
        }

        // Setup color picker
        val colorViews =
                arrayOf(
                        dialogBinding.colorOption1,
                        dialogBinding.colorOption2,
                        dialogBinding.colorOption3,
                        dialogBinding.colorOption4,
                        dialogBinding.colorOption5,
                        dialogBinding.colorOption6,
                        dialogBinding.colorOption7,
                        dialogBinding.colorOption8,
                        dialogBinding.colorOption9,
                        dialogBinding.colorOption10
                )

        fun updateColorSelection() {
            colorViews.forEachIndexed { index, colorView ->
                val isSelected = colorOptions[index] == selectedColor
                colorView.alpha = if (isSelected) 1.0f else 0.5f
                colorView.scaleX = if (isSelected) 1.1f else 1.0f
                colorView.scaleY = if (isSelected) 1.1f else 1.0f
            }
        }

        updateColorSelection()

        colorViews.forEachIndexed { index, colorView ->
            colorView.setOnClickListener {
                selectedColor = colorOptions[index]
                updateColorSelection()
            }
        }

        val dialog = AlertDialog.Builder(this).setView(dialogBinding.root).create()

        dialogBinding.btnSave.setOnClickListener {
            val name = dialogBinding.etCategoryName.text.toString().trim()
            val budgetText = dialogBinding.etBudgetLimit.text.toString().trim()

            if (name.isEmpty()) {
                Toast.makeText(this, "Please enter a category name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val budgetLimit = budgetText.toDoubleOrNull()
            if (budgetLimit == null || budgetLimit <= 0) {
                Toast.makeText(this, "Please enter a valid budget limit", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val category =
                    categoryToEdit?.copy(
                            name = name,
                            budgetLimit = budgetLimit,
                            color = selectedColor
                    )
                            ?: BudgetCategory(
                                    name = name,
                                    budgetLimit = budgetLimit,
                                    color = selectedColor
                            )

            lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.STARTED) {
                    try {
                        if (categoryToEdit != null) {
                            categoriesViewModel.updateCategory(category)
                        } else {
                            categoriesViewModel.insertCategory(category)
                        }
                        dialog.dismiss()
                    } catch (e: Exception) {
                        Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_SHORT)
                                .show()
                    }
                }
            }
        }

        dialogBinding.btnCancel.setOnClickListener { dialog.dismiss() }

        dialog.show()
    }

    // Recurring Expense Dialog Methods
    fun showAddRecurringExpenseDialog() {
        showRecurringExpenseDialog(null)
    }

    fun showEditRecurringExpenseDialog(recurringExpense: RecurringExpense) {
        showRecurringExpenseDialog(recurringExpense)
    }

    private fun showRecurringExpenseDialog(recurringExpense: RecurringExpense?) {
        val dialogBinding = DialogAddRecurringExpenseBinding.inflate(LayoutInflater.from(this))
        var selectedCategory: BudgetCategory? = null
        var selectedRecurrenceType: RecurrenceType? = null
        var selectedRecurrenceValue: Int = 0
        var currentAmount: Double = 0.0

        val dialog = AlertDialog.Builder(this).setView(dialogBinding.root).create()

        // Set title based on edit/add mode
        dialogBinding.tvRecurringExpenseFormTitle.text =
                if (recurringExpense != null) "Edit Recurring Expense" else "Add Recurring Expense"

        // Initialize with existing data if editing
        recurringExpense?.let { expense ->
            currentAmount = expense.amount
            selectedRecurrenceType = expense.recurrenceType
            selectedRecurrenceValue = expense.recurrenceValue

            dialogBinding.tvRecurringExpenseAmount.setText(
                    currencyPreferences.formatAmount(expense.amount)
            )
            dialogBinding.etRecurringExpenseNote.setText(expense.description)
            dialogBinding.tvRecurrenceType.setText(
                    getRecurrenceTypeDisplayName(expense.recurrenceType)
            )
            updateRecurrenceValueDisplay(
                    dialogBinding,
                    expense.recurrenceType,
                    expense.recurrenceValue
            )

            // Find and set category
            categoriesViewModel.allCategories.observe(this) { categories ->
                selectedCategory = categories.find { it.id == expense.categoryId }
                selectedCategory?.let { dialogBinding.tvSelectedRecurringCategory.setText(it.name) }
            }
        }

        // Close button
        dialogBinding.btnCloseRecurringExpenseForm.setOnClickListener { dialog.dismiss() }

        // Amount field - open calculator
        dialogBinding.tvRecurringExpenseAmount.setOnClickListener {
            val currentAmountText = dialogBinding.tvRecurringExpenseAmount.text.toString()
            val amount =
                    try {
                        currencyPreferences.parseAmount(currentAmountText)
                    } catch (e: Exception) {
                        0.0
                    }

            showCalculatorDialogForEdit(amount) { newAmount ->
                currentAmount = newAmount
                dialogBinding.tvRecurringExpenseAmount.setText(
                        currencyPreferences.formatAmount(newAmount)
                )
            }
        }

        // Recurrence type selector
        dialogBinding.tvRecurrenceType.setOnClickListener {
            showRecurrenceTypeSelectionDialog { recurrenceType ->
                selectedRecurrenceType = recurrenceType
                dialogBinding.tvRecurrenceType.setText(getRecurrenceTypeDisplayName(recurrenceType))

                // Show recurrence value picker
                dialogBinding.layoutRecurrenceValue.visibility = View.VISIBLE
                dialogBinding.tvRecurrenceValue.setText(
                        "Select ${getRecurrenceValueLabel(recurrenceType)}"
                )
                selectedRecurrenceValue = 0 // Reset value when type changes
            }
        }

        // Recurrence value selector
        dialogBinding.tvRecurrenceValue.setOnClickListener {
            selectedRecurrenceType?.let { type ->
                showRecurrenceValueSelectionDialog(type) { value ->
                    selectedRecurrenceValue = value
                    updateRecurrenceValueDisplay(dialogBinding, type, value)
                }
            }
        }

        // Category selector
        dialogBinding.tvSelectedRecurringCategory.setOnClickListener {
            showCategorySelectionDialog { category ->
                selectedCategory = category
                dialogBinding.tvSelectedRecurringCategory.setText(category.name)
            }
        }

        // Save button
        dialogBinding.btnSaveRecurringExpense.setOnClickListener {
            ErrorHandler.safeExecute(this, dialogBinding.root, "Failed to save recurring expense") {
                selectedCategory?.let { category ->
                    selectedRecurrenceType?.let { recurrenceType ->
                        val description =
                                ValidationUtils.sanitizeInput(
                                        dialogBinding.etRecurringExpenseNote.text.toString()
                                )

                        // Validate inputs
                        val amountValidation = ValidationUtils.validateExpenseAmount(currentAmount)
                        val descriptionValidation =
                                ValidationUtils.validateExpenseDescription(description)

                        when {
                            !amountValidation.isSuccess() -> {
                                ErrorHandler.handleValidationError(
                                        this,
                                        amountValidation,
                                        dialogBinding.root
                                )
                            }
                            !descriptionValidation.isSuccess() -> {
                                ErrorHandler.handleValidationError(
                                        this,
                                        descriptionValidation,
                                        dialogBinding.root
                                )
                                dialogBinding.etRecurringExpenseNote.error =
                                        descriptionValidation.getErrorMessage()
                            }
                            selectedRecurrenceValue == 0 -> {
                                ErrorHandler.handleValidationError(
                                        this,
                                        ValidationResult.Error(
                                                "Please select ${getRecurrenceValueLabel(recurrenceType)}"
                                        ),
                                        dialogBinding.root
                                )
                            }
                            else -> {
                                if (recurringExpense != null) {
                                    // Update existing
                                    val updatedExpense =
                                            recurringExpense.copy(
                                                    categoryId = category.id,
                                                    amount = currentAmount,
                                                    description = description,
                                                    recurrenceType = recurrenceType,
                                                    recurrenceValue = selectedRecurrenceValue
                                            )
                                    recurringExpensesViewModel.updateRecurringExpense(
                                            updatedExpense
                                    )
                                } else {
                                    // Create new
                                    val newExpense =
                                            RecurringExpense(
                                                    categoryId = category.id,
                                                    amount = currentAmount,
                                                    description = description,
                                                    recurrenceType = recurrenceType,
                                                    recurrenceValue = selectedRecurrenceValue
                                            )
                                    recurringExpensesViewModel.insertRecurringExpense(newExpense)

                                    // Check for background processing permissions after creating
                                    // first recurring expense
                                    checkBackgroundProcessingPermissionsAfterCreation()
                                }
                                dialog.dismiss()
                            }
                        }
                    }
                            ?: run {
                                ErrorHandler.handleValidationError(
                                        this,
                                        ValidationResult.Error("Please select recurrence type"),
                                        dialogBinding.root
                                )
                            }
                }
                        ?: run {
                            ErrorHandler.handleValidationError(
                                    this,
                                    ValidationResult.Error("Please select a category"),
                                    dialogBinding.root
                            )
                        }
            }
        }

        // Auto-select first category if adding new
        if (recurringExpense == null) {
            categoriesViewModel.allCategories.observe(this) { categories ->
                if (categories.isNotEmpty() && selectedCategory == null) {
                    selectedCategory = categories[0]
                    dialogBinding.tvSelectedRecurringCategory.setText(categories[0].name)
                }
            }
        }

        dialog.show()
    }

    private fun showRecurrenceTypeSelectionDialog(onTypeSelected: (RecurrenceType) -> Unit) {
        val types = arrayOf("Daily", "Weekly", "Monthly")
        AlertDialog.Builder(this)
                .setTitle("Select Recurrence Type")
                .setItems(types) { _, which ->
                    val recurrenceType =
                            when (which) {
                                0 -> RecurrenceType.DAILY
                                1 -> RecurrenceType.WEEKLY
                                2 -> RecurrenceType.MONTHLY
                                else -> RecurrenceType.DAILY
                            }
                    onTypeSelected(recurrenceType)
                }
                .show()
    }

    private fun showRecurrenceValueSelectionDialog(
            type: RecurrenceType,
            onValueSelected: (Int) -> Unit
    ) {
        when (type) {
            RecurrenceType.DAILY -> showTimePickerDialog(onValueSelected)
            RecurrenceType.WEEKLY -> showDayOfWeekPickerDialog(onValueSelected)
            RecurrenceType.MONTHLY -> showDayOfMonthPickerDialog(onValueSelected)
        }
    }

    private fun showTimePickerDialog(onTimeSelected: (Int) -> Unit) {
        val timePickerDialog =
                android.app.TimePickerDialog(
                        this,
                        { _, hourOfDay, _ -> onTimeSelected(hourOfDay) },
                        9, // Default to 9 AM
                        0,
                        false // 12-hour format
                )
        timePickerDialog.show()
    }

    private fun showDayOfWeekPickerDialog(onDaySelected: (Int) -> Unit) {
        val days =
                arrayOf(
                        "Sunday",
                        "Monday",
                        "Tuesday",
                        "Wednesday",
                        "Thursday",
                        "Friday",
                        "Saturday"
                )
        AlertDialog.Builder(this)
                .setTitle("Select Day of Week")
                .setItems(days) { _, which ->
                    onDaySelected(which + 1) // Calendar.SUNDAY = 1
                }
                .show()
    }

    private fun showDayOfMonthPickerDialog(onDaySelected: (Int) -> Unit) {
        val days = (1..31).map { "${it}${getDayOrdinalSuffix(it)}" }.toTypedArray()
        AlertDialog.Builder(this)
                .setTitle("Select Day of Month")
                .setItems(days) { _, which -> onDaySelected(which + 1) }
                .show()
    }

    private fun getRecurrenceTypeDisplayName(type: RecurrenceType): String {
        return when (type) {
            RecurrenceType.DAILY -> "Daily"
            RecurrenceType.WEEKLY -> "Weekly"
            RecurrenceType.MONTHLY -> "Monthly"
        }
    }

    private fun getRecurrenceValueLabel(type: RecurrenceType): String {
        return when (type) {
            RecurrenceType.DAILY -> "time"
            RecurrenceType.WEEKLY -> "day of week"
            RecurrenceType.MONTHLY -> "day of month"
        }
    }

    private fun updateRecurrenceValueDisplay(
            dialogBinding: DialogAddRecurringExpenseBinding,
            type: RecurrenceType,
            value: Int
    ) {
        val displayText =
                when (type) {
                    RecurrenceType.DAILY -> {
                        val calendar =
                                Calendar.getInstance().apply {
                                    set(Calendar.HOUR_OF_DAY, value)
                                    set(Calendar.MINUTE, 0)
                                }
                        val timeFormat = SimpleDateFormat("h:mm a", Locale.US)
                        timeFormat.format(calendar.time)
                    }
                    RecurrenceType.WEEKLY -> {
                        when (value) {
                            1 -> "Sunday"
                            2 -> "Monday"
                            3 -> "Tuesday"
                            4 -> "Wednesday"
                            5 -> "Thursday"
                            6 -> "Friday"
                            7 -> "Saturday"
                            else -> "Invalid day"
                        }
                    }
                    RecurrenceType.MONTHLY -> {
                        "$value${getDayOrdinalSuffix(value)}"
                    }
                }
        dialogBinding.tvRecurrenceValue.setText(displayText)
        dialogBinding.layoutRecurrenceValue.visibility = View.VISIBLE
    }

    private fun getDayOrdinalSuffix(day: Int): String {
        return when {
            day in 11..13 -> "th"
            day % 10 == 1 -> "st"
            day % 10 == 2 -> "nd"
            day % 10 == 3 -> "rd"
            else -> "th"
        }
    }

    // Background Processing & Fallback Methods
    private fun checkBackgroundProcessingPermissionsAfterCreation() {
        val recurrenceScheduler = RecurrenceScheduler(this)
        val prefs = getSharedPreferences("background_permissions", MODE_PRIVATE)
        val hasAskedBefore = prefs.getBoolean("has_asked_background_permission", false)

        // Only show dialog if we haven't asked before and background processing is not allowed
        if (!hasShownBackgroundPermissionDialog &&
                        !hasAskedBefore &&
                        !recurrenceScheduler.isBackgroundProcessingAllowed()
        ) {
            hasShownBackgroundPermissionDialog = true
            showBackgroundPermissionDialog()
        }
    }

    private fun showBackgroundPermissionDialog() {
        val prefs = getSharedPreferences("background_permissions", MODE_PRIVATE)

        AlertDialog.Builder(this)
                .setTitle("Background Processing")
                .setMessage(
                        "You've created a recurring expense! To automatically add future recurring expenses, this app needs permission to run in the background. Would you like to enable this?"
                )
                .setPositiveButton("Enable") { _, _ ->
                    // Mark as asked so we don't show again
                    prefs.edit().putBoolean("has_asked_background_permission", true).apply()

                    val recurrenceScheduler = RecurrenceScheduler(this)
                    recurrenceScheduler.getBatteryOptimizationIntent()?.let { intent ->
                        try {
                            startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(
                                            this,
                                            "Unable to open battery settings",
                                            Toast.LENGTH_SHORT
                                    )
                                    .show()
                        }
                    }
                }
                .setNegativeButton("Manual Mode") { _, _ ->
                    // Mark as asked so we don't show again
                    prefs.edit().putBoolean("has_asked_background_permission", true).apply()

                    Toast.makeText(
                                    this,
                                    "You can manually sync recurring expenses from the Recurring tab",
                                    Toast.LENGTH_LONG
                            )
                            .show()
                }
                .setNeutralButton("Later") { _, _ ->
                    // Don't mark as asked for "Later" - user might want to see it again next time
                }
                .show()
    }

    override fun onResume() {
        super.onResume()

        // Manual fallback: Process recurring expenses when app is opened
        // This ensures expenses are generated even if background processing is restricted
        processRecurringExpensesOnResume()
    }

    private fun processRecurringExpensesOnResume() {
        val recurrenceScheduler = RecurrenceScheduler(this)

        // Only process if background processing is restricted or hasn't run recently
        if (!recurrenceScheduler.isBackgroundProcessingAllowed()) {
            lifecycleScope.launch {
                try {
                    val generatedCount = recurrenceScheduler.processRecurringExpensesManually()
                    if (generatedCount > 0) {
                        Toast.makeText(
                                        this@MainActivity,
                                        "Generated $generatedCount recurring expense${if (generatedCount == 1) "" else "s"}",
                                        Toast.LENGTH_SHORT
                                )
                                .show()
                    }
                } catch (e: Exception) {
                    // Silently handle errors - don't bother user with technical issues
                }
            }
        }
    }

    private fun checkBiometricAuthentication() {
        // Check if biometric authentication is enabled
        if (!BiometricAuthHelper.isBiometricEnabled(this)) {
            return
        }

        // Check if biometric authentication is available
        if (!BiometricAuthHelper.isBiometricAvailable(this)) {
            // Biometric was enabled but is no longer available
            // Disable it automatically and show a message
            BiometricAuthHelper.setBiometricEnabled(this, false)
            Toast.makeText(
                            this,
                            "Biometric authentication was disabled because it's no longer available on this device",
                            Toast.LENGTH_LONG
                    )
                    .show()
            return
        }

        // Show biometric authentication prompt
        BiometricAuthHelper.authenticate(
                this,
                onSuccess = {
                    // Authentication successful, continue with app
                },
                onError = { error ->
                    // Authentication failed, show error and close app
                    AlertDialog.Builder(this)
                            .setTitle("Authentication Failed")
                            .setMessage(
                                    "Biometric authentication failed: $error\n\nThe app will now close for security."
                            )
                            .setPositiveButton("OK") { _, _ -> finish() }
                            .setCancelable(false)
                            .show()
                },
                onCancel = {
                    // User cancelled authentication, close app
                    AlertDialog.Builder(this)
                            .setTitle("Authentication Required")
                            .setMessage("Biometric authentication is required to access this app.")
                            .setPositiveButton("Retry") { _, _ -> checkBiometricAuthentication() }
                            .setNegativeButton("Exit") { _, _ -> finish() }
                            .setCancelable(false)
                            .show()
                }
        )
    }

    private fun refreshAllFragments() {
        // Refresh currency preferences instance
        currencyPreferences = CurrencyPreferences(this)

        // Get current fragment and refresh it using safer methods
        val currentFragment = supportFragmentManager.findFragmentById(binding.fragmentContainer.id)
        currentFragment?.let { fragment ->
            when (fragment) {
                is HomeFragment -> {
                    // Refresh home fragment's currency display
                    fragment.refreshCurrency()
                }
                is AllExpensesFragment -> {
                    // Refresh expenses fragment's currency display
                    fragment.refreshCurrency()
                }
                is CategoriesFragment -> {
                    // Refresh categories fragment's currency display
                    fragment.refreshCurrency()
                }
                is RecurringExpensesFragment -> {
                    // Refresh recurring expenses fragment's currency display
                    fragment.refreshCurrency()
                }
                else -> {
                    // For unknown fragment types, use the safer detach/attach as fallback
                    supportFragmentManager
                            .beginTransaction()
                            .detach(fragment)
                            .attach(fragment)
                            .commit()
                }
            }
        }
    }
}
