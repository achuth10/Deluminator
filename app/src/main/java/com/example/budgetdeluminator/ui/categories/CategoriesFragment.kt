package com.example.budgetdeluminator.ui.categories

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.budgetdeluminator.R
import com.example.budgetdeluminator.data.entity.BudgetCategory
import com.example.budgetdeluminator.databinding.DialogAddCategoryBinding
import com.example.budgetdeluminator.databinding.FragmentCategoriesBinding
import com.example.budgetdeluminator.ui.adapter.CategoryManagementAdapter
import kotlinx.coroutines.launch

class CategoriesFragment : Fragment() {

        private var _binding: FragmentCategoriesBinding? = null
        private val binding
                get() = _binding!!

        private lateinit var categoryAdapter: CategoryManagementAdapter
        private val categoriesViewModel: CategoriesViewModel by activityViewModels()

        override fun onCreateView(
                inflater: LayoutInflater,
                container: ViewGroup?,
                savedInstanceState: Bundle?
        ): View {
                _binding = FragmentCategoriesBinding.inflate(inflater, container, false)
                return binding.root
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
                super.onViewCreated(view, savedInstanceState)

                setupRecyclerView()
                setupObservers()
                setupClickListeners()
        }

        override fun onDestroyView() {
                super.onDestroyView()
                _binding = null
        }

        fun refreshCurrency() {
                // Refresh the adapter to update currency formatting in category budget limits
                categoryAdapter.notifyDataSetChanged()
        }

        private fun setupRecyclerView() {
                categoryAdapter =
                        CategoryManagementAdapter(
                                context = requireContext(),
                                onEditClick = { category -> showAddCategoryDialog(category) },
                                onDeleteClick = { category -> showDeleteCategoryDialog(category) }
                        )

                binding.recyclerViewCategories.apply {
                        adapter = categoryAdapter
                        layoutManager = LinearLayoutManager(requireContext())
                }
        }

        private fun setupObservers() {
                categoriesViewModel.allCategories.observe(viewLifecycleOwner) { categories ->
                        categoryAdapter.submitList(categories)

                        // Show/hide empty state
                        if (categories.isEmpty()) {
                                binding.recyclerViewCategories.visibility = View.GONE
                                binding.layoutEmptyState.visibility = View.VISIBLE
                        } else {
                                binding.recyclerViewCategories.visibility = View.VISIBLE
                                binding.layoutEmptyState.visibility = View.GONE
                        }
                }

                // Observe operation results for error feedback only
                categoriesViewModel.operationResult.observe(viewLifecycleOwner) { result ->
                        result?.let {
                                when (it) {
                                        is CategoriesViewModel.OperationResult.Error -> {
                                                Toast.makeText(
                                                                requireContext(),
                                                                it.message,
                                                                Toast.LENGTH_LONG
                                                        )
                                                        .show()
                                        }
                                        is CategoriesViewModel.OperationResult.Success -> {
                                                // Success - no toast needed, UI updates
                                                // automatically
                                        }
                                }
                                // Clear the result after handling it
                                categoriesViewModel.clearOperationResult()
                        }
                }
        }

        private fun setupClickListeners() {
                // FAB click listener removed - now handled by MainActivity
        }

        private fun showAddCategoryDialog(categoryToEdit: BudgetCategory? = null) {
                val dialogBinding =
                        DialogAddCategoryBinding.inflate(LayoutInflater.from(requireContext()))

                // Set dialog title based on whether we're adding or editing
                dialogBinding.tvDialogTitle.text =
                        if (categoryToEdit == null) "Add Category" else "Edit Category"

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
                        // Set switch and budget limit based on existing data
                        val hasBudgetLimit = category.budgetLimit != null
                        dialogBinding.switchBudgetLimit.isChecked = hasBudgetLimit
                        if (hasBudgetLimit) {
                                dialogBinding.etBudgetLimit.setText(category.budgetLimit.toString())
                        }
                        selectedColor = category.color
                }

                // Setup budget limit toggle functionality
                fun updateBudgetLimitVisibility(hasBudgetLimit: Boolean) {
                        if (hasBudgetLimit) {
                                dialogBinding.tilBudgetLimit.visibility = View.VISIBLE
                                dialogBinding.tvTrackingInfo.visibility = View.GONE
                        } else {
                                dialogBinding.tilBudgetLimit.visibility = View.GONE
                                dialogBinding.tvTrackingInfo.visibility = View.VISIBLE
                                dialogBinding.etBudgetLimit.setText("") // Clear any existing value
                        }
                }

                // Initialize visibility based on switch state
                updateBudgetLimitVisibility(dialogBinding.switchBudgetLimit.isChecked)

                // Setup switch change listener
                dialogBinding.switchBudgetLimit.setOnCheckedChangeListener { _, isChecked ->
                        updateBudgetLimitVisibility(isChecked)
                }

                // Setup color picker click listeners
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

                // Initialize color selection highlighting
                updateColorSelection(colorViews, colorOptions, selectedColor)

                colorViews.forEachIndexed { index, colorView ->
                        colorView.setOnClickListener {
                                selectedColor = colorOptions[index]
                                updateColorSelection(colorViews, colorOptions, selectedColor)
                        }
                }

                val dialog =
                        AlertDialog.Builder(requireContext()).setView(dialogBinding.root).create()
                dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

                dialogBinding.btnCancel.setOnClickListener { dialog.dismiss() }

                dialogBinding.btnSave.setOnClickListener {
                        val name = dialogBinding.etCategoryName.text.toString().trim()
                        val hasBudgetLimit = dialogBinding.switchBudgetLimit.isChecked
                        val budgetLimitText = dialogBinding.etBudgetLimit.text.toString().trim()

                        // Validate name
                        if (name.isEmpty()) {
                                Toast.makeText(
                                                requireContext(),
                                                "Please enter a category name",
                                                Toast.LENGTH_SHORT
                                        )
                                        .show()
                                return@setOnClickListener
                        }

                        // Validate name length
                        if (name.length > 50) {
                                Toast.makeText(
                                                requireContext(),
                                                "Category name must be 50 characters or less",
                                                Toast.LENGTH_SHORT
                                        )
                                        .show()
                                return@setOnClickListener
                        }

                        // Validate budget limit if enabled
                        var budgetLimit: Double? = null
                        if (hasBudgetLimit) {
                                if (budgetLimitText.isEmpty()) {
                                        Toast.makeText(
                                                        requireContext(),
                                                        "Please enter a budget limit or disable budget tracking",
                                                        Toast.LENGTH_SHORT
                                                )
                                                .show()
                                        return@setOnClickListener
                                }

                                val parsedLimit = budgetLimitText.toDoubleOrNull()
                                if (parsedLimit == null || parsedLimit <= 0) {
                                        Toast.makeText(
                                                        requireContext(),
                                                        "Please enter a valid budget amount",
                                                        Toast.LENGTH_SHORT
                                                )
                                                .show()
                                        return@setOnClickListener
                                }

                                if (parsedLimit > 999999.99) {
                                        Toast.makeText(
                                                        requireContext(),
                                                        "Budget amount cannot exceed $999,999.99",
                                                        Toast.LENGTH_SHORT
                                                )
                                                .show()
                                        return@setOnClickListener
                                }

                                budgetLimit = parsedLimit
                        }

                        if (categoryToEdit == null) {
                                // Adding new category
                                val newCategory =
                                        BudgetCategory(
                                                name = name,
                                                budgetLimit = budgetLimit,
                                                color = selectedColor
                                        )
                                categoriesViewModel.insertCategory(newCategory)
                        } else {
                                // Editing existing category
                                val updatedCategory =
                                        categoryToEdit.copy(
                                                name = name,
                                                budgetLimit = budgetLimit,
                                                color = selectedColor
                                        )
                                categoriesViewModel.updateCategory(updatedCategory)
                        }

                        dialog.dismiss()
                }

                dialog.show()
        }

        private fun updateColorSelection(
                colorViews: Array<View>,
                colorOptions: Array<String>,
                selectedColor: String
        ) {
                colorViews.forEachIndexed { index, colorView ->
                        val isSelected = colorOptions[index] == selectedColor
                        val drawableRes =
                                when (colorOptions[index]) {
                                        "#4CAF50" ->
                                                if (isSelected)
                                                        R.drawable.color_circle_green_selected
                                                else R.drawable.color_circle_green
                                        "#2196F3" ->
                                                if (isSelected)
                                                        R.drawable.color_circle_blue_selected
                                                else R.drawable.color_circle_blue
                                        "#FF9800" ->
                                                if (isSelected)
                                                        R.drawable.color_circle_orange_selected
                                                else R.drawable.color_circle_orange
                                        "#E91E63" ->
                                                if (isSelected)
                                                        R.drawable.color_circle_pink_selected
                                                else R.drawable.color_circle_pink
                                        "#9C27B0" ->
                                                if (isSelected)
                                                        R.drawable.color_circle_purple_selected
                                                else R.drawable.color_circle_purple
                                        "#FF5722" ->
                                                if (isSelected) R.drawable.color_circle_red_selected
                                                else R.drawable.color_circle_red
                                        "#00BCD4" ->
                                                if (isSelected)
                                                        R.drawable.color_circle_teal_selected
                                                else R.drawable.color_circle_teal
                                        "#3F51B5" ->
                                                if (isSelected)
                                                        R.drawable.color_circle_indigo_selected
                                                else R.drawable.color_circle_indigo
                                        "#FFC107" ->
                                                if (isSelected)
                                                        R.drawable.color_circle_amber_selected
                                                else R.drawable.color_circle_amber
                                        "#795548" ->
                                                if (isSelected)
                                                        R.drawable.color_circle_brown_selected
                                                else R.drawable.color_circle_brown
                                        else -> R.drawable.color_circle_green
                                }
                        colorView.setBackgroundResource(drawableRes)
                }
        }

        private fun showDeleteCategoryDialog(category: BudgetCategory) {
                // Get expense count first, then show dialog
                lifecycleScope.launch {
                        val expenseCount = categoriesViewModel.getCategoryExpenseCount(category.id)

                        val message =
                                if (expenseCount > 0) {
                                        "Are you sure you want to delete '${category.name}'? This will also delete $expenseCount associated expense(s). This action cannot be undone."
                                } else {
                                        "Are you sure you want to delete '${category.name}'? This action cannot be undone."
                                }

                        AlertDialog.Builder(requireContext())
                                .setTitle("Delete Category")
                                .setMessage(message)
                                .setPositiveButton("Delete") { _, _ ->
                                        categoriesViewModel.deleteCategory(category)
                                }
                                .setNegativeButton("Cancel", null)
                                .show()
                }
        }
}
