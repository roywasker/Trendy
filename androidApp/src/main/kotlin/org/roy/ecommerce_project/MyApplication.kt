package org.roy.ecommerce_project

import android.app.Application
import com.Trendy.di.initializeKoin
import com.google.firebase.Firebase
import com.google.firebase.initialize
import org.koin.android.ext.koin.androidContext

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initializeKoin(
            config = {
                androidContext(this@MyApplication)
            }
        )
        Firebase.initialize(context = this)
    }
}