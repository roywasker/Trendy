package org.roy.ecommerce_project.testing.fixtures

import com.Trendy.data.domain.AdminRepository
import com.Trendy.data.domain.CustomerRepository
import com.Trendy.data.domain.OrderRepository
import com.Trendy.data.domain.ProductRepository
import com.trendy.shared.domain.CartItem
import com.trendy.shared.domain.Customer
import com.trendy.shared.domain.Order
import com.trendy.shared.domain.Product
import com.trendy.shared.domain.ProductCategory
import com.trendy.shared.util.RequestState
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.storage.File
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** Factory for isolated fakes which never contact Firebase, Google, storage, or payment services. */
class TestRepositories(val store: TestStore = TestStore()) {
    val customer: CustomerRepository = FakeCustomerRepository(store)
    val product: ProductRepository = FakeProductRepository(store)
    val admin: AdminRepository = FakeAdminRepository(store)
    val order: OrderRepository = FakeOrderRepository(store)
}

private fun TestStore.withFailure(operation: TestOperation, onSuccess: () -> Unit, onError: (String) -> Unit): Boolean {
    val failure = consumeFailure(operation)
    return if (failure == null) false else {
        onError(failure)
        true
    }
}

class FakeCustomerRepository(private val store: TestStore) : CustomerRepository {
    override suspend fun createCustomer(user: FirebaseUser?, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (store.withFailure(TestOperation.CreateCustomer, onSuccess, onError)) return
        store.mutations += "createCustomer"
        onSuccess()
    }

    override fun getCurrentUserId(): String? = store.currentUserId

    override suspend fun signOut(): RequestState<Unit> {
        val failure = store.consumeFailure(TestOperation.SignOut)
        return if (failure == null) {
            store.currentUserId = null
            store.mutations += "signOut"
            RequestState.Success(Unit)
        } else RequestState.Error(failure)
    }

    override fun readCustomerFlow(): Flow<RequestState<Customer>> = store.customerState

    override suspend fun updateCustomer(customer: Customer, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (store.withFailure(TestOperation.UpdateCustomer, onSuccess, onError)) return
        val current = store.customerState.value.getSuccessDataOrNull()
        store.emitCustomer(customer.copy(cart = current?.cart.orEmpty(), isAdmin = current?.isAdmin ?: false))
        store.mutations += "updateCustomer:${customer.id}"
        onSuccess()
    }

    override suspend fun addItemToCard(cartItem: CartItem, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (store.withFailure(TestOperation.AddCartItem, onSuccess, onError)) return
        store.emitCustomer(store.customer().copy(cart = store.customer().cart + cartItem))
        store.mutations += "addCartItem:${cartItem.productId}"
        onSuccess()
    }

    override suspend fun updateCartItemQuantity(id: String, quantity: Int, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (store.withFailure(TestOperation.UpdateCartItem, onSuccess, onError)) return
        store.emitCustomer(store.customer().copy(cart = store.customer().cart.map { if (it.id == id) it.copy(quantity = quantity) else it }))
        store.mutations += "updateCartItem:$id:$quantity"
        onSuccess()
    }

    override suspend fun deleteCartItem(id: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (store.withFailure(TestOperation.DeleteCartItem, onSuccess, onError)) return
        store.emitCustomer(store.customer().copy(cart = store.customer().cart.filterNot { it.id == id }))
        store.mutations += "deleteCartItem:$id"
        onSuccess()
    }

    override suspend fun deleteAllCartItems(onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (store.withFailure(TestOperation.DeleteAllCartItems, onSuccess, onError)) return
        store.emitCustomer(store.customer().copy(cart = emptyList()))
        store.mutations += "deleteAllCartItems"
        onSuccess()
    }
}

class FakeProductRepository(private val store: TestStore) : ProductRepository {
    override fun getCurrentUserId(): String? = store.currentUserId
    override fun readDiscountedProducts(): Flow<RequestState<List<Product>>> = listing(ProductRead.Discounted) { it.filter { product -> product.isDiscounted } }
    override fun readNewProducts(): Flow<RequestState<List<Product>>> = listing(ProductRead.New) { it.filter { product -> product.isNew } }
    override fun readProductByIdFlow(id: String): Flow<RequestState<Product>> = combine(store.productByIdOverride(id), store.productsState) { override, state ->
        override ?: state.productById(id)
    }
    override fun readProductsByIdsFlow(ids: List<String>): Flow<RequestState<List<Product>>> = listing(ProductRead.ByIds) { products -> products.filter { it.id in ids } }
    override fun readProductsByCategoryFlow(category: ProductCategory): Flow<RequestState<List<Product>>> = listing(ProductRead.ByCategory) { products -> products.filter { it.category == category.name } }
    private fun listing(read: ProductRead, transform: (List<Product>) -> List<Product>): Flow<RequestState<List<Product>>> =
        combine(store.listingOverride(read), store.productsState) { override, state -> override ?: state.mapProducts(transform) }
}

private fun RequestState<List<Product>>.mapProducts(transform: (List<Product>) -> List<Product>): RequestState<List<Product>> = when (this) {
    is RequestState.Success -> RequestState.Success(transform(data))
    is RequestState.Error -> RequestState.Error(message)
    RequestState.Loading -> RequestState.Loading
    RequestState.Idle -> RequestState.Idle
}

private fun RequestState<List<Product>>.productById(id: String): RequestState<Product> = when (this) {
    is RequestState.Success -> data.firstOrNull { it.id == id }?.let { RequestState.Success(it) }
        ?: RequestState.Error("Product not found: $id")
    is RequestState.Error -> RequestState.Error(message)
    RequestState.Loading -> RequestState.Loading
    RequestState.Idle -> RequestState.Idle
}

class FakeAdminRepository(private val store: TestStore) : AdminRepository {
    override fun getCurrentUserId(): String? = store.currentUserId
    override suspend fun createNewProduct(product: Product, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (store.withFailure(TestOperation.CreateProduct, onSuccess, onError)) return
        store.emitProducts(store.productsState.value.getSuccessDataOrNull().orEmpty() + product)
        store.mutations += "createProduct:${product.id}"
        onSuccess()
    }
    override suspend fun uploadImageToStorage(file: File): String? {
        store.consumeFailure(TestOperation.UploadImage)?.let { throw IllegalStateException(it) }
        return store.uploadedImageUrl
    }
    override suspend fun deleteImageFromStorage(imageUrl: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (store.withFailure(TestOperation.DeleteImage, onSuccess, onError)) return
        store.mutations += "deleteImage:$imageUrl"
        onSuccess()
    }
    override fun readLastTenProducts(): Flow<RequestState<List<Product>>> = listing(ProductRead.LastTen) { it.takeLast(10) }
    override suspend fun readProductById(id: String): RequestState<Product> = when (val state = store.productsState.value) {
        is RequestState.Success -> state.data.firstOrNull { it.id == id }?.let { RequestState.Success(it) } ?: RequestState.Error("Product not found: $id")
        is RequestState.Error -> RequestState.Error(state.message)
        RequestState.Loading -> RequestState.Loading
        RequestState.Idle -> RequestState.Idle
    }
    override suspend fun updateProductThumbnail(productId: String, downloadUrl: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (store.withFailure(TestOperation.UpdateThumbnail, onSuccess, onError)) return
        replaceProduct(productId) { it.copy(thumbnail = downloadUrl) }
        store.mutations += "updateThumbnail:$productId"
        onSuccess()
    }
    override suspend fun updateProduct(product: Product, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (store.withFailure(TestOperation.UpdateProduct, onSuccess, onError)) return
        replaceProduct(product.id) { product }
        store.mutations += "updateProduct:${product.id}"
        onSuccess()
    }
    override suspend fun deleteProduct(productId: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (store.withFailure(TestOperation.DeleteProduct, onSuccess, onError)) return
        store.emitProducts(store.productsState.value.getSuccessDataOrNull().orEmpty().filterNot { it.id == productId })
        store.mutations += "deleteProduct:$productId"
        onSuccess()
    }
    override fun searchProductsByTitle(searchQuery: String): Flow<RequestState<List<Product>>> = listing(ProductRead.Search) { products ->
        products.filter { it.title.contains(searchQuery, ignoreCase = true) }
    }
    private fun listing(read: ProductRead, transform: (List<Product>) -> List<Product>): Flow<RequestState<List<Product>>> =
        combine(store.listingOverride(read), store.productsState) { override, state -> override ?: state.mapProducts(transform) }
    private fun replaceProduct(id: String, transform: (Product) -> Product) {
        store.emitProducts(store.productsState.value.getSuccessDataOrNull().orEmpty().map { if (it.id == id) transform(it) else it })
    }
}

class FakeOrderRepository(private val store: TestStore) : OrderRepository {
    override fun getCurrentUserId(): String? = store.currentUserId
    override suspend fun createTheOrder(order: Order, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (store.withFailure(TestOperation.CreateOrder, onSuccess, onError)) return
        store.createdOrders += order
        val customer = store.customerState.value.getSuccessDataOrNull()
        if (customer != null) store.emitCustomer(customer.copy(cart = emptyList()))
        store.mutations += "createOrder:${order.customerId}"
        onSuccess()
    }
}
