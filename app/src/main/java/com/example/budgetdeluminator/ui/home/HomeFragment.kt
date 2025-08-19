package com.example.budgetdeluminator.ui.home

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.example.budgetdeluminator.databinding.FragmentHomeBinding
import com.example.budgetdeluminator.ui.adapter.CategoryAdapter
import com.example.budgetdeluminator.utils.CurrencyPreferences
import com.example.budgetdeluminator.utils.DateUtils
import com.example.budgetdeluminator.utils.MoneyJokes
import com.example.budgetdeluminator.utils.SpendingRoastPreferences
import java.util.*

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding
        get() = _binding!!

    private lateinit var categoryAdapter: CategoryAdapter
    private val homeViewModel: HomeViewModel by activityViewModels()
    private lateinit var currencyPreferences: CurrencyPreferences

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        currencyPreferences = CurrencyPreferences(requireContext())

        // Hide content initially to prevent flickering
        initializeLoadingState()

        setupRecyclerView()
        setupObservers()
        setupClickListeners()
        setupSpendingRoast()
    }

    private fun setupRecyclerView() {
        categoryAdapter =
                CategoryAdapter(
                        requireContext(),
                        onCategoryClick = { categoryWithExpenses ->
                            // Navigate to expenses for this category - will be handled by
                            // MainActivity
                            (activity as? OnCategoryClickListener)?.onCategoryClicked(
                                    categoryWithExpenses
                            )
                        },
                        onCategoryLongClick = { categoryWithExpenses ->
                            // Same as regular click for now
                            (activity as? OnCategoryClickListener)?.onCategoryClicked(
                                    categoryWithExpenses
                            )
                        }
                )

        binding.recyclerViewCategories.apply {
            adapter = categoryAdapter
            layoutManager = GridLayoutManager(requireContext(), 2) // 2 columns
            // Add some padding for better spacing
            setPadding(4, 0, 4, 0)
        }
    }

    private fun setupClickListeners() {
        binding.chipCurrentMonth.setOnClickListener { showMonthSelector() }
    }

    private fun initializeLoadingState() {
        // Hide budget overview content initially
        binding.apply {
            chipCurrentMonth.text = ""
            tvTotalBudget.text = ""
            tvTotalSpent.text = ""
            tvRemaining.text = ""
            progressBarOverall.progress = 0

            // Show loading state and hide other content
            layoutLoading.visibility = View.VISIBLE
            recyclerViewCategories.visibility = View.GONE
            layoutEmptyState.visibility = View.GONE
        }
    }

    private fun setupObservers() {
        homeViewModel.categoriesWithExpenses.observe(viewLifecycleOwner) { categoriesWithExpenses ->
            categoryAdapter.submitList(categoriesWithExpenses)
            updateOverviewData()

            // Show content now that data is loaded
            showContentWithData(categoriesWithExpenses)
        }

        homeViewModel.selectedMonth.observe(viewLifecycleOwner) { _ -> updateCurrentMonthDisplay() }
    }

    private fun showContentWithData(
            categoriesWithExpenses:
                    List<com.example.budgetdeluminator.data.model.CategoryWithExpenses>
    ) {
        // Hide loading state now that data is loaded
        binding.layoutLoading.visibility = View.GONE

        // Show/hide appropriate content based on data
        if (categoriesWithExpenses.isEmpty()) {
            binding.recyclerViewCategories.visibility = View.GONE
            binding.layoutEmptyState.visibility = View.VISIBLE
        } else {
            binding.recyclerViewCategories.visibility = View.VISIBLE
            binding.layoutEmptyState.visibility = View.GONE
        }
    }

    private fun updateOverviewData() {
        val totalBudget = homeViewModel.getTotalBudget()
        val totalSpent = homeViewModel.getTotalSpent()
        val remaining = homeViewModel.getRemainingBudget()

        binding.apply {
            tvTotalBudget.text = currencyPreferences.formatAmountWithoutDecimals(totalBudget)
            tvTotalSpent.text = currencyPreferences.formatAmountWithoutDecimals(totalSpent)
            tvRemaining.text = currencyPreferences.formatAmountWithoutDecimals(remaining)

            // Update progress bar with three-color system
            val percentage =
                    if (totalBudget > 0) {
                        ((totalSpent / totalBudget) * 100).toInt()
                    } else 0
            progressBarOverall.progress = percentage.coerceAtMost(100)

            // Apply color to progress bar only
            val progressColor =
                    when {
                        percentage > 100 ->
                                android.graphics.Color.parseColor("#F44336") // Red for over budget
                        percentage >= 80 ->
                                android.graphics.Color.parseColor(
                                        "#FF9800"
                                ) // Orange/Yellow for warning
                        else -> android.graphics.Color.parseColor("#4CAF50") // Green for normal
                    }

            progressBarOverall.progressTintList =
                    android.content.res.ColorStateList.valueOf(progressColor)

            // Color left amount based on remaining budget percentage
            val leftColor =
                    when {
                        remaining < 0 ->
                                android.graphics.Color.parseColor(
                                        "#F44336"
                                ) // Red if exceeded budget
                        percentage >= 80 ->
                                android.graphics.Color.parseColor(
                                        "#FF9800"
                                ) // Yellow if less than 20% left
                        else -> android.graphics.Color.parseColor("#4CAF50") // Green otherwise
                    }
            tvRemaining.setTextColor(leftColor)
        }
    }

    private fun updateCurrentMonthDisplay() {
        binding.chipCurrentMonth.text = homeViewModel.getCurrentSelectedMonthName()
    }

    private fun showMonthSelector() {
        homeViewModel.availableMonths.value?.let { availableMonths ->
            if (availableMonths.isEmpty()) {
                return
            }

            val monthNames =
                    availableMonths
                            .map { (month, year) -> "${DateUtils.getMonthName(month)} $year" }
                            .toTypedArray()

            val currentSelection = homeViewModel.selectedMonth.value
            var selectedIndex =
                    if (currentSelection != null) {
                        availableMonths.indexOfFirst {
                            it.first == currentSelection.first &&
                                    it.second == currentSelection.second
                        }
                    } else {
                        -1
                    }
            if (selectedIndex == -1) selectedIndex = 0

            AlertDialog.Builder(requireContext())
                    .setTitle("Select Month")
                    .setSingleChoiceItems(monthNames, selectedIndex) { dialog, which ->
                        val selectedMonth = availableMonths[which]
                        homeViewModel.selectMonth(selectedMonth.first, selectedMonth.second)
                        dialog.dismiss()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun refreshCurrency() {
        // Update currency preferences instance
        currencyPreferences = CurrencyPreferences(requireContext())

        // Refresh the UI with new currency
        updateOverviewData()
        updateCurrentMonthDisplay()

        // Refresh the category adapter if it uses currency formatting
        categoryAdapter.notifyDataSetChanged()
    }

    private fun setupSpendingRoast() {
        // Check if spending roasts are enabled
        if (!SpendingRoastPreferences.isSpendingRoastEnabled(requireContext())) {
            binding.cardSpendingRoast.visibility = View.GONE
            return
        }

        // Get and display an expense-specific roast based on spending patterns
        val categoriesWithExpenses = homeViewModel.categoriesWithExpenses.value ?: emptyList()
        val totalBudget = homeViewModel.getTotalBudget()
        val totalSpent = homeViewModel.getTotalSpent()

        val roast =
                if (categoriesWithExpenses.isNotEmpty()) {
                    MoneyJokes.getExpenseSpecificRoast(
                            requireContext(),
                            categoriesWithExpenses,
                            totalBudget,
                            totalSpent
                    )
                } else {
                    MoneyJokes.getRandomJoke(requireContext())
                }
        binding.tvSpendingRoast.text = roast

        // Set up dismiss functionality
        binding.ivDismissRoast.setOnClickListener {
            // Hide the card immediately
            binding.cardSpendingRoast.visibility = View.GONE

            // Disable the setting so it doesn't show again
            SpendingRoastPreferences.setSpendingRoastEnabled(requireContext(), false)
        }

        // Add a subtle animation when the roast appears
        binding.cardSpendingRoast.alpha = 0f
        binding.cardSpendingRoast.animate().alpha(1f).setDuration(300).start()
    }

    interface OnCategoryClickListener {
        fun onCategoryClicked(
                categoryWithExpenses: com.example.budgetdeluminator.data.model.CategoryWithExpenses
        )
    }
}
