package com.example.budgetdeluminator.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.budgetdeluminator.data.model.Currency
import com.example.budgetdeluminator.databinding.ItemCurrencyBinding

class CurrencyAdapter(
        private val onCurrencyClick: (Currency) -> Unit,
        private var selectedCurrencyCode: String = ""
) : ListAdapter<Currency, CurrencyAdapter.CurrencyViewHolder>(CurrencyDiffCallback()) {

    fun updateSelectedCurrency(currencyCode: String) {
        val oldSelectedIndex = currentList.indexOfFirst { it.code == selectedCurrencyCode }
        val newSelectedIndex = currentList.indexOfFirst { it.code == currencyCode }

        selectedCurrencyCode = currencyCode

        if (oldSelectedIndex != -1) notifyItemChanged(oldSelectedIndex)
        if (newSelectedIndex != -1) notifyItemChanged(newSelectedIndex)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CurrencyViewHolder {
        val binding =
                ItemCurrencyBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CurrencyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CurrencyViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CurrencyViewHolder(private val binding: ItemCurrencyBinding) :
            RecyclerView.ViewHolder(binding.root) {

        fun bind(currency: Currency) {
            binding.apply {
                tvCurrencySymbol.text = currency.symbol
                tvCurrencyName.text = currency.name
                tvCurrencyCountry.text = currency.country

                // Show selection indicator
                ivSelectedIndicator.visibility =
                        if (currency.code == selectedCurrencyCode) {
                            View.VISIBLE
                        } else {
                            View.GONE
                        }

                root.setOnClickListener { onCurrencyClick(currency) }
            }
        }
    }

    class CurrencyDiffCallback : DiffUtil.ItemCallback<Currency>() {
        override fun areItemsTheSame(oldItem: Currency, newItem: Currency): Boolean {
            return oldItem.code == newItem.code
        }

        override fun areContentsTheSame(oldItem: Currency, newItem: Currency): Boolean {
            return oldItem == newItem
        }
    }
}
