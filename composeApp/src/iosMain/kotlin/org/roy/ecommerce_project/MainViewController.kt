package org.roy.ecommerce_project

import androidx.compose.ui.window.ComposeUIViewController
import com.Trendy.di.initializeKoin

fun MainViewController() = ComposeUIViewController(
    configure = { initializeKoin() }
) { App() }