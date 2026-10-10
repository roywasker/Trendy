package com.trendy.shared.domain

import com.trendy.shared.Resources
import org.jetbrains.compose.resources.DrawableResource

enum class Country(
    val dialCode: Int,
    val code: String,
    val flag: DrawableResource
) {
    Israel(
        dialCode = 972,
        code = "IL",
        flag = Resources.Flag.Israel
    ),
    Usa(
        dialCode = 1,
        code = "US",
        flag = Resources.Flag.Usa
    ),
    India(
        dialCode = 91,
        code = "IN",
        flag = Resources.Flag.India
    ),
    Serbia(
        dialCode = 381,
        code = "RS",
        flag = Resources.Flag.Serbia
    )
}
