package org.roy.ecommerce_project.testing

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.Trendy.checkout.CheckoutViewModel
import com.Trendy.payment_completed.PaymentViewModel
import com.Trendy.data.domain.OrderRepository
import com.trendy.shared.domain.*
import com.trendy.shared.util.RequestState
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancel
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.roy.ecommerce_project.testing.fixtures.*

@RunWith(AndroidJUnit4::class)
class PaymentViewModelIntegrationTest {
    private val cart=listOf(CartItem("a",TestFixtures.GADGET_NEW_ID,quantity=2),CartItem("b",TestFixtures.GADGET_SALE_ID,quantity=1))
    private val env=TestEnvironment(TestStore(customer=TestFixtures.customer(cart=cart)))
    private val models=mutableListOf<ViewModel>()
    @After fun close(){ models.forEach { it.viewModelScope.cancel() };env.close() }
    private fun checkout(orderRepository:OrderRepository=env.repositories.order,handle:SavedStateHandle=SavedStateHandle(mapOf("totalAmount" to "250.0"))):CheckoutViewModel =
        CheckoutViewModel(env.repositories.customer,orderRepository,handle,env.payments.api,env.repositories.product).also { models+=it;eventually {it.screenReady.isSuccess()} }
    private fun payment(success:Boolean?=true,token:String?="approved-order",error:String?=null):PaymentViewModel {
        val values=mutableMapOf<String,Any?>();success?.let {values["isSuccess"]=it};token?.let {values["token"]=it};error?.let {values["error"]=it}
        return PaymentViewModel(SavedStateHandle(values),env.repositories.customer,env.repositories.order,env.repositories.product,env.payments.api).also {models+=it}
    }
    @Test fun deliveryPersistsEditedProfileAndShippingSnapshot(){val vm=checkout();vm.updateAddress("20 Changed Street");var done=false;vm.payOnDelivery({done=true},{fail(it)});eventually {done};assertEquals("20 Changed Street",env.store.customer().address);assertEquals("20 Changed Street",env.store.createdOrders.single().shipping?.addressLine1)}
    @Test fun deliveryUsesCurrentPricesInsteadOfOldNavigationTotal(){env.store.emitProducts(TestFixtures.products().map {if(it.id==TestFixtures.GADGET_NEW_ID)it.copy(price=150.0)else it});val vm=checkout();var done=false;vm.payOnDelivery({done=true},{fail(it)});eventually {done};assertEquals(350.0,env.store.createdOrders.single().totalAmount,0.0)}
    @Test fun deliveryRoundsEachUnitPriceToCents(){env.store.emitProducts(TestFixtures.products().map {it.copy(price=0.1)});val vm=checkout();var done=false;vm.payOnDelivery({done=true},{fail(it)});eventually {done};assertEquals(0.3,env.store.createdOrders.single().totalAmount,0.0)}
    @Test fun missingCatalogueProductBlocksCheckout(){env.store.emitProducts(TestFixtures.products().filterNot {it.id==TestFixtures.GADGET_NEW_ID});val vm=checkout();var error:String?=null;vm.payOnDelivery({fail("Unexpected success")},{error=it});eventually {error!=null};assertTrue(env.store.createdOrders.isEmpty());assertTrue(error!!.contains("unavailable"))}
    @Test fun deliveryFailureRetainsCartAndAllowsRetry(){val vm=checkout();env.store.failNext(TestOperation.CreateOrder,"offline");var error:String?=null;vm.payOnDelivery({fail("Unexpected success")},{error=it});eventually {error!=null};assertFalse(vm.isSubmitting);assertEquals(cart,env.store.customer().cart);var done=false;vm.payOnDelivery({done=true},{fail(it)});eventually {done};assertEquals(1,env.store.createdOrders.size)}
    @Test fun profileFailureDoesNotCreateOrder(){env.store.failNext(TestOperation.UpdateCustomer,"offline");val vm=checkout();var error:String?=null;vm.payOnDelivery({fail("Unexpected success")},{error=it});eventually {error!=null};assertTrue(env.store.createdOrders.isEmpty());assertFalse(vm.isSubmitting)}
    @Test fun doubleTapStartsOnlyOneSubmission(){val gate=CompletableDeferred<Unit>();var calls=0;val delayed=object:OrderRepository by env.repositories.order {override suspend fun createTheOrder(order:Order,onSuccess:()->Unit,onError:(String)->Unit){calls++;gate.await();env.repositories.order.createTheOrder(order,onSuccess,onError)}};val vm=checkout(delayed);var done=false;vm.payOnDelivery({done=true},{fail(it)});vm.payOnDelivery({fail("duplicate")},{fail(it)});eventually {calls==1};assertTrue(vm.isSubmitting);gate.complete(Unit);eventually {done};assertEquals(1,env.store.createdOrders.size)}
    @Test fun failedOrderRetryReusesPersistentCheckoutId(){val handle=SavedStateHandle(mapOf("totalAmount" to "250"));val ids=mutableListOf<String>();val repo=object:OrderRepository by env.repositories.order {override suspend fun createTheOrder(order:Order,onSuccess:()->Unit,onError:(String)->Unit){ids+=order.id;onError("offline")}};val first=checkout(repo,handle);first.payOnDelivery({fail("success")},{});eventually {ids.size==1&&!first.isSubmitting};first.viewModelScope.cancel();val recreated=checkout(repo,handle);recreated.payOnDelivery({fail("success")},{});eventually {ids.size==2};assertEquals(ids[0],ids[1])}
    @Test fun changedCartCreatesNewSnapshot(){val handle=SavedStateHandle(mapOf("totalAmount" to "250"));val ids=mutableListOf<String>();val repo=object:OrderRepository by env.repositories.order {override suspend fun createTheOrder(order:Order,onSuccess:()->Unit,onError:(String)->Unit){ids+=order.id;onError("offline")}};val vm=checkout(repo,handle);vm.payOnDelivery({fail("success")},{});eventually {ids.size==1&&!vm.isSubmitting};env.store.emitCustomer(env.store.customer().copy(cart=cart.take(1)));eventually {vm.screenState.cart.size==1};vm.payOnDelivery({fail("success")},{});eventually {ids.size==2};assertNotEquals(ids[0],ids[1])}
    @Test fun postalCodeLeadingZeroSurvivesSaveAndShipping(){val vm=checkout();vm.updateCountry(Country.Usa);vm.updateShippingState("MA");vm.updatePostalCodeText("01234");var done=false;vm.payOnDelivery({done=true},{fail(it)});eventually {done};assertEquals("01234",env.store.customer().postalCodeText);assertEquals("01234",env.store.createdOrders.single().shipping?.postalCode)}
    @Test fun stateMustBeSubdivisionNotCountry(){val vm=checkout();vm.updateCountry(Country.Usa);vm.updateShippingState("Usa");assertFalse(vm.isFormValid);vm.updateShippingState("CA");assertTrue(vm.isFormValid)}
    @Test fun indiaNeedsState(){val vm=checkout();vm.updateCountry(Country.India);vm.updatePostalCodeText("110001");assertFalse(vm.isFormValid);vm.updateShippingState("Delhi");assertTrue(vm.isFormValid)}
    @Test fun changingCountryClearsPreviousState(){val vm=checkout();vm.updateCountry(Country.Usa);vm.updateShippingState("CA");vm.updateCountry(Country.India);assertEquals("",vm.screenState.shippingState)}
    @Test fun paypalPersistsEditedAddressBeforeOpeningBrowser(){val vm=checkout();vm.updateAddress("10 New Street");var opened=false;vm.payWithPayPal({opened=true},{fail(it)});eventually {opened};assertEquals("10 New Street",env.store.customer().address);assertEquals("10 New Street",env.payments.pending.getValue("approved-order").shipping?.addressLine1)}
    @Test fun temporaryPaypalPreparationFailureRecovers(){env.payments.authStatus=HttpStatusCode.Unauthorized;val vm=checkout();var error:String?=null;vm.payWithPayPal({fail("success")},{error=it});eventually {error!=null};env.payments.authStatus=HttpStatusCode.OK;var done=false;vm.payWithPayPal({done=true},{fail(it)});eventually {done};assertEquals(1,env.payments.openedUrls.size)}
    @Test fun paypalServerFailureAllowsRetry(){env.payments.checkoutStatus=HttpStatusCode.ServiceUnavailable;val vm=checkout();var error:String?=null;vm.payWithPayPal({fail("success")},{error=it});eventually {error!=null};assertFalse(vm.isSubmitting);env.payments.checkoutStatus=HttpStatusCode.OK;var done=false;vm.payWithPayPal({done=true},{fail(it)});eventually {done}}
    @Test fun confirmedPaymentUsesServerOrder(){env.payments.stagePayment();val vm=payment();eventually {vm.screenState.isSuccess()};assertEquals(cart,env.store.createdOrders.single().items);assertEquals(250.0,env.store.createdOrders.single().totalAmount,0.0)}
    @Test fun forgedCallbackCannotCreateLocalOrder(){val vm=payment(token="forged");eventually {vm.screenState.isError()};assertTrue(env.store.createdOrders.isEmpty());assertEquals(cart,env.store.customer().cart)}
    @Test fun callbackReplayUsesSameServerOrder(){env.payments.stagePayment();val first=payment();eventually {first.screenState.isSuccess()};val second=payment();eventually {second.screenState.isSuccess()};assertEquals(1,env.store.createdOrders.size)}
    @Test fun livePriceChangeAfterApprovalDoesNotChangePaidOrder(){env.payments.stagePayment();env.store.emitProducts(TestFixtures.products().map {it.copy(price=999.0)});val vm=payment();eventually {vm.screenState.isSuccess()};assertEquals(250.0,env.store.createdOrders.single().totalAmount,0.0)}
    @Test fun newCartItemsAfterApprovalRemainInCart(){env.payments.stagePayment();val added=CartItem("new",TestFixtures.DIY_ID,quantity=1);env.store.emitCustomer(env.store.customer().copy(cart=cart+added));val vm=payment();eventually {vm.screenState.isSuccess()};assertEquals(cart,env.store.createdOrders.single().items);assertEquals(listOf(added),env.store.customer().cart)}
    @Test fun rejectedServerCaptureCannotShowSuccess(){env.payments.stagePayment();env.payments.completeStatus=HttpStatusCode.Forbidden;env.payments.completeBody="{}";val vm=payment();eventually {vm.screenState.isError()};assertTrue(env.store.createdOrders.isEmpty())}
    @Test fun failedPersistenceKeepsCartAndRetryCanConfirm(){env.payments.stagePayment();env.store.failNext(TestOperation.CreateOrder,"commit failed");val first=payment();eventually {first.screenState.isError()};assertEquals(cart,env.store.customer().cart);first.retryConfirmation();eventually {first.screenState.isSuccess()};assertEquals(1,env.store.createdOrders.size)}
    @Test fun canceledPaymentNeverCallsCapture(){val vm=payment(false);eventually {vm.screenState.isError()};assertTrue(env.payments.requests.isEmpty());assertTrue(env.store.createdOrders.isEmpty())}
    @Test fun explicitFailureNeverCallsCapture(){val vm=payment(null,null,"Declined");eventually {vm.screenState.isError()};assertEquals("Declined",vm.screenState.getErrorMessage());assertTrue(env.payments.requests.isEmpty())}
    @Test fun missingCallbackStateShowsError(){val vm=payment(null,null);eventually {vm.screenState.isError()};assertTrue(env.store.createdOrders.isEmpty())}
    @Test fun internalDeliverySuccessDoesNotCreateSecondOrder(){val vm=payment(true,null);eventually {vm.screenState.isSuccess()};assertTrue(env.store.createdOrders.isEmpty());assertTrue(env.payments.requests.isEmpty())}
    @Test fun invalidAddressStopsCheckoutBeforeNetwork(){val vm=checkout();vm.updateAddress("");var error:String?=null;vm.payWithPayPal({fail("success")},{error=it});assertNotNull(error);assertTrue(env.payments.requests.isEmpty())}
    @Test fun emptyCartStopsCheckoutBeforeNetwork(){env.store.emitCustomer(env.store.customer().copy(cart=emptyList()));val vm=checkout();var error:String?=null;vm.payWithPayPal({fail("success")},{error=it});assertNotNull(error);assertTrue(env.payments.requests.isEmpty())}
    @Test fun cartUpdateDoesNotOverwriteUserEditedAddress(){val vm=checkout();vm.updateAddress("Edited draft");env.store.emitCustomer(env.store.customer().copy(cart=cart.take(1)));eventually {vm.screenState.cart.size==1};assertEquals("Edited draft",vm.screenState.address)}
    private fun eventually(condition:()->Boolean){val deadline=System.currentTimeMillis()+5000;while(!condition()&&System.currentTimeMillis()<deadline)Thread.sleep(20);assertTrue("Expected state was not reached",condition())}
}
