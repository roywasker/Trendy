package com.trendy.shared

import ecommerceproject.shared.generated.resources.Res
import ecommerceproject.shared.generated.resources.back_arrow
import ecommerceproject.shared.generated.resources.book
import ecommerceproject.shared.generated.resources.cat
import ecommerceproject.shared.generated.resources.check
import ecommerceproject.shared.generated.resources.checkmark_image
import ecommerceproject.shared.generated.resources.close
import ecommerceproject.shared.generated.resources.delete
import ecommerceproject.shared.generated.resources.dollar
import ecommerceproject.shared.generated.resources.edit
import ecommerceproject.shared.generated.resources.google_logo
import ecommerceproject.shared.generated.resources.grid
import ecommerceproject.shared.generated.resources.home
import ecommerceproject.shared.generated.resources.india
import ecommerceproject.shared.generated.resources.israel
import ecommerceproject.shared.generated.resources.log_in
import ecommerceproject.shared.generated.resources.log_out
import ecommerceproject.shared.generated.resources.map_pin
import ecommerceproject.shared.generated.resources.menu
import ecommerceproject.shared.generated.resources.minus
import ecommerceproject.shared.generated.resources.paypal_logo
import ecommerceproject.shared.generated.resources.plus
import ecommerceproject.shared.generated.resources.right_arrow
import ecommerceproject.shared.generated.resources.search
import ecommerceproject.shared.generated.resources.serbia
import ecommerceproject.shared.generated.resources.shopping_cart
import ecommerceproject.shared.generated.resources.shopping_cart_image
import ecommerceproject.shared.generated.resources.unlock
import ecommerceproject.shared.generated.resources.usa
import ecommerceproject.shared.generated.resources.user
import ecommerceproject.shared.generated.resources.vertical_menu
import ecommerceproject.shared.generated.resources.warning
import ecommerceproject.shared.generated.resources.weight

object Resources {
    object Icons {
        val Plus = Res.drawable.plus
        val Minus = Res.drawable.minus
        val SignIn = Res.drawable.log_in
        val SignOut = Res.drawable.log_out
        val Unlock = Res.drawable.unlock
        val Search = Res.drawable.search
        val Person = Res.drawable.user
        val Checkmark = Res.drawable.check
        val Edit = Res.drawable.edit
        val Menu = Res.drawable.menu
        val BackArrow = Res.drawable.back_arrow
        val RightArrow = Res.drawable.right_arrow
        val Home = Res.drawable.home
        val ShoppingCart = Res.drawable.shopping_cart
        val Categories = Res.drawable.grid
        val Dollar = Res.drawable.dollar
        val MapPin = Res.drawable.map_pin
        val Close = Res.drawable.close
        val Book = Res.drawable.book
        val VerticalMenu = Res.drawable.vertical_menu
        val Delete = Res.drawable.delete
        val Warning = Res.drawable.warning
        val Weight = Res.drawable.weight
    }
    object Images {
        val ShoppingCart = Res.drawable.shopping_cart_image
        val Checkmark = Res.drawable.checkmark_image
        val Cat = Res.drawable.cat
        val GoogleLogo = Res.drawable.google_logo
        val PaypalLogo = Res.drawable.paypal_logo
    }

    object Auth{
        const val primaryText = "Sigh in with google"
        const val secondaryText = "Please wait..."
        const val appName = "TRENDY"
        const val sighInText = "Sigh in to continue"
    }

    object Flag {
        val Israel = Res.drawable.israel
        val Usa = Res.drawable.usa
        val Serbia = Res.drawable.serbia
        val India = Res.drawable.india
    }
}