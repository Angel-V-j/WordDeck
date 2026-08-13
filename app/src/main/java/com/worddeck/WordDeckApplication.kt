package com.worddeck

import android.app.Application
import com.google.firebase.auth.FirebaseAuth
import com.worddeck.core.AppContainer

class WordDeckApplication : Application() {
    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer.create(this)

        if (BuildConfig.DEBUG) {
            // 10.0.2.2 lets the Android emulator reach localhost on the development computer.
            FirebaseAuth.getInstance().useEmulator("10.0.2.2", 9099)
        }
    }
}
