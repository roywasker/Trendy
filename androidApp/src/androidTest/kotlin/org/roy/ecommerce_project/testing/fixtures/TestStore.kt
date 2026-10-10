package org.roy.ecommerce_project.testing.fixtures

import com.trendy.shared.domain.CartItem
import com.trendy.shared.domain.Customer
import com.trendy.shared.domain.Product
import com.trendy.shared.domain.ProductCategory
import com.trendy.shared.util.RequestState
import kotlinx.coroutines.flow.MutableStateFlow

/** Mutable, in-memory backend shared by all test repositories. */
class TestStore(
    customer: Customer = TestFixtures.customer(),
    products: List<Product> = TestFixtures.products(),
    var currentUserId: String? = customer.id,
) {
    val customerState = MutableStateFlow<RequestState<Customer>>(RequestState.Success(customer))
    val productsState = MutableStateFlow<RequestState<List<Product>>>(RequestState.Success(products))
    private val productListingOverrides = ProductRead.entries.associateWith {
        MutableStateFlow<RequestState<List<Product>>?>(null)
    }
    private val productByIdOverrides = mutableMapOf<String, MutableStateFlow<RequestState<Product>?>>()
    val failures = mutableMapOf<TestOperation, String>()
    val mutations = mutableListOf<String>()
    val createdOrders = mutableListOf<com.trendy.shared.domain.Order>()
    var uploadedImageUrl: String? = "android.resource://org.roy.ecommerce_project/drawable/trendy_mark"

    fun customer(): Customer = customerState.value.getSuccessData()

    fun emitCustomer(customer: Customer) {
        customerState.value = RequestState.Success(customer)
    }

    fun emitCustomer(state: RequestState<Customer>) {
        customerState.value = state
    }

    fun emitCustomerError(message: String) {
        customerState.value = RequestState.Error(message)
    }

    fun emitCustomerLoading() {
        customerState.value = RequestState.Loading
    }

    fun emitProducts(products: List<Product>) {
        productsState.value = RequestState.Success(products)
    }

    fun emitProducts(state: RequestState<List<Product>>) {
        productsState.value = state
    }

    fun emitProductsError(message: String) {
        productsState.value = RequestState.Error(message)
    }

    fun emitProductsLoading() {
        productsState.value = RequestState.Loading
    }

    fun failNext(operation: TestOperation, message: String = "Test failure: $operation") {
        failures[operation] = message
    }

    /** Overrides a single product endpoint without changing the shared product catalogue. */
    fun emitProductReadState(read: ProductRead, state: RequestState<List<Product>>?) {
        productListingOverrides.getValue(read).value = state
    }

    /** Overrides a single product-detail endpoint without changing category/list endpoints. */
    fun emitProductByIdState(id: String, state: RequestState<Product>?) {
        productByIdOverrides.getOrPut(id) { MutableStateFlow(null) }.value = state
    }

    internal fun listingOverride(read: ProductRead): MutableStateFlow<RequestState<List<Product>>?> =
        productListingOverrides.getValue(read)

    internal fun productByIdOverride(id: String): MutableStateFlow<RequestState<Product>?> =
        productByIdOverrides.getOrPut(id) { MutableStateFlow(null) }

    internal fun consumeFailure(operation: TestOperation): String? = failures.remove(operation)
}

enum class ProductRead { New, Discounted, ByIds, ByCategory, LastTen, Search }

enum class TestOperation {
    CreateCustomer,
    SignOut,
    UpdateCustomer,
    AddCartItem,
    UpdateCartItem,
    DeleteCartItem,
    DeleteAllCartItems,
    CreateProduct,
    UploadImage,
    DeleteImage,
    UpdateThumbnail,
    UpdateProduct,
    DeleteProduct,
    CreateOrder,
}

object TestFixtures {
    const val CUSTOMER_ID = "customer-1"
    const val GADGET_NEW_ID = "gadget-new"
    const val GADGET_SALE_ID = "gadget-sale"
    const val FASHION_ID = "fashion-1"
    const val DECOR_ID = "decor-1"
    const val HEALTH_ID = "health-1"
    const val DIY_ID = "diy-1"

    fun customer(
        id: String = CUSTOMER_ID,
        cart: List<CartItem> = emptyList(),
        isAdmin: Boolean = false,
    ) = Customer(
        id = id,
        firstName = "Ada",
        lastName = "Lovelace",
        email = "ada@example.test",
        city = "Tel Aviv",
        postalCode = 61000,
        address = "1 Test Street",
        phoneNumber = com.trendy.shared.domain.PhoneNumber(972, "501234567"),
        cart = cart,
        isAdmin = isAdmin,
    )

    fun products(): List<Product> = listOf(
        product(GADGET_NEW_ID, "Nova Phone", ProductCategory.Gadgets, 100.0, isNew = true, isPopular = true),
        product(GADGET_SALE_ID, "Orbit Watch", ProductCategory.Gadgets, 50.0, isDiscounted = true),
        product(FASHION_ID, "Linen Jacket", ProductCategory.FashionStyle, 75.5),
        product(DECOR_ID, "Amber Lamp", ProductCategory.HomeDecor, 42.0),
        product(HEALTH_ID, "Balance Mat", ProductCategory.Health, 32.0),
        product(DIY_ID, "Maker Kit", ProductCategory.DIY, 18.5),
    )

    fun product(
        id: String = "product-1",
        title: String = "Test product",
        category: ProductCategory = ProductCategory.Gadgets,
        price: Double = 10.0,
        isNew: Boolean = false,
        isPopular: Boolean = false,
        isDiscounted: Boolean = false,
    ) = Product(
        id = id,
        createdAt = 1_700_000_000_000,
        title = title,
        description = "$title description",
        thumbnail = "android.resource://org.roy.ecommerce_project/drawable/trendy_mark",
        category = category.name,
        weight = 100,
        price = price,
        isNew = isNew,
        isPopular = isPopular,
        isDiscounted = isDiscounted,
    )
}
