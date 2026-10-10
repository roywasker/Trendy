package com.trendy.shared.domain

import kotlinx.serialization.Serializable

@Serializable
data class ShippingDetails(
    val fullName: String,
    val addressLine1: String,
    val city: String,
    val postalCode: String,
    val countryCode: String,
    val state: String = "",
)
fun validShippingState(country: Country, state: String): Boolean = when (country) {
    Country.Usa -> state.uppercase() in setOf("AL","AK","AZ","AR","CA","CO","CT","DE","DC","FL","GA","HI","ID","IL","IN","IA","KS","KY","LA","ME","MD","MA","MI","MN","MS","MO","MT","NE","NV","NH","NJ","NM","NY","NC","ND","OH","OK","OR","PA","RI","SC","SD","TN","TX","UT","VT","VA","WA","WV","WI","WY","PR","GU","VI","AS","MP")
    Country.India -> state.isNotBlank() && state.length <= 100
    else -> state.length <= 100
}

fun validPostalCode(country: Country, value: String): Boolean = when(country) {
    Country.Usa -> Regex("[0-9]{5}(-[0-9]{4})?").matches(value)
    Country.India -> Regex("[0-9]{6}").matches(value)
    Country.Israel -> Regex("([0-9]{5}|[0-9]{7})").matches(value)
    Country.Serbia -> Regex("[0-9]{5}").matches(value)
}
