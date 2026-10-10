package org.roy.ecommerce_project.testing

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.trendy.shared.component.*
import com.trendy.shared.component.dialog.CategoriesDialog
import com.trendy.shared.component.dialog.CountryPickerDialog
import com.trendy.shared.domain.Country
import com.trendy.shared.domain.ProductCategory
import com.trendy.shared.domain.QuantityCounterSize
import com.trendy.shared.util.DisplayResult
import com.trendy.shared.util.RequestState
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SharedComponentsTest : AppUiTestBase() {
    @Test fun quantityIncrement_updatesDisplayedQuantityAndCallback() {
        var value by mutableIntStateOf(1)
        ui.setContent { MaterialTheme { QuantityCounter(size = QuantityCounterSize.Small, value = value, onMinusClick = { value = it }, onPlusClick = { value = it }) } }
        clickDescription("Plus icon"); ui.onNodeWithText("+2").assertExists(); assertEquals(2, value)
    }
    @Test fun quantityDecrement_updatesDisplayedQuantityAndCallback() {
        var value by mutableIntStateOf(3)
        ui.setContent { MaterialTheme { QuantityCounter(size = QuantityCounterSize.Small, value = value, onMinusClick = { value = it }, onPlusClick = { value = it }) } }
        clickDescription("Minus icon"); ui.onNodeWithText("+2").assertExists(); assertEquals(2, value)
    }
    @Test fun minimumQuantity_doesNotInvokeDecrementCallback() {
        var calls = 0
        ui.setContent { MaterialTheme { QuantityCounter(size = QuantityCounterSize.Small, value = 1, onMinusClick = { calls++ }, onPlusClick = {}) } }
        clickDescription("Minus icon"); assertEquals(0, calls)
    }
    @Test fun maximumQuantity_doesNotInvokeIncrementCallback() {
        var calls = 0
        ui.setContent { MaterialTheme { QuantityCounter(size = QuantityCounterSize.Small, value = 10, onMinusClick = {}, onPlusClick = { calls++ }) } }
        clickDescription("Plus icon"); assertEquals(0, calls)
    }
    @Test fun primaryButton_enabledInvokesActionOnce() {
        var calls = 0
        ui.setContent { MaterialTheme { PrimaryButton(text = "Continue", onClick = { calls++ }) } }
        clickText("Continue"); assertEquals(1, calls)
    }
    @Test fun primaryButton_disabledDoesNotInvokeAction() {
        var calls = 0
        ui.setContent { MaterialTheme { PrimaryButton(text = "Continue", enable = false, onClick = { calls++ }) } }
        ui.onNodeWithText("Continue").assertIsNotEnabled().performTouchInput { click() }; assertEquals(0, calls)
    }
    @Test fun textField_editsAndPreservesUserInput() {
        var value by mutableStateOf("")
        ui.setContent { MaterialTheme { CustomTextField(value = value, onValueChange = { value = it }, placeholder = "Example") } }
        field("Example").performTextInput("Hello"); field("Example").assertTextContains("Hello"); assertEquals("Hello", value)
    }
    @Test fun disabledTextField_exposesDisabledSemantics() {
        ui.setContent { MaterialTheme { CustomTextField(value = "readonly", onValueChange = { fail("Read-only input changed") }, placeholder = "Example", enabled = false) } }
        field("Example").assertIsNotEnabled().assertTextContains("readonly")
    }
    @Test fun categoryDialog_confirmsSelectedCategory() {
        var selected: ProductCategory? = null
        ui.setContent { MaterialTheme { CategoriesDialog(ProductCategory.Gadgets, {}, { selected = it }) } }
        clickText("Health"); clickText("Confirm"); assertEquals(ProductCategory.Health, selected)
    }
    @Test fun categoryDialog_cancelDoesNotSubmitSelection() {
        var submitted = false; var dismissed = false
        ui.setContent { MaterialTheme { CategoriesDialog(ProductCategory.Gadgets, { dismissed = true }, { submitted = true }) } }
        clickText("Health"); clickText("Cancel"); assertTrue(dismissed); assertFalse(submitted)
    }
    @Test fun countryDialog_searchAndSelectionUsesSupportedDialCode() {
        var selected: Country? = null
        ui.setContent { MaterialTheme { CountryPickerDialog(Country.Israel, {}, { selected = it }) } }
        field("Dial Code").performTextInput("91"); clickText("+91 (IN)"); clickText("Confirm"); assertEquals(Country.India, selected)
    }
    @Test fun countryDialog_emptySearchResultCanRecover() {
        ui.setContent { MaterialTheme { CountryPickerDialog(Country.Israel, {}, {}) } }
        field("Dial Code").performTextInput("9999"); waitText("Dial code not found")
        field("Dial Code").performTextClearance(); waitText("+972 (IL)")
    }
    @Test fun requestState_loadingThenSuccessReplacesSpinner() {
        var state: RequestState<String> by mutableStateOf(RequestState.Loading)
        ui.setContent { MaterialTheme { state.DisplayResult(onLoading = { LoadingCard() }, onSuccess = { androidx.compose.material3.Text(it) }) } }
        ui.onNodeWithTag("loading").assertExists(); ui.runOnIdle { state = RequestState.Success("Ready") }; waitText("Ready")
        ui.onNodeWithTag("loading").assertDoesNotExist()
    }
    @Test fun requestState_errorDisplaysReadableMessage() {
        ui.setContent { MaterialTheme { RequestState.Error("Offline").DisplayResult(onError = { ErrorCard(message = it) }, onSuccess = {}) } }
        waitText("Offline")
    }
}
