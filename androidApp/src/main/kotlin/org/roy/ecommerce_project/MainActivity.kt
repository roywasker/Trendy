package org.roy.ecommerce_project

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.mmk.kmpnotifier.notification.NotifierManager
import com.mmk.kmpnotifier.notification.configuration.NotificationPlatformConfiguration
import com.mmk.kmpnotifier.permission.permissionUtil
import com.trendy.shared.util.PreferencesRepository
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    private val paymentPreferences: PreferencesRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                Color.TRANSPARENT,
                Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.light(
                Color.TRANSPARENT,
                Color.TRANSPARENT
            )
        )
        super.onCreate(savedInstanceState)
        NotifierManager.initialize(
            configuration = NotificationPlatformConfiguration.Android(
                notificationIconResId = R.drawable.ic_notification,
                showPushNotification = true,
            )
        )

        val permissionUtil by permissionUtil()
        permissionUtil.askNotificationPermission()
        if (savedInstanceState == null) handlePaymentIntent(intent)

        setContent {
            App()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handlePaymentIntent(intent)
    }

    private fun handlePaymentIntent(intent: Intent?) {
        val payment = PayPalReturnIntent.parse(intent?.data) ?: return
        paymentPreferences.savePayPalData(payment.isSuccess, payment.error, payment.token)
    }
}
