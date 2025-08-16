package com.example.budgetdeluminator.utils

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

object BiometricAuthHelper {

    private const val BIOMETRIC_PREFS = "biometric_prefs"
    private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"

    fun isBiometricEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(BIOMETRIC_PREFS, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)
    }

    fun setBiometricEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(BIOMETRIC_PREFS, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
    }

    fun isBiometricAvailable(context: Context): Boolean {
        val biometricManager = BiometricManager.from(context)
        return when (biometricManager.canAuthenticate(
                        BiometricManager.Authenticators.BIOMETRIC_WEAK
                )
        ) {
            BiometricManager.BIOMETRIC_SUCCESS -> true
            else -> false
        }
    }

    fun getBiometricUnavailableReason(context: Context): String {
        val biometricManager = BiometricManager.from(context)
        return when (biometricManager.canAuthenticate(
                        BiometricManager.Authenticators.BIOMETRIC_WEAK
                )
        ) {
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE ->
                    "No biometric features available on this device"
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE ->
                    "Biometric features are currently unavailable"
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED ->
                    "No biometric credentials enrolled. Please set up fingerprint or face unlock in device settings"
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED ->
                    "Security update required for biometric authentication"
            BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED ->
                    "Biometric authentication is not supported"
            BiometricManager.BIOMETRIC_STATUS_UNKNOWN -> "Biometric authentication status unknown"
            else -> "Biometric authentication is not available"
        }
    }

    fun authenticate(
            activity: FragmentActivity,
            onSuccess: () -> Unit,
            onError: (String) -> Unit,
            onCancel: () -> Unit = {}
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val biometricPrompt =
                BiometricPrompt(
                        activity,
                        executor,
                        object : BiometricPrompt.AuthenticationCallback() {
                            override fun onAuthenticationError(
                                    errorCode: Int,
                                    errString: CharSequence
                            ) {
                                super.onAuthenticationError(errorCode, errString)
                                if (errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                                                errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON
                                ) {
                                    onCancel()
                                } else {
                                    onError(errString.toString())
                                }
                            }

                            override fun onAuthenticationSucceeded(
                                    result: BiometricPrompt.AuthenticationResult
                            ) {
                                super.onAuthenticationSucceeded(result)
                                onSuccess()
                            }

                            override fun onAuthenticationFailed() {
                                super.onAuthenticationFailed()
                                onError("Authentication failed. Please try again.")
                            }
                        }
                )

        val promptInfo =
                BiometricPrompt.PromptInfo.Builder()
                        .setTitle("Authenticate to access Budget Deluminator")
                        .setSubtitle("Use your fingerprint or face to unlock the app")
                        .setNegativeButtonText("Cancel")
                        .build()

        biometricPrompt.authenticate(promptInfo)
    }
}
