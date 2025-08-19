package com.example.budgetdeluminator.ui.adapter

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.budgetdeluminator.data.model.CategoryWithExpenses
import com.example.budgetdeluminator.databinding.ItemCategoryBinding
import com.example.budgetdeluminator.utils.CurrencyPreferences

class CategoryAdapter(
        private val context: Context,
        private val onCategoryClick: (CategoryWithExpenses) -> Unit,
        private val onCategoryReorder: (List<CategoryWithExpenses>) -> Unit
) : ListAdapter<CategoryWithExpenses, CategoryAdapter.CategoryViewHolder>(CategoryDiffCallback()) {

    private val currencyPreferences = CurrencyPreferences(context)
    private var currentList = mutableListOf<CategoryWithExpenses>()

    override fun submitList(list: List<CategoryWithExpenses>?) {
        super.submitList(list)
        currentList.clear()
        if (list != null) {
            currentList.addAll(list)
        }
    }

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
            val isTrackingOnly = categoryWithExpenses.isTrackingOnly

            binding.apply {
                tvCategoryName.text = category.name
                tvSpentAmount.text = currencyPreferences.formatAmountWithoutDecimals(totalSpent)

                if (isTrackingOnly) {
                    // Tracking-only category display
                    tvRemainingAmount.text = "Tracking Only"
                    tvRemainingAmount.setTextColor(Color.parseColor("#757575")) // Neutral gray

                    // Hide progress bar for tracking-only categories
                    progressBarCategory.visibility = View.GONE
                } else {
                    // Budget category display (existing logic)
                    tvRemainingAmount.text =
                            currencyPreferences.formatAmountWithoutDecimals(remaining!!)
                    tvRemainingAmount.setTextColor(
                            when {
                                isOverBudget -> Color.parseColor("#F44336") // Red for over budget
                                percentage >= 80.0 ->
                                        Color.parseColor("#FF9800") // Orange/Yellow for warning
                                else -> Color.parseColor("#4CAF50") // Green for normal
                            }
                    )

                    // Show and set progress bar with three-color system
                    progressBarCategory.visibility = View.VISIBLE
                    progressBarCategory.progress = percentage.toInt().coerceAtMost(100)
                    progressBarCategory.progressTintList =
                            android.content.res.ColorStateList.valueOf(
                                    when {
                                        isOverBudget ->
                                                Color.parseColor("#F44336") // Red for over budget
                                        percentage >= 80.0 ->
                                                Color.parseColor(
                                                        "#FF9800"
                                                ) // Orange/Yellow for warning
                                        else -> Color.parseColor("#4CAF50") // Green for normal
                                    }
                            )
                }

                // Set click listeners
                root.setOnClickListener { onCategoryClick(categoryWithExpenses) }
                // Long press is handled by ItemTouchHelper for drag and drop functionality
            }
        }
    }

    // Drag and drop functionality
    fun onItemMove(fromPosition: Int, toPosition: Int): Boolean {
        if (fromPosition < currentList.size && toPosition < currentList.size) {
            val movedItem = currentList.removeAt(fromPosition)
            currentList.add(toPosition, movedItem)
            notifyItemMoved(fromPosition, toPosition)
            return true
        }
        return false
    }

    fun onItemMoveFinished() {
        onCategoryReorder(currentList.toList())
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

class CategoryItemTouchHelperCallback(private val adapter: CategoryAdapter) :
        ItemTouchHelper.Callback() {

    override fun isLongPressDragEnabled(): Boolean = true
    override fun isItemViewSwipeEnabled(): Boolean = false

    override fun getMovementFlags(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder
    ): Int {
        val dragFlags =
                ItemTouchHelper.UP or
                        ItemTouchHelper.DOWN or
                        ItemTouchHelper.LEFT or
                        ItemTouchHelper.RIGHT
        return makeMovementFlags(dragFlags, 0)
    }

    override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
    ): Boolean {
        return adapter.onItemMove(viewHolder.adapterPosition, target.adapterPosition)
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        // Not used since swiping is disabled
    }

    override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
        super.onSelectedChanged(viewHolder, actionState)
        if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
            viewHolder?.itemView?.alpha = 0.8f
        }
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)
        viewHolder.itemView.alpha = 1.0f
        adapter.onItemMoveFinished()
    }
}
