package org.roy.ecommerce_project

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.Trendy.data.domain.CustomerRepository
import com.trendy.shared.navigation.Screen
import com.Trendy.navigation.SetupNavGraph
import com.mmk.kmpauth.google.GoogleAuthCredentials
import com.mmk.kmpauth.google.GoogleAuthProvider
import com.trendy.shared.Constants
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject

@Composable
@Preview
fun App() {
    MaterialTheme {

        val customerRepository = koinInject<CustomerRepository>()
        val appReady = remember { mutableStateOf(false) }

        val isUserAuthenticated = remember { customerRepository.getCurrentUserId() != null }
        val startDestination = remember {
            if (isUserAuthenticated){
                Screen.HomeGraph
            }else{
                Screen.Auth
            }
        }

        LaunchedEffect(Unit){
            GoogleAuthProvider.create(
                credentials = GoogleAuthCredentials(serverId = Constants.WEB_CLIENT_ID)
            )
            appReady.value = true
        }

        AnimatedVisibility(
            modifier = Modifier.fillMaxSize(),
            visible = appReady.value
        ){
            SetupNavGraph(
                startDestination = startDestination
            )
        }
    }
}