package com.Trendy.cart

import ContentWithMessageBar
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import org.koin.compose.viewmodel.koinViewModel
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.Trendy.cart.component.CartItemCard
import com.trendy.shared.Resources
import com.trendy.shared.Surface
import com.trendy.shared.SurfaceBrand
import com.trendy.shared.SurfaceError
import com.trendy.shared.TextPrimary
import com.trendy.shared.TextWhite
import com.trendy.shared.component.InfoCard
import com.trendy.shared.component.LoadingCard
import com.trendy.shared.util.DisplayResult
import com.trendy.shared.util.RequestState
import rememberMessageBarState

@Composable
fun CartScreen() {
    val messageBarState = rememberMessageBarState()
    val viewModel = koinViewModel<CartViewModel>()
    val cartItemsWithProducts by viewModel.cartItemsWithProducts.collectAsState(RequestState.Loading)

    ContentWithMessageBar(
        contentBackgroundColor = Surface,
        messageBarState = messageBarState,
        errorMaxLines = 2,
        errorContainerColor = SurfaceError,
        errorContentColor = TextWhite,
        successContainerColor = SurfaceBrand,
        successContentColor = TextPrimary
    ) {
        cartItemsWithProducts.DisplayResult(
            onLoading = { LoadingCard(modifier = Modifier.fillMaxSize()) },
            onSuccess = { data ->
                if (data.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = data,
                            key = { it.first.id }
                        ) { pair ->
                            CartItemCard(
                                cartItem = pair.first,
                                product = pair.second,
                                onMinusClick = { quantity ->
                                    viewModel.updateCartItemQuantity(
                                        id = pair.first.id,
                                        quantity = quantity,
                                        onSuccess = {},
                                        onError = { messageBarState.addError(it) }
                                    )
                                },
                                onPlusClick = { quantity ->
                                    viewModel.updateCartItemQuantity(
                                        id = pair.first.id,
                                        quantity = quantity,
                                        onSuccess = {},
                                        onError = { messageBarState.addError(it) }
                                    )
                                },
                                onDeleteClick = {
                                    viewModel.deleteCartItem(
                                        id = pair.first.id,
                                        onSuccess = {},
                                        onError = { messageBarState.addError(it) }
                                    )
                                }
                            )
                        }
                    }
                } else {
                    InfoCard(
                        image = Resources.Images.ShoppingCart,
                        title = "Empty Cart",
                        subTitle = "Check some of our products."
                    )
                }
            },
            onError = { message ->
                InfoCard(
                    image = Resources.Images.Cat,
                    title = "Oops!",
                    subTitle = message
                )
            },
            transitionSpec = fadeIn() togetherWith fadeOut()
        )
    }
}