package com.example.budgetdeluminator.ui.adapter

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.budgetdeluminator.data.entity.BudgetCategory
import com.example.budgetdeluminator.databinding.ItemCategoryManagementBinding
import com.example.budgetdeluminator.utils.CurrencyPreferences

class CategoryManagementAdapter(
        private val context: Context,
        private val onEditClick: (BudgetCategory) -> Unit,
        private val onDeleteClick: (BudgetCategory) -> Unit
) :
        ListAdapter<BudgetCategory, CategoryManagementAdapter.CategoryViewHolder>(
                CategoryDiffCallback()
        ) {

    private val currencyPreferences = CurrencyPreferences(context)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding =
                ItemCategoryManagementBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                )
        return CategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CategoryViewHolder(private val binding: ItemCategoryManagementBinding) :
            RecyclerView.ViewHolder(binding.root) {

        fun bind(category: BudgetCategory) {
            binding.apply {
                tvCategoryName.text = category.name
                tvBudgetLimit.text =
                        if (category.budgetLimit != null) {
                            "Budget: ${currencyPreferences.formatAmount(category.budgetLimit)}"
                        } else {
                            "Unlimited"
                        }

                // Set card background color based on category color
                try {
                    val color = Color.parseColor(category.color)
                    root.setCardBackgroundColor(color)
                } catch (e: IllegalArgumentException) {
                    // Default to green if color parsing fails
                    root.setCardBackgroundColor(Color.parseColor("#4CAF50"))
                }

                // Set click listener on the entire card for editing
                root.setOnClickListener { onEditClick(category) }
                btnDeleteCategory.setOnClickListener { onDeleteClick(category) }
            }
        }
    }

    class CategoryDiffCallback : DiffUtil.ItemCallback<BudgetCategory>() {
        override fun areItemsTheSame(oldItem: BudgetCategory, newItem: BudgetCategory): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: BudgetCategory, newItem: BudgetCategory): Boolean {
            return oldItem == newItem
        }
    }
}
