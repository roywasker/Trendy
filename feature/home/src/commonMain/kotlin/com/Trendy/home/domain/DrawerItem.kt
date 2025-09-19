package com.Trendy.home.domain

import com.trendy.shared.Resources
import org.jetbrains.compose.resources.DrawableResource

enum class DrawerItem(
    val title: String,
    val icon: DrawableResource
) {
    Profile(
        title = "Profile",
        icon = Resources.Icons.Person
    ),
    Contact(
        title = "Contact us",
        icon = Resources.Icons.Edit
    ),
    SignOut(
        title = "SignOut",
        icon = Resources.Icons.SignOut
    ),
    Admin(
        title = "Admin Panel",
        icon = Resources.Icons.Unlock
    )
}