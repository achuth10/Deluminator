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
                binding.fabAddCategory.setOnClickListener { showAddCategoryDialog() }
        }

        private fun showAddCategoryDialog(categoryToEdit: BudgetCategory? = null) {
                val dialogBinding =
                        DialogAddCategoryBinding.inflate(LayoutInflater.from(requireContext()))

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

                dialogBinding.btnCancel.setOnClickListener { dialog.dismiss() }

                dialogBinding.btnSave.setOnClickListener {
                        val name = dialogBinding.etCategoryName.text.toString().trim()
                        val budgetLimitText = dialogBinding.etBudgetLimit.text.toString().trim()

                        if (name.isEmpty() || budgetLimitText.isEmpty()) {
                                Toast.makeText(
                                                requireContext(),
                                                "Please fill all fields",
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

                        val budgetLimit = budgetLimitText.toDoubleOrNull()
                        if (budgetLimit == null || budgetLimit <= 0) {
                                Toast.makeText(
                                                requireContext(),
                                                "Please enter a valid budget amount",
                                                Toast.LENGTH_SHORT
                                        )
                                        .show()
                                return@setOnClickListener
                        }

                        // Validate budget limit range
                        if (budgetLimit > 999999.99) {
                                Toast.makeText(
                                                requireContext(),
                                                "Budget amount cannot exceed $999,999.99",
                                                Toast.LENGTH_SHORT
                                        )
                                        .show()
                                return@setOnClickListener
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
