package com.example.budgetdeluminator.ui.settings

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.MenuItem
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.budgetdeluminator.MainActivity
import com.example.budgetdeluminator.R
import com.example.budgetdeluminator.data.model.CurrencyManager
import com.example.budgetdeluminator.databinding.ActivitySettingsBinding
import com.example.budgetdeluminator.databinding.DialogCurrencySelectionBinding
import com.example.budgetdeluminator.ui.adapter.CurrencyAdapter
import com.example.budgetdeluminator.ui.categories.CategoriesActivity
import com.example.budgetdeluminator.utils.CurrencyPreferences
import com.google.android.material.navigation.NavigationView

class SettingsActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var drawerToggle: ActionBarDrawerToggle
    private lateinit var currencyPreferences: CurrencyPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currencyPreferences = CurrencyPreferences(this)

        setupToolbar()
        setupNavigationDrawer()
        setupCurrencySettings()
        setupBackPressedHandler()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun setupNavigationDrawer() {
        drawerToggle =
                ActionBarDrawerToggle(
                        this,
                        binding.drawerLayout,
                        binding.toolbar,
                        R.string.app_name,
                        R.string.app_name
                )
        binding.drawerLayout.addDrawerListener(drawerToggle)
        drawerToggle.syncState()

        binding.navigationView.setNavigationItemSelectedListener(this)

        // Set the current menu item as selected
        binding.navigationView.setCheckedItem(R.id.nav_settings)
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

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_home -> {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
            R.id.nav_categories -> {
                startActivity(Intent(this, CategoriesActivity::class.java))
                finish()
            }
            R.id.nav_settings -> {
                // Already on settings screen
            }
        }
        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun setupBackPressedHandler() {
        onBackPressedDispatcher.addCallback(
                this,
                object : androidx.activity.OnBackPressedCallback(true) {
                    override fun handleOnBackPressed() {
                        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                            binding.drawerLayout.closeDrawer(GravityCompat.START)
                        } else {
                            finish()
                        }
                    }
                }
        )
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (drawerToggle.onOptionsItemSelected(item)) {
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }
}
