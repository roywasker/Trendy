package com.Trendy.di

import com.Trendy.auth.AuthViewModel
import com.Trendy.data.AdminRepositoryImpl
import com.Trendy.data.domain.AdminRepository
import com.Trendy.home.HomeGraphViewModel
import com.Trendy.products_overview.ProductsOverviewViewModel
import com.Trendy.category_search.CategorySearchViewModel
import com.Trendy.cart.CartViewModel
import com.Trendy.checkout.CheckoutViewModel
import com.Trendy.profile.ProfileViewModel
import com.Trendy.manage_product.ManageProductViewModel
import com.Trendy.admin_panel.AdminPanelViewModel
import com.Trendy.checkout.domain.PaypalApi
import com.Trendy.data.OrderRepositoryImpl
import com.trendy.details.DetailsViewModel
import com.Trendy.data.domain.CustomerRepository
import com.Trendy.data.domain.CustomerRepositoryImpl
import com.Trendy.data.domain.ProductRepository
import com.Trendy.data.ProductRepositoryImpl
import com.Trendy.data.domain.OrderRepository
import com.Trendy.payment_completed.PaymentViewModel
import com.trendy.shared.util.IntentHandler
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val sharedModule = module {
    single<CustomerRepository> { CustomerRepositoryImpl() }
    single<AdminRepository> { AdminRepositoryImpl() }
    single<ProductRepository> { ProductRepositoryImpl() }
    single<OrderRepository> { OrderRepositoryImpl(get()) }
    single<PaypalApi> { PaypalApi() }
    single<IntentHandler> { IntentHandler() }
    viewModelOf(::AuthViewModel)
    viewModelOf(::HomeGraphViewModel)
    viewModelOf(::ProfileViewModel)
    viewModelOf(::DetailsViewModel)
    viewModelOf(::CartViewModel)
    viewModelOf(::ManageProductViewModel)
    viewModelOf(::AdminPanelViewModel)
    viewModelOf(::ProductsOverviewViewModel)
    viewModelOf(::CategorySearchViewModel)
    viewModelOf(::CheckoutViewModel)
    viewModelOf(::PaymentViewModel)
}

expect val targetModule: Module

fun initializeKoin(
    config: (KoinApplication.() -> Unit)? = null
){
    startKoin {
        config?.invoke(this)
        modules(sharedModule, targetModule)
    }
}