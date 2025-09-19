package com.Trendy.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.Trendy.auth.AuthScreen
import com.Trendy.home.HomeGraphScreen
import com.Trendy.profile.ProfileScreen
import com.Trendy.admin_panel.AdminPanelScreen
import com.Trendy.category_search.CategorySearchScreen
import com.Trendy.checkout.CheckoutScreen
import com.Trendy.manage_product.ManageProductScreen
import com.Trendy.payment_completed.PaymentCompleted
import com.trendy.details.DetailsScreen
import com.trendy.shared.domain.ProductCategory
import com.trendy.shared.navigation.Screen
import com.trendy.shared.util.IntentHandlerHelper
import com.trendy.shared.util.PreferencesRepository
import org.koin.compose.koinInject

@Composable
fun SetupNavGraph(startDestination: Screen = Screen.Auth){
    val navController = rememberNavController()
    val preferencesData by PreferencesRepository.readPayPalDataFlow()
        .collectAsState(initial = null)

    LaunchedEffect(preferencesData) {
        preferencesData?.let { paymentCompleted ->
            if(paymentCompleted.token != null) {
                navController.navigate(paymentCompleted)
                PreferencesRepository.reset()
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ){
        composable<Screen.Auth> {
            AuthScreen(navigateHome = {
                navController.navigate(Screen.HomeGraph){
                    popUpTo<Screen.Auth> { inclusive = true }
                }
            })
        }
        composable<Screen.HomeGraph> {
            HomeGraphScreen(
                navigateToAuth = {
                    navController.navigate(Screen.Auth){
                        popUpTo<Screen.HomeGraph>{ inclusive = true}
                    }
                },
                navigateToProfile = {
                    navController.navigate(Screen.Profile){
                    }
                },
                navigateToAdminPanel = {
                    navController.navigate(Screen.AdminPanel){
                    }
                },
                navigateToDetails = { productId ->
                    navController.navigate(Screen.Details(id = productId))
                },
                navigateToCategorySearch = { category ->
                    navController.navigate(Screen.CategorySearch(category = category))
                },
                navigateToCheckout = {totalAmount ->
                    navController.navigate(Screen.Checkout(totalAmount = totalAmount))
                }
            )
        }
        composable<Screen.Profile> {
            ProfileScreen(
                navigateBack = {
                    navController.navigateUp()
                }
            )
        }
        composable<Screen.AdminPanel> {
            AdminPanelScreen(
                navigateBack = {
                    navController.navigateUp()
                },
                navigateManageProduct = { id ->
                    navController.navigate(Screen.ManageProduct(id))
                }
            )
        }
        composable<Screen.ManageProduct> {
            val id = it.toRoute<Screen.ManageProduct>().id
            ManageProductScreen(
                id = id,
                navigateBack = {
                    navController.navigateUp()
                }
            )
        }
        composable<Screen.Details> {
            DetailsScreen(
                navigateBack = {
                    navController.navigateUp()
                }
            )
        }
        composable<Screen.CategorySearch> {
            val category = ProductCategory.valueOf(it.toRoute<Screen.CategorySearch>().category)
            CategorySearchScreen(
                category = category,
                navigateBack = {
                    navController.navigateUp()
                },
                navigateToDetails = {
                    navController.navigate(Screen.Details(id = it))
                }
            )
        }
        composable<Screen.Checkout> {
            val totalAmount = it.toRoute<Screen.Checkout>().totalAmount
            CheckoutScreen(
                totalAmount = totalAmount.toDoubleOrNull() ?: 0.0,
                navigateBack = {
                    navController.navigateUp()
                },
                navigateToPaymentCompleted = { isSuccess, error ->
                    navController.navigate(Screen.PaymentCompleted(isSuccess, error))
                }
            )
        }
        composable<Screen.PaymentCompleted> {
            PaymentCompleted(
                navigateBack = {
                    navController.navigate(Screen.HomeGraph) {
                        launchSingleTop = true
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}