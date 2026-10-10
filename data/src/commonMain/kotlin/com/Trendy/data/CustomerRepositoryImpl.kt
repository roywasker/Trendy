package com.Trendy.data

import com.Trendy.data.domain.CustomerRepository
import com.trendy.shared.domain.CartItem
import com.trendy.shared.domain.Customer
import com.trendy.shared.util.RequestState
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

class CustomerRepositoryImpl(
    private val store: ShopStore = FirestoreShopStore(),
    private val userId: () -> String? = { Firebase.auth.currentUser?.uid },
) : CustomerRepository {
    override fun getCurrentUserId() = userId()
    suspend fun ensureCustomer(customer: Customer) {
        store.transaction {
            val existing = customer(customer.id)
            val existingRole = role(customer.id)
            if (existing == null) putCustomer(customer)
            if (existingRole == null) putRole(customer.id, false)
        }
    }
    override suspend fun createCustomer(user: FirebaseUser?, onSuccess: () -> Unit, onError: (String) -> Unit) = operation(onSuccess, onError) {
        requireNotNull(user) { "User is not available." }
        ensureCustomer(Customer(user.uid, user.displayName?.substringBefore(" ") ?: "Unknown", user.displayName?.substringAfterLast(" ") ?: "Unknown", user.email ?: "Unknown"))
    }
    override suspend fun signOut(): RequestState<Unit> = try {
        Firebase.auth.signOut(); RequestState.Success(Unit)
    } catch (e: CancellationException) { throw e } catch (e: Exception) { RequestState.Error("Error while signing out: "+e.message) }
    override fun readCustomerFlow(): Flow<RequestState<Customer>> {
        val id = getCurrentUserId() ?: return flow { emit(RequestState.Error("User is not available.")) }
        return store.observeCustomer(id).map<Customer?, RequestState<Customer>> { customer ->
            customer?.let { RequestState.Success(it) } ?: RequestState.Error("Customer does not exist.")
        }.catch { emit(RequestState.Error("Error while reading Customer: "+it.message)) }
    }
    override suspend fun updateCustomer(customer: Customer, onSuccess: () -> Unit, onError: (String) -> Unit) = mutate(onSuccess, onError) { existing ->
        require(customer.id == existing.id) { "Customer does not match the signed-in user." }
        existing.copy(firstName=customer.firstName, lastName=customer.lastName, city=customer.city, postalCode=customer.postalCode,
            postalCodeText=customer.postalCodeText, address=customer.address, phoneNumber=customer.phoneNumber,
            countryCode=customer.countryCode, shippingState=customer.shippingState)
    }
    override suspend fun addItemToCard(cartItem: CartItem, onSuccess: () -> Unit, onError: (String) -> Unit) = mutate(onSuccess, onError) {
        require(cartItem.quantity > 0) { "Quantity must be positive." }
        it.copy(cart = it.cart.filterNot { item -> item.id == cartItem.id } + cartItem)
    }
    override suspend fun updateCartItemQuantity(id: String, quantity: Int, onSuccess: () -> Unit, onError: (String) -> Unit) = mutate(onSuccess, onError) {
        require(quantity > 0) { "Quantity must be positive." }
        it.copy(cart = it.cart.map { item -> if (item.id == id) item.copy(quantity=quantity) else item })
    }
    override suspend fun deleteCartItem(id: String, onSuccess: () -> Unit, onError: (String) -> Unit) = mutate(onSuccess, onError) { it.copy(cart=it.cart.filterNot { item -> item.id == id }) }
    override suspend fun deleteAllCartItems(onSuccess: () -> Unit, onError: (String) -> Unit) = mutate(onSuccess, onError) { it.copy(cart=emptyList()) }
    private suspend fun mutate(onSuccess: () -> Unit, onError: (String) -> Unit, transform: (Customer) -> Customer) = operation(onSuccess, onError) {
        val id = requireNotNull(getCurrentUserId()) { "User is not available." }
        store.transaction { putCustomer(transform(requireNotNull(customer(id)) { "Customer does not exist." })) }
    }
    private suspend fun operation(onSuccess: () -> Unit, onError: (String) -> Unit, action: suspend () -> Unit) {
        try { action() } catch (e: CancellationException) { throw e } catch (e: Exception) { onError(e.message ?: "Operation failed."); return }
        onSuccess()
    }
}
