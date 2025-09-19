package com.trendy.shared.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.trendy.shared.component.dialog.CountryPickerDialog
import com.trendy.shared.domain.Country

@Composable
fun ProfileForm(
    modifier: Modifier = Modifier,
    country: Country,
    onCountrySelect: (Country) -> Unit,
    firstname: String,
    onFirstNameChange: (String) -> Unit,
    lastname: String,
    onLastNameChange: (String) -> Unit,
    city: String?,
    onCityChange: (String) -> Unit,
    email: String,
    postalCode: Int?,
    onPostalCodeChange: (Int?) -> Unit,
    address: String?,
    onAddressChange: (String) -> Unit,
    phoneNumber: String?,
    onPhoneNumberChange: (String) -> Unit
){
    var showCountryPicker by remember { mutableStateOf(false) }

    AnimatedVisibility(
        visible = showCountryPicker
    ){
        CountryPickerDialog(
            country = country,
            onDismiss = { showCountryPicker = false },
            onConfirmClick = { selectedCountry ->
                showCountryPicker = false
                onCountrySelect(selectedCountry)
            }
        )
    }

    Column (
       modifier = modifier
           .fillMaxSize()
           .verticalScroll(state = rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CustomTextField(
            value = firstname,
            onValueChange = onFirstNameChange,
            placeholder = "First Name",
            error = firstname.length !in 3..50
        )

        CustomTextField(
            value = lastname,
            onValueChange = onLastNameChange,
            placeholder = "Last Name",
            error = lastname.length !in 3..50
        )

        CustomTextField(
            value = email,
            onValueChange = {},
            placeholder = "Email",
            enabled = false
        )
        CustomTextField(
            value = city?: "",
            onValueChange = onCityChange,
            placeholder = "City",
            error = city?.length !in 3..50
        )

        CustomTextField(
            value = "${postalCode?: ""}",
            onValueChange = { onPostalCodeChange(it.toIntOrNull()) },
            placeholder = "Postal Code",
            error = postalCode == null || postalCode.toString().length !in 3..10,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number
            )
        )

        CustomTextField(
            value = address?: "",
            onValueChange = onAddressChange,
            placeholder = "Address",
            error = address?.length !in 3..10
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ){
           AlertTextField(
               text = "+${country.dialCode}",
               icon = country.flag,
               onClick = {
                   showCountryPicker = true
               }
           )

            Spacer(modifier = Modifier.width(12.dp))

            CustomTextField(
                value = phoneNumber?: "",
                onValueChange = onPhoneNumberChange,
                placeholder = "Phone Number",
                error = phoneNumber == null || phoneNumber.toString().length !in 5..10,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                )
            )
        }
    }
}