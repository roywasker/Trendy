package com.Trendy.profile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.Trendy.data.domain.CustomerRepository
import com.trendy.shared.domain.Country
import com.trendy.shared.domain.Customer
import com.trendy.shared.domain.PhoneNumber
import com.trendy.shared.util.RequestState
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class ProfileScreenState(
    val id: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val city: String? = null,
    val postalCode: Int? = null,
    val postalCodeText: String = "",
    val shippingState: String = "",
    val address: String? = null,
    val country: Country = Country.Israel,
    val phoneNumber: PhoneNumber? = null
)

class ProfileViewModel(
    private val customerRepository: CustomerRepository
) : ViewModel() {

    var screenReady: RequestState<Unit> by mutableStateOf(RequestState.Loading)
    var screenState: ProfileScreenState by mutableStateOf(ProfileScreenState())
        private set

    val isFormValid: Boolean
        get() = with(screenState){
            firstName.length in 3..50 &&
            lastName.length in 3..50 &&
            city?.length  in 3..50 &&
            (Regex("[0-9]{3,8}").matches(postalCodeText) || (country == Country.Usa && com.trendy.shared.domain.validPostalCode(country, postalCodeText))) && com.trendy.shared.domain.validShippingState(country, shippingState) &&
            address?.length  in 3..50 &&
            phoneNumber?.number?.length  in 5..50
        }
    init {
        viewModelScope.launch {
            customerRepository.readCustomerFlow().collectLatest { data ->
                if (data.isSuccess()){
                    val fetchedCustomer = data.getSuccessData()
                    screenState = ProfileScreenState(
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
                                ?: Country.Israel
                    )
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

    fun updateCustomer(
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            customerRepository.updateCustomer(
                customer = Customer(
                    id = screenState.id,
                    firstName = screenState.firstName,
                    lastName = screenState.lastName,
                    email = screenState.email,
                    city = screenState.city,
                    postalCode = screenState.postalCode,
                    postalCodeText = screenState.postalCodeText,
                    countryCode = screenState.country.code,
                    shippingState = screenState.shippingState,
                    address = screenState.address,
                    phoneNumber = screenState.phoneNumber
                ),
                onSuccess = onSuccess,
                onError = onError
            )
        }
    }
}
