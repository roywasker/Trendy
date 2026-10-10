package org.roy.ecommerce_project.testing

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import android.os.Bundle
import android.os.Build
import android.view.WindowManager

/** Empty debug-only host. Instrumented tests install the real app UI with isolated dependencies. */
class E2ETestActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (Build.VERSION.SDK_INT >= 27) { setTurnScreenOn(true); setShowWhenLocked(true) }
        renderer?.let { setContent(content = it) }
    }
    companion object { var renderer: (@Composable () -> Unit)? = null }
}
