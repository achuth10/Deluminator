package com.example.budgetdeluminator

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
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
import com.example.budgetdeluminator.utils.CurrencyPreferences
import java.text.SimpleDateFormat
import java.util.*

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
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)

    private lateinit var homeFragment: HomeFragment
    private lateinit var allExpensesFragment: AllExpensesFragment
    private lateinit var categoriesFragment: CategoriesFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currencyPreferences = CurrencyPreferences(this)
        setupToolbar()
        setupFragments()
        setupBottomNavigation()
        setupClickListeners()

        // Add default categories if database is empty
        addDefaultCategoriesIfNeeded()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
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
                    supportActionBar?.title = "Manage Categories"
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
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                startActivity(
                        Intent(
                                this,
                                com.example.budgetdeluminator.ui.settings.SettingsActivity::class
                                        .java
                        )
                )
                true
            }
            else -> super.onOptionsItemSelected(item)
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

    private fun showCalculatorDialog(preselectedCategory: BudgetCategory?) {
        val dialogBinding = DialogCalculatorBinding.inflate(LayoutInflater.from(this))
        var currentDisplay = "0"
        var expressionDisplay = ""
        var storedValue = 0.0
        var currentOperator: String? = null
        var waitingForOperand = true
        var justCalculated = false

        val dialog = AlertDialog.Builder(this).setView(dialogBinding.root).create()

        fun updateDisplay() {
            dialogBinding.tvCalculatorDisplay.text = currentDisplay
            dialogBinding.tvCalculatorExpression.text = expressionDisplay
        }

        fun getCurrentValue(): Double {
            return currentDisplay.toDoubleOrNull() ?: 0.0
        }

        fun addNumber(number: String) {
            if (waitingForOperand || currentDisplay == "0" || justCalculated) {
                currentDisplay = number
                waitingForOperand = false
                if (justCalculated) {
                    // Starting fresh after calculation
                    expressionDisplay = ""
                    justCalculated = false
                }
            } else {
                currentDisplay += number
            }
            updateDisplay()
        }

        fun addDecimal() {
            if (waitingForOperand || justCalculated) {
                currentDisplay = "0."
                waitingForOperand = false
                if (justCalculated) {
                    expressionDisplay = ""
                    justCalculated = false
                }
            } else if (!currentDisplay.contains(".")) {
                currentDisplay += "."
            }
            updateDisplay()
        }

        fun performCalculation() {
            if (currentOperator != null && !waitingForOperand) {
                val currentValue = getCurrentValue()

                // Add the current number to the expression
                expressionDisplay += currentDisplay

                val result =
                        when (currentOperator) {
                            "+" -> storedValue + currentValue
                            "-" -> storedValue - currentValue
                            "×" -> storedValue * currentValue
                            "÷" ->
                                    if (currentValue != 0.0) storedValue / currentValue
                                    else storedValue
                            else -> currentValue
                        }

                currentDisplay =
                        if (result == result.toLong().toDouble()) {
                            result.toLong().toString()
                        } else {
                            String.format("%.2f", result)
                        }

                storedValue = result
                currentOperator = null
                waitingForOperand = true
                justCalculated = true
                updateDisplay()
            }
        }

        fun addOperator(op: String) {
            val currentValue = getCurrentValue()

            if (currentOperator != null && !waitingForOperand) {
                // Chain calculation: perform previous operation first
                performCalculation()
                // After calculation, add the new operator to expression
                expressionDisplay += " $op "
            } else {
                storedValue = currentValue
                // Start or continue building expression
                if (expressionDisplay.isEmpty()) {
                    expressionDisplay = currentDisplay
                }
                expressionDisplay += " $op "
            }

            currentOperator = op
            waitingForOperand = true
            justCalculated = false
            updateDisplay()
        }

        fun backspace() {
            if (!waitingForOperand && currentDisplay.length > 1) {
                currentDisplay = currentDisplay.dropLast(1)
                if (currentDisplay.isEmpty() || currentDisplay == "-") {
                    currentDisplay = "0"
                    waitingForOperand = true
                }
            } else {
                currentDisplay = "0"
                waitingForOperand = true
            }
            justCalculated = false
            updateDisplay()
        }

        fun clear() {
            currentDisplay = "0"
            expressionDisplay = ""
            storedValue = 0.0
            currentOperator = null
            waitingForOperand = true
            justCalculated = false
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
            // If there's a pending operation, complete it first
            if (currentOperator != null && !waitingForOperand) {
                performCalculation()
            }

            // Get the final calculated value
            val finalValue = getCurrentValue()

            if (finalValue > 0) {
                dialog.dismiss()
                showExpenseFormDialog(finalValue, preselectedCategory)
            } else {
                Toast.makeText(this, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun showCalculatorDialogForEdit(
            initialAmount: Double,
            onAmountSelected: (Double) -> Unit
    ) {
        val dialogBinding = DialogCalculatorBinding.inflate(LayoutInflater.from(this))
        var currentDisplay = initialAmount.toString()
        var expressionDisplay = ""
        var storedValue = 0.0
        var currentOperator: String? = null
        var waitingForOperand = true
        var justCalculated = false

        val dialog = AlertDialog.Builder(this).setView(dialogBinding.root).create()

        fun updateDisplay() {
            dialogBinding.tvCalculatorDisplay.text = currentDisplay
            dialogBinding.tvCalculatorExpression.text = expressionDisplay
        }

        fun getCurrentValue(): Double {
            return currentDisplay.toDoubleOrNull() ?: 0.0
        }

        fun addNumber(number: String) {
            if (waitingForOperand || currentDisplay == "0" || justCalculated) {
                currentDisplay = number
                waitingForOperand = false
                if (justCalculated) {
                    expressionDisplay = ""
                    justCalculated = false
                }
            } else {
                currentDisplay += number
            }
            updateDisplay()
        }

        fun addDecimal() {
            if (waitingForOperand || justCalculated) {
                currentDisplay = "0."
                waitingForOperand = false
                if (justCalculated) {
                    expressionDisplay = ""
                    justCalculated = false
                }
            } else if (!currentDisplay.contains(".")) {
                currentDisplay += "."
            }
            updateDisplay()
        }

        fun performCalculation() {
            if (currentOperator != null && !waitingForOperand) {
                val currentValue = getCurrentValue()
                expressionDisplay += currentDisplay

                val result =
                        when (currentOperator) {
                            "+" -> storedValue + currentValue
                            "-" -> storedValue - currentValue
                            "×" -> storedValue * currentValue
                            "÷" ->
                                    if (currentValue != 0.0) storedValue / currentValue
                                    else storedValue
                            else -> currentValue
                        }

                currentDisplay =
                        if (result == result.toLong().toDouble()) {
                            result.toLong().toString()
                        } else {
                            String.format("%.2f", result)
                        }

                storedValue = result
                currentOperator = null
                waitingForOperand = true
                justCalculated = true
                updateDisplay()
            }
        }

        fun addOperator(op: String) {
            val currentValue = getCurrentValue()

            if (currentOperator != null && !waitingForOperand) {
                performCalculation()
                expressionDisplay += " $op "
            } else {
                storedValue = currentValue
                if (expressionDisplay.isEmpty()) {
                    expressionDisplay = currentDisplay
                }
                expressionDisplay += " $op "
            }

            currentOperator = op
            waitingForOperand = true
            justCalculated = false
            updateDisplay()
        }

        fun backspace() {
            if (!waitingForOperand && currentDisplay.length > 1) {
                currentDisplay = currentDisplay.dropLast(1)
                if (currentDisplay.isEmpty() || currentDisplay == "-") {
                    currentDisplay = "0"
                    waitingForOperand = true
                }
            } else {
                currentDisplay = "0"
                waitingForOperand = true
            }
            justCalculated = false
            updateDisplay()
        }

        fun clear() {
            currentDisplay = "0"
            expressionDisplay = ""
            storedValue = 0.0
            currentOperator = null
            waitingForOperand = true
            justCalculated = false
            updateDisplay()
        }

        // Initialize display with initial amount
        updateDisplay()

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
            if (currentOperator != null && !waitingForOperand) {
                performCalculation()
            }

            val finalValue = getCurrentValue()
            if (finalValue > 0) {
                dialog.dismiss()
                onAmountSelected(finalValue)
            } else {
                Toast.makeText(this, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun showExpenseFormDialog(amount: Double) {
        showExpenseFormDialog(amount, null)
    }

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
            selectedCategory?.let { category ->
                val description = dialogBinding.etExpenseNote.text.toString().trim()
                val expense =
                        Expense(
                                categoryId = category.id,
                                amount = amount,
                                description = description,
                                createdAt = selectedDate
                        )
                expensesViewModel.insertExpense(expense)
                dialog.dismiss()
            }
                    ?: run {
                        Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show()
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
            supportActionBar?.title = "Manage Categories"
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
            selectedCategory?.let { category ->
                val description = dialogBinding.etExpenseNote.text.toString().trim()
                val amountText = dialogBinding.tvExpenseAmount.text.toString()

                // Extract amount from formatted text
                val amount =
                        try {
                            currencyPreferences.parseAmount(amountText)
                        } catch (e: Exception) {
                            expense.amount // fallback to original amount if parsing fails
                        }

                val updatedExpense =
                        expense.copy(
                                categoryId = category.id,
                                amount = amount,
                                description = description,
                                createdAt = selectedDate
                        )
                expensesViewModel.updateExpense(updatedExpense)
                dialog.dismiss()
            }
                    ?: run {
                        Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show()
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
                .setMessage("Are you sure you want to delete this expense?")
                .setPositiveButton("Delete") { _, _ -> expensesViewModel.deleteExpense(expense) }
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
                                    listOf(
                                            BudgetCategory(
                                                    name = "Food & Dining",
                                                    budgetLimit = 500.0,
                                                    color = "#4CAF50" // Green
                                            ),
                                            BudgetCategory(
                                                    name = "Transportation",
                                                    budgetLimit = 300.0,
                                                    color = "#2196F3" // Blue
                                            ),
                                            BudgetCategory(
                                                    name = "Shopping",
                                                    budgetLimit = 400.0,
                                                    color = "#FF9800" // Orange
                                            ),
                                            BudgetCategory(
                                                    name = "Entertainment",
                                                    budgetLimit = 200.0,
                                                    color = "#E91E63" // Pink
                                            ),
                                            BudgetCategory(
                                                    name = "Bills & Utilities",
                                                    budgetLimit = 800.0,
                                                    color = "#9C27B0" // Purple
                                            ),
                                            BudgetCategory(
                                                    name = "Healthcare",
                                                    budgetLimit = 300.0,
                                                    color = "#FF5722" // Red
                                            ),
                                            BudgetCategory(
                                                    name = "Personal Care",
                                                    budgetLimit = 150.0,
                                                    color = "#00BCD4" // Teal
                                            ),
                                            BudgetCategory(
                                                    name = "Education",
                                                    budgetLimit = 250.0,
                                                    color = "#3F51B5" // Indigo
                                            )
                                    )

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
                .setMessage("Are you sure you want to delete this expense?")
                .setPositiveButton("Delete") { _, _ -> expensesViewModel.deleteExpense(expense) }
                .setNegativeButton("Cancel", null)
                .show()
    }
}
