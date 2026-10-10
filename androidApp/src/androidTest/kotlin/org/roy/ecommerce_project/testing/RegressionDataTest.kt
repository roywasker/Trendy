package org.roy.ecommerce_project.testing

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.Trendy.data.CustomerRepositoryImpl
import com.Trendy.data.OrderRepositoryImpl
import com.Trendy.data.ShopStore
import com.Trendy.data.ShopTransaction
import com.Trendy.data.productChunksFlow
import com.trendy.shared.domain.CartItem
import com.trendy.shared.domain.Customer
import com.trendy.shared.domain.Order
import com.trendy.shared.domain.Product
import com.trendy.shared.util.toCents
import com.trendy.shared.util.toUsdString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Regression coverage for the transactional repository contracts.  The store below deliberately
 * commits staged copies only after the transaction block succeeds, mirroring Firestore's atomic
 * transaction boundary without talking to Firebase.
 */
@RunWith(AndroidJUnit4::class)
class RegressionDataTest {

    @Test
    fun customerConcurrentAddUpdateDelete_preservesEveryIndependentChange() = runBlocking {
        val updated = cart("updated", quantity = 1)
        val deleted = cart("deleted", quantity = 1)
        val added = cart("added", quantity = 2)
        val store = InMemoryShopStore(customers = mapOf(USER_ID to customer(cart = listOf(updated, deleted))))
        val repository = customerRepository(store)

        listOf(
            async(Dispatchers.Default) { repository.updateCartItemQuantity(updated.id, 4, {}, ::fail) },
            async(Dispatchers.Default) { repository.deleteCartItem(deleted.id, {}, ::fail) },
            async(Dispatchers.Default) { repository.addItemToCard(added, {}, ::fail) },
        ).awaitAll()

        assertEquals(listOf(updated.copy(quantity = 4), added), store.storedCustomer(USER_ID)?.cart)
    }

    @Test
    fun customerUpdate_preservesCartAndRoleOwnedFields() = runBlocking {
        val cart = cart("line")
        val original = customer(cart = listOf(cart), isAdmin = false)
        val store = InMemoryShopStore(customers = mapOf(USER_ID to original), roles = mapOf(USER_ID to true))
        val repository = customerRepository(store)
        var succeeded = false

        repository.updateCustomer(
            original.copy(firstName = "Updated", city = "Haifa", cart = emptyList(), isAdmin = false),
            { succeeded = true },
            ::fail,
        )

        assertTrue(succeeded)
        assertEquals(listOf(cart), store.storedCustomer(USER_ID)?.cart)
        assertTrue(store.storedRole(USER_ID) == true)
        assertTrue(store.observeCustomer(USER_ID).first()?.isAdmin == true)
    }

    @Test
    fun ensureCustomer_repairsMissingRoleWithoutOverwritingExistingProfile() = runBlocking {
        val existing = customer(firstName = "Existing", cart = listOf(cart("line")))
        val store = InMemoryShopStore(customers = mapOf(USER_ID to existing))

        customerRepository(store).ensureCustomer(customer(firstName = "Replacement"))

        assertEquals(existing, store.storedCustomer(USER_ID))
        assertEquals(false, store.storedRole(USER_ID))
    }

    @Test
    fun ensureCustomer_preservesExistingAdminRole() = runBlocking {
        val store = InMemoryShopStore(
            customers = mapOf(USER_ID to customer()),
            roles = mapOf(USER_ID to true),
        )

        customerRepository(store).ensureCustomer(customer(firstName = "Ignored"))

        assertEquals(true, store.storedRole(USER_ID))
        assertTrue(store.observeCustomer(USER_ID).first()?.isAdmin == true)
    }

    @Test
    fun ensureCustomer_commitFailure_rollsBackCustomerAndRoleTogether() = runBlocking {
        val store = InMemoryShopStore().apply { failNextCommit() }

        val failure = runCatching { customerRepository(store).ensureCustomer(customer()) }.exceptionOrNull()

        assertEquals("commit failed", failure?.message)
        assertNull(store.storedCustomer(USER_ID))
        assertNull(store.storedRole(USER_ID))
    }

    @Test
    fun createOrder_writesOrderAndClearsOnlyPurchasedCartLines() = runBlocking {
        val bought = cart("bought", quantity = 2)
        val retained = cart("retained")
        val store = InMemoryShopStore(customers = mapOf(USER_ID to customer(cart = listOf(bought, retained))))
        val order = order(items = listOf(bought))
        var succeeded = false

        orderRepository(store).createTheOrder(order, { succeeded = true }, ::fail)

        assertTrue(succeeded)
        assertEquals(order, store.storedOrder(order.id))
        assertEquals(listOf(retained), store.storedCustomer(USER_ID)?.cart)
    }

    @Test
    fun createOrder_commitFailure_rollsBackOrderAndCartClear() = runBlocking {
        val line = cart("line")
        val store = InMemoryShopStore(customers = mapOf(USER_ID to customer(cart = listOf(line)))).apply { failNextCommit() }
        val order = order(items = listOf(line))
        var error: String? = null

        orderRepository(store).createTheOrder(order, {}, { error = it })

        assertEquals("commit failed", error)
        assertNull(store.storedOrder(order.id))
        assertEquals(listOf(line), store.storedCustomer(USER_ID)?.cart)
    }

    @Test
    fun createOrder_identicalRepeatIsIdempotentAndKeepsNewCartLines() = runBlocking {
        val bought = cart("bought")
        val later = cart("later")
        val store = InMemoryShopStore(customers = mapOf(USER_ID to customer(cart = listOf(bought))))
        val repository = orderRepository(store)
        val order = order(items = listOf(bought))

        repository.createTheOrder(order, {}, ::fail)
        customerRepository(store).addItemToCard(later, {}, ::fail)
        var repeated = false
        repository.createTheOrder(order, { repeated = true }, ::fail)

        assertTrue(repeated)
        assertEquals(order, store.storedOrder(order.id))
        assertEquals(listOf(later), store.storedCustomer(USER_ID)?.cart)
    }

    @Test
    fun createOrder_reusedIdWithDifferentPayloadIsRejectedWithoutCartMutation() = runBlocking {
        val line = cart("line")
        val store = InMemoryShopStore(customers = mapOf(USER_ID to customer(cart = listOf(line))))
        val repository = orderRepository(store)
        val first = order(id = "same-id", items = listOf(line))
        repository.createTheOrder(first, {}, ::fail)
        val different = first.copy(totalAmount = 99.0)
        var error: String? = null

        repository.createTheOrder(different, {}, { error = it })

        assertEquals("Order reference was already used.", error)
        assertEquals(first, store.storedOrder(first.id))
        assertTrue(store.storedCustomer(USER_ID)?.cart.orEmpty().isEmpty())
    }

    @Test
    fun createOrder_concurrentNewCartLineIsNeverRemoved() = runBlocking {
        val bought = cart("bought")
        val newlyAdded = cart("new")
        val store = InMemoryShopStore(customers = mapOf(USER_ID to customer(cart = listOf(bought))))
        val customerRepository = customerRepository(store)
        val orderRepository = orderRepository(store)

        listOf(
            async(Dispatchers.Default) { orderRepository.createTheOrder(order(items = listOf(bought)), {}, ::fail) },
            async(Dispatchers.Default) { customerRepository.addItemToCard(newlyAdded, {}, ::fail) },
        ).awaitAll()

        assertEquals(listOf(newlyAdded), store.storedCustomer(USER_ID)?.cart)
    }

    @Test
    fun createOrder_rejectsUnverifiedPaypalToken() = runBlocking {
        val line = cart("line")
        val store = InMemoryShopStore(customers = mapOf(USER_ID to customer(cart = listOf(line))))
        var error: String? = null

        orderRepository(store).createTheOrder(order(items = listOf(line), token = "paypal-token"), {}, { error = it })

        assertEquals("PayPal orders must be verified by the payment server.", error)
        assertTrue(store.storedCustomer(USER_ID)?.cart == listOf(line))
    }

    @Test
    fun createOrder_rejectsOrderForAnotherCustomer() = runBlocking {
        val line = cart("line")
        val store = InMemoryShopStore(customers = mapOf(USER_ID to customer(cart = listOf(line))))
        var error: String? = null

        orderRepository(store).createTheOrder(order(customerId = "other-user", items = listOf(line)), {}, { error = it })

        assertEquals("Order does not match the signed-in user.", error)
        assertTrue(store.storedCustomer(USER_ID)?.cart == listOf(line))
        assertTrue(store.orderIds().isEmpty())
    }

    @Test
    fun productChunks_emptyIdsEmitsEmptyWithoutSubscribing() = runBlocking {
        var subscriptions = 0

        val products = productChunksFlow(emptyList()) { subscriptions += 1; MutableStateFlow(emptyList()) }.first()

        assertTrue(products.isEmpty())
        assertEquals(0, subscriptions)
    }

    @Test
    fun productChunks_tenIdsUsesOneConcurrentSubscription() = runBlocking {
        val recorder = ChunkRecorder()
        val ids = (1..10).map { "p$it" }

        val products = productChunksFlow(ids, recorder::observe).first()

        assertEquals(listOf(ids), recorder.subscribedChunks)
        assertTrue(products.isEmpty())
    }

    @Test
    fun productChunks_elevenIdsUsesTwoSubscriptions() = runBlocking {
        val recorder = ChunkRecorder()
        val ids = (1..11).map { "p$it" }

        productChunksFlow(ids, recorder::observe).first()

        assertEquals(listOf(ids.take(10), ids.takeLast(1)), recorder.subscribedChunks)
    }

    @Test
    fun productChunks_twentyOneIdsUsesThreeSubscriptions() = runBlocking {
        val recorder = ChunkRecorder()
        val ids = (1..21).map { "p$it" }

        productChunksFlow(ids, recorder::observe).first()

        assertEquals(listOf(ids.take(10), ids.drop(10).take(10), ids.takeLast(1)), recorder.subscribedChunks)
    }

    @Test
    fun productChunks_updatesReplaceRemovedItemsAndDeduplicateAcrossChunks() = runBlocking {
        val recorder = ChunkRecorder()
        val ids = (1..11).map { "p$it" }
        val emissions = mutableListOf<List<Product>>()
        val job = launch {
            productChunksFlow(ids, recorder::observe).collect { emissions += it }
        }
        withTimeout(1_000) { while (recorder.subscribedChunks.size < 2) yield() }
        val first = product("p1", "First")
        val duplicate = product("p11", "Duplicate")
        val replacement = product("p2", "Replacement")

        recorder.emit(0, listOf(first, duplicate))
        recorder.emit(1, listOf(duplicate))
        withTimeout(1_000) { while (emissions.none { it.any { product -> product.id == first.id } }) yield() }
        recorder.emit(0, listOf(replacement))
        withTimeout(1_000) {
            while (emissions.lastOrNull()?.map(Product::id) != listOf(replacement.id, duplicate.id)) yield()
        }
        job.cancel()

        assertEquals(listOf(replacement, duplicate), emissions.last())
        assertFalse(emissions.last().any { it.id == "p1" })
        assertEquals(1, emissions.last().count { it.id == "p11" })
    }

    @Test
    fun moneyRoundsBinaryFractionSumToExactCents() {
        assertEquals(30L, (0.1 + 0.2).toCents())
        assertEquals("0.30", (0.1 + 0.2).toCents().toUsdString())
    }

    @Test
    fun moneyRoundsLeadingHalfCentUp() {
        assertEquals(1L, 0.005.toCents())
        assertEquals("0.01", 0.005.toCents().toUsdString())
    }

    private fun customer(
        firstName: String = "Ada",
        cart: List<CartItem> = emptyList(),
        isAdmin: Boolean = false,
    ) = Customer(
        id = USER_ID,
        firstName = firstName,
        lastName = "Lovelace",
        email = "ada@example.test",
        cart = cart,
        isAdmin = isAdmin,
    )

    private fun cart(id: String, quantity: Int = 1) = CartItem(id = id, productId = "product-$id", quantity = quantity)

    private fun order(
        id: String = "order-1",
        customerId: String = USER_ID,
        items: List<CartItem>,
        totalAmount: Double = 10.0,
        token: String? = null,
    ) = Order(id = id, customerId = customerId, items = items, totalAmount = totalAmount, token = token)

    private fun product(id: String, title: String) = Product(
        id = id,
        title = title,
        description = "Description",
        thumbnail = "android.resource://org.roy.ecommerce_project/drawable/trendy_mark",
        category = "Gadgets",
        price = 1.0,
    )

    private fun customerRepository(store: InMemoryShopStore) = CustomerRepositoryImpl(store) { USER_ID }

    private fun orderRepository(store: InMemoryShopStore): OrderRepositoryImpl =
        OrderRepositoryImpl(customerRepository(store), store) { USER_ID }

    private fun fail(message: String): Nothing = throw AssertionError(message)

    private class ChunkRecorder {
        val subscribedChunks = mutableListOf<List<String>>()
        private val flows = mutableListOf<MutableStateFlow<List<Product>>>()

        fun observe(ids: List<String>): Flow<List<Product>> {
            subscribedChunks += ids
            return MutableStateFlow(emptyList<Product>()).also(flows::add)
        }

        fun emit(chunk: Int, products: List<Product>) {
            flows[chunk].value = products
        }
    }

    private class InMemoryShopStore(
        customers: Map<String, Customer> = emptyMap(),
        roles: Map<String, Boolean> = emptyMap(),
        orders: Map<String, Order> = emptyMap(),
    ) : ShopStore {
        private val mutex = Mutex()
        private var customers = customers.toMutableMap()
        private var roles = roles.toMutableMap()
        private var orders = orders.toMutableMap()
        private val customerFlows = mutableMapOf<String, MutableStateFlow<Customer?>>()
        private var failCommit = false

        fun failNextCommit() { failCommit = true }
        fun storedCustomer(id: String): Customer? = customers[id]
        fun storedRole(id: String): Boolean? = roles[id]
        fun storedOrder(id: String): Order? = orders[id]
        fun orderIds(): Set<String> = orders.keys

        override suspend fun <T> transaction(block: suspend ShopTransaction.() -> T): T = mutex.withLock {
            val stagedCustomers = customers.toMutableMap()
            val stagedRoles = roles.toMutableMap()
            val stagedOrders = orders.toMutableMap()
            val result = block(object : ShopTransaction {
                override suspend fun customer(id: String): Customer? = stagedCustomers[id]
                override suspend fun role(id: String): Boolean? = stagedRoles[id]
                override suspend fun order(id: String): Order? = stagedOrders[id]
                override fun putCustomer(customer: Customer) { stagedCustomers[customer.id] = customer }
                override fun putRole(id: String, isAdmin: Boolean) { stagedRoles[id] = isAdmin }
                override fun putOrder(order: Order) { stagedOrders[order.id] = order }
            })
            if (failCommit) {
                failCommit = false
                throw IllegalStateException("commit failed")
            }
            customers = stagedCustomers
            roles = stagedRoles
            orders = stagedOrders
            (customers.keys + customerFlows.keys).forEach { id ->
                customerFlows.getOrPut(id) { MutableStateFlow(null) }.value = visibleCustomer(id)
            }
            result
        }

        override fun observeCustomer(id: String): Flow<Customer?> =
            customerFlows.getOrPut(id) { MutableStateFlow(visibleCustomer(id)) }

        private fun visibleCustomer(id: String): Customer? = customers[id]?.copy(isAdmin = roles[id] ?: false)
    }

    private companion object {
        const val USER_ID = "user-1"
    }
}
