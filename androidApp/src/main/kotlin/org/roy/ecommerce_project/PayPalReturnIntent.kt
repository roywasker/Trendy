package org.roy.ecommerce_project

import android.net.Uri
import com.trendy.shared.navigation.Screen
import com.trendy.shared.util.PayPalCallback

object PayPalReturnIntent {
    fun parse(uri: Uri?): Screen.PaymentCompleted? {
        if (uri?.scheme != "com.example.trendy" || !uri.isHierarchical || uri.host != "paypalpay") return null
        return PayPalCallback.parse(
            uri.getQueryParameter("success"), uri.getQueryParameter("cancel"), uri.getQueryParameter("token")
        )
    }
}
