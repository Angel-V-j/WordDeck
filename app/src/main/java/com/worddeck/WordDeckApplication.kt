package com.worddeck

import android.app.Application
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.worddeck.core.AppContainer
import com.worddeck.data.sync.SyncWorkScheduler

class WordDeckApplication : Application() {
    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            // 10.0.2.2 lets the Android emulator reach localhost on the development computer.
            FirebaseAuth.getInstance().useEmulator("10.0.2.2", 9099)
            FirebaseFirestore.getInstance().useEmulator("10.0.2.2", 8080)
        }

        appContainer = AppContainer.create(this)
        SyncWorkScheduler.enqueue(this)
    }
}
