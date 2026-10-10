package org.roy.ecommerce_project.testing

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.Trendy.admin_panel.AdminPanelViewModel
import com.Trendy.auth.AuthViewModel
import com.Trendy.cart.CartViewModel
import com.Trendy.category_search.CategorySearchViewModel
import com.Trendy.home.HomeGraphViewModel
import com.Trendy.manage_product.ManageProductViewModel
import com.Trendy.products_overview.ProductsOverviewViewModel
import com.Trendy.profile.ProfileViewModel
import com.trendy.details.DetailsViewModel
import com.trendy.shared.domain.CartItem
import com.trendy.shared.domain.Country
import com.trendy.shared.domain.PhoneNumber
import com.trendy.shared.domain.ProductCategory
import com.trendy.shared.util.RequestState
import dev.gitlive.firebase.storage.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.roy.ecommerce_project.testing.fixtures.ProductRead
import org.roy.ecommerce_project.testing.fixtures.TestFixtures
import org.roy.ecommerce_project.testing.fixtures.TestOperation
import org.roy.ecommerce_project.testing.fixtures.TestRepositories
import org.roy.ecommerce_project.testing.fixtures.TestStore

/** Integration coverage for real ViewModels backed only by deterministic in-memory repositories. */
@RunWith(AndroidJUnit4::class)
class ViewModelIntegrationTest {

    @Test
    fun seededCatalogue_coversEveryCategoryAndMerchandisingCollection() {
        val products = TestFixtures.products()

        assertEquals(ProductCategory.entries.toSet(), products.map { ProductCategory.valueOf(it.category) }.toSet())
        assertTrue(products.any { it.isNew })
        assertTrue(products.any { it.isDiscounted })
        assertTrue(products.any { it.isPopular })
    }

    @Test
    fun auth_createsCustomerThroughRepositoryCallback() {
        val repos = TestRepositories()
        var created = false

        AuthViewModel(repos.customer).createCustomer(null, { created = true }, { throw AssertionError(it) })

        eventually { created }
        assertTrue(repos.store.mutations.contains("createCustomer"))
    }

    @Test
    fun auth_forwardsCustomerCreationFailure() {
        val repos = TestRepositories()
        repos.store.failNext(TestOperation.CreateCustomer, "account rejected")
        var error: String? = null

        AuthViewModel(repos.customer).createCustomer(null, {}, { error = it })

        eventually { error != null }
        assertEquals("account rejected", error)
    }

    @Test
    fun details_loadsProduct_andAddsRequestedQuantityToCart() = runBlocking {
        val repos = TestRepositories()
        val viewModel = DetailsViewModel(repos.product, repos.customer, SavedStateHandle(mapOf("id" to TestFixtures.GADGET_NEW_ID)))

        val product = withTimeout(2_000) { viewModel.product.first { it.isSuccess() }.getSuccessData() }
        viewModel.updateQuantity(3)
        var success = false
        viewModel.addItemToCart({ success = true }, { throw AssertionError(it) })

        eventually { success }
        assertEquals(TestFixtures.GADGET_NEW_ID, product.id)
        assertEquals(3, repos.store.customer().cart.single().quantity)
    }

    @Test
    fun details_reportsMissingRouteId_withoutMutatingCart() {
        val repos = TestRepositories()
        val viewModel = DetailsViewModel(repos.product, repos.customer, SavedStateHandle())
        var error: String? = null

        viewModel.addItemToCart({}, { error = it })

        eventually { error != null }
        assertEquals("Product id is not found.", error)
        assertTrue(repos.store.customer().cart.isEmpty())
    }

    @Test
    fun details_surfacesProductReadFailure() = runBlocking {
        val store = TestStore().also {
            it.emitProductByIdState(TestFixtures.GADGET_NEW_ID, RequestState.Error("offline"))
        }
        val repos = TestRepositories(store)
        val viewModel = DetailsViewModel(repos.product, repos.customer, SavedStateHandle(mapOf("id" to TestFixtures.GADGET_NEW_ID)))

        val state = withTimeout(2_000) { viewModel.product.first { it.isError() } }

        assertEquals("offline", state.getErrorMessage())
    }

    @Test
    fun details_forwardsAddToCartFailure_withoutAddingItem() {
        val repos = TestRepositories()
        repos.store.failNext(TestOperation.AddCartItem, "cart rejected")
        val viewModel = DetailsViewModel(repos.product, repos.customer, SavedStateHandle(mapOf("id" to TestFixtures.GADGET_NEW_ID)))
        var error: String? = null

        viewModel.addItemToCart({}, { error = it })

        eventually { error != null }
        assertEquals("cart rejected", error)
        assertTrue(repos.store.customer().cart.isEmpty())
    }

    @Test
    fun cart_combinesItemsAndProducts_andCalculatesQuantityMutation() = runBlocking {
        val item = CartItem(id = "cart-1", productId = TestFixtures.GADGET_NEW_ID, quantity = 2)
        val repos = TestRepositories(TestStore(customer = TestFixtures.customer(cart = listOf(item))))
        val viewModel = CartViewModel(repos.customer, repos.product)

        val rows = withTimeout(2_000) { viewModel.cartItemsWithProducts.first { it.isSuccess() }.getSuccessData() }
        var updated = false
        viewModel.updateCartItemQuantity(item.id, 4, { updated = true }, { throw AssertionError(it) })

        eventually { updated }
        assertEquals(1, rows.size)
        assertEquals(100.0, rows.single().second.price, 0.0)
        assertEquals(4, repos.store.customer().cart.single().quantity)
    }

    @Test
    fun cart_deleteFailure_keepsCartAndReportsError() {
        val item = CartItem(id = "cart-1", productId = TestFixtures.GADGET_NEW_ID, quantity = 1)
        val repos = TestRepositories(TestStore(customer = TestFixtures.customer(cart = listOf(item))))
        repos.store.failNext(TestOperation.DeleteCartItem, "cannot delete")
        var error: String? = null

        CartViewModel(repos.customer, repos.product).deleteCartItem(item.id, {}, { error = it })

        eventually { error != null }
        assertEquals("cannot delete", error)
        assertEquals(listOf(item), repos.store.customer().cart)
    }

    @Test
    fun cart_deletesExistingItem() {
        val item = CartItem(id = "cart-1", productId = TestFixtures.GADGET_NEW_ID, quantity = 1)
        val repos = TestRepositories(TestStore(customer = TestFixtures.customer(cart = listOf(item))))
        var deleted = false

        CartViewModel(repos.customer, repos.product).deleteCartItem(item.id, { deleted = true }, { throw AssertionError(it) })

        eventually { deleted }
        assertTrue(repos.store.customer().cart.isEmpty())
    }

    @Test
    fun cart_updateQuantityFailure_keepsOriginalQuantity() {
        val item = CartItem(id = "cart-1", productId = TestFixtures.GADGET_NEW_ID, quantity = 1)
        val repos = TestRepositories(TestStore(customer = TestFixtures.customer(cart = listOf(item))))
        repos.store.failNext(TestOperation.UpdateCartItem, "quantity rejected")
        var error: String? = null

        CartViewModel(repos.customer, repos.product).updateCartItemQuantity(item.id, 9, {}, { error = it })

        eventually { error != null }
        assertEquals("quantity rejected", error)
        assertEquals(1, repos.store.customer().cart.single().quantity)
    }

    @Test
    fun cart_surfacesIndependentProductsByIdsFailure() = runBlocking {
        val item = CartItem(id = "cart-1", productId = TestFixtures.GADGET_NEW_ID, quantity = 1)
        val store = TestStore(customer = TestFixtures.customer(cart = listOf(item))).also {
            it.emitProductReadState(ProductRead.ByIds, RequestState.Error("catalogue unavailable"))
        }
        val state = withTimeout(2_000) {
            CartViewModel(TestRepositories(store).customer, TestRepositories(store).product)
                .cartItemsWithProducts.first { it.isError() }
        }

        assertEquals("catalogue unavailable", state.getErrorMessage())
    }

    @Test
    fun cart_keepsDeletedProductAsRemovableUnavailableRow() = runBlocking {
        val item = CartItem(id = "cart-1", productId = "deleted-product", quantity = 1)
        val repos = TestRepositories(TestStore(customer = TestFixtures.customer(cart = listOf(item))))

        val rows = withTimeout(2_000) {
            CartViewModel(repos.customer, repos.product).cartItemsWithProducts.first { it.isSuccess() }.getSuccessData()
        }

        assertEquals(item, rows.single().first)
        assertFalse(rows.single().second.isAvailable)
    }

    @Test
    fun cart_propagatesCustomerLoading() = runBlocking {
        val store = TestStore().also { it.emitCustomerLoading() }

        val state = withTimeout(2_000) {
            CartViewModel(TestRepositories(store).customer, TestRepositories(store).product)
                .cartItemsWithProducts.first { it.isLoading() }
        }

        assertTrue(state.isLoading())
    }

    @Test
    fun cart_propagatesCustomerError() = runBlocking {
        val store = TestStore().also { it.emitCustomerError("customer offline") }

        val state = withTimeout(2_000) {
            CartViewModel(TestRepositories(store).customer, TestRepositories(store).product)
                .cartItemsWithProducts.first { it.isError() }
        }

        assertEquals("customer offline", state.getErrorMessage())
    }

    @Test
    fun home_calculatesCartTotal_andSignsOut() = runBlocking {
        val cart = listOf(
            CartItem(id = "a", productId = TestFixtures.GADGET_NEW_ID, quantity = 2),
            CartItem(id = "b", productId = TestFixtures.GADGET_SALE_ID, quantity = 3),
        )
        val repos = TestRepositories(TestStore(customer = TestFixtures.customer(cart = cart)))
        val viewModel = HomeGraphViewModel(repos.customer, repos.product)

        val total = withTimeout(2_000) { viewModel.totalPrice.first { it.isSuccess() }.getSuccessData() }
        var signedOut = false
        viewModel.sighOut({ signedOut = true }, { throw AssertionError(it) })

        eventually { signedOut }
        assertEquals(350.0, total, 0.0)
        assertNull(repos.store.currentUserId)
    }

    @Test
    fun home_blocksCheckoutWithProductsMissingFromCatalogue() = runBlocking {
        val cart = listOf(CartItem(id="missing",productId="deleted",quantity=5))
        val repos=TestRepositories(TestStore(customer=TestFixtures.customer(cart=cart)))
        val state=withTimeout(2_000) { HomeGraphViewModel(repos.customer,repos.product).totalPrice.first { it.isError() } }
        assertTrue(state.getErrorMessage().contains("unavailable"))
    }

    @Test
    fun home_signOutFailure_keepsCurrentUserAndForwardsError() {
        val repos = TestRepositories()
        repos.store.failNext(TestOperation.SignOut, "sign out rejected")
        var error: String? = null

        HomeGraphViewModel(repos.customer, repos.product).sighOut({}, { error = it })

        eventually { error != null }
        assertEquals("sign out rejected", error)
        assertEquals(TestFixtures.CUSTOMER_ID, repos.store.currentUserId)
    }

    @Test
    fun home_propagatesCustomerErrorToCartAndTotal() = runBlocking {
        val store = TestStore().also { it.emitCustomerError("session expired") }
        val viewModel = HomeGraphViewModel(TestRepositories(store).customer, TestRepositories(store).product)

        val state = withTimeout(2_000) { viewModel.totalPrice.first { it.isError() } }

        assertEquals("session expired", state.getErrorMessage())
    }

    @Test
    fun productsOverview_mergesNewAndDiscountedCollections() = runBlocking {
        val viewModel = ProductsOverviewViewModel(TestRepositories().product)

        val products = withTimeout(2_000) { viewModel.products.first { it.isSuccess() }.getSuccessData() }

        assertEquals(setOf(TestFixtures.GADGET_NEW_ID, TestFixtures.GADGET_SALE_ID), products.map { it.id }.toSet())
    }

    @Test
    fun productsOverview_propagatesNewCollectionFailure() = runBlocking {
        val store = TestStore().also { it.emitProductReadState(ProductRead.New, RequestState.Error("new unavailable")) }
        val viewModel = ProductsOverviewViewModel(TestRepositories(store).product)

        val state = withTimeout(2_000) { viewModel.products.first { it.isError() } }

        assertEquals("new unavailable", state.getErrorMessage())
    }

    @Test
    fun productsOverview_propagatesDiscountedCollectionFailure() = runBlocking {
        val store = TestStore().also { it.emitProductReadState(ProductRead.Discounted, RequestState.Error("discount unavailable")) }
        val viewModel = ProductsOverviewViewModel(TestRepositories(store).product)

        val state = withTimeout(2_000) { viewModel.products.first { it.isError() } }

        assertEquals("discount unavailable", state.getErrorMessage())
    }

    @Test
    fun productsOverview_staysLoadingWhileACollectionIsLoading() = runBlocking {
        val store = TestStore().also { it.emitProductReadState(ProductRead.New, RequestState.Loading) }
        val viewModel = ProductsOverviewViewModel(TestRepositories(store).product)

        val state = withTimeout(2_000) { viewModel.products.first { it.isLoading() } }

        assertTrue(state.isLoading())
    }

    @Test
    fun categorySearch_filtersCaseInsensitivelyWithinRequestedCategory() = runBlocking {
        val viewModel = CategorySearchViewModel(
            TestRepositories().product,
            SavedStateHandle(mapOf("category" to ProductCategory.Gadgets.name)),
        )
        withTimeout(3_000) { viewModel.filteredProducts.first { it.isSuccess() } }
        viewModel.updateSearchQuery("ORBIT")

        val products = withTimeout(3_000) {
            viewModel.filteredProducts.first { it.isSuccess() && it.getSuccessData().size == 1 }.getSuccessData()
        }

        assertEquals(TestFixtures.GADGET_SALE_ID, products.single().id)
    }

    @Test
    fun categorySearch_propagatesCategoryReadError() = runBlocking {
        val store = TestStore().also { it.emitProductReadState(ProductRead.ByCategory, RequestState.Error("category unavailable")) }
        val viewModel = CategorySearchViewModel(TestRepositories(store).product, SavedStateHandle())

        val state = withTimeout(2_000) { viewModel.filteredProducts.first { it.isError() } }

        assertEquals("category unavailable", state.getErrorMessage())
    }

    @Test
    fun categorySearch_returnsEmptySuccessForNoMatchingTitle() = runBlocking {
        val viewModel = CategorySearchViewModel(TestRepositories().product, SavedStateHandle())
        withTimeout(3_000) { viewModel.filteredProducts.first { it.isSuccess() } }
        viewModel.updateSearchQuery("not-present")

        val products = withTimeout(3_000) {
            viewModel.filteredProducts.first { it.isSuccess() && it.getSuccessData().isEmpty() }.getSuccessData()
        }

        assertTrue(products.isEmpty())
    }

    @Test
    fun categorySearch_clearingQuery_restoresCategoryResults() = runBlocking {
        val viewModel = CategorySearchViewModel(TestRepositories().product, SavedStateHandle())
        withTimeout(3_000) { viewModel.filteredProducts.first { it.isSuccess() } }
        viewModel.updateSearchQuery("Nova")
        withTimeout(3_000) { viewModel.filteredProducts.first { it.isSuccess() && it.getSuccessData().size == 1 } }
        viewModel.updateSearchQuery("")

        val restored = withTimeout(3_000) {
            viewModel.filteredProducts.first { it.isSuccess() && it.getSuccessData().size == 2 }.getSuccessData()
        }

        assertEquals(setOf(TestFixtures.GADGET_NEW_ID, TestFixtures.GADGET_SALE_ID), restored.map { it.id }.toSet())
    }

    @Test
    fun categorySearch_queryEnteredWhileLoading_filtersWhenProductsArrive() = runBlocking {
        val store = TestStore().also { it.emitProductReadState(ProductRead.ByCategory, RequestState.Loading) }
        val viewModel = CategorySearchViewModel(
            TestRepositories(store).product,
            SavedStateHandle(mapOf("category" to ProductCategory.Gadgets.name)),
        )
        withTimeout(2_000) { viewModel.filteredProducts.first { it.isLoading() } }
        viewModel.updateSearchQuery("orbit")
        store.emitProductReadState(ProductRead.ByCategory, null)

        val products = withTimeout(3_000) {
            viewModel.filteredProducts.first { it.isSuccess() && it.getSuccessData().map { product -> product.id } == listOf(TestFixtures.GADGET_SALE_ID) }
                .getSuccessData()
        }

        assertEquals(listOf(TestFixtures.GADGET_SALE_ID), products.map { it.id })
    }

    @Test
    fun categorySearch_activeQuery_refreshesWhenCategoryProductsChange() = runBlocking {
        val store = TestStore()
        val viewModel = CategorySearchViewModel(
            TestRepositories(store).product,
            SavedStateHandle(mapOf("category" to ProductCategory.Gadgets.name)),
        )
        withTimeout(3_000) { viewModel.filteredProducts.first { it.isSuccess() } }
        viewModel.updateSearchQuery("orbit")
        withTimeout(3_000) {
            viewModel.filteredProducts.first { it.isSuccess() && it.getSuccessData().size == 1 }
        }
        store.emitProducts(TestFixtures.products() + TestFixtures.product(
            id = "orbit-mini",
            title = "Orbit Mini",
            category = ProductCategory.Gadgets,
            price = 25.0,
        ))

        val refreshed = withTimeout(3_000) {
            viewModel.filteredProducts.first { it.isSuccess() && it.getSuccessData().size == 2 }.getSuccessData()
        }

        assertEquals(setOf(TestFixtures.GADGET_SALE_ID, "orbit-mini"), refreshed.map { it.id }.toSet())
    }

    @Test
    fun adminSearch_returnsMatchingProducts_andEmptyQueryRestoresRecentProducts() = runBlocking {
        val viewModel = AdminPanelViewModel(TestRepositories().admin)
        viewModel.updateSearchQuery("lamp")
        val searched = withTimeout(3_000) { viewModel.filteredProducts.first { it.isSuccess() && it.getSuccessData().size == 1 }.getSuccessData() }
        viewModel.updateSearchQuery("")
        val all = withTimeout(3_000) { viewModel.filteredProducts.first { it.isSuccess() && it.getSuccessData().size == TestFixtures.products().size }.getSuccessData() }

        assertEquals(TestFixtures.DECOR_ID, searched.single().id)
        assertEquals(TestFixtures.products().size, all.size)
    }

    @Test
    fun adminSearch_returnsEmptySuccessForMissingTitle() = runBlocking {
        val viewModel = AdminPanelViewModel(TestRepositories().admin)
        viewModel.updateSearchQuery("nothing-here")

        val products = withTimeout(3_000) {
            viewModel.filteredProducts.first { it.isSuccess() && it.getSuccessData().isEmpty() }.getSuccessData()
        }

        assertTrue(products.isEmpty())
    }

    @Test
    fun adminSearch_propagatesInitialRecentProductsError() = runBlocking {
        val store = TestStore().also { it.emitProductReadState(ProductRead.LastTen, RequestState.Error("admin catalogue unavailable")) }
        val viewModel = AdminPanelViewModel(TestRepositories(store).admin)

        val state = withTimeout(2_000) { viewModel.filteredProducts.first { it.isError() } }

        assertEquals("admin catalogue unavailable", state.getErrorMessage())
    }

    @Test
    fun adminSearch_propagatesSearchEndpointError() = runBlocking {
        val store = TestStore().also { it.emitProductReadState(ProductRead.Search, RequestState.Error("search unavailable")) }
        val viewModel = AdminPanelViewModel(TestRepositories(store).admin)
        viewModel.updateSearchQuery("phone")

        val state = withTimeout(3_000) { viewModel.filteredProducts.first { it.isError() } }

        assertEquals("search unavailable", state.getErrorMessage())
    }

    @Test
    fun profile_loadsCompleteCustomer_andChangingCountryChangesDialCode() {
        val viewModel = ProfileViewModel(TestRepositories().customer)

        eventually { viewModel.screenReady.isSuccess() }
        viewModel.updateCountry(Country.Usa)

        assertEquals("Ada", viewModel.screenState.firstName)
        assertEquals(Country.Usa.dialCode, viewModel.screenState.phoneNumber?.dialCode)
    }

    @Test
    fun profile_validation_requiresEveryAddressField() {
        val viewModel = ProfileViewModel(TestRepositories().customer)
        eventually { viewModel.screenReady.isSuccess() }
        viewModel.updateAddress("x")

        assertFalse("A malformed address must invalidate the form even with a postal code", viewModel.isFormValid)
    }

    @Test
    fun profile_acceptsMinimumAndMaximumFirstNameLengths() {
        val viewModel = readyProfile()
        viewModel.updateFirstName("Ada")
        assertTrue(viewModel.isFormValid)
        viewModel.updateFirstName("a".repeat(50))
        assertTrue(viewModel.isFormValid)
    }

    @Test
    fun profile_rejectsFirstNameOutsideAllowedRange() {
        val viewModel = readyProfile()
        viewModel.updateFirstName("Al")
        assertFalse(viewModel.isFormValid)
        viewModel.updateFirstName("a".repeat(51))
        assertFalse(viewModel.isFormValid)
    }

    @Test
    fun profile_acceptsMinimumAndMaximumLastNameLengths() {
        val viewModel = readyProfile()
        viewModel.updateLastName("Doe")
        assertTrue(viewModel.isFormValid)
        viewModel.updateLastName("d".repeat(50))
        assertTrue(viewModel.isFormValid)
    }

    @Test
    fun profile_rejectsLastNameOutsideAllowedRange() {
        val viewModel = readyProfile()
        viewModel.updateLastName("Li")
        assertFalse(viewModel.isFormValid)
        viewModel.updateLastName("l".repeat(51))
        assertFalse(viewModel.isFormValid)
    }

    @Test
    fun profile_acceptsCityAndAddressAtBothBoundaries() {
        val viewModel = readyProfile()
        viewModel.updateCity("Tel")
        viewModel.updateAddress("One")
        assertTrue(viewModel.isFormValid)
        viewModel.updateCity("c".repeat(50))
        viewModel.updateAddress("a".repeat(50))
        assertTrue(viewModel.isFormValid)
    }

    @Test
    fun profile_rejectsCityOutsideAllowedRange() {
        val viewModel = readyProfile()
        viewModel.updateCity("TL")
        assertFalse(viewModel.isFormValid)
        viewModel.updateCity("c".repeat(51))
        assertFalse(viewModel.isFormValid)
    }

    @Test
    fun profile_rejectsAddressOutsideAllowedRange() {
        val viewModel = readyProfile()
        viewModel.updateAddress("No")
        assertFalse(viewModel.isFormValid)
        viewModel.updateAddress("a".repeat(51))
        assertFalse(viewModel.isFormValid)
    }

    @Test
    fun profile_requiresPostalCodeWithThreeToEightDigits() {
        val viewModel = readyProfile()
        viewModel.updatePostalCode(null)
        assertFalse(viewModel.isFormValid)
        viewModel.updatePostalCode(12)
        assertFalse(viewModel.isFormValid)
        viewModel.updatePostalCode(123)
        assertTrue(viewModel.isFormValid)
        viewModel.updatePostalCode(12_345_678)
        assertTrue(viewModel.isFormValid)
        viewModel.updatePostalCode(123_456_789)
        assertFalse(viewModel.isFormValid)
    }

    @Test
    fun profile_requiresPhoneWithFiveToFiftyDigits() {
        val viewModel = readyProfile()
        viewModel.updatePhoneNumber("1234")
        assertFalse(viewModel.isFormValid)
        viewModel.updatePhoneNumber("12345")
        assertTrue(viewModel.isFormValid)
        viewModel.updatePhoneNumber("1".repeat(50))
        assertTrue(viewModel.isFormValid)
        viewModel.updatePhoneNumber("1".repeat(51))
        assertFalse(viewModel.isFormValid)
    }

    @Test
    fun profile_unknownDialCode_defaultsToIsrael() {
        val store = TestStore(customer = TestFixtures.customer().copy(phoneNumber = PhoneNumber(999, "1234567")))
        val viewModel = ProfileViewModel(TestRepositories(store).customer)

        eventually { viewModel.screenReady.isSuccess() }

        assertEquals(Country.Israel, viewModel.screenState.country)
    }

    @Test
    fun profile_update_preservesCartAndAdminRole() {
        val cart = listOf(CartItem(id = "cart-1", productId = TestFixtures.GADGET_NEW_ID, quantity = 1))
        val repos = TestRepositories(TestStore(customer = TestFixtures.customer(cart = cart, isAdmin = true)))
        val viewModel = ProfileViewModel(repos.customer)
        eventually { viewModel.screenReady.isSuccess() }
        viewModel.updateFirstName("Grace")
        var updated = false
        viewModel.updateCustomer({ updated = true }, { throw AssertionError(it) })

        eventually { updated }
        assertEquals("Grace", repos.store.customer().firstName)
        assertEquals(cart, repos.store.customer().cart)
        assertTrue(repos.store.customer().isAdmin)
    }

    @Test
    fun profile_updateFailure_forwardsErrorAndDoesNotChangeCustomer() {
        val repos = TestRepositories()
        val viewModel = ProfileViewModel(repos.customer)
        eventually { viewModel.screenReady.isSuccess() }
        viewModel.updateFirstName("Grace")
        repos.store.failNext(TestOperation.UpdateCustomer, "profile rejected")
        var error: String? = null

        viewModel.updateCustomer({}, { error = it })

        eventually { error != null }
        assertEquals("profile rejected", error)
        assertEquals("Ada", repos.store.customer().firstName)
    }

    @Test
    fun profile_surfacesCustomerLoadError() {
        val store = TestStore().also { it.emitCustomerError("customer unavailable") }
        val viewModel = ProfileViewModel(TestRepositories(store).customer)

        eventually { viewModel.screenReady.isError() }

        assertEquals("customer unavailable", viewModel.screenReady.getErrorMessage())
    }

    @Test
    fun profile_remainsLoadingUntilCustomerIsAvailable() {
        val store = TestStore().also { it.emitCustomerLoading() }
        val viewModel = ProfileViewModel(TestRepositories(store).customer)

        assertTrue(viewModel.screenReady.isLoading())
    }

    @Test
    fun manageProduct_loadsExistingProduct_andEditsItsFields() {
        val repos = TestRepositories()
        val viewModel = ManageProductViewModel(repos.admin, SavedStateHandle(mapOf("id" to TestFixtures.GADGET_NEW_ID)))

        eventually { viewModel.screenState.id == TestFixtures.GADGET_NEW_ID }
        viewModel.updateTitle("Updated phone")
        var success = false
        viewModel.updateProduct({ success = true }, { throw AssertionError(it) })

        eventually { success }
        assertEquals("Updated phone", repos.store.productsState.value.getSuccessData().first { it.id == TestFixtures.GADGET_NEW_ID }.title)
    }

    @Test
    fun manageProduct_rejectsInvalidCreateWithoutWritingProduct() {
        val repos = TestRepositories()
        val viewModel = ManageProductViewModel(repos.admin, SavedStateHandle())
        var error: String? = null

        viewModel.updateTitle("   ")
        viewModel.updateDescription("Description")
        viewModel.updateThumbnail("icon")
        viewModel.updatePrice(Double.NaN)
        viewModel.createNewProduct({}, { error = it })

        eventually { error != null }
        assertTrue(error!!.isNotBlank())
        assertEquals(TestFixtures.products().size, repos.store.productsState.value.getSuccessData().size)
    }

    @Test
    fun manageProduct_createsValidProductWithAllSelectedFlags() {
        val repos = TestRepositories()
        val viewModel = ManageProductViewModel(repos.admin, SavedStateHandle())
        populateValidProduct(viewModel)
        viewModel.updateNew(true)
        viewModel.updatePopular(true)
        viewModel.updateDiscounted(true)
        var success = false

        viewModel.createNewProduct({ success = true }, { throw AssertionError(it) })

        eventually { success }
        val created = repos.store.productsState.value.getSuccessData().last()
        assertEquals("Created product", created.title)
        assertTrue(created.isNew && created.isPopular && created.isDiscounted)
    }

    @Test
    fun manageProduct_forwardsCreateFailure_withoutWritingProduct() {
        val repos = TestRepositories()
        val viewModel = ManageProductViewModel(repos.admin, SavedStateHandle())
        populateValidProduct(viewModel)
        repos.store.failNext(TestOperation.CreateProduct, "create rejected")
        var error: String? = null

        viewModel.createNewProduct({}, { error = it })

        eventually { error != null }
        assertEquals("create rejected", error)
        assertEquals(TestFixtures.products().size, repos.store.productsState.value.getSuccessData().size)
    }

    @Test
    fun manageProduct_rejectsInvalidUpdate_withoutRepositoryWrite() {
        val repos = TestRepositories()
        val viewModel = ManageProductViewModel(repos.admin, SavedStateHandle(mapOf("id" to TestFixtures.GADGET_NEW_ID)))
        eventually { viewModel.screenState.id == TestFixtures.GADGET_NEW_ID }
        viewModel.updatePrice(0.0)
        var error: String? = null

        viewModel.updateProduct({}, { error = it })

        assertEquals("Please fill in the information ", error)
        assertFalse(repos.store.mutations.any { it.startsWith("updateProduct:") })
    }

    @Test
    fun manageProduct_forwardsUpdateFailure() {
        val repos = TestRepositories()
        val viewModel = ManageProductViewModel(repos.admin, SavedStateHandle(mapOf("id" to TestFixtures.GADGET_NEW_ID)))
        eventually { viewModel.screenState.id == TestFixtures.GADGET_NEW_ID }
        repos.store.failNext(TestOperation.UpdateProduct, "update rejected")
        var error: String? = null

        viewModel.updateProduct({}, { error = it })

        eventually { error != null }
        assertEquals("update rejected", error)
    }

    @Test
    fun manageProduct_nullImage_setsReadableUploadError() {
        val viewModel = ManageProductViewModel(TestRepositories().admin, SavedStateHandle())

        viewModel.uploadImageToStorage(null) {}

        assertTrue(viewModel.uploadImageState.isError())
        assertEquals("File is null", viewModel.uploadImageState.getErrorMessage())
    }

    @Test
    fun manageProduct_uploadsNewProductImage_andUpdatesThumbnailState() {
        val repos = TestRepositories()
        val viewModel = ManageProductViewModel(repos.admin, SavedStateHandle())
        var success = false

        viewModel.uploadImageToStorage(testFile(), { success = true })

        eventually { success }
        assertTrue(viewModel.uploadImageState.isSuccess())
        assertEquals(repos.store.uploadedImageUrl, viewModel.screenState.thumbnail)
    }

    @Test
    fun manageProduct_uploadFailure_setsErrorWithoutSuccessCallback() {
        val repos = TestRepositories()
        repos.store.failNext(TestOperation.UploadImage, "storage unavailable")
        val viewModel = ManageProductViewModel(repos.admin, SavedStateHandle())
        var success = false

        viewModel.uploadImageToStorage(testFile(), { success = true })

        eventually { viewModel.uploadImageState.isError() }
        assertFalse(success)
        assertTrue(viewModel.uploadImageState.getErrorMessage().contains("storage unavailable"))
    }

    @Test
    fun manageProduct_existingProductImageUpload_updatesStoredThumbnail() {
        val repos = TestRepositories()
        val viewModel = ManageProductViewModel(repos.admin, SavedStateHandle(mapOf("id" to TestFixtures.GADGET_NEW_ID)))
        eventually { viewModel.screenState.id == TestFixtures.GADGET_NEW_ID }
        repos.store.uploadedImageUrl = "android.resource://org.roy.ecommerce_project/drawable/ic_notification"
        var success = false

        viewModel.uploadImageToStorage(testFile(), { success = true })

        eventually { success }
        assertEquals(repos.store.uploadedImageUrl, repos.store.productsState.value.getSuccessData().first { it.id == TestFixtures.GADGET_NEW_ID }.thumbnail)
    }

    @Test
    fun manageProduct_thumbnailUpdateFailure_setsUploadError() {
        val repos = TestRepositories()
        val viewModel = ManageProductViewModel(repos.admin, SavedStateHandle(mapOf("id" to TestFixtures.GADGET_NEW_ID)))
        eventually { viewModel.screenState.id == TestFixtures.GADGET_NEW_ID }
        repos.store.failNext(TestOperation.UpdateThumbnail, "thumbnail rejected")

        viewModel.uploadImageToStorage(testFile()) {}

        eventually { viewModel.uploadImageState.isError() }
        assertEquals("thumbnail rejected", viewModel.uploadImageState.getErrorMessage())
    }

    @Test
    fun manageProduct_deleteImageForNewProduct_resetsThumbnailAndState() {
        val repos = TestRepositories()
        val viewModel = ManageProductViewModel(repos.admin, SavedStateHandle())
        viewModel.updateThumbnail("android.resource://org.roy.ecommerce_project/drawable/trendy_mark")
        viewModel.updateThumbnailUploaderState(RequestState.Success(Unit))
        var success = false

        viewModel.deleteImageFromStorage({ success = true }, { throw AssertionError(it) })

        eventually { success }
        assertEquals("", viewModel.screenState.thumbnail)
        assertTrue(viewModel.uploadImageState.isIdle())
    }

    @Test
    fun manageProduct_deleteImageFailure_forwardsErrorAndPreservesThumbnail() {
        val repos = TestRepositories()
        repos.store.failNext(TestOperation.DeleteImage, "delete image rejected")
        val viewModel = ManageProductViewModel(repos.admin, SavedStateHandle())
        viewModel.updateThumbnail("original")
        var error: String? = null

        viewModel.deleteImageFromStorage({}, { error = it })

        eventually { error != null }
        assertEquals("delete image rejected", error)
        assertEquals("original", viewModel.screenState.thumbnail)
    }

    @Test
    fun manageProduct_deleteFailure_keepsProductAndReportsError() {
        val repos = TestRepositories()
        repos.store.failNext(TestOperation.DeleteProduct, "delete rejected")
        val viewModel = ManageProductViewModel(repos.admin, SavedStateHandle(mapOf("id" to TestFixtures.GADGET_NEW_ID)))
        var error: String? = null

        eventually { viewModel.productLoadState.isSuccess() }
        viewModel.deleteProduct({}, { error = it })

        eventually { error != null }
        assertEquals("delete rejected", error)
        assertTrue(repos.store.productsState.value.getSuccessData().any { it.id == TestFixtures.GADGET_NEW_ID })
    }

    @Test
    fun manageProduct_deletesProductThenRemovesItsImage() {
        val repos = TestRepositories()
        val viewModel = ManageProductViewModel(repos.admin, SavedStateHandle(mapOf("id" to TestFixtures.GADGET_NEW_ID)))
        eventually { viewModel.screenState.id == TestFixtures.GADGET_NEW_ID }
        var success = false

        viewModel.deleteProduct({ success = true }, { throw AssertionError(it) })

        eventually { success }
        assertTrue(repos.store.productsState.value.getSuccessData().none { it.id == TestFixtures.GADGET_NEW_ID })
        assertTrue(repos.store.mutations.any { it.startsWith("deleteImage:") })
    }

    private fun populateValidProduct(viewModel: ManageProductViewModel) {
        viewModel.updateTitle("Created product")
        viewModel.updateDescription("A valid test description")
        viewModel.updateThumbnail("android.resource://org.roy.ecommerce_project/drawable/trendy_mark")
        viewModel.updateCategory(ProductCategory.DIY)
        viewModel.updateWeight(200)
        viewModel.updatePrice(20.0)
    }

    private fun testFile() = File(Uri.parse("content://org.roy.ecommerce_project.test/image"))

    private fun readyProfile(): ProfileViewModel = ProfileViewModel(TestRepositories().customer).also {
        eventually { it.screenReady.isSuccess() }
    }

    private fun eventually(message: String = "condition was not met", condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 2_500
        while (!condition() && System.currentTimeMillis() < deadline) Thread.sleep(20)
        assertTrue(message, condition())
    }
}
