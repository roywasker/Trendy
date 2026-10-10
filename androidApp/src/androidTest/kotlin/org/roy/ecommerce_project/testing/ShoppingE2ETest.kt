package org.roy.ecommerce_project.testing

import androidx.compose.ui.test.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.trendy.shared.domain.CartItem
import com.trendy.shared.domain.ProductCategory
import com.trendy.shared.navigation.Screen
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.roy.ecommerce_project.testing.fixtures.*

@RunWith(AndroidJUnit4::class)
class ShoppingE2ETest : AppUiTestBase() {
    private fun seededCart(quantity: Int = 1) = env.store.emitCustomer(
        TestFixtures.customer(cart = listOf(CartItem(id = "line-1", productId = TestFixtures.GADGET_NEW_ID, quantity = quantity)))
    )
    private fun category(category: ProductCategory, title: String) {
        launch(); waitText("Discounted Products"); categories(); clickText(category.title); waitText(title)
    }
    @Test fun catalogue_displaysNewAndDiscountedProducts() { launch(); waitText("Nova Phone"); waitText("Orbit Watch") }
    @Test fun emptyCatalogue_displaysEmptyState() { env.store.emitProducts(emptyList()); launch(); waitText("Empty product list.") }
    @Test fun catalogueFailure_displaysBackendError() { env.store.emitProductsError("Catalogue unavailable"); launch(); waitText("Catalogue unavailable") }
    @Test fun catalogueLoading_transitionsToProducts() {
        env.store.emitProductsLoading(); launch(); ui.onNodeWithTag("loading").assertExists()
        ui.runOnIdle { env.store.emitProducts(TestFixtures.products()) }; waitText("Nova Phone")
    }
    @Test fun gadgetCategory_displaysOnlyGadgets() { category(ProductCategory.Gadgets, "Nova Phone"); ui.onNodeWithText("Linen Jacket").assertDoesNotExist() }
    @Test fun fashionCategory_displaysFashion() = category(ProductCategory.FashionStyle, "Linen Jacket")
    @Test fun homeDecorCategory_displaysDecor() = category(ProductCategory.HomeDecor, "Amber Lamp")
    @Test fun healthCategory_displaysHealth() = category(ProductCategory.Health, "Balance Mat")
    @Test fun diyCategory_displaysDiy() = category(ProductCategory.DIY, "Maker Kit")
    @Test fun categorySearch_filtersAndClearsResults() {
        category(ProductCategory.Gadgets, "Nova Phone"); clickDescription("Search icon")
        ui.onNode(hasSetTextAction()).performTextInput("Orbit"); waitText("Orbit Watch")
        waitAbsent("Nova Phone"); ui.onNodeWithText("Nova Phone").assertDoesNotExist(); clickDescription("Close icon"); waitText("Nova Phone")
    }
    @Test fun categorySearch_noMatches_displaysEmptyState() {
        category(ProductCategory.Gadgets, "Nova Phone"); clickDescription("Search icon")
        ui.onNode(hasSetTextAction()).performTextInput("zzzz"); waitText("We couldn’t find any product.")
    }
    @Test fun categoryBackendFailure_displaysError() {
        env.store.emitProductReadState(ProductRead.ByCategory, com.trendy.shared.util.RequestState.Error("Category unavailable"))
        launch(Screen.CategorySearch(ProductCategory.Gadgets.name)); waitText("Category unavailable")
    }
    @Test fun productDetails_showDescriptionPriceAndWeight() {
        launch(); clickText("Nova Phone"); waitText("Add to Cart"); waitText("Nova Phone description"); waitText("100g")
        ui.onNodeWithText("$100.0").assertExists()
    }
    @Test fun diyDetails_hideWeight() {
        launch(Screen.Details(TestFixtures.DIY_ID)); waitText("Add to Cart")
        ui.onNodeWithContentDescription("Weight icon").assertDoesNotExist()
    }
    @Test fun missingProduct_displaysError() { launch(Screen.Details("missing")); waitText("Oops!") }
    @Test fun detailsReadFailure_doesNotOfferAddToCart() {
        env.store.emitProductByIdState(TestFixtures.GADGET_NEW_ID, com.trendy.shared.util.RequestState.Error("Detail unavailable"))
        launch(Screen.Details(TestFixtures.GADGET_NEW_ID)); waitText("Detail unavailable")
        ui.onNodeWithText("Add to Cart").assertDoesNotExist()
    }
    @Test fun productToCartJourney_addsCorrectProduct() {
        launch(); clickText("Nova Phone"); clickText("Add to Cart"); waitText("Product added to cart.")
        clickDescription("Back Arrow icon"); cart(); waitText("Nova Phone")
        assertEquals(TestFixtures.GADGET_NEW_ID, env.store.customer().cart.single().productId)
    }
    @Test fun addToCartFailure_keepsCartEmpty() {
        env.store.failNext(TestOperation.AddCartItem, "Cannot add item"); launch(Screen.Details(TestFixtures.GADGET_NEW_ID))
        clickText("Add to Cart"); waitText("Cannot add item"); assertTrue(env.store.customer().cart.isEmpty())
    }
    @Test fun emptyCart_hasNoCheckoutAction() {
        launch(); waitText("Discounted Products"); cart(); waitText("Empty Cart")
        ui.onNodeWithContentDescription("RightArrow icon").assertDoesNotExist()
    }
    @Test fun cartQuantity_incrementAndDecrementPersist() {
        seededCart(); launch(); waitText("Discounted Products"); cart(); waitText("Nova Phone")
        clickDescription("Plus icon"); ui.waitUntil { env.store.customer().cart.single().quantity == 2 }
        clickDescription("Minus icon"); ui.waitUntil { env.store.customer().cart.single().quantity == 1 }
    }
    @Test fun cartQuantity_cannotGoBelowOne() {
        seededCart(); launch(); waitText("Discounted Products"); cart(); waitText("Nova Phone")
        clickDescription("Minus icon"); assertEquals(1, env.store.customer().cart.single().quantity)
    }
    @Test fun cartQuantity_cannotExceedTen() {
        seededCart(10); launch(); waitText("Discounted Products"); cart(); waitText("Nova Phone")
        clickDescription("Plus icon"); assertEquals(10, env.store.customer().cart.single().quantity)
    }
    @Test fun cartMutationFailure_showsErrorAndPreservesQuantity() {
        seededCart(); env.store.failNext(TestOperation.UpdateCartItem, "Cannot change quantity")
        launch(); waitText("Discounted Products"); cart(); waitText("Nova Phone")
        clickDescription("Plus icon"); waitText("Cannot change quantity"); assertEquals(1, env.store.customer().cart.single().quantity)
    }
    @Test fun deleteCartItem_displaysEmptyState() {
        seededCart(); launch(); waitText("Discounted Products"); cart(); waitText("Nova Phone")
        clickDescription("Delete icon"); waitText("Empty Cart"); assertTrue(env.store.customer().cart.isEmpty())
    }
    @Test fun cartDeleteFailure_keepsItem() {
        seededCart(); env.store.failNext(TestOperation.DeleteCartItem, "Cannot delete item")
        launch(); waitText("Discounted Products"); cart(); waitText("Nova Phone")
        clickDescription("Delete icon"); waitText("Cannot delete item"); assertEquals(1, env.store.customer().cart.size)
    }
    @Test fun cartBackendFailure_displaysError() {
        env.store.emitCustomerError("Customer unavailable"); launch(Screen.HomeGraph); cart(); waitText("Customer unavailable")
    }
    @Test fun removedProduct_isNotShownAsPurchasableCartItem() {
        env.store.emitCustomer(TestFixtures.customer(cart = listOf(CartItem("orphan", "missing", quantity = 1))))
        launch(); waitText("Discounted Products"); cart(); waitText("Product unavailable"); ui.onNodeWithText("Empty Cart").assertDoesNotExist()
    }
    @Test fun checkout_receivesQuantityAdjustedTotal() {
        seededCart(3); launch(); waitText("Discounted Products"); cart(); waitText("Nova Phone")
        clickDescription("RightArrow icon"); waitText("Checkout"); ui.onNodeWithText("$300.0").assertExists()
    }
    @Test fun repeatedBottomNavigation_doesNotLoseCart() {
        seededCart(); launch(); waitText("Discounted Products"); cart(); categories(); cart(); waitText("Nova Phone")
        assertEquals(1, env.store.customer().cart.size)
    }
    @Test fun activityRecreation_preservesCatalogueAndSession() {
        launch(); waitText("Discounted Products"); ui.activityRule.scenario.recreate(); waitText("Discounted Products")
        assertNotNull(env.store.currentUserId)
    }
    @Test fun contactEntry_isVisibleButHasNoImplementedNavigation() {
        launch(); waitText("Discounted Products"); openDrawer(); clickText("Contact us")
        ui.onNodeWithText("Contact us").assertExists()
        assertTrue(env.store.createdOrders.isEmpty())
        assertTrue(env.payments.openedUrls.isEmpty())
    }
    @Test fun deletedCatalogueProductCanBeRemovedFromCart() {
        env.store.emitCustomer(TestFixtures.customer(cart=listOf(CartItem("orphan","deleted-product",quantity=1))))
        launch();waitText("Discounted Products");cart();waitText("Product unavailable")
        clickDescription("Delete icon");waitText("Empty Cart");assertTrue(env.store.customer().cart.isEmpty())
    }
}
