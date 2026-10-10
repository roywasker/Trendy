package com.Trendy.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import com.mmk.kmpauth.firebase.google.GoogleButtonUiContainerFirebase
import dev.gitlive.firebase.auth.FirebaseUser

/** UI boundary for the external account chooser, keeping app navigation in the real screen. */
typealias GoogleAuthContent = @Composable (
    onResult: (Result<FirebaseUser?>) -> Unit,
    content: @Composable (onClick: () -> Unit) -> Unit,
) -> Unit

val LocalGoogleAuthContent = staticCompositionLocalOf<GoogleAuthContent> {
    { onResult, content ->
        GoogleButtonUiContainerFirebase(linkAccount = false, onResult = onResult) {
            content { onClick() }
        }
    }
}
