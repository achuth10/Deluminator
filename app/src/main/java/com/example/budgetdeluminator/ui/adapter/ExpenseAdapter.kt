package com.example.budgetdeluminator.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.budgetdeluminator.data.entity.Expense
import com.example.budgetdeluminator.databinding.ItemExpenseBinding
import com.example.budgetdeluminator.utils.CurrencyPreferences
import java.text.SimpleDateFormat
import java.util.*

class ExpenseAdapter(
        private val context: Context,
        private val onExpenseClick: (Expense) -> Unit,
        private val onExpenseLongClick: (Expense) -> Unit
) : ListAdapter<Expense, ExpenseAdapter.ExpenseViewHolder>(ExpenseDiffCallback()) {

    private val currencyPreferences = CurrencyPreferences(context)
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)
    private val timeFormat = SimpleDateFormat("h:mm a", Locale.US)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewHolder {
        val binding = ItemExpenseBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ExpenseViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ExpenseViewHolder(private val binding: ItemExpenseBinding) :
            RecyclerView.ViewHolder(binding.root) {

        fun bind(expense: Expense) {
            binding.apply {
                tvExpenseDescription.text = expense.description.ifEmpty { "Expense" }
                tvExpenseAmount.text = currencyPreferences.formatAmount(expense.amount)

                // Format date
                val date = Date(expense.createdAt)
                val today = Date()
                val yesterday = Date(today.time - 24 * 60 * 60 * 1000)

                tvExpenseDate.text =
                        when {
                            isSameDay(date, today) -> "Today, ${timeFormat.format(date)}"
                            isSameDay(date, yesterday) -> "Yesterday, ${timeFormat.format(date)}"
                            else -> dateFormat.format(date)
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

    class ExpenseDiffCallback : DiffUtil.ItemCallback<Expense>() {
        override fun areItemsTheSame(oldItem: Expense, newItem: Expense): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Expense, newItem: Expense): Boolean {
            return oldItem == newItem
        }
    }
}
