package org.roy.ecommerce_project.testing

import org.junit.runner.RunWith
import org.junit.runners.Suite

/** Run this single class in Android Studio to execute the complete isolated Android regression suite. */
@RunWith(Suite::class)
@Suite.SuiteClasses(
    RegressionUiStateTest::class,
    RegressionDataTest::class,
    ViewModelIntegrationTest::class,
    PaymentViewModelIntegrationTest::class,
    PaypalApiIntegrationTest::class,
    PaymentCallbackTest::class,
    AuthenticationE2ETest::class,
    ShoppingE2ETest::class,
    ProfileE2ETest::class,
    AdminE2ETest::class,
    CheckoutE2ETest::class,
    SharedComponentsTest::class,
)
class AllFeaturesTestSuite
