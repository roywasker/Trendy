package com.trendy.shared.util

import com.trendy.shared.navigation.Screen

object PayPalCallback {
    fun parse(success: String?, cancel: String?, token: String?): Screen.PaymentCompleted {
        val orderToken = token?.takeIf { it.isNotBlank() }
        return when {
            cancel?.toBooleanStrictOrNull() == true -> Screen.PaymentCompleted(
                isSuccess = false, error = "Payment has been canceled.", token = orderToken
            )
            success?.toBooleanStrictOrNull() == true && orderToken != null ->
                Screen.PaymentCompleted(isSuccess = true, error = null, token = orderToken)
            success?.toBooleanStrictOrNull() == true -> Screen.PaymentCompleted(
                isSuccess = false, error = "Payment confirmation is missing an order reference.", token = null
            )
            else -> Screen.PaymentCompleted(
                isSuccess = false, error = "Payment could not be confirmed.", token = orderToken
            )
        }
    }
}
