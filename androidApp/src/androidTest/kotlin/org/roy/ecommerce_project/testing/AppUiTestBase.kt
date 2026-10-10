package org.roy.ecommerce_project.testing

import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.Trendy.auth.LocalGoogleAuthContent
import com.Trendy.navigation.SetupNavGraph
import com.trendy.shared.navigation.Screen
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.roy.ecommerce_project.App
import org.roy.ecommerce_project.testing.fixtures.TestStore

abstract class AppUiTestBase {
    @get:Rule val ui = createAndroidComposeRule<E2ETestActivity>()
    protected lateinit var env: TestEnvironment
    protected var googleFailure: String? = null

    @Before fun resetEnvironment() { env = TestEnvironment(TestStore()); env.start() }
    @After fun closeEnvironment() {
        E2ETestActivity.renderer = null
        ui.runOnUiThread { ui.activity.setContent { } }
        ui.waitForIdle()
        env.close()
    }
    protected fun launch(route: Screen? = null) {
        val render: @androidx.compose.runtime.Composable () -> Unit = {
            MaterialTheme {
                CompositionLocalProvider(LocalGoogleAuthContent provides { result, content ->
                    content {
                        if (googleFailure != null) result(Result.failure(IllegalStateException(googleFailure)))
                        else { env.store.currentUserId = env.store.customer().id; result(Result.success(null)) }
                    }
                }) {
                    if (route == null) App(initializeGoogleAuth = {}) else SetupNavGraph(route)
                }
            }
        }
        E2ETestActivity.renderer = render
        ui.setContent(render)
    }
    protected fun waitText(text: String) {
        ui.waitUntil(8_000) {
            try {
                val nodes = ui.onAllNodesWithText(text, substring = true)
                nodes.fetchSemanticsNodes().indices.any { nodes[it].isDisplayed() }
            } catch (e: IllegalStateException) {
                if (e.message?.contains("No compose hierarchies") == true) false else throw e
            }
        }
        ui.onAllNodesWithText(text, substring = true).onFirst().assertIsDisplayed()
    }
    protected fun waitAbsent(text: String) {
        ui.waitUntil(8_000) { ui.onAllNodesWithText(text).fetchSemanticsNodes().isEmpty() }
    }
    protected fun clickText(text: String) { waitText(text); ui.onAllNodesWithText(text).onFirst().performClick() }
    protected fun clickDescription(description: String) = ui.onNodeWithContentDescription(description).performClick()
    protected fun field(name: String): SemanticsNodeInteraction = ui.onNodeWithTag("field_${name.lowercase().replace(' ', '_')}")
    protected fun replaceField(name: String, value: String) { field(name).performScrollTo().performTextReplacement(value) }
    protected fun openDrawer() { clickDescription("Menu icon"); waitText("Profile") }
    protected fun cart() { clickDescription("Navigate to Cart"); waitText("Cart") }
    protected fun categories() { clickDescription("Navigate to Categories"); waitText("Categories") }
}
