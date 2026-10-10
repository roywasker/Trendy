package org.roy.ecommerce_project.testing

import androidx.compose.ui.test.*
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.trendy.shared.domain.ProductCategory
import com.trendy.shared.navigation.Screen
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.roy.ecommerce_project.testing.fixtures.*

@RunWith(AndroidJUnit4::class)
class AdminE2ETest : AppUiTestBase() {
    private fun admin() {
        env.store.emitCustomer(TestFixtures.customer(isAdmin = true)); launch(); waitText("Discounted Products")
        openDrawer(); clickText("Admin Panel"); waitText("Nova Phone")
    }
    private fun edit() { admin(); clickText("Nova Phone"); waitText("Edit Product") }
    private fun newProduct() { admin(); clickDescription("Add icon"); waitText("New Product") }
    @Test fun regularCustomer_doesNotSeeAdminEntry() {
        launch(); waitText("Discounted Products"); openDrawer(); ui.onNodeWithText("Admin Panel").assertDoesNotExist()
    }
    @Test fun adminCustomer_canOpenManagementAndReturn() { admin(); clickDescription("BackArrow icon"); waitText("Discounted Products") }
    @Test fun adminSearch_filtersByTitleAndCanClear() {
        admin(); clickDescription("Search icon"); ui.onNode(hasSetTextAction()).performTextInput("Orbit")
        waitText("Orbit Watch"); waitAbsent("Nova Phone"); ui.onNodeWithText("Nova Phone").assertDoesNotExist()
        clickDescription("Close icon"); waitText("Nova Phone")
    }
    @Test fun adminSearch_noMatches_displaysEmptyState() {
        admin(); clickDescription("Search icon"); ui.onNode(hasSetTextAction()).performTextInput("zzzz"); waitText("Products not found.")
    }
    @Test fun adminReadFailure_displaysBackendError() {
        env.store.emitProductReadState(ProductRead.LastTen, com.trendy.shared.util.RequestState.Error("Admin catalogue unavailable"))
        launch(Screen.AdminPanel); waitText("Admin catalogue unavailable")
    }
    @Test fun newProduct_startsWithDisabledSubmit() { newProduct(); ui.onNodeWithText("Add new product").assertIsNotEnabled() }
    @Test fun newProduct_photoCancellationCanRetry() {
        env.picker.selection = null; newProduct(); clickDescription("Add icon"); waitText("File is null"); clickText("Try Again")
        ui.onNodeWithContentDescription("Add icon").assertExists()
    }
    @Test fun newProduct_uploadFailureCanRetry() {
        env.store.failNext(TestOperation.UploadImage, "Storage unavailable"); newProduct(); clickDescription("Add icon")
        waitText("Storage unavailable"); clickText("Try Again"); clickDescription("Add icon"); waitText("Image uploaded successfully")
    }
    @Test fun newProductJourney_uploadAndCreatePersistsAllFields() {
        newProduct(); clickDescription("Add icon"); waitText("Image uploaded successfully")
        replaceField("Title", "Test Camera"); replaceField("Description", "A test camera for the catalogue"); replaceField("Price", "129.5")
        clickText("Add new product"); waitText("Product added successfully")
        val product = env.store.productsState.value.getSuccessData().single { it.title == "Test Camera" }
        assertEquals(129.5, product.price, 0.001); assertTrue(product.thumbnail.isNotBlank())
    }
    @Test fun newProduct_negativePriceDisablesSubmit() {
        newProduct(); clickDescription("Add icon"); waitText("Image uploaded successfully")
        replaceField("Title", "Test Camera"); replaceField("Description", "Test camera description"); replaceField("Price", "-1")
        ui.onNodeWithText("Add new product").assertIsNotEnabled()
    }
    @Test fun editProduct_loadsStoredValues() { edit(); field("Title").assertTextContains("Nova Phone"); field("Price").performScrollTo().assertTextContains("100.0") }
    @Test fun editProduct_updatesTitleAndPrice() {
        edit(); replaceField("Title", "Nova Pro"); replaceField("Price", "120.0"); clickText("Update"); waitText("Product updated successfully")
        val product = env.store.productsState.value.getSuccessData().single { it.id == TestFixtures.GADGET_NEW_ID }
        assertEquals("Nova Pro", product.title); assertEquals(120.0, product.price, 0.001)
    }
    @Test fun editProductFailure_preservesStoredValues() {
        env.store.failNext(TestOperation.UpdateProduct, "Update unavailable"); edit(); replaceField("Title", "Nova Pro")
        clickText("Update"); waitText("Update unavailable")
        assertEquals("Nova Phone", env.store.productsState.value.getSuccessData().single { it.id == TestFixtures.GADGET_NEW_ID }.title)
    }
    @Test fun selectDiyCategory_hidesWeightField() {
        edit(); ui.onNodeWithText("Gadgets").performScrollTo().performClick(); clickText("DIY"); clickText("Confirm")
        ui.onNodeWithTag("field_weight").assertDoesNotExist(); clickText("Update"); waitText("Product updated successfully")
        assertEquals(ProductCategory.DIY.name, env.store.productsState.value.getSuccessData().single { it.id == TestFixtures.GADGET_NEW_ID }.category)
    }
    @Test fun categoryDialogCancel_keepsCategory() {
        edit(); ui.onNodeWithText("Gadgets").performScrollTo().performClick(); clickText("DIY"); clickText("Cancel")
        ui.onNodeWithText("Gadgets").assertExists()
    }
    @Test fun merchandisingSwitches_persistNewPopularAndDiscounted() {
        edit()
        val switches = ui.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.ToggleableState))
        switches[0].performScrollTo().performClick()
        switches[1].performScrollTo().performClick()
        switches[2].performScrollTo().performClick()
        clickText("Update"); waitText("Product updated successfully")
        val product = env.store.productsState.value.getSuccessData().single { it.id == TestFixtures.GADGET_NEW_ID }
        assertFalse(product.isNew); assertFalse(product.isPopular); assertTrue(product.isDiscounted)
    }
    @Test fun deleteThumbnail_disablesSubmitAndRemovesImage() {
        edit(); clickDescription("Delete icon"); waitText("Thumbnail removed successfully.")
        ui.onNodeWithText("Update").assertIsNotEnabled()
        assertEquals("", env.store.productsState.value.getSuccessData().single { it.id == TestFixtures.GADGET_NEW_ID }.thumbnail)
    }
    @Test fun deleteThumbnailFailure_preservesImage() {
        env.store.failNext(TestOperation.DeleteImage, "Cannot remove image"); edit(); clickDescription("Delete icon"); waitText("Cannot remove image")
        assertTrue(env.store.productsState.value.getSuccessData().single { it.id == TestFixtures.GADGET_NEW_ID }.thumbnail.isNotBlank())
    }
    @Test fun deleteProduct_removesItemAndReturnsToAdmin() {
        edit(); clickDescription("VerticalMenu icon"); clickText("Delete"); waitText("Admin Panel")
        assertFalse(env.store.productsState.value.getSuccessData().any { it.id == TestFixtures.GADGET_NEW_ID })
    }
    @Test fun deleteProductFailure_keepsProductAndScreen() {
        env.store.failNext(TestOperation.DeleteProduct, "Cannot delete product"); edit(); clickDescription("VerticalMenu icon"); clickText("Delete")
        waitText("Cannot delete product"); waitText("Edit Product")
        assertTrue(env.store.productsState.value.getSuccessData().any { it.id == TestFixtures.GADGET_NEW_ID })
    }
}
