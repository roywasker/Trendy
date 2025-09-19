package com.Trendy.admin_panel

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trendy.shared.BebasNeueFont
import com.trendy.shared.BorderIdle
import com.trendy.shared.ButtonPrimary
import com.trendy.shared.FontSize
import com.trendy.shared.IconPrimary
import com.trendy.shared.Resources
import com.trendy.shared.Surface
import com.trendy.shared.SurfaceLighter
import com.trendy.shared.TextPrimary
import com.trendy.shared.component.InfoCard
import com.trendy.shared.component.LoadingCard
import com.trendy.shared.component.ProductCard
import com.trendy.shared.util.DisplayResult
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelScreen(
    navigateBack: () -> Unit,
    navigateManageProduct: (String?) -> Unit
){

    val viewModel = koinViewModel<AdminPanelViewModel>()
    val products = viewModel.filteredProducts.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    var searchBarVisible by mutableStateOf(false)

    Scaffold(
        containerColor = Surface,
        topBar = {
            AnimatedContent(
                targetState = searchBarVisible,
            ){ visible ->
                if (visible) {
                    SearchBar(
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .fillMaxWidth(),
                        inputField = {
                            SearchBarDefaults.InputField(
                                modifier = Modifier.fillMaxWidth(),
                                query = searchQuery,
                                onQueryChange = viewModel::updateSearchQuery,
                                placeholder = {
                                    Text(
                                        text = "Search here",
                                        fontSize = FontSize.REGULAR,
                                        color = TextPrimary
                                    )
                                },
                                trailingIcon = {
                                    IconButton(
                                        modifier = Modifier.size(14.dp),
                                        onClick = {
                                            if (searchQuery.isNotEmpty()){
                                                viewModel.updateSearchQuery("")
                                            }else{
                                                searchBarVisible = false
                                            }
                                        }){
                                        Icon(
                                            painter = painterResource(Resources.Icons.Close),
                                            contentDescription = "Close icon",
                                        )
                                    }
                                },
                                expanded = false,
                                onExpandedChange = {},
                                onSearch = {},
                            )
                        },
                        colors = SearchBarDefaults.colors(
                            containerColor = SurfaceLighter,
                            dividerColor = BorderIdle,
                        ),
                        expanded = false,
                        onExpandedChange = {},
                        content = {},
                    )
                }else{
                    TopAppBar(
                        title = {
                            Text(
                                text = "Admin Panel",
                                fontFamily = BebasNeueFont(),
                                fontSize = FontSize.LARGE,
                                color = TextPrimary
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { navigateBack() }) {
                                Icon(
                                    painter = painterResource(Resources.Icons.BackArrow),
                                    contentDescription = "BackArrow icon",
                                    tint = IconPrimary
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = { searchBarVisible = true }) {
                                Icon(
                                    painter = painterResource(Resources.Icons.Search),
                                    contentDescription = "Search icon",
                                    tint = IconPrimary
                                )
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = Surface,
                            scrolledContainerColor = Surface,
                            navigationIconContentColor = IconPrimary,
                            titleContentColor = TextPrimary,
                            actionIconContentColor = IconPrimary
                        )
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navigateManageProduct(null) },
                containerColor = ButtonPrimary,
                contentColor = IconPrimary,
                content = {
                    Icon(
                        painter = painterResource(Resources.Icons.Plus),
                        contentDescription = "Add icon"
                    )
                }
            )
        }
    ){ padding ->

        products.value.DisplayResult(
            modifier = Modifier
                .padding(
                    top = padding.calculateTopPadding(),
                    bottom = padding.calculateBottomPadding()
                ),
            onLoading = { LoadingCard(modifier = Modifier.fillMaxSize()) },
            onSuccess = { lastProducts ->
                AnimatedContent(
                    targetState = lastProducts
                ) { products ->
                    if (products.isNotEmpty()) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(all = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(
                                items = lastProducts,
                                key = { it.id }
                            ) { product ->
                                ProductCard(
                                    product = product,
                                    onClick = { navigateManageProduct(product.id) }
                                )
                            }
                        }
                    } else {
                        InfoCard(
                            image = Resources.Images.Cat,
                            title = "Oops!",
                            subTitle = "Products not found."
                        )
                    }
                }
            },
            onError = { message ->
                InfoCard(
                    image = Resources.Images.Cat,
                    title = "Oops!",
                    subTitle = message,
                )
            }
        )
    }
}