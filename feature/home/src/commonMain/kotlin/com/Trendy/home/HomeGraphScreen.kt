package com.Trendy.home

import ContentWithMessageBar
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.Trendy.cart.CartScreen
import com.Trendy.categories.CategoriesScreen
import com.Trendy.category_search.CategorySearchScreen
import com.Trendy.home.component.BottomBar
import com.Trendy.home.component.CustomDrawer
import com.Trendy.home.domain.BottomBarDestination
import com.Trendy.home.domain.CustomDrawerState
import com.Trendy.home.domain.isOpened
import com.Trendy.home.domain.opposite
import com.Trendy.products_overview.ProductsOverviewScreen
import com.trendy.shared.Alpha
import com.trendy.shared.BebasNeueFont
import com.trendy.shared.FontSize
import com.trendy.shared.IconPrimary
import com.trendy.shared.Resources
import com.trendy.shared.Surface
import com.trendy.shared.SurfaceLighter
import com.trendy.shared.TextPrimary
import com.trendy.shared.domain.ProductCategory
import com.trendy.shared.navigation.Screen
import com.trendy.shared.util.RequestState
import com.trendy.shared.util.getScreenWidth
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import rememberMessageBarState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeGraphScreen(
    navigateToAuth: () -> Unit,
    navigateToProfile: () -> Unit,
    navigateToAdminPanel: () -> Unit,
    navigateToCategorySearch: (String) -> Unit,
    navigateToDetails: (String) -> Unit,
    navigateToCheckout: (String) -> Unit,
    ){

    val viewModel = koinViewModel<HomeGraphViewModel>()
    val customer by viewModel.customer.collectAsState()
    val totalAmount by viewModel.totalPrice.collectAsState(RequestState.Loading)
    val messageBarState = rememberMessageBarState()

    val navController = rememberNavController()
    val currentRoute = navController.currentBackStackEntryAsState()
    val selectedDestination by remember {
        derivedStateOf {
            val route = currentRoute.value?.destination?.route.toString()
            when{
                route.contains(BottomBarDestination.ProductsOverView.screen.toString()) -> BottomBarDestination.ProductsOverView
                route.contains(BottomBarDestination.Cart.screen.toString()) -> BottomBarDestination.Cart
                route.contains(BottomBarDestination.Categories.screen.toString()) -> BottomBarDestination.Categories
                else -> BottomBarDestination.ProductsOverView
            }
        }
    }

    val screenWidth = remember { getScreenWidth() }
    var drawerState by remember { mutableStateOf(CustomDrawerState.Closed) }

    val offsetValue = remember { derivedStateOf { (screenWidth/1.5).dp } }
    val animatedOffset = animateDpAsState(
        targetValue = if (drawerState.isOpened()) offsetValue.value else 0.dp
    )

    val animatedScale = animateFloatAsState(
        targetValue = if (drawerState.isOpened()) 0.9f else 1f
    )

    val animatedRadius = animateDpAsState(
        targetValue = if (drawerState.isOpened()) 20.dp else 0.dp
    )

    val animatedBackGround = animateColorAsState(
        targetValue = if (drawerState.isOpened()) SurfaceLighter else Surface
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(animatedBackGround.value)
            .systemBarsPadding()
    ) {
        CustomDrawer(
            customer = customer,
            onProfileClick = { navigateToProfile()},
            onContactUsClick = {},
            onSignOutClick = {
                viewModel.sighOut(
                    onSuccess = {
                        navigateToAuth()
                    },
                    onError = {message ->
                        messageBarState.addError(message)
                    }
                )
            },
            onAdminPanelClick = {
                navigateToAdminPanel()
            }
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(size = animatedRadius.value))
                .offset(x= animatedOffset.value)
                .scale(scale = animatedScale.value)
                .shadow(
                    elevation = 20.dp,
                    shape = RoundedCornerShape(size = animatedRadius.value),
                    ambientColor = Color.Black.copy(alpha = Alpha.DISABLED),
                    spotColor = Color.Black.copy(alpha = Alpha.DISABLED)
                )
        ) {
            Scaffold(
                containerColor = Surface,
                topBar = {
                    CenterAlignedTopAppBar(
                        title = {
                            AnimatedContent(
                                targetState = selectedDestination
                            ) { destination ->
                                Text(
                                    text = destination.title,
                                    fontFamily = BebasNeueFont(),
                                    fontSize = FontSize.LARGE,
                                    color = TextPrimary
                                )
                            }
                        },
                        actions = {
                            AnimatedVisibility(
                                visible = selectedDestination == BottomBarDestination.Cart
                            ){
                                if (customer.isSuccess() && customer.getSuccessData().cart.isNotEmpty()) {
                                    IconButton(onClick = {
                                        if (totalAmount.isSuccess()) {
                                            navigateToCheckout(totalAmount.getSuccessData().toString())
                                        } else if (totalAmount.isError()) {
                                            messageBarState.addError("Error while calculating total amount")
                                        }
                                    }) {
                                        Icon(
                                            painter = painterResource(Resources.Icons.RightArrow),
                                            contentDescription = "RightArrow icon",
                                            tint = IconPrimary
                                        )
                                    }
                                }
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = {
                                drawerState = drawerState.opposite() }) {
                                Icon(
                                    painter = painterResource(if (drawerState.isOpened()) Resources.Icons.Close else Resources.Icons.Menu),
                                    contentDescription = if (drawerState.isOpened()) "Close icon" else "Menu icon",
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
            ) { padding ->
                ContentWithMessageBar(
                    messageBarState = messageBarState,
                    modifier = Modifier
                        .padding(
                            top = padding.calculateTopPadding(),
                            bottom = padding.calculateBottomPadding()
                        ),
                    errorMaxLines = 2,
                    contentBackgroundColor = Surface
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                    ) {
                        NavHost(
                            modifier = Modifier.weight(1f),
                            navController = navController,
                            startDestination = Screen.ProductsOverView
                        ) {
                            composable<Screen.ProductsOverView> {
                                ProductsOverviewScreen(
                                    navigateToDetails = navigateToDetails
                                )
                            }
                            composable<Screen.Cart> {
                                CartScreen()
                            }
                            composable<Screen.Categories> {
                                CategoriesScreen(
                                    navigateToCategorySearch = navigateToCategorySearch
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))

                        Box(
                            modifier = Modifier
                                .padding(all = 12.dp)
                        ) {
                            BottomBar(
                                selected = selectedDestination,
                                onSelect = { destination ->
                                    navController.navigate(destination.screen) {
                                        launchSingleTop = true
                                        popUpTo<Screen.ProductsOverView> {
                                            saveState = true
                                            inclusive = false
                                        }
                                        restoreState = true
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}