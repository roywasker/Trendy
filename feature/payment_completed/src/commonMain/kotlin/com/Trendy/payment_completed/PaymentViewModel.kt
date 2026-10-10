package com.Trendy.payment_completed

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.Trendy.checkout.domain.PaypalApi
import com.Trendy.data.domain.CustomerRepository
import com.Trendy.data.domain.OrderRepository
import com.Trendy.data.domain.ProductRepository
import com.trendy.shared.domain.CartItem
import com.trendy.shared.domain.Product
import com.trendy.shared.util.RequestState
import com.trendy.shared.util.toCents
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class PaymentViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val customerRepository: CustomerRepository,
    private val orderRepository: OrderRepository,
    private val productRepository: ProductRepository,
    private val paypalApi: PaypalApi,
) : ViewModel() {
    var screenState: RequestState<Unit> by mutableStateOf(RequestState.Loading)
        private set
    private var confirming = false
    val canRetryConfirmation: Boolean
        get() = savedStateHandle.get<Boolean>("isSuccess") == true && !savedStateHandle.get<String>("token").isNullOrBlank()
    init { processPayment() }
    fun retryConfirmation() {
        if (canRetryConfirmation && screenState.isError()) processPayment()
    }
    private fun processPayment() {
        if (confirming) return
        confirming = true
        screenState = RequestState.Loading
        viewModelScope.launch {
            try {
            val success = savedStateHandle.get<Boolean>("isSuccess")
            val token = savedStateHandle.get<String>("token")
            val error = savedStateHandle.get<String>("error")
            screenState = when {
                success == true && token.isNullOrBlank() -> RequestState.Success(Unit) // Internal pay-on-delivery navigation.
                success == true -> try {
                    paypalApi.confirmPayment(requireNotNull(token))
                    RequestState.Success(Unit)
                } catch (e: CancellationException) { throw e }
                catch (e: Exception) { RequestState.Error(e.message ?: "Payment could not be confirmed.") }
                error != null -> RequestState.Error(error)
                success == false -> RequestState.Error("Payment has been canceled.")
                else -> RequestState.Error("Payment could not be confirmed.")
            }
            } finally { confirming = false }
        }
    }
    fun calculateTotalPrice(cartItems: List<CartItem>, products: List<Product>): Double =
        cartItems.sumOf { item -> (products.find { it.id == item.productId }?.price ?: 0.0).toCents() * item.quantity } / 100.0
}
