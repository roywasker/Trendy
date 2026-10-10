package org.roy.ecommerce_project.testing

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner

/** Replaces MyApplication so instrumentation never installs the production repositories. */
class TrendyTestRunner : AndroidJUnitRunner() {
    override fun newApplication(cl: ClassLoader, className: String, context: Context): Application =
        super.newApplication(cl, IsolatedTestApplication::class.java.name, context)
}

class IsolatedTestApplication : Application()
