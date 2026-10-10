package org.roy.ecommerce_project.testing

import androidx.lifecycle.SavedStateHandle
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.Trendy.auth.AuthScreen
import com.Trendy.auth.AuthViewModel
import com.Trendy.auth.LocalGoogleAuthContent
import com.Trendy.data.domain.CustomerRepository
import com.Trendy.category_search.CategorySearchViewModel
import com.Trendy.manage_product.ManageProductViewModel
import com.trendy.shared.domain.ProductCategory
import com.trendy.shared.util.RequestState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import dev.gitlive.firebase.auth.FirebaseUser
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.Rule
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import org.roy.ecommerce_project.testing.fixtures.TestFixtures
import org.roy.ecommerce_project.testing.fixtures.TestRepositories
import org.roy.ecommerce_project.testing.fixtures.TestStore

@RunWith(AndroidJUnit4::class)
class RegressionUiStateTest {
    @get:Rule val ui = createAndroidComposeRule<E2ETestActivity>()

    @After
    fun stopTestKoin() {
        ui.runOnUiThread { ui.activity.setContent {} }
        stopKoin()
    }

    @Test
    fun storedCategoryParser_returnsNullForUnknownValue() {
        assertEquals(ProductCategory.DIY, ProductCategory.fromStoredName("DIY"))
        assertEquals(null, ProductCategory.fromStoredName("Legacy category"))
    }

    @Test
    fun categorySearch_invalidSavedCategory_fallsBackToGadgets() = runBlocking {
        val viewModel = CategorySearchViewModel(
            TestRepositories().product,
            SavedStateHandle(mapOf("category" to "Legacy category")),
        )

        val products = withTimeout(2_000) {
            viewModel.filteredProducts.first { it.isSuccess() }.getSuccessData()
        }

        assertTrue(products.isNotEmpty())
        assertTrue(products.all { it.category == ProductCategory.Gadgets.name })
    }

    @Test
    fun editProduct_missingProduct_surfacesErrorAndBlocksSave() {
        val viewModel = ManageProductViewModel(
            TestRepositories().admin,
            SavedStateHandle(mapOf("id" to "missing-product")),
        )
        eventually { viewModel.productLoadState.isError() }
        populateValidProduct(viewModel)
        var error: String? = null

        viewModel.updateProduct({}, { error = it })

        assertEquals("Product not found: missing-product", error)
        assertFalse(viewModel.productLoadState.isSuccess())
    }

    @Test
    fun editProduct_unknownStoredCategory_surfacesErrorAndBlocksSave() {
        val product = TestFixtures.product(id = "legacy", category = ProductCategory.Gadgets).copy(category = "Legacy category")
        val store = TestStore(products = listOf(product))
        val viewModel = ManageProductViewModel(
            TestRepositories(store).admin,
            SavedStateHandle(mapOf("id" to product.id)),
        )
        eventually { viewModel.productLoadState.isError() }
        populateValidProduct(viewModel)
        var error: String? = null

        viewModel.updateProduct({}, { error = it })

        assertEquals("Unsupported product category.", error)
        assertFalse(store.mutations.any { it.startsWith("updateProduct:") })
    }

    @Test
    fun authentication_keepsGoogleButtonLoadingUntilCustomerCreationCompletes() {
        val repository = DeferredCustomerRepository()
        stopKoin()
        startKoin {
            modules(module {
                single<CustomerRepository> { repository }
                viewModelOf(::AuthViewModel)
            })
        }
        ui.setContent {
            MaterialTheme {
                CompositionLocalProvider(LocalGoogleAuthContent provides { result, content ->
                    content { result(Result.success(null)) }
                }) {
                    AuthScreen(navigateHome = {})
                }
            }
        }

        ui.onNodeWithText("Sign in with Google").performClick()
        ui.waitUntil(2_000) { repository.createCalls == 1 }
        ui.onNodeWithText("Please wait...").assertExists()
        assertEquals(1, repository.createCalls)

        repository.completeWithError("Customer creation failed")
        ui.waitUntil(2_000) { ui.onAllNodesWithText("Sign in with Google").fetchSemanticsNodes().isNotEmpty() }
    }

    private fun populateValidProduct(viewModel: ManageProductViewModel) {
        viewModel.updateTitle("Valid product")
        viewModel.updateDescription("Valid description")
        viewModel.updateThumbnail("android.resource://org.roy.ecommerce_project/drawable/trendy_mark")
        viewModel.updatePrice(10.0)
    }

    private fun eventually(condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 2_000
        while (!condition() && System.currentTimeMillis() < deadline) Thread.sleep(20)
        assertTrue(condition())
    }

    private class DeferredCustomerRepository : CustomerRepository by TestRepositories().customer {
        var createCalls = 0
            private set
        private var onSuccess: (() -> Unit)? = null
        private var onError: ((String) -> Unit)? = null

        override suspend fun createCustomer(
            user: FirebaseUser?,
            onSuccess: () -> Unit,
            onError: (String) -> Unit,
        ) {
            createCalls++
            this.onSuccess = onSuccess
            this.onError = onError
        }

        fun completeWithError(message: String) {
            onError?.invoke(message)
        }
    }
    @Test fun adminSearchMatchesUppercaseAndMixedCaseAgainstStoredLowercaseTitle() = runBlocking {
        val products=listOf(TestFixtures.product(title="nova phone"), TestFixtures.product(id="orbit",title="orbit watch"))
        val repository=com.Trendy.data.AdminRepositoryImpl(userId={"test-user"},productSnapshots={kotlinx.coroutines.flow.flowOf(products)})
        for(query in listOf("NOVA","  Nova  ")) {
            val result=repository.searchProductsByTitle(query).first().getSuccessData()
            assertEquals(listOf("NOVA PHONE"),result.map {it.title})
        }
        assertTrue(repository.searchProductsByTitle("Missing").first().getSuccessData().isEmpty())
    }
    @Test fun compiledConstantsContainNoMerchantCredentialFields() {
        val names=com.trendy.shared.Constants::class.java.declaredFields.map { it.name }
        assertFalse(names.any { it in listOf("PAYPAL_AUTH_KEY","PAYPAL_SECRET_ID","PAYPAL_CLIENT_ID") })
    }
}
