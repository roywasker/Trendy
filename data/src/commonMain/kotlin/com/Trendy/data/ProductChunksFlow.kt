package com.Trendy.data

import com.trendy.shared.domain.Product
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf

/** Subscribes to every chunk concurrently and replaces each snapshot instead of appending it. */
fun productChunksFlow(ids: List<String>, observe: (List<String>) -> Flow<List<Product>>): Flow<List<Product>> {
    val chunks = ids.distinct().chunked(10)
    if (chunks.isEmpty()) return flowOf(emptyList())
    return combine(chunks.map(observe)) { snapshots -> snapshots.flatMap { it }.distinctBy { it.id } }
}
