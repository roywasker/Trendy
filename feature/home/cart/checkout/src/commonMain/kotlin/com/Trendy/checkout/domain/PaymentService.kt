package com.Trendy.checkout.domain

import com.trendy.shared.domain.CartItem
import com.trendy.shared.domain.Order
import com.trendy.shared.domain.ShippingDetails
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.app
import dev.gitlive.firebase.auth.auth
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpHeaders
import kotlinx.serialization.Serializable

@Serializable
data class CheckoutRequest(val checkoutId: String, val expectedAmount: String, val cart: List<CartItem>, val shipping: ShippingDetails)
@Serializable
data class ApprovedCheckout(val orderId: String, val approvalUrl: String, val amount: String)
@Serializable
data class CompleteCheckoutRequest(val orderId: String)
@Serializable
private data class CallableRequest<T>(val data: T)
@Serializable
private data class CallableResponse<T>(val result: T)
@Serializable
private data class CallableError(val error: CallableErrorDetail)
@Serializable
private data class CallableErrorDetail(val message: String)
data class PaymentSession(val token: String, val endpoint: String)

interface PaymentService {
    suspend fun prepare()
    suspend fun createCheckout(request: CheckoutRequest): ApprovedCheckout
    suspend fun completeCheckout(orderId: String): Order
}

/** Firebase callable protocol; merchant credentials and PayPal access tokens never enter the app. */
class FirebasePaymentService(
    private val client: HttpClient = createPaypalHttpClient(),
    private val session: suspend () -> PaymentSession = {
        val user = requireNotNull(Firebase.auth.currentUser) { "Please sign in before paying." }
        val token = requireNotNull(user.getIdToken(false)) { "Sign-in expired. Please sign in again." }
        val project = requireNotNull(Firebase.app.options.projectId) { "Payment service is not configured." }
        PaymentSession(token, "https://us-central1-$project.cloudfunctions.net")
    },
) : PaymentService {
    override suspend fun prepare() { session() }
    override suspend fun createCheckout(request: CheckoutRequest): ApprovedCheckout = call("createPaypalCheckout", request)
    override suspend fun completeCheckout(orderId: String): Order = call("completePaypalCheckout", CompleteCheckoutRequest(orderId))
    private suspend inline fun <reified T, reified R> call(function: String, data: T): R {
        val signedIn = session()
        val response = client.post("${signedIn.endpoint}/$function") {
            headers { append(HttpHeaders.Authorization, "Bearer ${signedIn.token}"); append(HttpHeaders.ContentType, "application/json") }
            setBody(CallableRequest(data))
        }
        if (response.status.value !in 200..299) {
            val message = runCatching { response.body<CallableError>().error.message }.getOrNull()
            error(message ?: "Payment could not be confirmed. Please try again. (${response.status.value})")
        }
        return response.body<CallableResponse<R>>().result
    }
}
