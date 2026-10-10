package com.Trendy.data

import com.trendy.shared.domain.Customer
import com.trendy.shared.domain.Order
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** All reads happen before writes; Firestore retries the complete block on contention. */
interface ShopTransaction {
    suspend fun customer(id: String): Customer?
    suspend fun role(id: String): Boolean?
    suspend fun order(id: String): Order?
    fun putCustomer(customer: Customer)
    fun putRole(id: String, isAdmin: Boolean)
    fun putOrder(order: Order)
}
interface ShopStore {
    suspend fun <T> transaction(block: suspend ShopTransaction.() -> T): T
    fun observeCustomer(id: String): Flow<Customer?>
}
class FirestoreShopStore : ShopStore {
    override suspend fun <T> transaction(block: suspend ShopTransaction.() -> T): T {
        val db = Firebase.firestore
        return db.runTransaction {
            val transaction = this
            block(object : ShopTransaction {
                override suspend fun customer(id: String): Customer? = transaction.get(db.collection("customer").document(id))
                    .let { if (it.exists) it.data<Customer>() else null }
                override suspend fun role(id: String): Boolean? = transaction.get(db.collection("customer").document(id).collection("PrivateData").document("role"))
                    .let { if (it.exists) it.get<Boolean>("isAdmin") else null }
                override suspend fun order(id: String): Order? = transaction.get(db.collection("order").document(id))
                    .let { if (it.exists) it.data<Order>() else null }
                override fun putCustomer(customer: Customer) { transaction.set(db.collection("customer").document(customer.id), customer, merge = true) { encodeDefaults = true } }
                override fun putRole(id: String, isAdmin: Boolean) { transaction.set(db.collection("customer").document(id).collection("PrivateData").document("role"), mapOf("isAdmin" to isAdmin)) }
                override fun putOrder(order: Order) { transaction.set(db.collection("order").document(order.id), order) { encodeDefaults = true } }
            })
        }
    }
    override fun observeCustomer(id: String): Flow<Customer?> {
        val ref = Firebase.firestore.collection("customer").document(id)
        return combine(ref.snapshots, ref.collection("PrivateData").document("role").snapshots) { customer, role ->
            if (customer.exists) customer.data<Customer>().copy(isAdmin = if (role.exists) role.get("isAdmin") else false) else null
        }
    }
}
