package org.roy.ecommerce_project.testing

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.Trendy.admin_panel.AdminPanelViewModel
import com.Trendy.auth.AuthViewModel
import com.Trendy.cart.CartViewModel
import com.Trendy.category_search.CategorySearchViewModel
import com.Trendy.checkout.CheckoutViewModel
import com.Trendy.checkout.domain.PaypalApi
import com.Trendy.data.domain.AdminRepository
import com.Trendy.data.domain.CustomerRepository
import com.Trendy.data.domain.OrderRepository
import com.Trendy.data.domain.ProductRepository
import com.Trendy.home.HomeGraphViewModel
import com.Trendy.manage_product.ImagePicker
import com.Trendy.manage_product.ManageProductViewModel
import com.Trendy.payment_completed.PaymentViewModel
import com.Trendy.products_overview.ProductsOverviewViewModel
import com.Trendy.profile.ProfileViewModel
import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.MapSettings
import com.russhwolf.settings.observable.makeObservable
import com.trendy.details.DetailsViewModel
import com.trendy.shared.util.IntentHandler
import com.trendy.shared.util.PreferencesRepository
import dev.gitlive.firebase.storage.File
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import org.roy.ecommerce_project.testing.fixtures.TestRepositories
import org.roy.ecommerce_project.testing.fixtures.TestStore

/** Strictly in-process HTTP transport. Any unexpected endpoint fails instead of using a network. */
class TestPayments(val store: TestStore = TestStore()) {
    val requests = mutableListOf<HttpRequestData>()
    var authStatus = HttpStatusCode.OK
    var authBody = ""
    var checkoutStatus = HttpStatusCode.OK
    var checkoutBody: String? = null
    var completeStatus = HttpStatusCode.OK
    var completeBody: String? = null
    var failure: Exception? = null
    val openedUrls = mutableListOf<String>()
    var onBrowserOpened: (String) -> Unit = {}
    val pending = mutableMapOf<String, com.trendy.shared.domain.Order>()
    private val json = Json { ignoreUnknownKeys=true; encodeDefaults=true }
    fun stagePayment(token: String = "approved-order", amount: Double? = null) {
        val customer=store.customer()
        val products=store.productsState.value.getSuccessData().associateBy { it.id }
        pending[token]=com.trendy.shared.domain.Order(id="paypal_$token", customerId=customer.id, items=customer.cart.toList(),
            totalAmount=amount ?: customer.cart.sumOf { (products[it.productId]?.price ?: 0.0)*it.quantity },token=token)
    }
    val client = HttpClient(MockEngine { request ->
        failure?.let { throw it }
        requests += request
        val data=json.parseToJsonElement(request.body.toByteArray().decodeToString()).jsonObject.getValue("data").jsonObject
        when (request.url.encodedPath) {
            "/createPaypalCheckout" -> {
                val checkout=json.decodeFromString<com.Trendy.checkout.domain.CheckoutRequest>(data.toString())
                if (checkoutStatus.value in 200..299 && checkoutBody == null) {
                    pending["approved-order"]=com.trendy.shared.domain.Order(id="paypal_approved-order",customerId=store.customer().id,
                        items=checkout.cart.toList(),totalAmount=checkout.expectedAmount.toDouble(),token="approved-order",shipping=checkout.shipping)
                }
                respond(checkoutBody ?: "{\"result\":{\"orderId\":\"approved-order\",\"approvalUrl\":\"https://www.sandbox.paypal.com/checkoutnow?token=approved-order\",\"amount\":\"${checkout.expectedAmount}\"}}",
                    checkoutStatus, headersOf("Content-Type", "application/json"))
            }
            "/completePaypalCheckout" -> {
                val token=data.getValue("orderId").jsonPrimitive.content
                val order=pending[token]
                val failureMessage=store.consumeFailure(org.roy.ecommerce_project.testing.fixtures.TestOperation.CreateOrder)
                when {
                    completeBody != null -> respond(completeBody!!, completeStatus, headersOf("Content-Type", "application/json"))
                    failureMessage != null -> respond("{\"error\":{\"message\":${json.encodeToString(failureMessage)}}}", HttpStatusCode.ServiceUnavailable, headersOf("Content-Type", "application/json"))
                    order == null -> respond("{\"error\":{\"message\":\"Unknown payment reference\"}}", HttpStatusCode.Forbidden, headersOf("Content-Type", "application/json"))
                    else -> {
                        if (store.createdOrders.none { it.id == order.id }) {
                            store.createdOrders += order
                            store.emitCustomer(store.customer().copy(cart=store.customer().cart.filterNot { line -> order.items.any { it.id==line.id } }))
                        }
                        respond("{\"result\":${json.encodeToString(order)}}", completeStatus, headersOf("Content-Type", "application/json"))
                    }
                }
            }
            else -> throw AssertionError("Unexpected test endpoint: ${request.url.encodedPath}")
        }
    }) { install(ContentNegotiation) { json(json) } }
    val api = PaypalApi(
        service=com.Trendy.checkout.domain.FirebasePaymentService(client) {
            check(authStatus == HttpStatusCode.OK && store.currentUserId != null) { "Payment session unavailable: $authStatus" }
            com.Trendy.checkout.domain.PaymentSession("test-firebase-id-token", "https://payments.invalid")
        },
        openBrowser={ url -> openedUrls += url; onBrowserOpened(url) },
    )
    fun close() = client.close()
}

class TestImagePicker : ImagePicker {
    var selection: File? = File(Uri.parse("android.resource://org.roy.ecommerce_project/drawable/trendy_mark"))
    private var request by mutableIntStateOf(0)
    @Composable
    override fun InitializePhotoPicker(onImageSelect: (File?) -> Unit) {
        LaunchedEffect(request) { if (request > 0) onImageSelect(selection) }
    }
    override fun open() { request++ }
}

@OptIn(ExperimentalSettingsApi::class)
class TestEnvironment(val store: TestStore = TestStore()) {
    val repositories = TestRepositories(store)
    val payments = TestPayments(store)
    val picker = TestImagePicker()
    val preferences = PreferencesRepository(MapSettings().makeObservable())
    fun start() {
        stopKoin()
        startKoin {
            modules(module {
                single<CustomerRepository> { repositories.customer }
                single<ProductRepository> { repositories.product }
                single<AdminRepository> { repositories.admin }
                single<OrderRepository> { repositories.order }
                single<PaypalApi> { payments.api }
                single<ImagePicker> { picker }
                single<PreferencesRepository> { preferences }
                single<IntentHandler> { IntentHandler() }
                viewModelOf(::AuthViewModel)
                viewModelOf(::HomeGraphViewModel)
                viewModelOf(::ProductsOverviewViewModel)
                viewModelOf(::CategorySearchViewModel)
                viewModelOf(::DetailsViewModel)
                viewModelOf(::CartViewModel)
                viewModelOf(::ProfileViewModel)
                viewModelOf(::AdminPanelViewModel)
                viewModelOf(::ManageProductViewModel)
                viewModelOf(::CheckoutViewModel)
                viewModelOf(::PaymentViewModel)
            })
        }
    }
    fun close() { stopKoin(); payments.close() }
}
