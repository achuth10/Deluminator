package com.example.budgetdeluminator.ui.settings

import android.app.AlertDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.budgetdeluminator.data.model.CurrencyManager
import com.example.budgetdeluminator.databinding.ActivitySettingsBinding
import com.example.budgetdeluminator.databinding.DialogCurrencySelectionBinding
import com.example.budgetdeluminator.ui.adapter.CurrencyAdapter
import com.example.budgetdeluminator.utils.CurrencyPreferences

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var currencyPreferences: CurrencyPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currencyPreferences = CurrencyPreferences(this)

        setupToolbar()
        setupCurrencySettings()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun setupCurrencySettings() {
        updateCurrencyDisplay()

        binding.layoutCurrencySelector.setOnClickListener { showCurrencySelectionDialog() }
    }

    private fun updateCurrencyDisplay() {
        val selectedCurrency = currencyPreferences.getSelectedCurrency()
        binding.tvSelectedCurrency.text = "${selectedCurrency.name} (${selectedCurrency.symbol})"
    }

    private fun showCurrencySelectionDialog() {
        val dialogBinding = DialogCurrencySelectionBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(this).setView(dialogBinding.root).create()

        val currencyAdapter =
                CurrencyAdapter(
                        onCurrencyClick = { currency ->
                            currencyPreferences.setSelectedCurrency(currency)
                            updateCurrencyDisplay()
                            dialog.dismiss()
                        },
                        selectedCurrencyCode = currencyPreferences.getSelectedCurrency().code
                )

        dialogBinding.recyclerViewCurrencies.apply {
            adapter = currencyAdapter
            layoutManager = LinearLayoutManager(this@SettingsActivity)
        }

        var filteredCurrencies = CurrencyManager.currencies
        currencyAdapter.submitList(filteredCurrencies)

        // Search functionality
        dialogBinding.etCurrencySearch.addTextChangedListener(
                object : TextWatcher {
                    override fun beforeTextChanged(
                            s: CharSequence?,
                            start: Int,
                            count: Int,
                            after: Int
                    ) {}
                    override fun onTextChanged(
                            s: CharSequence?,
                            start: Int,
                            before: Int,
                            count: Int
                    ) {}
                    override fun afterTextChanged(s: Editable?) {
                        val query = s.toString().trim()
                        filteredCurrencies =
                                if (query.isEmpty()) {
                                    CurrencyManager.currencies
                                } else {
                                    CurrencyManager.currencies.filter { currency ->
                                        currency.name.contains(query, ignoreCase = true) ||
                                                currency.code.contains(query, ignoreCase = true) ||
                                                currency.country.contains(
                                                        query,
                                                        ignoreCase = true
                                                ) ||
                                                currency.symbol.contains(query, ignoreCase = true)
                                    }
                                }
                        currencyAdapter.submitList(filteredCurrencies)
                    }
                }
        )

        dialog.show()
    }
}
