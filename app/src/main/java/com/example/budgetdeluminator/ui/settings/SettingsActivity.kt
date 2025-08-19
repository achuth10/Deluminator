package com.example.budgetdeluminator.ui.settings

import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.budgetdeluminator.data.model.CurrencyManager
import com.example.budgetdeluminator.databinding.ActivitySettingsBinding
import com.example.budgetdeluminator.ui.adapter.CurrencyDropdownAdapter
import com.example.budgetdeluminator.ui.adapter.ThemeDropdownAdapter
import com.example.budgetdeluminator.utils.BiometricAuthHelper
import com.example.budgetdeluminator.utils.CurrencyPreferences
import com.example.budgetdeluminator.utils.ThemePreferences

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var currencyPreferences: CurrencyPreferences
    private lateinit var themePreferences: ThemePreferences
    private lateinit var spendingRoastPrefs: SharedPreferences

    companion object {
        const val SPENDING_ROAST_PREFS = "spending_roast_prefs"
        const val KEY_SPENDING_ROAST_ENABLED = "spending_roast_enabled"
        const val RESULT_CURRENCY_CHANGED = "currency_changed"
    }

    private var currencyChanged = false

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply theme before calling super.onCreate()
        themePreferences = ThemePreferences(this)
        themePreferences.applyTheme()

        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currencyPreferences = CurrencyPreferences(this)
        spendingRoastPrefs = getSharedPreferences(SPENDING_ROAST_PREFS, MODE_PRIVATE)

        // Set secure flag if biometric authentication is enabled
        BiometricAuthHelper.updateSecureFlag(this)

        setupToolbar()
        setupCurrencySettings()
        setupThemeSettings()
        setupSpendingRoastSettings()
        setupBiometricSettings()
        setupInfoButtons()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun finish() {
        if (currencyChanged) {
            val resultIntent = Intent()
            resultIntent.putExtra(RESULT_CURRENCY_CHANGED, true)
            setResult(RESULT_OK, resultIntent)
        }
        super.finish()
    }

    private fun setupCurrencySettings() {
        val currencyAdapter = CurrencyDropdownAdapter(this, CurrencyManager.currencies)
        binding.actvCurrency.setAdapter(currencyAdapter)

        // Configure AutoCompleteTextView for search functionality
        binding.actvCurrency.threshold = 1 // Start filtering after 1 character
        binding.actvCurrency.dropDownHeight = 600 // Set reasonable dropdown height

        // Set initial selection
        val selectedCurrency = currencyPreferences.getSelectedCurrency()
        binding.actvCurrency.setText("${selectedCurrency.name} (${selectedCurrency.symbol})", false)

        // Handle selection
        binding.actvCurrency.setOnItemClickListener { _, _, position, _ ->
            val currency = currencyAdapter.getCurrencyAt(position)
            currency?.let {
                currencyPreferences.setSelectedCurrency(it)
                binding.actvCurrency.setText("${it.name} (${it.symbol})", false)
                currencyChanged = true
            }
        }

        // Handle text changes to ensure valid selection
        binding.actvCurrency.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                // Reset to current selection if invalid text
                val currentCurrency = currencyPreferences.getSelectedCurrency()
                binding.actvCurrency.setText(
                        "${currentCurrency.name} (${currentCurrency.symbol})",
                        false
                )
            }
        }
    }

    private fun setupThemeSettings() {
        val themeValues = themePreferences.getAvailableThemes()
        val themeNames = themeValues.map { themePreferences.getThemeDisplayName(it) }
        val themeAdapter = ThemeDropdownAdapter(this, themeNames, themeValues)
        binding.actvTheme.setAdapter(themeAdapter)

        // Set initial selection
        val selectedTheme = themePreferences.getThemeMode()
        val initialPosition = themeAdapter.getPositionOfThemeValue(selectedTheme)
        if (initialPosition >= 0) {
            binding.actvTheme.setText(themePreferences.getThemeDisplayName(selectedTheme), false)
        }

        // Handle selection
        binding.actvTheme.setOnItemClickListener { _, _, position, _ ->
            val themeValue = themeAdapter.getThemeValueAt(position)
            themeValue?.let {
                themePreferences.setThemeMode(it)
                themePreferences.applyTheme()
                binding.actvTheme.setText(themePreferences.getThemeDisplayName(it), false)

                // Recreate activity to apply theme change
                recreate()
            }
        }
    }

    private fun setupSpendingRoastSettings() {
        // Set initial state
        val isEnabled = spendingRoastPrefs.getBoolean(KEY_SPENDING_ROAST_ENABLED, true)
        binding.switchSpendingRoast.isChecked = isEnabled

        // Handle switch toggle
        binding.switchSpendingRoast.setOnCheckedChangeListener { _, isChecked ->
            spendingRoastPrefs.edit().putBoolean(KEY_SPENDING_ROAST_ENABLED, isChecked).apply()
        }

        // Handle layout click to toggle switch
        binding.layoutSpendingRoastToggle.setOnClickListener {
            binding.switchSpendingRoast.toggle()
        }
    }

    private fun setupBiometricSettings() {
        // Set initial state
        val isEnabled = BiometricAuthHelper.isBiometricEnabled(this)
        binding.switchBiometric.isChecked = isEnabled

        // Check if biometric authentication is available
        val isBiometricAvailable = BiometricAuthHelper.isBiometricAvailable(this)

        if (!isBiometricAvailable) {
            // Disable the switch if biometric is not available
            binding.switchBiometric.isEnabled = false
            binding.layoutBiometricToggle.alpha = 0.5f
        }

        // Handle switch toggle
        binding.switchBiometric.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked && !isBiometricAvailable) {
                // Show error and revert switch
                binding.switchBiometric.isChecked = false
                Toast.makeText(
                                this,
                                BiometricAuthHelper.getBiometricUnavailableReason(this),
                                Toast.LENGTH_LONG
                        )
                        .show()
                return@setOnCheckedChangeListener
            }

            if (isChecked) {
                // Test biometric authentication before enabling
                BiometricAuthHelper.authenticate(
                        this,
                        onSuccess = {
                            BiometricAuthHelper.setBiometricEnabled(this, true)
                            Toast.makeText(
                                            this,
                                            "Biometric authentication enabled",
                                            Toast.LENGTH_SHORT
                                    )
                                    .show()
                        },
                        onError = { error ->
                            binding.switchBiometric.isChecked = false
                            Toast.makeText(this, "Failed to enable: $error", Toast.LENGTH_LONG)
                                    .show()
                        },
                        onCancel = { binding.switchBiometric.isChecked = false }
                )
            } else {
                BiometricAuthHelper.setBiometricEnabled(this, false)
                Toast.makeText(this, "Biometric authentication disabled", Toast.LENGTH_SHORT).show()
            }
        }

        // Handle layout click to toggle switch (only if biometric is available)
        binding.layoutBiometricToggle.setOnClickListener {
            if (isBiometricAvailable) {
                binding.switchBiometric.toggle()
            } else {
                Toast.makeText(
                                this,
                                BiometricAuthHelper.getBiometricUnavailableReason(this),
                                Toast.LENGTH_LONG
                        )
                        .show()
            }
        }
    }

    private fun setupInfoButtons() {
        binding.ivCurrencyInfo.setOnClickListener {
            showInfoDialog(
                    "Currency Settings",
                    "The selected currency will be used throughout the app for displaying amounts, budgets, and expenses. You can search and select from a comprehensive list of world currencies."
            )
        }

        binding.ivThemeInfo.setOnClickListener {
            showInfoDialog(
                    "Theme Settings",
                    "Choose your preferred theme appearance:\n\n• Light - Bright interface with light backgrounds\n• Dark - Dark interface that's easier on the eyes\n• System Default - Automatically matches your device's theme setting"
            )
        }

        binding.ivSpendingRoastInfo.setOnClickListener {
            showInfoDialog(
                    "Spending Roasts",
                    "Enable witty and humorous comments about your spending habits that appear on the home screen. These playful roasts change each time you open the app and are designed to make budget tracking more engaging and fun! 😏"
            )
        }

        binding.ivBiometricInfo.setOnClickListener {
            showInfoDialog(
                    "Biometric Authentication",
                    "Secure your budget app with fingerprint or face unlock. When enabled:\n\n• You'll need to authenticate with biometrics each time you open the app\n• App content will be hidden from the recent apps screen\n• Screenshots will be prevented for enhanced security\n\nNote: Your device must have biometric authentication set up in system settings for this feature to work."
            )
        }
    }

    private fun showInfoDialog(title: String, message: String) {
        AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Got it", null)
                .show()
    }

    fun isSpendingRoastEnabled(): Boolean {
        return spendingRoastPrefs.getBoolean(KEY_SPENDING_ROAST_ENABLED, true)
    }
}
