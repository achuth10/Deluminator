package com.example.budgetdeluminator.ui.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.budgetdeluminator.data.entity.BudgetCategory
import com.example.budgetdeluminator.databinding.ItemCategorySelectionBinding

class CategorySelectionAdapter(
    private val onCategorySelected: (BudgetCategory) -> Unit
) : ListAdapter<BudgetCategory, CategorySelectionAdapter.CategoryViewHolder>(CategoryDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemCategorySelectionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CategoryViewHolder(private val binding: ItemCategorySelectionBinding) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(category: BudgetCategory) {
            binding.apply {
                tvCategorySelectionName.text = category.name
                
                // Set category color
                try {
                    val color = Color.parseColor(category.color)
                    viewCategoryColorIndicator.backgroundTintList = android.content.res.ColorStateList.valueOf(color)
                } catch (e: IllegalArgumentException) {
                    viewCategoryColorIndicator.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#4CAF50"))
                }
                
                // Hide favorite star for now
                ivCategoryFavorite.visibility = android.view.View.GONE
                
                root.setOnClickListener { onCategorySelected(category) }
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
