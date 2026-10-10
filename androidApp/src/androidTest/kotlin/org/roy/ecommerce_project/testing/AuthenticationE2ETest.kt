package org.roy.ecommerce_project.testing

import androidx.compose.ui.test.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.roy.ecommerce_project.testing.fixtures.TestOperation

@RunWith(AndroidJUnit4::class)
class AuthenticationE2ETest : AppUiTestBase() {
    @Test fun signedOutUser_seesGoogleSignIn() {
        env.store.currentUserId = null; launch()
        waitText("Sign in with Google")
        ui.onNodeWithContentDescription("Navigate to Cart").assertDoesNotExist()
    }
    @Test fun signedInUser_entersCatalogueDirectly() {
        launch(); waitText("Discounted Products")
        ui.onNodeWithText("Sign in with Google").assertDoesNotExist()
    }
    @Test fun googleSuccess_createsCustomerAndNavigatesToCatalogue() {
        env.store.currentUserId = null; launch(); clickText("Sign in with Google")
        waitText("Discounted Products")
        assertTrue(env.store.mutations.contains("createCustomer"))
    }
    @Test fun googleNetworkFailure_showsReadableErrorAndCanRetry() {
        env.store.currentUserId = null; googleFailure = "A network error"; launch(); clickText("Sign in with Google")
        waitText("Internet connection unavailable."); googleFailure = null
        clickText("Sign in with Google"); waitText("Discounted Products")
    }
    @Test fun googleCancellation_staysOnSignInWithoutCreatingCustomer() {
        env.store.currentUserId = null; googleFailure = "Idtoken is null"; launch(); clickText("Sign in with Google")
        waitText("Sign in canceled."); assertFalse(env.store.mutations.contains("createCustomer"))
    }
    @Test fun googleConfigurationFailure_isVisibleAndDoesNotNavigate() {
        env.store.currentUserId = null; googleFailure = "10:"; launch(); clickText("Sign in with Google")
        waitText("10:"); ui.onNodeWithContentDescription("Navigate to Cart").assertDoesNotExist()
    }
    @Test fun customerCreationFailure_staysOnAuthentication() {
        env.store.currentUserId = null; env.store.failNext(TestOperation.CreateCustomer, "Could not save customer")
        launch(); clickText("Sign in with Google"); waitText("Could not save customer")
        ui.onNodeWithContentDescription("Navigate to Cart").assertDoesNotExist()
    }
    @Test fun signOut_returnsToAuthenticationAndClearsSession() {
        launch(); waitText("Discounted Products"); openDrawer(); clickText("SignOut")
        waitText("Sign in with Google"); assertNull(env.store.currentUserId)
    }
    @Test fun signOutFailure_keepsSessionAndDisplaysError() {
        env.store.failNext(TestOperation.SignOut, "Sign out unavailable"); launch(); waitText("Discounted Products")
        openDrawer(); clickText("SignOut"); waitText("Sign out unavailable")
        assertNotNull(env.store.currentUserId)
    }
}
