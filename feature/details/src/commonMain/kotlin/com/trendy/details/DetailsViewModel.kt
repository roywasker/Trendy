package com.trendy.details

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.Trendy.data.domain.CustomerRepository
import com.Trendy.data.domain.ProductRepository
import com.trendy.shared.domain.CartItem
import com.trendy.shared.util.RequestState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DetailsViewModel(
    private val productRepository: ProductRepository,
    private val customerRepository: CustomerRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val product = productRepository.readProductByIdFlow(
        savedStateHandle.get<String>("id") ?: ""
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RequestState.Loading
    )

    var quantity by mutableStateOf(1)
        private set

    var selectedFlavor: String? by mutableStateOf(null)
        private set

    fun updateQuantity(value: Int) {
        quantity = value
    }

    fun addItemToCart(
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val productId = savedStateHandle.get<String>("id")
            if (productId != null) {
                customerRepository.addItemToCard(
                    cartItem = CartItem(
                        productId = productId,
                        quantity = quantity
                    ),
                    onSuccess = onSuccess,
                    onError = onError
                )
            } else {
                onError("Product id is not found.")
            }
        }
    }
}