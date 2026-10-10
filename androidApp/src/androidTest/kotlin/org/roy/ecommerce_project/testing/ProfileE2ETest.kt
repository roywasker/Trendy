package org.roy.ecommerce_project.testing

import androidx.compose.ui.test.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.roy.ecommerce_project.testing.fixtures.TestOperation

@RunWith(AndroidJUnit4::class)
class ProfileE2ETest : AppUiTestBase() {
    private fun profile() { launch(); waitText("Discounted Products"); openDrawer(); clickText("Profile"); waitText("My Profile") }
    @Test fun profile_loadsCustomerFields() {
        profile(); field("First Name").assertTextContains("Ada"); field("Last Name").assertTextContains("Lovelace")
    }
    @Test fun profile_emailCannotBeEdited() { profile(); field("Email").assertIsNotEnabled().assertTextContains("ada@example.test") }
    @Test fun profileUpdate_persistsNameAndCity() {
        profile(); replaceField("First Name", "Grace"); replaceField("City", "Haifa"); clickText("Update")
        waitText("Profile updated successfully"); assertEquals("Grace", env.store.customer().firstName); assertEquals("Haifa", env.store.customer().city)
    }
    @Test fun profileUpdateFailure_preservesStoredProfile() {
        env.store.failNext(TestOperation.UpdateCustomer, "Profile save failed"); profile(); replaceField("City", "Haifa")
        clickText("Update"); waitText("Profile save failed"); assertEquals("Tel Aviv", env.store.customer().city)
    }
    @Test fun shortFirstName_disablesUpdate() { profile(); replaceField("First Name", "Al"); ui.onNodeWithText("Update").assertIsNotEnabled() }
    @Test fun blankLastName_disablesUpdate() { profile(); replaceField("Last Name", ""); ui.onNodeWithText("Update").assertIsNotEnabled() }
    @Test fun blankCity_disablesUpdate() { profile(); replaceField("City", ""); ui.onNodeWithText("Update").assertIsNotEnabled() }
    @Test fun invalidPostalCode_disablesUpdate() { profile(); replaceField("Postal Code", "12"); ui.onNodeWithText("Update").assertIsNotEnabled() }
    @Test fun blankAddress_disablesUpdateEvenWithValidPostalCode() { profile(); replaceField("Address", ""); ui.onNodeWithText("Update").assertIsNotEnabled() }
    @Test fun shortPhone_disablesUpdateEvenWithValidPostalCode() { profile(); replaceField("Phone Number", "1234"); ui.onNodeWithText("Update").assertIsNotEnabled() }
    @Test fun countrySelection_updatesStoredDialCode() {
        profile(); clickText("+972"); waitText("Select a Country"); clickText("+1 (US)"); clickText("Confirm")
        replaceField("State / Province", "MA")
        clickText("Update"); waitText("Profile updated successfully"); assertEquals(1, env.store.customer().phoneNumber?.dialCode)
    }
    @Test fun countrySelectionCancel_keepsOriginalDialCode() {
        profile(); clickText("+972"); clickText("+1 (US)"); clickText("Cancel")
        ui.onNodeWithText("+972").assertExists(); assertEquals(972, env.store.customer().phoneNumber?.dialCode)
    }
    @Test fun countrySearch_unknownCode_displaysErrorAndCanClear() {
        profile(); clickText("+972"); field("Dial Code").performTextInput("9999"); waitText("Dial code not found")
        field("Dial Code").performTextClearance(); waitText("+972 (IL)"); clickText("Cancel")
    }
    @Test fun profileBack_returnsToCatalogue() { profile(); clickDescription("BackArrow icon"); waitText("Discounted Products") }
    @Test fun profileReadError_displaysErrorInsteadOfForm() {
        env.store.emitCustomerError("Cannot load profile"); launch(com.trendy.shared.navigation.Screen.Profile); waitText("Cannot load profile")
        ui.onNodeWithTag("field_first_name").assertDoesNotExist()
    }
}
