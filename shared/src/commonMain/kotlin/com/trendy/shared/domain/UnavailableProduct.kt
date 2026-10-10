package com.trendy.shared.domain

fun unavailableProduct(id: String) = Product(id=id, title="Product unavailable", description="This product is no longer available. Remove it from your cart.", thumbnail="", category=ProductCategory.Gadgets.name, price=0.0, isAvailable=false)
