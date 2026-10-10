package com.trendy.shared.util

import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.Settings
import com.russhwolf.settings.observable.makeObservable
import com.trendy.shared.navigation.Screen
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

@OptIn(ExperimentalSettingsApi::class)
class PreferencesRepository(
    private val settings: ObservableSettings
) {
    constructor() : this(Settings().makeObservable())

    private val IS_SUCCESS = "isSuccess_paypal"
    private val ERROR = "error_paypal"
    private val TOKEN = "token_paypal"

    fun savePayPalData(
        isSuccess: Boolean?,
        error: String?,
        token: String?,
    ) {
        reset()
        isSuccess?.let { settings.putBoolean(IS_SUCCESS, it) }
        error?.let { settings.putString(ERROR, it) }
        token?.let { settings.putString(TOKEN, it) }
    }

    fun readPayPalDataFlow(): Flow<Screen.PaymentCompleted?> = callbackFlow {
        fun getCurrentPaymentProcessed(): Screen.PaymentCompleted? {
            if (!settings.hasKey(IS_SUCCESS) && !settings.hasKey(ERROR) && !settings.hasKey(TOKEN)) return null
            return Screen.PaymentCompleted(
                isSuccess = settings.getBooleanOrNull(IS_SUCCESS),
                error = settings.getStringOrNull(ERROR),
                token = settings.getStringOrNull(TOKEN)
            )
        }

        while (true) {
            this.send(getCurrentPaymentProcessed())
            delay(1000) // Check for updates every second
        }
    }

    fun reset() {
        settings.remove(IS_SUCCESS)
        settings.remove(ERROR)
        settings.remove(TOKEN)
    }
}
