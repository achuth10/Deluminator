package com.example.budgetdeluminator

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
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
import com.example.budgetdeluminator.ui.categories.CategoriesActivity
import com.example.budgetdeluminator.ui.categories.CategoriesViewModel
import com.example.budgetdeluminator.ui.expenses.AllExpensesFragment
import com.example.budgetdeluminator.ui.expenses.ExpensesViewModel
import com.example.budgetdeluminator.ui.home.HomeFragment
import com.example.budgetdeluminator.ui.home.HomeViewModel
import com.example.budgetdeluminator.utils.CurrencyPreferences
import com.google.android.material.navigation.NavigationView
import java.text.SimpleDateFormat
import java.util.*

class MainActivity :
        AppCompatActivity(),
        NavigationView.OnNavigationItemSelectedListener,
        HomeFragment.OnCategoryClickListener,
        AllExpensesFragment.OnExpenseClickListener,
        AllExpensesFragment.OnExpenseDeleteListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var drawerToggle: ActionBarDrawerToggle
    private val homeViewModel: HomeViewModel by viewModels()
    private val categoriesViewModel: CategoriesViewModel by viewModels()
    private val expensesViewModel: ExpensesViewModel by viewModels()
    private lateinit var currencyPreferences: CurrencyPreferences
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)

    private lateinit var homeFragment: HomeFragment
    private lateinit var allExpensesFragment: AllExpensesFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currencyPreferences = CurrencyPreferences(this)
        setupToolbar()
        setupNavigationDrawer()
        setupFragments()
        setupBottomNavigation()
        setupClickListeners()
        setupBackPressedHandler()

        // Add some sample data if database is empty
        addSampleDataIfNeeded()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun setupNavigationDrawer() {
        drawerToggle = ActionBarDrawerToggle(this, binding.drawerLayout, binding.toolbar, 0, 0)
        binding.drawerLayout.addDrawerListener(drawerToggle)
        drawerToggle.syncState()

        binding.navigationView.setNavigationItemSelectedListener(this)

        // Set the home menu item as selected
        binding.navigationView.setCheckedItem(R.id.nav_home)
    }

    private fun setupFragments() {
        homeFragment = HomeFragment()
        allExpensesFragment = AllExpensesFragment()

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
                    true
                }
                R.id.nav_all_expenses -> {
                    replaceFragment(allExpensesFragment)
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

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_home -> {
                // Already on home screen - just close drawer
            }
            R.id.nav_categories -> {
                startActivity(Intent(this, CategoriesActivity::class.java))
            }
            R.id.nav_settings -> {
                startActivity(
                        Intent(
                                this,
                                com.example.budgetdeluminator.ui.settings.SettingsActivity::class
                                        .java
                        )
                )
            }
        }
        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun setupBackPressedHandler() {
        onBackPressedDispatcher.addCallback(
                this,
                object : androidx.activity.OnBackPressedCallback(true) {
                    override fun handleOnBackPressed() {
                        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                            binding.drawerLayout.closeDrawer(GravityCompat.START)
                        } else {
                            finish()
                        }
                    }
                }
        )
    }

    private fun startExpenseEntryFlow() {
        showCalculatorDialog()
    }

    private fun showCalculatorDialog() {
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
                showExpenseFormDialog(finalValue)
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
        val dialogBinding = DialogAddExpenseBinding.inflate(LayoutInflater.from(this))
        var selectedCategory: BudgetCategory? = null
        var selectedDate = System.currentTimeMillis() // Default to current time

        dialogBinding.tvExpenseAmount.setText(currencyPreferences.formatAmount(amount))
        updateDateDisplay(dialogBinding, selectedDate)

        val dialog = AlertDialog.Builder(this).setView(dialogBinding.root).create()

        // Close button
        dialogBinding.btnCloseExpenseForm.setOnClickListener { dialog.dismiss() }

        // Amount field - open calculator when clicked
        dialogBinding.tvExpenseAmount.setOnClickListener {
            dialog.dismiss()
            showCalculatorDialog()
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
                val description =
                        dialogBinding.etExpenseNote.text.toString().trim().ifEmpty { "Expense" }
                val expense =
                        Expense(
                                categoryId = category.id,
                                amount = amount,
                                description = description,
                                createdAt = selectedDate
                        )
                expensesViewModel.insertExpense(expense)
                Toast.makeText(this, "Expense added successfully", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
                    ?: run {
                        Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show()
                    }
        }

        // Auto-select first category if available
        categoriesViewModel.allCategories.observe(this) { categories ->
            if (categories.isNotEmpty() && selectedCategory == null) {
                selectedCategory = categories[0]
                dialogBinding.tvSelectedCategory.setText(categories[0].name)
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
            startActivity(Intent(this, CategoriesActivity::class.java))
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
                val description =
                        dialogBinding.etExpenseNote.text.toString().trim().ifEmpty { "Expense" }
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
                Toast.makeText(this, "Expense updated successfully", Toast.LENGTH_SHORT).show()
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

        dialogBinding.btnAddExpenseFromList.setOnClickListener {
            dialog.dismiss()
            startExpenseEntryFlow()
        }

        dialogBinding.btnCloseExpenses.setOnClickListener { dialog.dismiss() }

        dialog.show()
    }

    private fun showDeleteExpenseDialog(expense: Expense) {
        AlertDialog.Builder(this)
                .setTitle("Delete Expense")
                .setMessage("Are you sure you want to delete this expense?")
                .setPositiveButton("Delete") { _, _ ->
                    expensesViewModel.deleteExpense(expense)
                    Toast.makeText(this, "Expense deleted", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Cancel", null)
                .show()
    }

    private fun addSampleDataIfNeeded() {
        // Add sample categories if none exist
        homeViewModel.categoriesWithExpenses.observe(
                this,
                object : Observer<List<CategoryWithExpenses>> {
                    override fun onChanged(value: List<CategoryWithExpenses>) {
                        if (value.isEmpty()) {
                            // Add sample categories
                            val sampleCategories =
                                    listOf(
                                            BudgetCategory(
                                                    name = "Food & Dining",
                                                    budgetLimit = 500.0
                                            ),
                                            BudgetCategory(
                                                    name = "Transportation",
                                                    budgetLimit = 200.0
                                            ),
                                            BudgetCategory(
                                                    name = "Entertainment",
                                                    budgetLimit = 150.0
                                            ),
                                            BudgetCategory(name = "Shopping", budgetLimit = 300.0),
                                            BudgetCategory(
                                                    name = "Bills & Utilities",
                                                    budgetLimit = 800.0
                                            )
                                    )

                            sampleCategories.forEach { category ->
                                categoriesViewModel.insertCategory(category)
                            }

                            // Add sample expenses after a short delay to ensure categories are
                            // inserted
                            binding.root.postDelayed({ addSampleExpenses() }, 500)
                        }
                        // Remove observer after first call
                        homeViewModel.categoriesWithExpenses.removeObserver(this)
                    }
                }
        )
    }

    private fun addSampleExpenses() {
        // Add some sample expenses to make the app more realistic
        val sampleExpenses =
                listOf(
                        Expense(
                                categoryId = 1,
                                amount = 25.50,
                                description = "Lunch at restaurant",
                                createdAt = System.currentTimeMillis() - 2 * 60 * 60 * 1000
                        ),
                        Expense(
                                categoryId = 1,
                                amount = 45.20,
                                description = "Grocery shopping",
                                createdAt = System.currentTimeMillis() - 1 * 24 * 60 * 60 * 1000
                        ),
                        Expense(
                                categoryId = 1,
                                amount = 12.75,
                                description = "Coffee",
                                createdAt = System.currentTimeMillis() - 3 * 60 * 60 * 1000
                        ),
                        Expense(
                                categoryId = 2,
                                amount = 15.00,
                                description = "Bus fare",
                                createdAt = System.currentTimeMillis() - 5 * 60 * 60 * 1000
                        ),
                        Expense(
                                categoryId = 2,
                                amount = 35.00,
                                description = "Gas",
                                createdAt = System.currentTimeMillis() - 2 * 24 * 60 * 60 * 1000
                        ),
                        Expense(
                                categoryId = 3,
                                amount = 18.99,
                                description = "Movie ticket",
                                createdAt = System.currentTimeMillis() - 3 * 24 * 60 * 60 * 1000
                        ),
                        Expense(
                                categoryId = 3,
                                amount = 9.99,
                                description = "Streaming service",
                                createdAt = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000
                        ),
                        Expense(
                                categoryId = 4,
                                amount = 89.99,
                                description = "New shoes",
                                createdAt = System.currentTimeMillis() - 1 * 24 * 60 * 60 * 1000
                        ),
                        Expense(
                                categoryId = 5,
                                amount = 120.00,
                                description = "Electricity bill",
                                createdAt = System.currentTimeMillis() - 5 * 24 * 60 * 60 * 1000
                        )
                )

        sampleExpenses.forEach { expense -> expensesViewModel.insertExpense(expense) }
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
                .setPositiveButton("Delete") { _, _ ->
                    expensesViewModel.deleteExpense(expense)
                    Toast.makeText(this, "Expense deleted", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Cancel", null)
                .show()
    }
}
