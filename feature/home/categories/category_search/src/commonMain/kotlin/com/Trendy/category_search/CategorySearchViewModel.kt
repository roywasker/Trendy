package com.Trendy.category_search

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.Trendy.data.domain.ProductRepository
import com.trendy.shared.domain.ProductCategory
import com.trendy.shared.util.RequestState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

class CategorySearchViewModel(
    private val productRepository: ProductRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val products = productRepository.readProductsByCategoryFlow(
        category = ProductCategory.fromStoredName(savedStateHandle.get<String>("category"))
            ?: ProductCategory.Gadgets
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RequestState.Loading
    )

    private var _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    fun updateSearchQuery(value: String) {
        _searchQuery.value = value
    }

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val filteredProducts = combine(products, searchQuery.debounce(500)) { state, query ->
        if (state.isSuccess()) {
            RequestState.Success(state.getSuccessData().filter {
                query.isBlank() || it.title.contains(query, ignoreCase = true)
            })
        } else state
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = RequestState.Loading
        )
}
