package org.roy.ecommerce_project.testing

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.Trendy.checkout.domain.Amount
import com.Trendy.checkout.domain.ShippingAddress
import com.trendy.shared.domain.CartItem
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.roy.ecommerce_project.testing.fixtures.*

@RunWith(AndroidJUnit4::class)
class PaypalApiIntegrationTest {
    private val cart=listOf(CartItem("line1",TestFixtures.GADGET_NEW_ID,quantity=2))
    private val payments=TestPayments(TestStore(customer=TestFixtures.customer(cart=cart)))
    private val shipping=ShippingAddress("1 Test Street",city="Boston",state="MA",postalCode="01234",countryCode="US")
    @After fun close() = payments.close()
    private suspend fun checkout(value:String="200.00",success:()->Unit={},error:(String)->Unit={throw AssertionError(it)},id:String="checkout1",items:List<CartItem> = cart) =
        payments.api.beginCheckout(Amount("USD",value),"Ada Lovelace",shipping,success,error,checkoutId=id,cart=items)
    private fun rejected(value:String) = runBlocking { var error:String?=null;checkout(value,error={error=it});assertNotNull(error);assertTrue(payments.openedUrls.isEmpty());assertTrue(payments.requests.isEmpty()) }
    @Test fun rejects_zeroAmount() = rejected("0")
    @Test fun rejects_negativeAmount() = rejected("-1")
    @Test fun rejects_nanAmount() = rejected("NaN")
    @Test fun rejects_infinityAmount() = rejected("Infinity")
    @Test fun rejects_emptyAmount() = rejected("")
    @Test fun rejects_notNumericAmount() = rejected("abc")
    @Test fun canonicalMoneyPrecision() = runBlocking { checkout("0.30000000000000004");val data=Json.parseToJsonElement(payments.requests.single().body.toByteArray().decodeToString()).jsonObject["data"]!!.jsonObject;assertEquals("0.30",data["expectedAmount"]!!.jsonPrimitive.content) }
    @Test fun requestContainsFirebaseBearerAndNoMerchantAuthentication() = runBlocking { checkout();assertEquals("Bearer test-firebase-id-token",payments.requests.single().headers["Authorization"]);assertEquals("/createPaypalCheckout",payments.requests.single().url.encodedPath);assertTrue(payments.requests.none { it.url.host.contains("paypal.com") }) }
    @Test fun shippingRetainsLeadingZeroAndRealState() = runBlocking { checkout();val data=Json.parseToJsonElement(payments.requests.single().body.toByteArray().decodeToString()).jsonObject["data"]!!.jsonObject["shipping"]!!.jsonObject;assertEquals("01234",data["postalCode"]!!.jsonPrimitive.content);assertEquals("MA",data["state"]!!.jsonPrimitive.content) }
    @Test fun emptyCartCannotStartCheckout() = runBlocking { var error:String?=null;checkout(error={error=it},items=emptyList());assertNotNull(error);assertTrue(payments.requests.isEmpty()) }
    @Test fun missingReferenceCannotStartCheckout() = runBlocking { var error:String?=null;checkout(error={error=it},id="");assertNotNull(error);assertTrue(payments.requests.isEmpty()) }
    @Test fun unsupportedCurrencyCannotStartCheckout() = runBlocking { var error:String?=null;payments.api.beginCheckout(Amount("EUR","20.00"),"Test",shipping,{}, {error=it},"id",cart);assertNotNull(error);assertTrue(payments.requests.isEmpty()) }
    @Test fun temporarySessionFailureCanRecoverWithoutReopeningScreen() = runBlocking { payments.authStatus=HttpStatusCode.Unauthorized;var error:String?=null;checkout(error={error=it});assertNotNull(error);payments.authStatus=HttpStatusCode.OK;checkout();assertEquals(1,payments.openedUrls.size) }
    @Test fun payingBeforeInitialPreparationStillPreparesSession() = runBlocking { checkout();assertEquals("ready",payments.api.accessToken.value);assertEquals(1,payments.openedUrls.size) }
    @Test fun rejects_badRequestFromPaymentServer() = runBlocking { payments.checkoutStatus=HttpStatusCode.BadRequest;payments.checkoutBody="{}";var error:String?=null;checkout(error={error=it});assertNotNull(error);assertTrue(payments.openedUrls.isEmpty()) }
    @Test fun rejects_unauthorizedFromPaymentServer() = runBlocking { payments.checkoutStatus=HttpStatusCode.Unauthorized;payments.checkoutBody="{}";var error:String?=null;checkout(error={error=it});assertNotNull(error);assertTrue(payments.openedUrls.isEmpty()) }
    @Test fun rejects_forbiddenFromPaymentServer() = runBlocking { payments.checkoutStatus=HttpStatusCode.Forbidden;payments.checkoutBody="{}";var error:String?=null;checkout(error={error=it});assertNotNull(error);assertTrue(payments.openedUrls.isEmpty()) }
    @Test fun rejects_serverFailureFromPaymentServer() = runBlocking { payments.checkoutStatus=HttpStatusCode.InternalServerError;payments.checkoutBody="{}";var error:String?=null;checkout(error={error=it});assertNotNull(error);assertTrue(payments.openedUrls.isEmpty()) }
    @Test fun rejects_unavailableFromPaymentServer() = runBlocking { payments.checkoutStatus=HttpStatusCode.ServiceUnavailable;payments.checkoutBody="{}";var error:String?=null;checkout(error={error=it});assertNotNull(error);assertTrue(payments.openedUrls.isEmpty()) }
    @Test fun rejects_emptyResponse() = runBlocking { payments.checkoutBody="""{}""";var error:String?=null;checkout(error={error=it});assertNotNull(error);assertTrue(payments.openedUrls.isEmpty()) }
    @Test fun rejects_invalidJson() = runBlocking { payments.checkoutBody="""bad-json""";var error:String?=null;checkout(error={error=it});assertNotNull(error);assertTrue(payments.openedUrls.isEmpty()) }
    @Test fun rejects_missingOrderId() = runBlocking { payments.checkoutBody="""{"result":{"orderId":"","approvalUrl":"https://www.paypal.com/checkout","amount":"200.00"}}""";var error:String?=null;checkout(error={error=it});assertNotNull(error);assertTrue(payments.openedUrls.isEmpty()) }
    @Test fun rejects_wrongAmount() = runBlocking { payments.checkoutBody="""{"result":{"orderId":"x","approvalUrl":"https://www.paypal.com/checkout","amount":"1.00"}}""";var error:String?=null;checkout(error={error=it});assertNotNull(error);assertTrue(payments.openedUrls.isEmpty()) }
    @Test fun rejects_untrustedApprovalUrl() = runBlocking { payments.checkoutBody="""{"result":{"orderId":"x","approvalUrl":"https://attacker.invalid/checkout","amount":"200.00"}}""";var error:String?=null;checkout(error={error=it});assertNotNull(error);assertTrue(payments.openedUrls.isEmpty()) }
    @Test fun rejects_httpApprovalUrl() = runBlocking { payments.checkoutBody="""{"result":{"orderId":"x","approvalUrl":"http://www.paypal.com/checkout","amount":"200.00"}}""";var error:String?=null;checkout(error={error=it});assertNotNull(error);assertTrue(payments.openedUrls.isEmpty()) }
    @Test fun forgedCallbackRejectedByServer() = runBlocking { try {payments.api.confirmPayment("forged");fail("Expected rejection")}catch(e:IllegalStateException){assertTrue(payments.store.createdOrders.isEmpty())} }
    @Test fun blankCallbackCannotReachServer() = runBlocking { try {payments.api.confirmPayment("");fail("Expected rejection")}catch(e:IllegalArgumentException){assertTrue(payments.requests.isEmpty())} }
    @Test fun verifiedCallbackStoresOneOrderOnReplay() = runBlocking { checkout();val a=payments.api.confirmPayment("approved-order");val b=payments.api.confirmPayment("approved-order");assertEquals(a.id,b.id);assertEquals(1,payments.store.createdOrders.size) }
    @Test fun callbackUsesFrozenCheckoutRatherThanLiveCart() = runBlocking { checkout();payments.store.emitCustomer(payments.store.customer().copy(cart=listOf(CartItem("later",TestFixtures.DIY_ID,quantity=1))));val order=payments.api.confirmPayment("approved-order");assertEquals(cart,order.items);assertEquals(200.0,order.totalAmount,0.0);assertEquals("later",payments.store.customer().cart.single().id) }
    @Test fun captureFailureNeverClaimsSuccess() = runBlocking { payments.stagePayment();payments.completeStatus=HttpStatusCode.Forbidden;payments.completeBody="{}";try {payments.api.confirmPayment("approved-order");fail("Expected rejection")}catch(e:IllegalStateException){assertTrue(payments.store.createdOrders.isEmpty())} }
    @Test fun cancelledTransportPropagatesCancellation() = runBlocking { payments.failure=kotlinx.coroutines.CancellationException("cancelled");try {checkout();fail("Expected cancellation")}catch(e:kotlinx.coroutines.CancellationException){assertTrue(payments.openedUrls.isEmpty())} }
    @Test fun networkFailureCanRetrySameCheckout() = runBlocking { payments.failure=java.io.IOException("offline");var error:String?=null;checkout(error={error=it});assertNotNull(error);payments.failure=null;checkout();assertEquals(1,payments.openedUrls.size) }
}
