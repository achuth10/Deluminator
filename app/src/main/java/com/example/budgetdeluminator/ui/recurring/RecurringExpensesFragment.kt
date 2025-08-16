package com.example.budgetdeluminator.ui.recurring

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.budgetdeluminator.MainActivity
import com.example.budgetdeluminator.data.entity.RecurringExpense
import com.example.budgetdeluminator.databinding.FragmentRecurringExpensesBinding
import com.example.budgetdeluminator.ui.adapter.RecurringExpenseAdapter
import com.example.budgetdeluminator.utils.ErrorHandler
import com.example.budgetdeluminator.utils.RecurrenceScheduler
import kotlinx.coroutines.launch

class RecurringExpensesFragment : Fragment() {

    private var _binding: FragmentRecurringExpensesBinding? = null
    private val binding
        get() = _binding!!

    private val viewModel: RecurringExpensesViewModel by viewModels()
    private lateinit var recurringExpenseAdapter: RecurringExpenseAdapter

    private var showActiveOnly = false

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecurringExpensesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupClickListeners()
        observeData()
    }

    private fun setupRecyclerView() {
        recurringExpenseAdapter =
                RecurringExpenseAdapter(
                        context = requireContext(),
                        onExpenseClick = { recurringExpense ->
                            // TODO: Show edit recurring expense dialog
                            showEditRecurringExpenseDialog(recurringExpense)
                        },
                        onExpenseLongClick = { recurringExpense ->
                            showDeleteRecurringExpenseDialog(recurringExpense)
                        },
                        onToggleActiveClick = { recurringExpense, isActive ->
                            viewModel.toggleActiveStatus(recurringExpense, isActive)
                            Toast.makeText(
                                            requireContext(),
                                            if (isActive) "Recurring expense activated"
                                            else "Recurring expense deactivated",
                                            Toast.LENGTH_SHORT
                                    )
                                    .show()
                        }
                )

        binding.recyclerViewRecurringExpenses.apply {
            adapter = recurringExpenseAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun setupClickListeners() {
        // FAB click listener removed - now handled by MainActivity

        binding.btnSyncRecurring.setOnClickListener { syncRecurringExpenses() }

        binding.chipShowActiveOnly.setOnCheckedChangeListener { _, isChecked ->
            showActiveOnly = isChecked
            observeData()
        }

        binding.swipeRefreshLayout.setOnRefreshListener {
            // Refresh is handled automatically by LiveData
            binding.swipeRefreshLayout.isRefreshing = false
            // Also sync recurring expenses on pull-to-refresh
            syncRecurringExpenses()
        }
    }

    private fun observeData() {
        // Remove any existing observers
        viewModel.allRecurringExpenses.removeObservers(viewLifecycleOwner)
        viewModel.activeRecurringExpenses.removeObservers(viewLifecycleOwner)

        if (showActiveOnly) {
            viewModel.activeRecurringExpenses.observe(viewLifecycleOwner) { expenses ->
                updateUI(expenses)
            }
        } else {
            viewModel.allRecurringExpenses.observe(viewLifecycleOwner) { expenses ->
                updateUI(expenses)
            }
        }
    }

    private fun updateUI(expenses: List<RecurringExpense>) {
        if (expenses.isEmpty()) {
            binding.recyclerViewRecurringExpenses.visibility = View.GONE
            binding.layoutEmptyState.visibility = View.VISIBLE
        } else {
            binding.recyclerViewRecurringExpenses.visibility = View.VISIBLE
            binding.layoutEmptyState.visibility = View.GONE
            recurringExpenseAdapter.submitList(expenses)
        }
    }

    private fun showAddRecurringExpenseDialog() {
        (requireActivity() as MainActivity).showAddRecurringExpenseDialog()
    }

    private fun showEditRecurringExpenseDialog(recurringExpense: RecurringExpense) {
        (requireActivity() as MainActivity).showEditRecurringExpenseDialog(recurringExpense)
    }

    private fun showDeleteRecurringExpenseDialog(recurringExpense: RecurringExpense) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Delete Recurring Expense")
                .setMessage(
                        "Are you sure you want to delete this recurring expense? This will not affect expenses that have already been generated."
                )
                .setPositiveButton("Delete") { _, _ ->
                    ErrorHandler.safeDatabaseExecute(
                            requireActivity(),
                            "deleting recurring expense"
                    ) {
                        viewModel.deleteRecurringExpense(recurringExpense)
                        Toast.makeText(
                                        requireContext(),
                                        "Recurring expense deleted",
                                        Toast.LENGTH_SHORT
                                )
                                .show()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
    }

    private fun syncRecurringExpenses() {
        binding.btnSyncRecurring.isEnabled = false
        binding.btnSyncRecurring.text = "Syncing..."

        lifecycleScope.launch {
            try {
                val scheduler = RecurrenceScheduler(requireContext())
                val generatedCount = scheduler.processRecurringExpensesManually()

                // Sync completed silently - no toast notification needed
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Sync failed: ${e.message}", Toast.LENGTH_SHORT)
                        .show()
            } finally {
                binding.btnSyncRecurring.isEnabled = true
                binding.btnSyncRecurring.text = "Sync"
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    fun refreshCurrency() {
        // Refresh the adapter to update currency formatting
        recurringExpenseAdapter.refreshCurrency()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
