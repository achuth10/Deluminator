package com.example.budgetdeluminator.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.budgetdeluminator.R
import com.example.budgetdeluminator.data.entity.RecurringExpense
import com.example.budgetdeluminator.data.entity.RecurrenceType
import com.example.budgetdeluminator.databinding.ItemRecurringExpenseBinding
import com.example.budgetdeluminator.utils.CurrencyPreferences
import java.text.SimpleDateFormat
import java.util.*

class RecurringExpenseAdapter(
    private val context: Context,
    private val onExpenseClick: (RecurringExpense) -> Unit,
    private val onExpenseLongClick: (RecurringExpense) -> Unit,
    private val onToggleActiveClick: (RecurringExpense, Boolean) -> Unit
) : ListAdapter<RecurringExpense, RecurringExpenseAdapter.RecurringExpenseViewHolder>(
    RecurringExpenseDiffCallback()
) {

    private val currencyPreferences = CurrencyPreferences(context)
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecurringExpenseViewHolder {
        val binding = ItemRecurringExpenseBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RecurringExpenseViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecurringExpenseViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class RecurringExpenseViewHolder(private val binding: ItemRecurringExpenseBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(recurringExpense: RecurringExpense) {
            binding.apply {
                // Basic expense info
                tvExpenseAmount.text = currencyPreferences.formatAmount(recurringExpense.amount)
                tvExpenseDescription.text = recurringExpense.description.takeIf { it.isNotBlank() }
                    ?: "No description"

                // Recurrence info
                tvRecurrenceInfo.text = getRecurrenceDisplayText(recurringExpense)

                // Last generated info
                if (recurringExpense.lastGeneratedAt > 0) {
                    tvLastGenerated.text = "Last: ${dateFormat.format(Date(recurringExpense.lastGeneratedAt))}"
                    tvLastGenerated.visibility = View.VISIBLE
                } else {
                    tvLastGenerated.text = "Never generated"
                    tvLastGenerated.visibility = View.VISIBLE
                }

                // Active status
                switchActive.isChecked = recurringExpense.isActive
                switchActive.setOnCheckedChangeListener { _, isChecked ->
                    onToggleActiveClick(recurringExpense, isChecked)
                }

                // Status indicator
                val statusColor = if (recurringExpense.isActive) {
                    context.getColor(R.color.success_color)
                } else {
                    context.getColor(R.color.md_theme_light_outline)
                }
                viewStatusIndicator.setBackgroundColor(statusColor)

                // Click listeners
                root.setOnClickListener { onExpenseClick(recurringExpense) }
                root.setOnLongClickListener {
                    onExpenseLongClick(recurringExpense)
                    true
                }

                // Apply neumorphic styling based on active status [[memory:6322481]]
                val alpha = if (recurringExpense.isActive) 1.0f else 0.6f
                root.alpha = alpha
            }
        }

        private fun getRecurrenceDisplayText(recurringExpense: RecurringExpense): String {
            return when (recurringExpense.recurrenceType) {
                RecurrenceType.DAILY -> {
                    val hour = recurringExpense.recurrenceValue
                    val timeFormat = SimpleDateFormat("h:mm a", Locale.US)
                    val calendar = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, hour)
                        set(Calendar.MINUTE, 0)
                    }
                    "Daily at ${timeFormat.format(calendar.time)}"
                }
                RecurrenceType.WEEKLY -> {
                    val dayOfWeek = when (recurringExpense.recurrenceValue) {
                        1 -> "Sunday"
                        2 -> "Monday"
                        3 -> "Tuesday"
                        4 -> "Wednesday"
                        5 -> "Thursday"
                        6 -> "Friday"
                        7 -> "Saturday"
                        else -> "Invalid day"
                    }
                    "Weekly on $dayOfWeek at 5:00 AM"
                }
                RecurrenceType.MONTHLY -> {
                    val dayOfMonth = recurringExpense.recurrenceValue
                    val suffix = when {
                        dayOfMonth in 11..13 -> "th"
                        dayOfMonth % 10 == 1 -> "st"
                        dayOfMonth % 10 == 2 -> "nd"
                        dayOfMonth % 10 == 3 -> "rd"
                        else -> "th"
                    }
                    "Monthly on the $dayOfMonth$suffix at 5:00 AM"
                }
            }
        }
    }
}

class RecurringExpenseDiffCallback : DiffUtil.ItemCallback<RecurringExpense>() {
    override fun areItemsTheSame(oldItem: RecurringExpense, newItem: RecurringExpense): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: RecurringExpense, newItem: RecurringExpense): Boolean {
        return oldItem == newItem
    }
}
