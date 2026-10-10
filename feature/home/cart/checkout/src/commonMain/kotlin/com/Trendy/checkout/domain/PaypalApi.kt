package com.Trendy.checkout.domain

import com.trendy.shared.domain.CartItem
import com.trendy.shared.domain.Order
import com.trendy.shared.domain.ShippingDetails
import com.trendy.shared.util.toCents
import com.trendy.shared.util.toUsdString
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import openWebBrowser

fun createPaypalHttpClient(): HttpClient = HttpClient {
    install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true; encodeDefaults = true }) }
}
class PaypalApi(
    private val service: PaymentService = FirebasePaymentService(),
    private val openBrowser: (String) -> Unit = { openWebBrowser(url=it) },
) {
    private val readiness = MutableStateFlow("")
    /** Readiness marker, never a merchant access token. */
    val accessToken = readiness.asStateFlow()
    suspend fun fetchAccessToken(onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        try { service.prepare(); readiness.value="ready" } catch (e: CancellationException) { throw e }
        catch (e: Exception) { readiness.value=""; onError(e.message ?: "Unable to prepare payment."); return }
        onSuccess("ready")
    }
    suspend fun beginCheckout(amount: Amount, fullName: String, shippingAddress: ShippingAddress,
        onSuccess: () -> Unit, onError: (String) -> Unit,
        checkoutId: String = "", cart: List<CartItem> = emptyList()) {
        try {
            val value = requireNotNull(amount.value.toDoubleOrNull()) { "Invalid amount." }
            require(amount.currencyCode == "USD" && value > 0 && value.isFinite()) { "Invalid amount." }
            require(checkoutId.isNotBlank() && cart.isNotEmpty()) { "Your checkout could not be restored. Please review your cart." }
            // Prepare on every attempt, including after an initial network/auth failure.
            service.prepare()
            readiness.value="ready"
            val checkout = service.createCheckout(CheckoutRequest(checkoutId, value.toCents().toUsdString(), cart,
                ShippingDetails(fullName, shippingAddress.addressLine1, shippingAddress.city, shippingAddress.postalCode, shippingAddress.countryCode, shippingAddress.state)))
            require(checkout.orderId.isNotBlank() && checkout.amount == value.toCents().toUsdString()) { "The checkout amount changed. Please review your cart." }
            require(checkout.approvalUrl.startsWith("https://www.paypal.com/") || checkout.approvalUrl.startsWith("https://www.sandbox.paypal.com/")) { "Invalid payment approval URL." }
            withContext(Dispatchers.Main) { openBrowser(checkout.approvalUrl) }
        } catch (e: CancellationException) { throw e } catch (e: Exception) { onError(e.message ?: "Unable to start payment."); return }
        onSuccess()
    }
    suspend fun confirmPayment(orderId: String): Order {
        require(orderId.isNotBlank()) { "Payment confirmation is missing an order reference." }
        val order = service.completeCheckout(orderId)
        require(order.token == orderId && order.totalAmount > 0 && order.totalAmount.isFinite() && order.items.isNotEmpty()) { "Payment confirmation is invalid." }
        return order
    }
}
