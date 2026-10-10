package com.Trendy.data

import com.Trendy.data.domain.ProductRepository
import com.trendy.shared.domain.Product
import com.trendy.shared.domain.ProductCategory
import com.trendy.shared.util.RequestState
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest

class ProductRepositoryImpl : ProductRepository {
    override fun getCurrentUserId() = Firebase.auth.currentUser?.uid

    override fun readDiscountedProducts(): Flow<RequestState<List<Product>>> = channelFlow {
        try {
            val userId = getCurrentUserId()
            if (userId != null) {
                val database = Firebase.firestore
                database.collection(collectionPath = "product")
                    .where { "isDiscounted" equalTo true }
                    .snapshots
                    .collectLatest { query ->
                        val products = query.documents.map { document ->
                            Product(
                                id = document.id,
                                title = document.get(field = "title"),
                                createdAt = document.get(field = "createdAt"),
                                description = document.get(field = "description"),
                                thumbnail = document.get(field = "thumbnail"),
                                category = document.get(field = "category"),
                                weight = document.get(field = "weight"),
                                price = document.get(field = "price"),
                                isPopular = document.get(field = "isPopular"),
                                isDiscounted = document.get(field = "isDiscounted"),
                                isNew = document.get(field = "isNew")
                            )
                        }
                        send(RequestState.Success(data = products.map { it.copy(title = it.title.uppercase()) }))
                    }
            } else {
                send(RequestState.Error("User is not available."))
            }
        } catch (e: Exception) {
            send(RequestState.Error("Error while reading the last 10 items from the database: ${e.message}"))
        }
    }

    override fun readNewProducts(): Flow<RequestState<List<Product>>> = channelFlow {
        try {
            val userId = getCurrentUserId()
            if (userId != null) {
                val database = Firebase.firestore
                database.collection(collectionPath = "product")
                    .where { "isNew" equalTo true }
                    .snapshots
                    .collectLatest { query ->
                        val products = query.documents.map { document ->
                            Product(
                                id = document.id,
                                title = document.get(field = "title"),
                                createdAt = document.get(field = "createdAt"),
                                description = document.get(field = "description"),
                                thumbnail = document.get(field = "thumbnail"),
                                category = document.get(field = "category"),
                                weight = document.get(field = "weight"),
                                price = document.get(field = "price"),
                                isPopular = document.get(field = "isPopular"),
                                isDiscounted = document.get(field = "isDiscounted"),
                                isNew = document.get(field = "isNew")
                            )
                        }
                        send(RequestState.Success(data = products.map { it.copy(title = it.title.uppercase()) }))
                    }
            } else {
                send(RequestState.Error("User is not available."))
            }
        } catch (e: Exception) {
            send(RequestState.Error("Error while reading the last 10 items from the database: ${e.message}"))
        }
    }

    override fun readProductByIdFlow(id: String): Flow<RequestState<Product>> = channelFlow {
        try {
            val userId = getCurrentUserId()
            if (userId != null) {
                val database = Firebase.firestore
                database.collection(collectionPath = "product")
                    .document(id)
                    .snapshots
                    .collectLatest { document ->
                        if (document.exists) {
                            val product = Product(
                                id = document.id,
                                title = document.get(field = "title"),
                                createdAt = document.get(field = "createdAt"),
                                description = document.get(field = "description"),
                                thumbnail = document.get(field = "thumbnail"),
                                category = document.get(field = "category"),
                                weight = document.get(field = "weight"),
                                price = document.get(field = "price"),
                                isPopular = document.get(field = "isPopular"),
                                isDiscounted = document.get(field = "isDiscounted"),
                                isNew = document.get(field = "isNew")
                            )
                            send(RequestState.Success(product.copy(title = product.title.uppercase())))
                        } else {
                            send(RequestState.Error("Selected product does not exist."))
                        }
                    }
            } else {
                send(RequestState.Error("User is not available."))
            }
        } catch (e: Exception) {
            send(RequestState.Error("Error while reading a selected product: ${e.message}"))
        }
    }

    override fun readProductsByIdsFlow(ids: List<String>): Flow<RequestState<List<Product>>> =
        productChunksFlow(ids) { chunk ->
            Firebase.firestore.collection("product").where { "id" inArray chunk }.snapshots.map { query ->
                query.documents.map { document -> document.data<Product>().copy(id=document.id, title=document.get<String>("title").uppercase()) }
            }
        }.map<List<Product>, RequestState<List<Product>>> { RequestState.Success(it) }
            .catch { emit(RequestState.Error("Error while reading products: " + it.message)) }

    override fun readProductsByCategoryFlow(category: ProductCategory): Flow<RequestState<List<Product>>> =
        channelFlow {
            try {
                val userId = getCurrentUserId()
                if (userId != null) {
                    val database = Firebase.firestore
                    database.collection(collectionPath = "product")
                        .where { "category" equalTo category.name }
                        .snapshots
                        .collectLatest { query ->
                            val products = query.documents.map { document ->
                                Product(
                                    id = document.id,
                                    title = document.get(field = "title"),
                                    createdAt = document.get(field = "createdAt"),
                                    description = document.get(field = "description"),
                                    thumbnail = document.get(field = "thumbnail"),
                                    category = document.get(field = "category"),
                                    weight = document.get(field = "weight"),
                                    price = document.get(field = "price"),
                                    isPopular = document.get(field = "isPopular"),
                                    isDiscounted = document.get(field = "isDiscounted"),
                                    isNew = document.get(field = "isNew")
                                )
                            }
                            send(RequestState.Success(products.map { it.copy(title = it.title.uppercase()) }))
                        }
                } else {
                    send(RequestState.Error("User is not available."))
                }
            } catch (e: Exception) {
                send(RequestState.Error("Error while reading a selected product: ${e.message}"))
            }
        }
}