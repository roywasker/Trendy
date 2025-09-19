package com.trendy.shared.domain

import androidx.compose.ui.graphics.Color
import com.trendy.shared.CategoryBlue
import com.trendy.shared.CategoryYellow
import com.trendy.shared.CategoryRed
import com.trendy.shared.CategoryPurple
import com.trendy.shared.CategoryGreen
import kotlinx.serialization.Serializable
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
@Serializable
data class Product(
    val id: String,
    val createdAt: Long = Clock.System.now().toEpochMilliseconds(),
    val title: String,
    val description: String,
    val thumbnail: String,
    val category: String,
    val weight: Int? = null,
    val price: Double,
    val isPopular: Boolean = false,
    val isDiscounted: Boolean = false,
    val isNew: Boolean = false
)

enum class ProductCategory (
    val title: String,
    val categoryColor: Color
){
    Gadgets(
        title = "Gadgets",
        categoryColor = CategoryYellow
    ),
    FashionStyle(
        title = "Fashion & Style",
        categoryColor = CategoryBlue
    ),
    HomeDecor(
        title = "Home Decor",
        categoryColor = CategoryGreen
    ),
    Health(
        title = "Health",
        categoryColor = CategoryPurple
    ),
    DIY(
        title = "DIY",
        categoryColor = CategoryRed
    )
}