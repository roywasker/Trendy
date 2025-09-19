package com.Trendy.auth

import ContentWithMessageBar
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.Trendy.auth.component.GoogleButton
import com.mmk.kmpauth.firebase.google.GoogleButtonUiContainerFirebase
import com.trendy.shared.Alpha
import com.trendy.shared.BebasNeueFont
import com.trendy.shared.FontSize
import com.trendy.shared.Resources
import com.trendy.shared.Surface
import com.trendy.shared.SurfaceError
import com.trendy.shared.TextPrimary
import com.trendy.shared.TextSecondary
import com.trendy.shared.TextWhite
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import org.koin.viewmodel.factory.KoinViewModelFactory
import rememberMessageBarState

@Composable

fun AuthScreen(
    navigateHome: () -> Unit
){

    val coroutineScope = rememberCoroutineScope()
    val viewModel = koinViewModel<AuthViewModel>()
    val messageBarState = rememberMessageBarState()
    var loadingState by remember { mutableStateOf(false) }

    Scaffold { padding ->
        ContentWithMessageBar(
            contentBackgroundColor = Surface,
            modifier = Modifier
                .padding(top = padding.calculateTopPadding(),
                    bottom = padding.calculateBottomPadding()),
            messageBarState = messageBarState,
            errorMaxLines = 2,
            errorContainerColor = SurfaceError ,
            errorContentColor = TextWhite
        ){
            Column(
                modifier = Modifier.fillMaxSize()
                    .padding(all = 24.dp)
            ) {

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = Resources.Auth.appName,
                        textAlign = TextAlign.Center,
                        fontFamily = BebasNeueFont(),
                        fontSize = FontSize.EXTRA_LARGE,
                        color = TextSecondary
                    )

                    Text(
                        modifier = Modifier.fillMaxWidth()
                            .alpha(Alpha.HALF),
                        text = Resources.Auth.sighInText,
                        textAlign = TextAlign.Center,
                        fontSize = FontSize.EXTRA_REGULAR,
                        color = TextPrimary
                    )
                }
                GoogleButtonUiContainerFirebase(
                    linkAccount = false,
                    onResult = { result ->
                        result.onSuccess { user ->
                            viewModel.createCustomer(
                                user = user,
                                onSuccess = {
                                    coroutineScope.launch {
                                        delay(1000)
                                        navigateHome()
                                    }
                                },
                                onError = { message ->
                                    messageBarState.addError(message)
                                }
                            )
                            loadingState = false
                        }
                            .onFailure { error ->
                                if (error.message?.contains("A network error") == true) {
                                    messageBarState.addError("Internet connection unavailable.")
                                } else if (error.message?.contains("Idtoken is null") == true) {
                                    messageBarState.addError("Sign in canceled.")
                                } else {
                                    messageBarState.addError(error.message ?: "Unknown")
                                }
                                loadingState = false
                            }
                    }
                ) {
                    GoogleButton(
                        loading = loadingState,
                        onClick = {
                            loadingState = true
                            this@GoogleButtonUiContainerFirebase.onClick()
                        }
                    )
                }
            }
        }
    }
}