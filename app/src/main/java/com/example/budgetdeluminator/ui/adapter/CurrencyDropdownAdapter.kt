package com.example.budgetdeluminator.ui.adapter

import android.content.Context
import android.widget.ArrayAdapter
import android.widget.Filter
import com.example.budgetdeluminator.data.model.Currency

class CurrencyDropdownAdapter(context: Context, currencies: List<Currency>) :
        ArrayAdapter<String>(context, android.R.layout.simple_dropdown_item_1line) {

    private val allCurrencies = currencies.sortedBy { it.name }
    private var filteredCurrencies = allCurrencies.toMutableList()

    init {
        addAll(allCurrencies.map { "${it.name} (${it.symbol})" })
    }

    fun getCurrencyAt(position: Int): Currency? {
        return if (position >= 0 && position < filteredCurrencies.size) {
            filteredCurrencies[position]
        } else null
    }

    fun getPositionOfCurrency(currency: Currency): Int {
        return allCurrencies.indexOfFirst { it.code == currency.code }
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val query = constraint?.toString()?.lowercase() ?: ""

                val filtered =
                        if (query.isEmpty()) {
                            allCurrencies
                        } else {
                            allCurrencies.filter { currency ->
                                currency.name.lowercase().contains(query) ||
                                        currency.code.lowercase().contains(query) ||
                                        currency.country.lowercase().contains(query) ||
                                        currency.symbol.contains(query)
                            }
                        }

                return FilterResults().apply {
                    values = filtered
                    count = filtered.size
                }
            }

            @Suppress("UNCHECKED_CAST")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                filteredCurrencies.clear()
                if (results?.values != null) {
                    filteredCurrencies.addAll(results.values as List<Currency>)
                }

                clear()
                addAll(filteredCurrencies.map { "${it.name} (${it.symbol})" })
                notifyDataSetChanged()
            }
        }
    }
}
