package com.example.budgetdeluminator.ui.adapter

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.budgetdeluminator.data.model.CategoryWithExpenses
import com.example.budgetdeluminator.databinding.ItemCategoryBinding
import com.example.budgetdeluminator.utils.CurrencyPreferences

class CategoryAdapter(
        private val context: Context,
        private val onCategoryClick: (CategoryWithExpenses) -> Unit,
        private val onCategoryLongClick: (CategoryWithExpenses) -> Unit
) : ListAdapter<CategoryWithExpenses, CategoryAdapter.CategoryViewHolder>(CategoryDiffCallback()) {

    private val currencyPreferences = CurrencyPreferences(context)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding =
                ItemCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CategoryViewHolder(private val binding: ItemCategoryBinding) :
            RecyclerView.ViewHolder(binding.root) {

        fun bind(categoryWithExpenses: CategoryWithExpenses) {
            val category = categoryWithExpenses.category
            val totalSpent = categoryWithExpenses.totalSpent
            val remaining = categoryWithExpenses.remainingBudget
            val percentage = categoryWithExpenses.budgetPercentage
            val isOverBudget = categoryWithExpenses.isOverBudget

            binding.apply {
                tvCategoryName.text = category.name
                tvSpentAmount.text = currencyPreferences.formatAmount(totalSpent)

                // Set remaining amount with proper color using three-color system
                tvRemainingAmount.text = currencyPreferences.formatAmount(remaining)
                tvRemainingAmount.setTextColor(
                        when {
                            isOverBudget -> Color.parseColor("#F44336") // Red for over budget
                            percentage >= 80.0 ->
                                    Color.parseColor("#FF9800") // Orange/Yellow for warning
                            else -> Color.parseColor("#4CAF50") // Green for normal
                        }
                )

                // Set progress bar with three-color system
                progressBarCategory.progress = percentage.toInt().coerceAtMost(100)
                progressBarCategory.progressTintList =
                        android.content.res.ColorStateList.valueOf(
                                when {
                                    isOverBudget ->
                                            Color.parseColor("#F44336") // Red for over budget
                                    percentage >= 80.0 ->
                                            Color.parseColor("#FF9800") // Orange/Yellow for warning
                                    else -> Color.parseColor("#4CAF50") // Green for normal
                                }
                        )

                // Set click listeners
                root.setOnClickListener { onCategoryClick(categoryWithExpenses) }
                root.setOnLongClickListener {
                    onCategoryLongClick(categoryWithExpenses)
                    true
                }
            }
        }
    }

    class CategoryDiffCallback : DiffUtil.ItemCallback<CategoryWithExpenses>() {
        override fun areItemsTheSame(
                oldItem: CategoryWithExpenses,
                newItem: CategoryWithExpenses
        ): Boolean {
            return oldItem.category.id == newItem.category.id
        }

        override fun areContentsTheSame(
                oldItem: CategoryWithExpenses,
                newItem: CategoryWithExpenses
        ): Boolean {
            return oldItem == newItem
        }
    }
}
