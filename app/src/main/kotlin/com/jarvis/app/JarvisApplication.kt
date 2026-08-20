package com.jarvis.app

import android.app.Application

class JarvisApplication : Application() {

    lateinit var container: JarvisAppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = JarvisAppContainer(this)
    }
}
