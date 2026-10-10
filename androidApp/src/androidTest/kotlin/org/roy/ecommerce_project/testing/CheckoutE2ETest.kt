package org.roy.ecommerce_project.testing

import androidx.compose.ui.test.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.trendy.shared.domain.CartItem
import com.trendy.shared.navigation.Screen
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.roy.ecommerce_project.testing.fixtures.*

@RunWith(AndroidJUnit4::class)
class CheckoutE2ETest : AppUiTestBase() {
    private fun checkout() {
        env.store.emitCustomer(TestFixtures.customer(cart = listOf(CartItem("line-1", TestFixtures.GADGET_NEW_ID, quantity = 2))))
        launch(); waitText("Discounted Products"); cart(); waitText("Nova Phone"); clickDescription("RightArrow icon"); waitText("Checkout")
    }
    @Test fun checkout_prefillsProfileAndTotal() { checkout(); field("First Name").assertTextContains("Ada"); ui.onNodeWithText("$200.0").assertExists() }
    @Test fun invalidAddress_disablesBothPaymentMethods() {
        checkout(); replaceField("Address", ""); ui.onNodeWithText("Pay with PayPal").assertIsNotEnabled(); ui.onNodeWithText("Pay on Delivery").assertIsNotEnabled()
    }
    @Test fun deliveryPaymentJourney_createsOneOrderClearsCartAndReturnsHome() {
        checkout(); clickText("Pay on Delivery"); waitText("Success!"); assertEquals(1, env.store.createdOrders.size)
        assertEquals(200.0, env.store.createdOrders.single().totalAmount, 0.001); assertTrue(env.store.customer().cart.isEmpty())
        clickText("Go back"); waitText("Discounted Products"); cart(); waitText("Empty Cart")
    }
    @Test fun deliveryProfileFailure_doesNotCreateOrder() {
        env.store.failNext(TestOperation.UpdateCustomer, "Shipping update failed"); checkout(); clickText("Pay on Delivery")
        waitText("Shipping update failed"); assertTrue(env.store.createdOrders.isEmpty()); assertEquals(1, env.store.customer().cart.size)
    }
    @Test fun deliveryOrderFailure_displaysFailureAndKeepsCart() {
        env.store.failNext(TestOperation.CreateOrder, "Order rejected"); checkout(); clickText("Pay on Delivery"); waitText("Order rejected")
        assertTrue(env.store.createdOrders.isEmpty()); assertEquals(1, env.store.customer().cart.size)
    }
    @Test fun paypalApprovalJourney_usesMockHttpCreatesOneOrderAndClearsCart() {
        env.payments.onBrowserOpened = { env.preferences.savePayPalData(true, null, "approved-order") }
        checkout(); clickText("Pay with PayPal"); waitText("Success!")
        assertEquals(1, env.payments.openedUrls.size); assertEquals(1, env.store.createdOrders.size)
        assertEquals("approved-order", env.store.createdOrders.single().token); assertTrue(env.store.customer().cart.isEmpty())
    }
    @Test fun paypalCancellation_showsFailureWithoutOrder() {
        env.payments.onBrowserOpened = { env.preferences.savePayPalData(false, "Payment has been canceled.", "cancel-order") }
        checkout(); clickText("Pay with PayPal"); waitText("Payment has been canceled."); assertTrue(env.store.createdOrders.isEmpty())
    }
    @Test fun paypalHttpFailure_staysOnCheckoutWithoutOrder() {
        env.payments.checkoutStatus = io.ktor.http.HttpStatusCode.BadRequest; env.payments.checkoutBody = "{}"
        checkout(); clickText("Pay with PayPal"); waitText("Payment could not be confirmed")
        assertTrue(env.store.createdOrders.isEmpty()); assertTrue(env.payments.openedUrls.isEmpty())
    }
    @Test fun paypalAuthenticationFailure_doesNotOpenBrowser() {
        env.payments.authStatus = io.ktor.http.HttpStatusCode.Unauthorized; env.payments.authBody = "{}"
        checkout(); clickText("Pay with PayPal"); waitText("Payment session unavailable")
        assertTrue(env.payments.openedUrls.isEmpty()); assertTrue(env.store.createdOrders.isEmpty())
    }
    @Test fun paypalOrderPersistenceFailure_showsFailureInsteadOfSuccess() {
        env.store.failNext(TestOperation.CreateOrder, "Cannot save paid order")
        env.payments.onBrowserOpened = { env.preferences.savePayPalData(true, null, "approved-order") }
        checkout(); clickText("Pay with PayPal"); waitText("Cannot save paid order")
        ui.onNodeWithText("Success!").assertDoesNotExist()
    }
    @Test fun checkoutBack_preservesCart() { checkout(); clickDescription("Back arrow icon"); waitText("Cart"); assertEquals(1, env.store.customer().cart.size) }
    @Test fun canceledPaymentWithoutToken_isStillHandledByNavigation() {
        launch(); waitText("Discounted Products"); ui.runOnIdle { env.preferences.savePayPalData(false, "Canceled without token", null) }
        waitText("Canceled without token"); assertTrue(env.store.createdOrders.isEmpty())
    }
    @Test fun paymentFailureBack_reopensCatalogue() {
        launch(Screen.PaymentCompleted(false, "Payment declined", null)); waitText("Payment declined"); clickText("Go back"); waitText("Discounted Products")
    }
    @Test fun paypalConfirmationRetryAfterPersistenceFailureCreatesOnlyOneOrder() {
        env.store.failNext(TestOperation.CreateOrder, "Retryable order save failure")
        env.payments.onBrowserOpened = { env.preferences.savePayPalData(true, null, "approved-order") }
        checkout();clickText("Pay with PayPal");waitText("Retryable order save failure")
        clickText("Retry confirmation");waitText("Success!");assertEquals(1, env.store.createdOrders.size)
    }
}
