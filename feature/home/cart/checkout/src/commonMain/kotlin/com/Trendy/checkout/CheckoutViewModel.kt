package com.Trendy.checkout

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.Trendy.checkout.domain.Amount
import com.Trendy.checkout.domain.PaypalApi
import com.Trendy.checkout.domain.ShippingAddress
import com.Trendy.data.domain.CustomerRepository
import com.Trendy.data.domain.ProductRepository
import com.trendy.shared.domain.Product
import com.trendy.shared.domain.ShippingDetails
import com.trendy.shared.util.toCents
import com.trendy.shared.util.toUsdString
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import kotlin.uuid.Uuid
import com.Trendy.data.domain.OrderRepository
import com.trendy.shared.domain.CartItem
import com.trendy.shared.domain.Country
import com.trendy.shared.domain.Customer
import com.trendy.shared.domain.Order
import com.trendy.shared.domain.PhoneNumber
import com.trendy.shared.util.RequestState
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.ranges.contains

data class CheckoutScreenState(
    val id: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val city: String? = null,
    val postalCode: Int? = null,
    val postalCodeText: String = "",
    val shippingState: String = "",
    val address: String? = null,
    val country: Country = Country.Serbia,
    val phoneNumber: PhoneNumber? = null,
    val cart: List<CartItem> = emptyList(),
)

class CheckoutViewModel(
    private val customerRepository: CustomerRepository,
    private val orderRepository: OrderRepository,
    private val savedStateHandle: SavedStateHandle,
    private val paypalApi: PaypalApi,
    private val productRepository: ProductRepository? = null,
) : ViewModel() {
    var screenReady: RequestState<Unit> by mutableStateOf(RequestState.Loading)
    var screenState: CheckoutScreenState by mutableStateOf(CheckoutScreenState())
        private set

    var isSubmitting by mutableStateOf(false)
        private set
    private var hydrated = false

    val isFormValid: Boolean
        get() = with(screenState) {
            firstName.length in 3..50 &&
                    lastName.length in 3..50 &&
                    city?.length in 3..50 &&
                    com.trendy.shared.domain.validPostalCode(country, postalCodeText) && com.trendy.shared.domain.validShippingState(country, shippingState) &&
                    address?.length in 3..50 &&
                    phoneNumber?.number?.length in 5..30
        }

    init {
        viewModelScope.launch {
            paypalApi.fetchAccessToken(
                onSuccess = { token ->
                    // The token remains inside the payment client.
                },
                onError = { message ->
                    println(message)
                }
            )
        }
        viewModelScope.launch {
            customerRepository.readCustomerFlow().collectLatest { data ->
                if (data.isSuccess()) {
                    val fetchedCustomer = data.getSuccessData()
                    if (!hydrated) screenState = CheckoutScreenState(
                        id = fetchedCustomer.id,
                        firstName = fetchedCustomer.firstName,
                        lastName = fetchedCustomer.lastName,
                        email = fetchedCustomer.email,
                        city = fetchedCustomer.city,
                        postalCode = fetchedCustomer.postalCode,
                            postalCodeText = fetchedCustomer.postalCodeText ?: fetchedCustomer.postalCode?.toString().orEmpty(),
                            shippingState = fetchedCustomer.shippingState,
                        address = fetchedCustomer.address,
                        phoneNumber = fetchedCustomer.phoneNumber,
                        country = Country.entries.firstOrNull { it.code == fetchedCustomer.countryCode || (fetchedCustomer.countryCode == null && it.dialCode == fetchedCustomer.phoneNumber?.dialCode) }
                            ?: Country.Serbia,
                        cart = fetchedCustomer.cart
                    )
                    else screenState = screenState.copy(cart = fetchedCustomer.cart)
                    hydrated = true
                    screenReady = RequestState.Success(Unit)
                } else if (data.isError()) {
                    screenReady = RequestState.Error(data.getErrorMessage())
                }
            }
        }
    }

    fun updateFirstName(value: String) {
        screenState = screenState.copy(firstName = value)
    }

    fun updateLastName(value: String) {
        screenState = screenState.copy(lastName = value)
    }

    fun updateCity(value: String) {
        screenState = screenState.copy(city = value)
    }

    fun updatePostalCode(value: Int?) {
        screenState = screenState.copy(postalCode = value, postalCodeText = value?.toString().orEmpty())
    }

    fun updatePostalCodeText(value: String) { screenState = screenState.copy(postalCodeText = value, postalCode = value.toIntOrNull()) }

    fun updateShippingState(value: String) { screenState = screenState.copy(shippingState = value.trim().uppercase()) }

    fun updateAddress(value: String) {
        screenState = screenState.copy(address = value)
    }

    fun updateCountry(value: Country) {
        screenState = screenState.copy(
            country = value,
            shippingState = if (screenState.country == value) screenState.shippingState else "",
            phoneNumber = screenState.phoneNumber?.copy(
                dialCode = value.dialCode
            )
        )
    }

    fun updatePhoneNumber(value: String) {
        screenState = screenState.copy(
            phoneNumber = PhoneNumber(
                dialCode = screenState.country.dialCode,
                number = value
            )
        )
    }

    fun payOnDelivery(onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (isSubmitting || !validateCheckout(onError)) return
        isSubmitting = true
        viewModelScope.launch {
            try {
                val snapshot = prepareOrder()
                var customerError: String? = null
                customerRepository.updateCustomer(customerSnapshot(), {}, { customerError = it })
                check(customerError == null) { customerError.orEmpty() }
                orderRepository.createTheOrder(snapshot, { onSuccess() }, { message -> isSubmitting=false; onError(message) })
            } catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (e: Exception) { isSubmitting=false; onError(e.message ?: "Unable to create order.") }
        }
    }
    fun payWithPayPal(onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (isSubmitting || !validateCheckout(onError)) return
        isSubmitting = true
        viewModelScope.launch {
            try {
                val snapshot = prepareOrder()
                var customerError: String? = null
                customerRepository.updateCustomer(customerSnapshot(), {}, { customerError = it })
                check(customerError == null) { customerError.orEmpty() }
                val shipping = requireNotNull(snapshot.shipping)
                paypalApi.beginCheckout(
                    amount=Amount("USD", snapshot.totalAmount.toCents().toUsdString()), fullName=shipping.fullName,
                    shippingAddress=ShippingAddress(shipping.addressLine1, city=shipping.city, state=shipping.state,
                        postalCode=shipping.postalCode, countryCode=shipping.countryCode),
                    checkoutId=snapshot.id, cart=snapshot.items,
                    onSuccess={ isSubmitting=false; onSuccess() }, onError={ isSubmitting=false; onError(it) })
            } catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (e: Exception) { isSubmitting=false; onError(e.message ?: "Unable to start payment.") }
        }
    }
    private fun customerSnapshot() = Customer(id=screenState.id, firstName=screenState.firstName, lastName=screenState.lastName,
        email=screenState.email, city=screenState.city, postalCode=screenState.postalCode, postalCodeText=screenState.postalCodeText,
        countryCode=screenState.country.code, shippingState=screenState.shippingState, address=screenState.address, phoneNumber=screenState.phoneNumber)

    private suspend fun prepareOrder(): Order {
        val state = screenState
        val items = state.cart.toList()
        val shipping = ShippingDetails("${state.firstName} ${state.lastName}", state.address.orEmpty(), state.city.orEmpty(),
            state.postalCodeText, state.country.code, state.shippingState)
        val encoded = savedStateHandle.get<String>("pendingCheckout")
        val previous = encoded?.let { runCatching { Json.decodeFromString<Order>(it) }.getOrNull() }
        if (previous != null && previous.items == items && previous.shipping == shipping) return previous
        val products = productRepository?.readProductsByIdsFlow(items.map { it.productId })?.first { it.isSuccess() || it.isError() }
        if (products?.isError() == true) error(products.getErrorMessage())
        val amount = if (products != null) {
            val catalogue = products.getSuccessData().associateBy { it.id }
            items.sumOf { item ->
                val product = requireNotNull(catalogue[item.productId]) { "Remove unavailable products from your cart before checkout." }
                require(item.quantity > 0 && product.isAvailable && product.price > 0) { "Your cart contains an unavailable product." }
                product.price.toCents() * item.quantity
            }
        } else requireNotNull(savedStateHandle.get<String>("totalAmount")?.toDoubleOrNull()).toCents()
        require(amount > 0) { "The payment amount is invalid." }
        val order = Order(customerId=state.id, items=items, totalAmount=amount / 100.0, shipping=shipping)
        savedStateHandle["pendingCheckout"] = Json.encodeToString(order)
        return order
    }

    private fun validateCheckout(onError: (String) -> Unit): Boolean {
        if (!isFormValid) {
            onError("Please fill in the information ")
            return false
        }
        if (screenState.cart.isEmpty()) {
            onError("Your cart is empty.")
            return false
        }
        val amount = savedStateHandle.get<String>("totalAmount")?.toDoubleOrNull()
        if (amount == null || !amount.isFinite() || amount <= 0.0) {
            onError("Total amount couldn't be calculated.")
            return false
        }
        return true
    }
}
