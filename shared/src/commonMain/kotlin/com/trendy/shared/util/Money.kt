package com.trendy.shared.util

import kotlin.math.roundToLong

/** USD is represented as integral cents at every checkout boundary. */
fun Double.toCents(): Long {
    require(isFinite() && this >= 0 && this <= Long.MAX_VALUE / 100.0) { "Invalid monetary amount." }
    return (this * 100).roundToLong()
}
fun Long.toUsdString(): String {
    require(this >= 0) { "Invalid monetary amount." }
    return "${this / 100}.${(this % 100).toString().padStart(2, '0')}"
}
