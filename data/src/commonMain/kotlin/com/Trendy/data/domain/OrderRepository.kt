package com.Trendy.data.domain

import com.trendy.shared.domain.Order

interface OrderRepository {

    fun getCurrentUserId(): String?

    suspend fun createTheOrder(
        order: Order,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    )
}