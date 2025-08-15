package com.example.budgetdeluminator

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.budgetdeluminator.data.entity.BudgetCategory
import com.example.budgetdeluminator.data.entity.Expense
import com.example.budgetdeluminator.data.model.CategoryWithExpenses
import com.example.budgetdeluminator.databinding.ActivityMainBinding
import com.example.budgetdeluminator.databinding.DialogAddExpenseBinding
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
import com.example.budgetdeluminator.utils.Calculator
import com.example.budgetdeluminator.utils.Constants
import com.example.budgetdeluminator.utils.CurrencyPreferences
import com.example.budgetdeluminator.utils.ErrorHandler
import com.example.budgetdeluminator.utils.ValidationResult
import com.example.budgetdeluminator.utils.ValidationUtils
import java.text.SimpleDateFormat
import java.util.*

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
    private lateinit var currencyPreferences: CurrencyPreferences
    private val dateFormat = SimpleDateFormat(Constants.DATE_FORMAT_DISPLAY, Locale.US)

    private lateinit var homeFragment: HomeFragment
    private lateinit var allExpensesFragment: AllExpensesFragment
    private lateinit var categoriesFragment: CategoriesFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currencyPreferences = CurrencyPreferences(this)

        setupFragments()
        setupBottomNavigation()
        setupClickListeners()

        // Add default categories if database is empty
        addDefaultCategoriesIfNeeded()
    }

    private fun setupFragments() {
        homeFragment = HomeFragment()
        allExpensesFragment = AllExpensesFragment()
        categoriesFragment = CategoriesFragment()

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
                    binding.fabAddExpense.show()
                    true
                }
                R.id.nav_all_expenses -> {
                    replaceFragment(allExpensesFragment)
                    supportActionBar?.title = "All Expenses"
                    binding.fabAddExpense.show()
                    true
                }
                R.id.nav_categories -> {
                    replaceFragment(categoriesFragment)
                    supportActionBar?.title = "Categories"
                    binding.fabAddExpense.hide()
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
        binding.fabAddExpense.setOnClickListener { startExpenseEntryFlow() }
        binding.fabAddExpense.setOnLongClickListener {
            startActivity(
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

            // Perform any pending calculation before getting the final value
            if (state.currentOperation != Calculator.Operation.NONE && !state.waitingForOperand) {
                calculator.calculate()
            }

            // Get the final calculated value
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

            // Perform any pending calculation before getting the final value
            if (state.currentOperation != Calculator.Operation.NONE && !state.waitingForOperand) {
                calculator.calculate()
            }

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
                                Toast.makeText(
                                                this,
                                                Constants.SUCCESS_EXPENSE_SAVED,
                                                Toast.LENGTH_SHORT
                                        )
                                        .show()
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
                                Toast.makeText(
                                                this,
                                                Constants.SUCCESS_EXPENSE_UPDATED,
                                                Toast.LENGTH_SHORT
                                        )
                                        .show()
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

        dialogBinding.tvExpensesTitle.text = "Expenses - ${category.name}"

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

        // Observe expenses for this category
        expensesViewModel.getExpensesByCategory(category.id).observe(this) { expenses ->
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

    private fun addDefaultCategoriesIfNeeded() {
        // Add sample categories if none exist
        homeViewModel.categoriesWithExpenses.observe(
                this,
                object : Observer<List<CategoryWithExpenses>> {
                    override fun onChanged(value: List<CategoryWithExpenses>) {
                        if (value.isEmpty()) {
                            // Add comprehensive default categories with appropriate colors
                            val defaultCategories =
                                    Constants.DEFAULT_CATEGORIES.map { defaultCategory ->
                                        BudgetCategory(
                                                name = defaultCategory.name,
                                                budgetLimit = defaultCategory.budgetLimit,
                                                color = defaultCategory.color
                                        )
                                    }

                            defaultCategories.forEach { category ->
                                categoriesViewModel.insertCategory(category)
                            }
                        }
                        // Remove observer after first call
                        homeViewModel.categoriesWithExpenses.removeObserver(this)
                    }
                }
        )
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
}
