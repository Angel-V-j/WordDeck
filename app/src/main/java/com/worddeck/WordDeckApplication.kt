package com.worddeck

import android.app.Application
import com.worddeck.core.AppContainer

class WordDeckApplication : Application() {
    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer.create(this)
    }
}
