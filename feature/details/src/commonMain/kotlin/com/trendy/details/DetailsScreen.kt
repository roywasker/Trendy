package com.trendy.details

import ContentWithMessageBar
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import rememberMessageBarState
import androidx.compose.runtime.getValue
import androidx.compose.ui.text.style.TextOverflow
import com.trendy.shared.BebasNeueFont
import com.trendy.shared.BorderIdle
import com.trendy.shared.FontSize
import com.trendy.shared.IconPrimary
import com.trendy.shared.Resources
import com.trendy.shared.RobotoCondensedFont
import com.trendy.shared.Surface
import com.trendy.shared.SurfaceBrand
import com.trendy.shared.SurfaceError
import com.trendy.shared.SurfaceLighter
import com.trendy.shared.TextPrimary
import com.trendy.shared.TextSecondary
import com.trendy.shared.TextWhite
import com.trendy.shared.component.InfoCard
import com.trendy.shared.component.LoadingCard
import com.trendy.shared.component.PrimaryButton
import com.trendy.shared.component.QuantityCounter
import com.trendy.shared.domain.ProductCategory
import com.trendy.shared.domain.QuantityCounterSize
import com.trendy.shared.util.DisplayResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsScreen(navigateBack: () -> Unit) {
    val messageBarState = rememberMessageBarState()
    val viewModel = koinViewModel<DetailsViewModel>()
    val product by viewModel.product.collectAsState()
    val quantity = viewModel.quantity

    Scaffold(
        containerColor = Surface,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Details",
                        fontFamily = BebasNeueFont(),
                        fontSize = FontSize.LARGE,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = navigateBack) {
                        Icon(
                            painter = painterResource(Resources.Icons.BackArrow),
                            contentDescription = "Back Arrow icon",
                            tint = IconPrimary
                        )
                    }
                },
                actions = {
                    QuantityCounter(
                        size = QuantityCounterSize.Large,
                        value = quantity,
                        onMinusClick = viewModel::updateQuantity,
                        onPlusClick = viewModel::updateQuantity
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Surface,
                    scrolledContainerColor = Surface,
                    navigationIconContentColor = IconPrimary,
                    titleContentColor = TextPrimary,
                    actionIconContentColor = IconPrimary
                )
            )
        }
    ) { padding ->
        product.DisplayResult(
            onLoading = { LoadingCard(modifier = Modifier.fillMaxSize()) },
            onSuccess = { selectedProduct ->
                ContentWithMessageBar(
                    contentBackgroundColor = Surface,
                    modifier = Modifier
                        .padding(
                            top = padding.calculateTopPadding(),
                            bottom = padding.calculateBottomPadding()
                        ),
                    messageBarState = messageBarState,
                    errorMaxLines = 2,
                    errorContainerColor = SurfaceError,
                    errorContentColor = TextWhite,
                    successContainerColor = SurfaceBrand,
                    successContentColor = TextPrimary
                ) {
                    Column {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 24.dp)
                                .padding(top = 12.dp)
                        ) {
                            AsyncImage(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(300.dp)
                                    .clip(RoundedCornerShape(size = 12.dp))
                                    .border(
                                        width = 1.dp,
                                        color = BorderIdle,
                                        shape = RoundedCornerShape(size = 12.dp)
                                    ),
                                model = ImageRequest.Builder(LocalPlatformContext.current)
                                    .data(selectedProduct.thumbnail)
                                    .crossfade(enable = true)
                                    .build(),
                                contentDescription = "Product thumbnail image",
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                AnimatedContent(
                                    targetState = selectedProduct.category
                                ) { category ->
                                    if (ProductCategory.valueOf(category) == ProductCategory.DIY) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    } else {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                modifier = Modifier.size(14.dp),
                                                painter = painterResource(Resources.Icons.Weight),
                                                contentDescription = "Weight icon"
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "${selectedProduct.weight}g",
                                                fontSize = FontSize.REGULAR,
                                                color = TextPrimary
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = "$${selectedProduct.price}",
                                    fontSize = FontSize.MEDIUM,
                                    color = TextSecondary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = selectedProduct.title,
                                fontSize = FontSize.EXTRA_MEDIUM,
                                fontWeight = FontWeight.Medium,
                                fontFamily = RobotoCondensedFont(),
                                color = TextPrimary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = selectedProduct.description,
                                fontSize = FontSize.REGULAR,
                                lineHeight = FontSize.REGULAR * 1.3,
                                color = TextPrimary
                            )
                        }
                        Column(
                            modifier = Modifier
                                .background( SurfaceLighter)
                                .padding(all = 24.dp)
                        ) {
//                            if (selectedProduct.flavors?.isNotEmpty() == true) {
//                                FlowRow(
//                                    modifier = Modifier.fillMaxWidth(),
//                                    verticalArrangement = Arrangement.spacedBy(8.dp),
//                                    horizontalArrangement = Arrangement.Center
//                                ) {
//                                    selectedProduct.flavors?.forEach { flavor ->
//                                        FlavorChip(
//                                            flavor = flavor,
//                                            isSelected = selectedFlavor == flavor,
//                                            onClick = { viewModel.updateFlavor(flavor) }
//                                        )
//                                        Spacer(modifier = Modifier.width(8.dp))
//                                    }
//                                }
//                                Spacer(modifier = Modifier.height(24.dp))
//                            }
                            PrimaryButton(
                                icon = Resources.Icons.ShoppingCart,
                                text = "Add to Cart",
                                onClick = {
                                    viewModel.addItemToCart(
                                        onSuccess = { messageBarState.addSuccess("Product added to cart.") },
                                        onError = { message -> messageBarState.addError(message) }
                                    )
                                }
                            )
                        }
                    }
                }
            },
            onError = { message ->
                InfoCard(
                    image = Resources.Images.Cat,
                    title = "Oops!",
                    subTitle = message
                )
            }
        )
    }
}