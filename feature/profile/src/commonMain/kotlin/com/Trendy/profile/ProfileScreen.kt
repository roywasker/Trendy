package com.Trendy.profile

import ContentWithMessageBar
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.trendy.shared.BebasNeueFont
import com.trendy.shared.FontSize
import com.trendy.shared.IconPrimary
import com.trendy.shared.Resources
import com.trendy.shared.Surface
import com.trendy.shared.TextPrimary
import com.trendy.shared.component.InfoCard
import com.trendy.shared.component.LoadingCard
import com.trendy.shared.component.PrimaryButton
import com.trendy.shared.component.ProfileForm
import com.trendy.shared.util.DisplayResult
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import rememberMessageBarState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navigateBack: () -> Unit
){

    val viewModel = koinViewModel<ProfileViewModel>()
    val screenState = viewModel.screenState
    val screenReady = viewModel.screenReady
    val isFormValid = viewModel.isFormValid
    val messageBarState = rememberMessageBarState()

    Scaffold(
        containerColor = Surface,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "My Profile",
                        fontFamily = BebasNeueFont(),
                        fontSize = FontSize.LARGE,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navigateBack() }) {
                        Icon(
                            painter = painterResource(Resources.Icons.BackArrow),
                            contentDescription = "BackArrow icon",
                            tint = IconPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Surface,
                    scrolledContainerColor = Surface,
                    navigationIconContentColor = IconPrimary,
                    titleContentColor = TextPrimary,
                    actionIconContentColor = IconPrimary
                )
            )
        }
    ){ padding ->
        ContentWithMessageBar(
            modifier = Modifier
                .padding(
                    top = padding.calculateTopPadding(),
                    bottom = padding.calculateBottomPadding()
                ),
            messageBarState = messageBarState,
            errorMaxLines = 2,
            contentBackgroundColor = Surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
                    .padding(
                        top = 12.dp,
                        bottom = 24.dp
                    )
                    .imePadding()
                ) {
                screenReady.DisplayResult(
                    onLoading = { LoadingCard(modifier = Modifier.fillMaxSize()) },
                    onSuccess = {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                        ) {
                            ProfileForm(
                                modifier = Modifier.weight(1f),
                                country = screenState.country,
                                onCountrySelect = viewModel::updateCountry,
                                firstname = screenState.firstName,
                                onFirstNameChange = viewModel::updateFirstName,
                                lastname = screenState.lastName,
                                onLastNameChange = viewModel::updateLastName,
                                email = screenState.email,
                                city = screenState.city,
                                onCityChange = viewModel::updateCity,
                                postalCode = screenState.postalCode,
                                onPostalCodeChange = viewModel::updatePostalCode,
                                phoneNumber = screenState.phoneNumber?.number,
                                onPhoneNumberChange = viewModel::updatePhoneNumber,
                                address = screenState.address,
                                onAddressChange = viewModel::updateAddress
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            PrimaryButton(
                                text = "Update",
                                icon = Resources.Icons.Checkmark,
                                enable = isFormValid,
                                onClick = {
                                    viewModel.updateCustomer(
                                        onSuccess = {
                                            messageBarState.addSuccess("Profile updated successfully")
                                        },
                                        onError = { message ->
                                            messageBarState.addError(message)
                                        }
                                    )
                                }
                            )
                        }
                    },
                    onError = { message ->
                        InfoCard(
                            image = Resources.Images.Cat,
                            title = "Oops!",
                            subTitle = message
                        )
                    }
                )
            }
        }
    }
}