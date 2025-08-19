package com.example.budgetdeluminator.ui.expenses

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.budgetdeluminator.data.entity.Expense
import com.example.budgetdeluminator.data.model.ExpenseWithCategory
import com.example.budgetdeluminator.databinding.FragmentAllExpensesBinding
import com.example.budgetdeluminator.ui.adapter.DateRangeDropdownAdapter
import com.example.budgetdeluminator.ui.adapter.GroupedExpenseAdapter
import com.example.budgetdeluminator.utils.CurrencyPreferences
import com.example.budgetdeluminator.utils.ExpenseGroupingUtils
import java.text.SimpleDateFormat
import java.util.*

class AllExpensesFragment : Fragment() {

    private var _binding: FragmentAllExpensesBinding? = null
    private val binding
        get() = _binding!!

    private lateinit var expenseAdapter: GroupedExpenseAdapter
    private val expensesViewModel: ExpensesViewModel by activityViewModels()
    private lateinit var currencyPreferences: CurrencyPreferences
    private lateinit var dateRangeAdapter: DateRangeDropdownAdapter
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)

    private var startDate: Long = 0
    private var endDate: Long = 0
    private var currentRangeValue: String = "last_30_days"

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAllExpensesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        currencyPreferences = CurrencyPreferences(requireContext())
        setupDefaultDateRange()
        setupRecyclerView()
        setupDateRangeDropdown()
        setupObservers()
    }

    private fun setupDefaultDateRange() {
        val calendar = Calendar.getInstance()
        endDate = calendar.timeInMillis

        // Set start date to 30 days ago
        calendar.add(Calendar.DAY_OF_MONTH, -30)
        startDate = calendar.timeInMillis

        updateDateRangeDisplay()
    }

    private fun setupRecyclerView() {
        expenseAdapter =
                GroupedExpenseAdapter(
                        requireContext(),
                        onExpenseClick = { expense ->
                            // Handle expense edit - will be handled by MainActivity
                            (activity as? OnExpenseClickListener)?.onExpenseClicked(expense)
                        },
                        onExpenseLongClick = { expense ->
                            // Handle expense delete - will be handled by MainActivity
                            (activity as? OnExpenseDeleteListener)?.onExpenseDeleteRequested(
                                    expense
                            )
                        }
                )

        binding.recyclerViewAllExpenses.apply {
            adapter = expenseAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun setupDateRangeDropdown() {
        val rangeNames =
                listOf("Last 7 days", "Last 30 days", "Last 3 months", "This year", "Custom")
        val rangeValues =
                listOf("last_7_days", "last_30_days", "last_3_months", "this_year", "custom")

        dateRangeAdapter = DateRangeDropdownAdapter(requireContext(), rangeNames, rangeValues)
        binding.actvDateRange.setAdapter(dateRangeAdapter)

        // Set initial selection
        val initialPosition = dateRangeAdapter.getPositionOfRangeValue(currentRangeValue)
        if (initialPosition >= 0) {
            binding.actvDateRange.setText(dateRangeAdapter.getRangeNameAt(initialPosition), false)
        }

        // Handle selection
        binding.actvDateRange.setOnItemClickListener { _, _, position, _ ->
            val rangeValue = dateRangeAdapter.getRangeValueAt(position)
            val rangeName = dateRangeAdapter.getRangeNameAt(position)

            rangeValue?.let { value ->
                currentRangeValue = value
                rangeName?.let { name -> binding.actvDateRange.setText(name, false) }

                when (value) {
                    "last_7_days", "last_30_days", "last_3_months", "this_year" -> {
                        applyPredefinedDateRange(value)
                    }
                    "custom" -> {
                        showCustomDateRangePicker()
                    }
                }
            }
        }

        // Handle focus change to ensure valid selection
        binding.actvDateRange.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                // Reset to current selection if invalid text
                val currentPosition = dateRangeAdapter.getPositionOfRangeValue(currentRangeValue)
                if (currentPosition >= 0) {
                    binding.actvDateRange.setText(
                            dateRangeAdapter.getRangeNameAt(currentPosition),
                            false
                    )
                }
            }
        }
    }

    private fun setupObservers() {
        expensesViewModel.allExpensesWithCategory.observe(viewLifecycleOwner) {
                allExpensesWithCategory ->
            val filteredExpenses = filterExpensesByDateRange(allExpensesWithCategory)

            if (filteredExpenses.isEmpty()) {
                binding.recyclerViewAllExpenses.visibility = View.GONE
                binding.layoutEmptyState.visibility = View.VISIBLE
            } else {
                binding.recyclerViewAllExpenses.visibility = View.VISIBLE
                binding.layoutEmptyState.visibility = View.GONE

                // Group expenses by date and submit to adapter
                val groupedExpenses = ExpenseGroupingUtils.groupExpensesByDate(filteredExpenses)
                expenseAdapter.submitList(groupedExpenses)
            }

            updateFilteredTotal(filteredExpenses)
        }
    }

    private fun filterExpensesByDateRange(
            expensesWithCategory: List<ExpenseWithCategory>
    ): List<ExpenseWithCategory> {
        return expensesWithCategory
                .filter { expenseWithCategory ->
                    expenseWithCategory.expense.createdAt >= startDate &&
                            expenseWithCategory.expense.createdAt <= endDate
                }
                .sortedByDescending { it.expense.createdAt }
    }

    private fun updateFilteredTotal(filteredExpenses: List<ExpenseWithCategory>) {
        val total = filteredExpenses.sumOf { it.expense.amount }
        binding.tvFilteredTotal.text = currencyPreferences.formatAmount(total)
    }

    private fun applyPredefinedDateRange(rangeValue: String) {
        val calendar = Calendar.getInstance()
        endDate = calendar.timeInMillis

        when (rangeValue) {
            "last_7_days" -> calendar.add(Calendar.DAY_OF_MONTH, -7)
            "last_30_days" -> calendar.add(Calendar.DAY_OF_MONTH, -30)
            "last_3_months" -> calendar.add(Calendar.MONTH, -3)
            "this_year" -> calendar.set(Calendar.DAY_OF_YEAR, 1)
        }

        startDate = calendar.timeInMillis
        setupObservers() // Refresh data
    }

    private fun updateDateRangeDisplay() {
        // This method is now handled by the dropdown selection
        // But we keep it for backward compatibility with custom date ranges
        when (currentRangeValue) {
            "custom" -> {
                val customText =
                        "${dateFormat.format(Date(startDate))} - ${dateFormat.format(Date(endDate))}"
                binding.actvDateRange.setText(customText, false)
            }
        }
    }

    private fun isDateRange(expectedStart: Long, expectedEnd: Long): Boolean {
        val dayInMillis = 24 * 60 * 60 * 1000
        return Math.abs(startDate - expectedStart) < dayInMillis &&
                Math.abs(endDate - expectedEnd) < dayInMillis
    }

    private fun showCustomDateRangePicker() {
        val calendar = Calendar.getInstance()

        // First pick start date
        DatePickerDialog(
                        requireContext(),
                        { _, year, month, dayOfMonth ->
                            val startCalendar = Calendar.getInstance()
                            startCalendar.set(year, month, dayOfMonth, 0, 0, 0)
                            startDate = startCalendar.timeInMillis

                            // Then pick end date
                            DatePickerDialog(
                                            requireContext(),
                                            { _, endYear, endMonth, endDayOfMonth ->
                                                val endCalendar = Calendar.getInstance()
                                                endCalendar.set(
                                                        endYear,
                                                        endMonth,
                                                        endDayOfMonth,
                                                        23,
                                                        59,
                                                        59
                                                )
                                                endDate = endCalendar.timeInMillis

                                                currentRangeValue = "custom"
                                                updateDateRangeDisplay()
                                                setupObservers() // Refresh data
                                            },
                                            calendar.get(Calendar.YEAR),
                                            calendar.get(Calendar.MONTH),
                                            calendar.get(Calendar.DAY_OF_MONTH)
                                    )
                                    .show()
                        },
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH)
                )
                .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun refreshCurrency() {
        // Update currency preferences instance
        currencyPreferences = CurrencyPreferences(requireContext())

        // Refresh the filtered total with new currency
        expensesViewModel.allExpensesWithCategory.value?.let { allExpensesWithCategory ->
            val filteredExpenses = filterExpensesByDateRange(allExpensesWithCategory)
            updateFilteredTotal(filteredExpenses)
        }

        // Refresh the adapter to update currency formatting in expense items
        expenseAdapter.notifyDataSetChanged()
    }

    interface OnExpenseClickListener {
        fun onExpenseClicked(expense: Expense)
    }

    interface OnExpenseDeleteListener {
        fun onExpenseDeleteRequested(expense: Expense)
    }
}
