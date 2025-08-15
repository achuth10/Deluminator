package com.example.budgetdeluminator.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.budgetdeluminator.data.entity.BudgetCategory
import com.example.budgetdeluminator.databinding.ItemCategoryManagementBinding
import java.text.NumberFormat
import java.util.*

class CategoryManagementAdapter(
    private val onEditClick: (BudgetCategory) -> Unit,
    private val onDeleteClick: (BudgetCategory) -> Unit
) : ListAdapter<BudgetCategory, CategoryManagementAdapter.CategoryViewHolder>(CategoryDiffCallback()) {

    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemCategoryManagementBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CategoryViewHolder(private val binding: ItemCategoryManagementBinding) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(category: BudgetCategory) {
            binding.apply {
                tvCategoryName.text = category.name
                tvBudgetLimit.text = "Budget: ${currencyFormat.format(category.budgetLimit)}"
                
                btnEditCategory.setOnClickListener { onEditClick(category) }
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
