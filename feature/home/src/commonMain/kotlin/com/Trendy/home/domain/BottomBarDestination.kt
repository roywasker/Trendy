package com.Trendy.home.domain

import com.trendy.shared.Resources
import com.trendy.shared.navigation.Screen
import org.jetbrains.compose.resources.DrawableResource

enum class BottomBarDestination(
    val icon: DrawableResource,
    val title: String,
    val screen: Screen
) {
    ProductsOverView(
        icon = Resources.Icons.Home,
        title = "Trendy",
        screen = Screen.ProductsOverView
    ),
    Cart(
        icon = Resources.Icons.ShoppingCart,
        title = "Cart",
        screen = Screen.Cart
    ),
    Categories(
        icon = Resources.Icons.Categories,
        title = "Categories",
        screen = Screen.Categories
    )
}