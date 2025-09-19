package com.Trendy.data.domain

import com.trendy.shared.domain.CartItem
import com.trendy.shared.domain.Customer
import com.trendy.shared.util.RequestState
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest

class CustomerRepositoryImpl: CustomerRepository {
    override suspend fun createCustomer(
        user: FirebaseUser?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        try {

            if (user != null){
                val customerCollection = Firebase.firestore.collection(collectionPath = "customer")
                val customer = Customer(
                    id = user.uid,
                    firstName = user.displayName?.split(" ")?.firstOrNull() ?:"Unknown",
                    lastName = user.displayName?.split(" ")?.lastOrNull() ?:"Unknown",
                    email = user.email ?: "Unknown",
                )

                val customerExists = customerCollection.document(user.uid).get().exists

                if (customerExists){
                    onSuccess()
                }else{
                    customerCollection.document(user.uid).set(customer)
                    customerCollection.document(user.uid)
                        .collection("PrivateData")
                        .document("role")
                        .set(mapOf("isAdmin" to false))
                    onSuccess()
                }

            }else{
                onError("User is not available")
            }

        }catch (e: Exception){
            onError("Error while creating a Customer: ${e.message}")
        }
    }

    override fun getCurrentUserId(): String? {
        return Firebase.auth.currentUser?.uid
    }

    override suspend fun signOut(): RequestState<Unit> {
        return try {
            Firebase.auth.signOut()
            RequestState.Success(data = Unit)
        }catch (e: Exception){
            RequestState.Error("Error while signing out ${e.message}")
        }
    }

    override fun readCustomerFlow(): Flow<RequestState<Customer>> = channelFlow {
        try {
            val userId = getCurrentUserId()
            if (userId != null){
                val database = Firebase.firestore
                    database.collection(collectionPath = "customer")
                    .document(userId)
                    .snapshots
                    .collectLatest { document ->
                        if (document.exists){
                            val privateData = database.collection(collectionPath = "customer")
                                .document(userId)
                                .collection("PrivateData")
                                .document("role")
                                .get()
                            val customer = Customer(
                                id = document.id,
                                firstName = document.get("firstName"),
                                lastName = document.get("lastName"),
                                email = document.get("email"),
                                city = document.get("city"),
                                postalCode = document.get("postalCode"),
                                address = document.get("address"),
                                phoneNumber = document.get("phoneNumber"),
                                cart = document.get("cart"),
                                isAdmin = privateData.get("isAdmin")
                            )
                            send(RequestState.Success(data = customer))
                        }else{
                            send(RequestState.Error("Queried customer does not exist."))
                        }
                    }
            }else{
                send(RequestState.Error("User is not available"))
            }

        } catch (e: Exception) {
            send(RequestState.Error("Error while reading Customer: ${e.message}"))
        }
    }

    override suspend fun updateCustomer(
        customer: Customer,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val userId = getCurrentUserId()
            if (userId != null){
                val customerCollection = Firebase.firestore.collection(collectionPath = "customer")
                val customerExists = customerCollection.document(customer.id).get()
                if (customerExists.exists){
                    customerCollection.document(userId)
                        .update(
                            "firstName" to customer.firstName,
                            "lastName" to customer.lastName,
                            "city" to customer.city,
                            "postalCode" to customer.postalCode,
                            "address" to customer.address,
                            "phoneNumber" to customer.phoneNumber
                        )
                    onSuccess()
                }else{
                    onError("Customer does not exist")
                }
            }else{
                onError("User is not available")
            }

        }catch (e: Exception){
            onError("Error while updating Customer: ${e.message}")
        }
    }

    override suspend fun addItemToCard(
        cartItem: CartItem,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val currentUserId = getCurrentUserId()
            if (currentUserId != null) {
                val database = Firebase.firestore
                val customerCollection = database.collection(collectionPath = "customer")

                val existingCustomer = customerCollection
                    .document(currentUserId)
                    .get()
                if (existingCustomer.exists) {
                    val existingCart = existingCustomer.get<List<CartItem>>("cart")
                    val updatedCart = existingCart + cartItem
                    customerCollection.document(currentUserId)
                        .set(
                            data = mapOf("cart" to updatedCart),
                            merge = true
                        )
                    onSuccess()
                } else {
                    onError("Select customer does not exist.")
                }
            } else {
                onError("User is not available.")
            }
        } catch (e: Exception) {
            onError("Error while adding a product to cart: ${e.message}")
        }
    }

    override suspend fun updateCartItemQuantity(
        id: String,
        quantity: Int,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val currentUserId = getCurrentUserId()
            if (currentUserId != null) {
                val database = Firebase.firestore
                val customerCollection = database.collection(collectionPath = "customer")

                val existingCustomer = customerCollection
                    .document(currentUserId)
                    .get()
                if (existingCustomer.exists) {
                    val existingCart = existingCustomer.get<List<CartItem>>("cart")
                    val updatedCart = existingCart.map { cartItem ->
                        if (cartItem.id == id) {
                            cartItem.copy(quantity = quantity)
                        } else cartItem
                    }
                    customerCollection.document(currentUserId)
                        .update(data = mapOf("cart" to updatedCart))
                    onSuccess()
                } else {
                    onError("Select customer does not exist.")
                }
            } else {
                onError("User is not available.")
            }
        } catch (e: Exception) {
            onError("Error while updating a product to cart: ${e.message}")
        }
    }

    override suspend fun deleteCartItem(
        id: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val currentUserId = getCurrentUserId()
            if (currentUserId != null) {
                val database = Firebase.firestore
                val customerCollection = database.collection(collectionPath = "customer")

                val existingCustomer = customerCollection
                    .document(currentUserId)
                    .get()
                if (existingCustomer.exists) {
                    val existingCart = existingCustomer.get<List<CartItem>>("cart")
                    val updatedCart = existingCart.filterNot { it.id == id }
                    customerCollection.document(currentUserId)
                        .update(data = mapOf("cart" to updatedCart))
                    onSuccess()
                } else {
                    onError("Select customer does not exist.")
                }
            } else {
                onError("User is not available.")
            }
        } catch (e: Exception) {
            onError("Error while deleting a product from cart: ${e.message}")
        }
    }

    override suspend fun deleteAllCartItems(onSuccess: () -> Unit, onError: (String) -> Unit) {
        try {
            val currentUserId = getCurrentUserId()
            if (currentUserId != null) {
                val database = Firebase.firestore
                val customerCollection = database.collection(collectionPath = "customer")

                val existingCustomer = customerCollection
                    .document(currentUserId)
                    .get()
                if (existingCustomer.exists) {
                    customerCollection.document(currentUserId)
                        .update(data = mapOf("cart" to emptyList<List<CartItem>>()))
                    onSuccess()
                } else {
                    onError("Select customer does not exist.")
                }
            } else {
                onError("User is not available.")
            }
        } catch (e: Exception) {
            onError("Error while deleting all products from cart: ${e.message}")
        }
    }
}