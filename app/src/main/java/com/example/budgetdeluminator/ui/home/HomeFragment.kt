package com.example.budgetdeluminator.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
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
            layoutManager = LinearLayoutManager(requireContext())
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

            // Update progress bar
            val percentage =
                    if (totalBudget > 0) {
                        ((totalSpent / totalBudget) * 100).toInt()
                    } else 0
            progressBarOverall.progress = percentage.coerceAtMost(100)
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
