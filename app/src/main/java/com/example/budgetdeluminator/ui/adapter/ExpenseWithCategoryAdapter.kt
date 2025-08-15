package com.example.budgetdeluminator.ui.adapter

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.budgetdeluminator.data.entity.Expense
import com.example.budgetdeluminator.data.model.ExpenseWithCategory
import com.example.budgetdeluminator.databinding.ItemExpenseWithCategoryBinding
import com.example.budgetdeluminator.utils.CurrencyPreferences
import java.text.SimpleDateFormat
import java.util.*

class ExpenseWithCategoryAdapter(
        private val context: Context,
        private val onExpenseClick: (Expense) -> Unit,
        private val onExpenseLongClick: (Expense) -> Unit
) :
        ListAdapter<ExpenseWithCategory, ExpenseWithCategoryAdapter.ExpenseViewHolder>(
                ExpenseWithCategoryDiffCallback()
        ) {

    private val currencyPreferences = CurrencyPreferences(context)
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)
    private val timeFormat = SimpleDateFormat("h:mm a", Locale.US)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewHolder {
        val binding =
                ItemExpenseWithCategoryBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                )
        return ExpenseViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ExpenseViewHolder(private val binding: ItemExpenseWithCategoryBinding) :
            RecyclerView.ViewHolder(binding.root) {

        fun bind(expenseWithCategory: ExpenseWithCategory) {
            val expense = expenseWithCategory.expense
            val category = expenseWithCategory.category

            binding.apply {
                // Expense information
                tvExpenseDescription.text = expense.description.ifEmpty { "Expense" }
                tvExpenseAmount.text = currencyPreferences.formatAmount(expense.amount)

                // Category information
                tvCategoryName.text = category.name

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

        private fun isSameDay(date1: Date, date2: Date): Boolean {
            val cal1 = Calendar.getInstance().apply { time = date1 }
            val cal2 = Calendar.getInstance().apply { time = date2 }
            return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                    cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
        }
    }

    class ExpenseWithCategoryDiffCallback : DiffUtil.ItemCallback<ExpenseWithCategory>() {
        override fun areItemsTheSame(
                oldItem: ExpenseWithCategory,
                newItem: ExpenseWithCategory
        ): Boolean {
            return oldItem.expense.id == newItem.expense.id
        }

        override fun areContentsTheSame(
                oldItem: ExpenseWithCategory,
                newItem: ExpenseWithCategory
        ): Boolean {
            return oldItem == newItem
        }
    }
}
