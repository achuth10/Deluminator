package com.example.budgetdeluminator.ui.adapter

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.budgetdeluminator.data.entity.Expense
import com.example.budgetdeluminator.data.model.ExpenseListItem
import com.example.budgetdeluminator.databinding.ItemDateHeaderBinding
import com.example.budgetdeluminator.databinding.ItemExpenseWithCategoryBinding
import com.example.budgetdeluminator.utils.CurrencyPreferences
import java.util.*

class GroupedExpenseAdapter(
        private val context: Context,
        private val onExpenseClick: (Expense) -> Unit,
        private val onExpenseLongClick: (Expense) -> Unit
) : ListAdapter<ExpenseListItem, RecyclerView.ViewHolder>(ExpenseListItemDiffCallback()) {

    private val currencyPreferences = CurrencyPreferences(context)

    companion object {
        private const val TYPE_DATE_HEADER = 0
        private const val TYPE_EXPENSE_ITEM = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is ExpenseListItem.DateHeader -> TYPE_DATE_HEADER
            is ExpenseListItem.ExpenseItem -> TYPE_EXPENSE_ITEM
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_DATE_HEADER -> {
                val binding =
                        ItemDateHeaderBinding.inflate(
                                LayoutInflater.from(parent.context),
                                parent,
                                false
                        )
                DateHeaderViewHolder(binding)
            }
            TYPE_EXPENSE_ITEM -> {
                val binding =
                        ItemExpenseWithCategoryBinding.inflate(
                                LayoutInflater.from(parent.context),
                                parent,
                                false
                        )
                ExpenseViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is ExpenseListItem.DateHeader -> {
                (holder as DateHeaderViewHolder).bind(item)
            }
            is ExpenseListItem.ExpenseItem -> {
                (holder as ExpenseViewHolder).bind(item.expenseWithCategory)
            }
        }
    }

    inner class DateHeaderViewHolder(private val binding: ItemDateHeaderBinding) :
            RecyclerView.ViewHolder(binding.root) {

        fun bind(dateHeader: ExpenseListItem.DateHeader) {
            binding.apply {
                tvDateHeader.text = dateHeader.formattedDate
                tvDailyTotal.text = currencyPreferences.formatAmount(dateHeader.totalAmount)
            }
        }
    }

    inner class ExpenseViewHolder(private val binding: ItemExpenseWithCategoryBinding) :
            RecyclerView.ViewHolder(binding.root) {

        fun bind(
                expenseWithCategory: com.example.budgetdeluminator.data.model.ExpenseWithCategory
        ) {
            val expense = expenseWithCategory.expense
            val category = expenseWithCategory.category

            binding.apply {
                // Category information
                tvCategoryName.text = category.name
                tvExpenseDescription.text = expense.description.ifEmpty { category.name }
                tvExpenseAmount.text = currencyPreferences.formatAmount(expense.amount)

                // Set card background color based on category color
                try {
                    val color = Color.parseColor(category.color)
                    cardExpense.setCardBackgroundColor(color)
                } catch (e: IllegalArgumentException) {
                    // Default colors for different categories
                    val defaultColor =
                            when (category.name.lowercase()) {
                                "entertainment" -> Color.parseColor("#E91E63") // Pink
                                "food" -> Color.parseColor("#FF9800") // Orange
                                "family" -> Color.parseColor("#9C27B0") // Purple
                                "grocery" -> Color.parseColor("#4CAF50") // Green
                                "living" -> Color.parseColor("#00BCD4") // Cyan
                                else -> Color.parseColor("#607D8B") // Blue Grey
                            }
                    cardExpense.setCardBackgroundColor(defaultColor)
                }

                // Set click listeners
                root.setOnClickListener { onExpenseClick(expense) }
                root.setOnLongClickListener {
                    onExpenseLongClick(expense)
                    true
                }
            }
        }
    }

    class ExpenseListItemDiffCallback : DiffUtil.ItemCallback<ExpenseListItem>() {
        override fun areItemsTheSame(oldItem: ExpenseListItem, newItem: ExpenseListItem): Boolean {
            return when {
                oldItem is ExpenseListItem.DateHeader && newItem is ExpenseListItem.DateHeader -> {
                    oldItem.formattedDate == newItem.formattedDate
                }
                oldItem is ExpenseListItem.ExpenseItem &&
                        newItem is ExpenseListItem.ExpenseItem -> {
                    oldItem.expenseWithCategory.expense.id == newItem.expenseWithCategory.expense.id
                }
                else -> false
            }
        }

        override fun areContentsTheSame(
                oldItem: ExpenseListItem,
                newItem: ExpenseListItem
        ): Boolean {
            return oldItem == newItem
        }
    }
}
