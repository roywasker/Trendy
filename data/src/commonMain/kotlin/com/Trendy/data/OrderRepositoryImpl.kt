package com.Trendy.data

import com.Trendy.data.domain.CustomerRepository
import com.Trendy.data.domain.OrderRepository
import com.trendy.shared.domain.Order
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import kotlinx.coroutines.CancellationException

class OrderRepositoryImpl(
    private val customerRepository: CustomerRepository,
    private val store: ShopStore = FirestoreShopStore(),
    private val userId: () -> String? = { Firebase.auth.currentUser?.uid },
) : OrderRepository {
    override fun getCurrentUserId() = userId()
    override suspend fun createTheOrder(order: Order, onSuccess: () -> Unit, onError: (String) -> Unit) {
        try {
            val id = requireNotNull(getCurrentUserId()) { "User is not available." }
            require(order.customerId == id) { "Order does not match the signed-in user." }
            require(order.token == null) { "PayPal orders must be verified by the payment server." }
            require(order.items.isNotEmpty() && order.items.all { it.quantity > 0 }) { "The order is empty or invalid." }
            require(order.totalAmount.isFinite() && order.totalAmount > 0) { "The amount is invalid." }
            store.transaction {
                val existing = order(order.id)
                val customer = requireNotNull(customer(id)) { "Customer does not exist." }
                if (existing != null) {
                    require(existing == order) { "Order reference was already used." }
                } else {
                    require(order.items.all { item -> customer.cart.any { it == item } }) { "Your cart changed. Please review checkout again." }
                    putOrder(order)
                    putCustomer(customer.copy(cart = customer.cart.filterNot { line -> order.items.any { it.id == line.id } }))
                }
            }
        } catch (e: CancellationException) { throw e } catch (e: Exception) { onError(e.message ?: "Order failed."); return }
        onSuccess()
    }
}
