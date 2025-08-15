package com.example.budgetdeluminator.ui.home

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
        setupRecyclerView()
        setupObservers()
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

    private fun setupObservers() {
        homeViewModel.categoriesWithExpenses.observe(viewLifecycleOwner) { categoriesWithExpenses ->
            categoryAdapter.submitList(categoriesWithExpenses)
            updateOverviewData()
        }
    }

    private fun updateOverviewData() {
        val totalBudget = homeViewModel.getTotalBudget()
        val totalSpent = homeViewModel.getTotalSpent()
        val remaining = homeViewModel.getRemainingBudget()

        binding.apply {
            tvTotalBudget.text = currencyPreferences.formatAmount(totalBudget)
            tvTotalSpent.text = currencyPreferences.formatAmount(totalSpent)
            tvRemaining.text = currencyPreferences.formatAmount(remaining)

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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    interface OnCategoryClickListener {
        fun onCategoryClicked(
                categoryWithExpenses: com.example.budgetdeluminator.data.model.CategoryWithExpenses
        )
    }
}
